package com.app.amigo

import android.content.SharedPreferences
import android.net.Uri
import android.net.VpnService
import android.os.Build
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
            "Переподключение №: $triedCount/$initialCount"
        else
            "Переподключение №: $currentCount"
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
//            val cats = arrayListOf(jobIncoming, jobControl, jobEncapsulate, jobData)
            var result = true
//            for ((index, element) in cats.withIndex()) {
//                Log.e(TAG, "$index + $element " + element?.isCompleted)
//            }
            arrayOf(jobIncoming, jobControl, jobEncapsulate, jobData).forEach {
//                Log.e(TAG, "isAllJobCompleted " + it)
                if (it != null) {
                    if (it.isCompleted != true) {
                        result = false
//                        it.cancel()
//                        it.join()
                    }
                }
            }
            return result
        }

    private val mutex = Mutex()
    private var isClosing = false

    private val handler = CoroutineExceptionHandler { _, exception ->
        if (!isClosing) {
            Log.e(TAG, "CoroutineException " + exception)
            kill(exception)
        }
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

// крашится при @!@*****exception: java.net.UnknownHostException: Unable to resolve host "tychina.keenetic.pro": No address associated with hostname *****isClosing: false

            Log.d(TAG, "*****exception: " + exception + " *****isClosing: " + isClosing)
            if (exception != null) {
                Log.e(TAG, "*****exception.message: " + exception.message)
                Log.e(TAG, "*****exception.localizedMessage: " + exception.localizedMessage)
                Log.e(TAG, "*****exception.cause: " + exception.cause)
            }
            mutex.withLock {
//                Log.d(TAG, "An unexpected event occurred " + exception.toString())
                if (!isClosing) {
                    isClosing = true
                    controlQueue.add(0)
                    if (exception != null && exception !is SuicideException) {
                        observer?.close()
                        inform("An unexpected event occurred", exception)
                        Log.e(TAG, "!is SuicideException: " + exception)
                    }
//                    Log.e(TAG, "@!@***** 1 *****")
                    if (exception != null)
                        when (exception.localizedMessage) {
                            "com.app.amigo.DISCONNECT", "No address associated with hostname"/*, "Kill this client as intended"*/ -> {
                                StatusPreference.STATUS.setValue(prefs,  "")
//                                observer?.close()
                            }
                        }
                    // release ConnectivityManager resource
//                    Log.e(TAG, "observer " + observer)
                    observer?.close() // смотри внутрь
//                    Log.e(TAG, "@!@***** 2 *****")

                    // no more packets needed to be retrieved
                    ipTerminal.release()
//                    Log.e(TAG, "@!@***** 3 *****")
                    jobData?.cancel()
//                    jobData?.join() //**
//                    jobIncoming, jobControl, jobEncapsulate, jobData
//                    Log.e(TAG, "@!@***** 4 *****")
                    jobEncapsulate?.cancel()
//                    jobEncapsulate?.join() //**
//                    Log.e(TAG, "@!@***** 5 *****")
                    // wait until SstpClient.sendLastGreeting() is invoked
//                    jobIncoming?.cancel() //**
                    jobIncoming?.join()
//                    Log.e(TAG, "@!@***** 6 *****")
                    // wait until jobControl finishes sending messages
                    withTimeout(10_000) {
                        while (isActive) {
                            if (jobControl?.isCompleted == false) {
                                delay(100)
                            } else break
                        }
                    }
//                    Log.e(TAG, "@!@***** 7 *****")
                    // avoid jobControl being stuck with socket
                    sslTerminal.release()
//                    Log.e(TAG, "@!@***** 8 *****")
                    // ensure jobControl is completed
                    jobControl?.cancel()
//                    jobControl?.join() //**
//                    Log.e(TAG, "@!@***** 9 *****")

                    if (exception != null) {
                        if (exception.localizedMessage != "com.app.amigo.DISCONNECT" &&
                            reconnectionSettings.isEnabled &&
                            BoolPreference.HOME_CONNECTOR.getValue(prefs)
                        ) {
                            if (reconnectionSettings.isRetryable) {
                                tryReconnection()
                                return@withLock
                            } else {
                                inform("Exhausted retry counts", null)
//                                makeNotification(
//                                    NOTIFICATION_ID,
//                                    "Failed to reconnect: Exhausted retry counts"
//                                )
                            }
                        }
                    }
                    Log.d(TAG, "kill: bye")
                    bye()
//                    Log.e(TAG, "@!@***** 10 *****")
                }
            }
        }
    }

    private fun bye() {
        Log.d(TAG, "bye")
        inform("Terminate VPN connection", null)
        logStream?.close()
        BoolPreference.HOME_CONNECTOR.setValueFragment(false)
//        prefs.edit().putBoolean(BoolPreference.HOME_CONNECTOR.name, false).apply()
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
        StatusPreference.STATUS.setValue(prefs,  "")
    }

    private fun tryReconnection() {
        launch {
            Log.d(TAG, "tryReconnection")
            reconnectionSettings.consumeCount()
            val str = reconnectionSettings.generateMessage()
            StatusPreference.STATUS.setValue(prefs, str)
            vpnService.helper.updateNotification(str)
            val startTime = System.currentTimeMillis()
            val result = withTimeoutOrNull(10_000) {
                while (true) {
                    if (isAllJobCompleted) {
                        //                        Log.e(TAG, "isAllJobCompleted == true")
                        return@withTimeoutOrNull true
                    } else {
                        //                        Log.e(TAG, "isAllJobCompleted == false")
                        delay(1)
                    }
                }
            }
            val totalTime = System.currentTimeMillis() - startTime
//            Log.e(TAG, "tryReconnection: result == $result")
//            Log.e(TAG, "measureTimeMillis: " + totalTime)
            val delay_ms = reconnectionSettings.intervalMillis - totalTime
//            Log.e(TAG, "delay_ms: " + delay_ms)
            if (delay_ms > 0) delay(delay_ms)
            if (result == null && !reconnectionSettings.isRetryable) {
                inform("The last session cannot be cleaned up", null)
//                makeNotification(NOTIFICATION_ID, "Failed to reconnect2")
                bye()
            } else {
//                NotificationManagerCompat.from(vpnService.applicationContext).also {
//                    it.cancel(NOTIFICATION_ID) // удалить notifi
//                }

    //*******
//                val wifiManager = vpnService.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//                val wifiName = wifiManager.deviceName()
//                wifiManager.getConnectionInfo()

//                val connManager =
//                    vpnService.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//                val networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
//                if (networkInfo?.isConnected == true) {
//                    val wifiManager =
//                        vpnService.applicationContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
//                    val connectionInfo = wifiManager.connectionInfo
//                    if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.ssid)) {
//                        Log.e("ssid", connectionInfo.ssid)
//                    }
//                }
//                else{
//                    Log.e("ssid", "No Connection")
//                }
           //********
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
                    Log.e(TAG, "***** launchJobIncoming CALL_DISCONNECT_IN_PROGRESS_1*****")
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

//    private fun makeNotification(id: Int, message: String) {
//        val builder =
//            NotificationCompat.Builder(vpnService.applicationContext, vpnService.CHANNEL_ID).also {
//                it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
//                it.setContentText(message)
//                it.priority = NotificationCompat.PRIORITY_LOW
//                it.setAutoCancel(true)
////                it.setSound(null)
//            }
//        NotificationManagerCompat.from(vpnService.applicationContext).also {
//            it.notify(id, builder.build())
//        }
//        inform(message, null)
//    }
}
