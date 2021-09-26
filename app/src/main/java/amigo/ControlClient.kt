package com.app.amigo

import android.content.SharedPreferences
import android.net.Uri
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.app.amigo.fragment.BoolPreference
import com.app.amigo.fragment.IntPreference
import com.app.amigo.fragment.StatusPreference
import com.app.amigo.layer.*
import com.app.amigo.misc.*
import com.app.amigo.unit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.chromium.base.Log
import java.io.BufferedOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

internal class ReconnectionSettings(prefs: SharedPreferences) {
    internal val isEnabled = BoolPreference.RECONNECTION_ENABLED.getValue(prefs)
    private val initialCount =
        if (isEnabled) IntPreference.RECONNECTION_COUNT.getValue(prefs) else 0
    private var currentCount = initialCount
    private val interval = IntPreference.RECONNECTION_INTERVAL.getValue(prefs)
    internal val intervalMillis = (interval * 1000).toLong()
    internal val isRetryable: Boolean
        get() = currentCount > 0 || initialCount == 0

    internal fun resetCount() {
        currentCount = initialCount
    }

    internal fun consumeCount() {
        if (initialCount > 0) currentCount-- else currentCount++
    }

    internal fun generateMessage(): String {
        val triedCount = initialCount - currentCount
        return if (initialCount > 0)
            "Reconnection (COUNT: $triedCount/$initialCount)"
        else
            "Reconnection (COUNT: $currentCount)"
    }
}

internal class ControlClient(internal val vpnService: SstpVpnService) :
    CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {
    private var TAG = "@!@ControlClient"
    private var prefs = PreferenceManager.getDefaultSharedPreferences(vpnService.applicationContext)

    internal lateinit var networkSetting: NetworkSetting
    internal lateinit var status: DualClientStatus
    internal lateinit var builder: VpnService.Builder
    internal lateinit var incomingBuffer: IncomingBuffer
    private var observer: NetworkObserver? = null
    internal val controlQueue = LinkedBlockingQueue<Any>()
    internal var logStream: BufferedOutputStream? = null
    internal val reconnectionSettings = ReconnectionSettings(prefs)

    internal lateinit var sslTerminal: SslTerminal
    private lateinit var sstpClient: SstpClient
    private lateinit var pppClient: PppClient
    internal lateinit var ipTerminal: IpTerminal

    private var jobIncoming: Job? = null
    private var jobControl: Job? = null
    private var jobEncapsulate: Job? = null
    private var jobData: Job? = null
    private val isAllJobCompleted: Boolean
        get() {
            arrayOf(jobIncoming, jobControl, jobEncapsulate, jobData).forEach {
                if (it?.isCompleted != true) {
                    return false
                }
            }
            return true
        }

    private val mutex = Mutex()
    private var isClosing = false

    private val handler = CoroutineExceptionHandler { _, exception ->
        if (!isClosing) kill(exception)
    }

    init {
        initialize()
    }

    fun initialize() {
        Log.d(TAG, "initialize")
//        prefs = PreferenceManager.getDefaultSharedPreferences(vpnService.applicationContext)
        networkSetting = NetworkSetting(prefs)
        status = DualClientStatus()
        builder = vpnService.Builder()
        incomingBuffer = IncomingBuffer(networkSetting.BUFFER_INCOMING, this)
        controlQueue.clear()
        isClosing = false
    }

    internal fun kill(exception: Throwable?) {
        launch {
            mutex.withLock {
//                Log.d(TAG, "An unexpected event occurred " + exception.toString())
                if (!isClosing) {
                    isClosing = true
                    controlQueue.add(0)
//                    if (exception != null && exception !is SuicideException) {
//                        inform("An unexpected event occurred", exception)
//                    }
                    Log.d(TAG, "@!@exception.localizedMessage " + exception?.localizedMessage)
                    if (exception != null) when (exception.localizedMessage) {
                        "com.app.amigo.DISCONNECT", "Kill this client as intended" -> {
                            prefs.edit().putString(StatusPreference.STATUS.name, "").apply()
                            observer?.close()
//                    prefs.edit().putString(StatusPreference.STATUS.name, "").apply()
                        }
                    }
                    // release ConnectivityManager resource
//                    observer?.close()

                    // no more packets needed to be retrieved
                    ipTerminal.release()
                    jobData?.cancel()
                    jobEncapsulate?.cancel()
                    // wait until SstpClient.sendLastGreeting() is invoked
                    jobIncoming?.join()
                    // wait until jobControl finishes sending messages
                    withTimeout(10_000) {
                        while (isActive) {
                            if (jobControl?.isCompleted == false) {
                                delay(100)
                            } else break
                        }
                    }
                    // avoid jobControl being stuck with socket
                    sslTerminal.release()
                    // ensure jobControl is completed
                    jobControl?.cancel()

                    if (exception != null) {
                        if (exception is SuicideException || (exception.localizedMessage != "com.app.amigo.DISCONNECT" && reconnectionSettings.isEnabled && BoolPreference.HOME_CONNECTOR.getValue(prefs))) {
                            if (reconnectionSettings.isRetryable) {
                                tryReconnection()
                                return@withLock
                            } else {
                                inform("Exhausted retry counts", null)
                                makeNotification(0, "Failed to reconnect: Exhausted retry counts")
                            }
                        }
                    }
                    bye()
                }
            }
        }
    }

    private fun bye() {
        Log.d(TAG, "bye")
        inform("Terminate VPN connection", null)
        logStream?.close()
        prefs.edit().putBoolean(BoolPreference.HOME_CONNECTOR.name, false).apply()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//            vpnService.startForegroundService(
//                Intent(
//                    vpnService,
//                    SstpVpnService::class.java
//                ).setAction(VpnAction.ACTION_STOP.value)
//            )
//        else
//            vpnService.startService(
//                Intent(
//                    vpnService,
//                    SstpVpnService::class.java
//                ).setAction(VpnAction.ACTION_STOP.value)
//            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vpnService.stopForeground(true)
        }
        vpnService.stopSelf()
//        vpnService.stopForeground(true)
//        vpnService.stopSelf()
        prefs.edit().putString(StatusPreference.STATUS.name, "").apply()
    }

    private fun tryReconnection() {
        launch {
            Log.d(TAG, "tryReconnection")
            reconnectionSettings.consumeCount()
            val str = reconnectionSettings.generateMessage()
            prefs.edit().putString(StatusPreference.STATUS.name, str).apply()
            makeNotification(0, str)
            delay(reconnectionSettings.intervalMillis)
            val result = withTimeoutOrNull(10_000) {
                while (true) {
                    if (isAllJobCompleted) {
                        return@withTimeoutOrNull true
                    } else {
                        delay(1)
                    }
                }
            }
            if (result == null && !reconnectionSettings.isRetryable) {
                inform("The last session cannot be cleaned up", null)
                makeNotification(0, "Failed to reconnect2")
                bye()
            } else {
                NotificationManagerCompat.from(vpnService.applicationContext).also {
                    it.cancel(0) // удалить notifi
                }
                initialize()
                run()
            }
        }
    }

    fun run() {
        Log.d(TAG, "run")
        if (networkSetting.LOG_DO_SAVE_LOG && logStream == null) {
            prepareLog()
        }
        inform("Establish VPN connection", null)
        prepareLayers()
        launchJobIncoming()
        launchJobControl()
    }

    private fun launchJobIncoming() {
        jobIncoming = launch(handler) {
            while (isActive) {
                sstpClient.proceed()
                pppClient.proceed()
                if (isClosing) {
                    status.sstp = SstpStatus.CALL_DISCONNECT_IN_PROGRESS_1
                }
            }
        }
    }

    private fun launchJobControl() {
        jobControl = launch(handler) {
            val controlBuffer = ByteBuffer.allocate(CONTROL_BUFFER_SIZE)
            while (isActive) {
                val candidate = controlQueue.take()
                if (candidate == 0) break
                controlBuffer.clear()
                when (candidate) {
                    is ControlPacket -> {
                        candidate.write(controlBuffer)
                    }
                    is PppFrame -> {
                        controlBuffer.putShort(PacketType.DATA.value)
                        controlBuffer.putShort((candidate._length + 8).toShort())
                        candidate.write(controlBuffer)
                    }
                    else -> throw Exception("Invalid Control Unit")
                }
                controlBuffer.flip()
                sslTerminal.send(controlBuffer)
            }
        }
    }

    private fun launchJobEncapsulate(channel: Channel<ByteBuffer>) {
        jobEncapsulate = launch(handler) { // buffer packets
            val dataBuffer = ByteBuffer.allocate(networkSetting.BUFFER_OUTGOING)
            val minCapacity = networkSetting.currentMtu + 8
            val ipv4Version: Int = (0x4).shl(28)
            val ipv6Version: Int = (0x6).shl(28)
            val versionMask: Int = (0xF).shl(28)
            var polled: ByteBuffer?
            fun encapsulate(src: ByteBuffer): Boolean // true if data protocol is enabled
            {
                val header = src.getInt(0)
                val version = when (header and versionMask) {
                    ipv4Version -> {
                        if (!networkSetting.PPP_IPv4_ENABLED) return false
                        PppProtocol.IP.value
                    }
                    ipv6Version -> {
                        if (!networkSetting.PPP_IPv6_ENABLED) return false
                        PppProtocol.IPV6.value
                    }
                    else -> throw Exception("Invalid data protocol was detected")
                }
                dataBuffer.putShort(PacketType.DATA.value)
                dataBuffer.putShort((src.limit() + 8).toShort())
                dataBuffer.putShort(PPP_HEADER)
                dataBuffer.putShort(version)
                dataBuffer.put(src)

                return true
            }
            while (isActive) {
                dataBuffer.clear()
                if (!encapsulate(channel.receive())) continue
                while (isActive) {
                    polled = channel.tryReceive().getOrNull() // !!!было channel.pool()
                    if (polled != null) {
                        encapsulate(polled)
                        if (dataBuffer.remaining() < minCapacity) break
                    } else {
                        break
                    }
                }
                dataBuffer.flip()
                sslTerminal.send(dataBuffer)
            }
        }
    }

    internal fun launchJobData() {
        jobData = launch(handler) {
            val channel = Channel<ByteBuffer>(0)
            val readBufferAlpha = ByteBuffer.allocate(networkSetting.currentMtu)
            val readBufferBeta = ByteBuffer.allocate(networkSetting.currentMtu)
            var isBlockingAlpha = true
            launchJobEncapsulate(channel)
            suspend fun read(dst: ByteBuffer) {
                dst.run {
                    clear()
                    position(
                        ipTerminal.ipInput.read(
                            array(),
                            0,
                            networkSetting.currentMtu
                        )
                    )
                    flip()
                }
                channel.send(dst)
            }

            while (isActive) {
                isBlockingAlpha = if (isBlockingAlpha) {
                    read(readBufferAlpha)
                    false
                } else {
                    read(readBufferBeta)
                    true
                }
            }
        }
    }

    internal fun attachNetworkObserver() {
        observer = NetworkObserver(this)
    }

    private fun prepareLayers() {
        sslTerminal = SslTerminal(this)
        sstpClient = SstpClient(this)
        pppClient = PppClient(this)
        ipTerminal = IpTerminal(this)
    }

    private fun prepareLog() {
        val currentTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val filename = "log_osc_${currentTime}.txt"
        val uri = Uri.parse(networkSetting.LOG_DIR)
        DocumentFile.fromTreeUri(vpnService, uri)!!.createFile("text/plain", filename).also {
            logStream = BufferedOutputStream(vpnService.contentResolver.openOutputStream(it!!.uri))
        }
    }

    private fun makeNotification(id: Int, message: String) {
        val builder =
            NotificationCompat.Builder(vpnService.applicationContext, vpnService.CHANNEL_ID).also {
                it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
                it.setContentText(message)
                it.priority = NotificationCompat.PRIORITY_LOW
                it.setAutoCancel(true)
//                it.setSound(null)
            }
        NotificationManagerCompat.from(vpnService.applicationContext).also {
            it.notify(id, builder.build())
        }
        inform(message, null)
    }
}
