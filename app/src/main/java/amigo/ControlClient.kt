package com.app.amigo

import android.content.Context
import android.content.SharedPreferences
import android.net.*
import android.net.wifi.WifiManager
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.app.amigo.fragment.BoolPreference
import com.app.amigo.fragment.IntPreference
import com.app.amigo.fragment.SetPreference
import com.app.amigo.fragment.StatusPreference
import com.app.amigo.layer.*
import com.app.amigo.misc.*
import com.app.amigo.unit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.chromium.base.Log
import java.io.BufferedOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

internal enum class enumStateVPN(val value: String) {
    VPN_STOP("com.app.amigo.VPN_STOP"),
    VPN_CONNECTING("com.app.amigo.VPN_CONNECTING"),
    VPN_CONNECTED("com.app.amigo.VPN_CONNECTED"),
    VPN_DISCONNECTING("com.app.amigo.VPN_DISCONNECTING"),
    VPN_DISCONNECTED("com.app.amigo.VPN_DISCONNECTED"),

    TEST("com.app.amigo.TEST"),
}


internal class ReconnectionSettings(prefs: SharedPreferences) {
    val TAG = "@!@ControlClient"
    internal val isEnabled = BoolPreference.RECONNECTION_ENABLED.getValue(prefs)
    private val initialCount =
        if (isEnabled) IntPreference.RECONNECTION_COUNT.getValue(prefs) else 0
    internal var currentCount = initialCount
    private val interval = IntPreference.RECONNECTION_INTERVAL.getValue(prefs)
    internal val intervalMillis = (interval * 1000).toLong()
    internal val isRetryable: Boolean
        get() = isEnabled && (currentCount > 0 || initialCount == 0)

    internal val isReconnection: Boolean
        get() = isEnabled && ((currentCount > 0 && initialCount == 0) || (currentCount > 0 && initialCount > 0))

    internal fun resetCount() {
        Log.i(TAG, "resetCount currentCount=$currentCount")
        currentCount = initialCount
    }

    internal fun consumeCount() {
        if (initialCount > 0) currentCount-- else currentCount++
        startTime = System.currentTimeMillis()
        Log.i(TAG, "consumeCount currentCount=$currentCount")
    }

    private var startTime: Long = 0
    internal fun setStartTime() {
        startTime = System.currentTimeMillis()
    }

    internal fun getDelay_ms(): Long {
        val delay_ms = intervalMillis - (System.currentTimeMillis() - startTime)
        if (delay_ms > 0)
            return delay_ms
        else
            return 0
    }

    internal fun generateMessage(): String {
        val triedCount = initialCount - currentCount
        return if (initialCount > 0)
            "reconnection: $triedCount/$initialCount"
        else
            "reconnection: $currentCount"
    }
}

internal class StateAndSettings(prefs: SharedPreferences) {
    internal var wifi_ssid: String = ""
    internal var wifi_ip: String = ""
    internal var wifi_dns: String = ""
    internal var cellular_state: String = ""
    internal var networkTransport: EnumTransport = EnumTransport.TRANSPORT_NONE
    internal var vpn_state: enumStateVPN = enumStateVPN.VPN_DISCONNECTED
    internal var vpn_net: String = "no"
    internal var vpn_ip: String = ""
    internal var vpn_dns: String = ""
}

internal class ControlClient(internal val vpnService: MainService) :
    CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {
    val TAG = "@!@ControlClient"
    private var prefs = PreferenceManager.getDefaultSharedPreferences(vpnService.applicationContext)

    internal val channel =
        Channel<Int>(CONFLATED) // создает канал с размером буфера равный 1. Повторный вызов offer или send перезаписывает текущее значение в буфере, при этом приостановка корутины не происходит. Поэтому ресивер будет считывать всегда самое последнее значение из канала.
    var number: Int = 0

    @Synchronized
    fun numberAdd(x: Int = 1) {
        number = number + x
    }

    internal lateinit var networkSetting: NetworkSetting
    internal lateinit var status: DualClientStatus
    internal lateinit var builder: VpnService.Builder
    internal lateinit var incomingBuffer: IncomingBuffer
    private var observer: NetworkObserver? = null
    internal val controlQueue = LinkedBlockingQueue<Any>()
    internal var logStream: BufferedOutputStream? = null
    internal val reconnectionSettings = ReconnectionSettings(prefs)
    internal val stateAndSettings = StateAndSettings(prefs)

    internal var sslTerminal: SslTerminal? = null
    private lateinit var sstpClient: SstpClient
    private lateinit var pppClient: PppClient
    internal var ipTerminal: IpTerminal? = null

    private var jobRun: Job? = null

    @Synchronized
    internal fun getJobRun(): Job? {
        return jobRun
    }

    @Synchronized
    internal fun setJobRun(job: Job?) {
        jobRun = job
    }

    private var jobIncoming: Job? = null
    private var jobControl: Job? = null
    private var jobEncapsulate: Job? = null
    private var jobData: Job? = null
    private val isAllJobCompleted: Boolean
        get() {
            val arr = arrayListOf(jobIncoming, jobControl, jobEncapsulate, jobData)
            var result = true
            for ((index, element) in arr.withIndex()) {
                Log.e(TAG, "$index + $element " + element?.isCompleted)
            }
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
//    private var isClosing = true

    private  val handler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "***** start exception exception.localizedMessage ${exception.localizedMessage}")
//        if (stateAndSettings.isVPNConnected) {
        Log.d(TAG, "INFO jobIncoming: ${jobIncoming}")
        Log.d(TAG, "INFO jobControl: ${jobControl}")
//        }
        launchJobRun(exception)
    }

    init {
        initialize()
    }

    fun initialize() {
//        launchJobService()
        Log.d(TAG, "initialize")
//        prefs = PreferenceManager.getDefaultSharedPreferences(vpnService.applicationContext)
        networkSetting = NetworkSetting(prefs)
        status = DualClientStatus()
        builder = vpnService.Builder()
        incomingBuffer = IncomingBuffer(networkSetting.BUFFER_INCOMING, this)
        controlQueue.clear()
//        isClosing = false
//        if(jobService == null)
//        launchJobService()
    }

    // 1 если stateService=Start и (VPN_DISCONNECTED или VPN_DISCONNECTING) и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- коннект-впн
    // 2 если stateService=Start и (VPN_CONNECTED или VPN_CONNECTING) и (TRANSPORT_HOMEWIFI или TRANSPORT_NONE) --- дисконнект-впн
    // 3 если stateService=Stop и (VPN_CONNECTED или VPN_CONNECTING) --- дисконнект-впн
    internal fun launchJobRun(exception: Throwable = Throwable("myDefaultException")) {

        launch {
            // отправляем данные через канал
            mutex.withLock {
//                numberAdd()
                channel.send(0)
            }
        }
    }

    internal fun initJobRun() {
        Log.e(
            TAG,
            "***** ***** ***** ***** ***** ***** initJobRun()"
        )

        if ((getJobRun() == null)/* || (jobStateMachine?.isCompleted == true) || (jobStateMachine?.isCancelled == true)*/) {
//            reconnectionSettings.resetCount()
            Log.d(TAG, "START JOB0")
            var stop = false
            Log.d(TAG, "START JOB1")
            setJobRun(launch() {
                Log.d(TAG, "START JOB2")
                if(channel.isEmpty) channel.send(0)
                Log.d(TAG, "START JOB3")
                while (!stop) {
                    Log.d(TAG, "START JOB4")
                    if (!reconnectionSettings.isReconnection) channel.receive()

                    Log.e(
                        TAG,
                        "000000 vpnService.stateService: {${
                            BoolPreference.HOME_CONNECTOR.getValue(
                                PreferenceManager.getDefaultSharedPreferences(
                                    vpnService.applicationContext
                                )
                            )
                        }} " +
                                "stateAndSettings.state: {${stateAndSettings.vpn_state}} " +
                                "stateAndSettings.networkTransport: {${stateAndSettings.networkTransport}}"
                    )


                    // 2 если stateService=Start и (VPN_CONNECTED или VPN_CONNECTING) и (TRANSPORT_HOMEWIFI или TRANSPORT_NONE) --- дисконнект-впн
                    if (((BoolPreference.HOME_CONNECTOR.getValue(
                            PreferenceManager.getDefaultSharedPreferences(
                                vpnService.applicationContext
                            )
                        ))
                                && ((stateAndSettings.vpn_state == enumStateVPN.VPN_CONNECTED)
                                || (stateAndSettings.vpn_state == enumStateVPN.VPN_CONNECTING))
                                && ((stateAndSettings.networkTransport == EnumTransport.TRANSPORT_HOMEWIFI)
                                || (stateAndSettings.networkTransport == EnumTransport.TRANSPORT_NONE))
                                )
                        // 3 если stateService=Stop и (VPN_CONNECTED или VPN_CONNECTING) --- дисконнект-впн
                        || ((!BoolPreference.HOME_CONNECTOR.getValue(
                            PreferenceManager.getDefaultSharedPreferences(
                                vpnService.applicationContext
                            )
                        ))
//                                    && ((stateAndSettings.vpn_state == enumStateVPN.VPN_CONNECTED)
//                                    || (stateAndSettings.vpn_state == enumStateVPN.VPN_CONNECTING))
                                )
                        || (reconnectionSettings.currentCount != 0)
                    ) {
                        if ((!BoolPreference.HOME_CONNECTOR.getValue(
                                PreferenceManager.getDefaultSharedPreferences(
                                    vpnService.applicationContext
                                )
                            ))
                        ) {
                            stop = true
                        }

//                        disconnect()
                        Log.i(TAG, "*****start fun disconnect()*****")
                        stateAndSettings.vpn_state = enumStateVPN.VPN_DISCONNECTING
                        controlQueue.add(0)
                        // release ConnectivityManager resource
                        observer?.close() // смотри внутрь .close()
                        // no more packets needed to be retrieved
                        ipTerminal?.release()
                        jobData?.cancel()
                        jobEncapsulate?.cancel()
                        // wait until SstpClient.sendLastGreeting() is invoked
                        jobIncoming?.join()
                        // wait until jobControl finishes sending messages
                        try {
                            withTimeout(10_000) {
                                while (isActive) {
                                    if (jobControl?.isCompleted == false) {
                                        delay(1)
                                    } else break
                                }
                            }
                        } catch (ex: TimeoutCancellationException) {
                            Log.e(
                                TAG,
                                "*****Timeout jobControl?.isCompleted in disconnect()*****"
                            )
                        }
                        // avoid jobControl being stuck with socket
                        sslTerminal?.release()
                        // ensure jobControl is completed
                        jobControl?.cancel()
                        Log.i(
                            TAG,
                            "*****end disconnect()***** stateAndSettings.vpn_state = ${stateAndSettings.vpn_state}"
                        )

//                        waitDisconnect()
                        Log.i(TAG, "*****start fun waitDisconnect()*****")
                        // ждем VPN_DISCONNECTED
                        val result = withTimeoutOrNull(10_000) {
                            while (isActive) {
//                if (vpnService.stateService != EnumStateService.SERVICE_START) {
//                    Log.i(TAG, "Service=Stop - exit!!!")
//                    return@withTimeoutOrNull
//                }
                                if (stateAndSettings.vpn_state == enumStateVPN.VPN_DISCONNECTING) {
                                    Log.i(
                                        TAG,
                                        "wait VPN_DISCONNECTED stateAndSettings.state=${stateAndSettings.vpn_state}"
                                    )
                                    delay(1000)
                                } else if (stateAndSettings.vpn_state == enumStateVPN.VPN_DISCONNECTED) {
//                                        Log.d(TAG, "VPN_DISCONNECTED_1 !!!")
                                    return@withTimeoutOrNull
//                } else {
//                    Log.i(TAG, "other state - exit!!!")
//                    return@withTimeoutOrNull
                                }
                                if (stateAndSettings.vpn_net == "no") {
                                    stateAndSettings.vpn_state =
                                        enumStateVPN.VPN_DISCONNECTED
//                                        Log.d(TAG, "VPN_DISCONNECTED_2 !!!")
                                }
                            }
                        }
                        if (result == null) {
                            Log.i(TAG, "Timeout VPN_DISCONNECTING ---- stop")
                            stateAndSettings.vpn_state = enumStateVPN.VPN_DISCONNECTED
//                           if(!channel.isEmpty) stop = true
//                            BoolPreference.HOME_CONNECTOR.setEnabled(true)
                        }

                    }
                    // 1 если stateService=Start и (VPN_DISCONNECTED или VPN_DISCONNECTING) и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- коннект-впн
                    if ((BoolPreference.HOME_CONNECTOR.getValue(
                            PreferenceManager.getDefaultSharedPreferences(
                                vpnService.applicationContext
                            )
                        ))
                        && ((stateAndSettings.vpn_state == enumStateVPN.VPN_DISCONNECTED)
                                || (stateAndSettings.vpn_state == enumStateVPN.VPN_DISCONNECTING))
                        && ((stateAndSettings.networkTransport == EnumTransport.TRANSPORT_WIFI)
                                || (stateAndSettings.networkTransport == EnumTransport.TRANSPORT_CELLULAR))
                    ) {

//                        connect()
                        if (stateAndSettings.vpn_state != enumStateVPN.VPN_CONNECTING) {
                            Log.i(TAG, "*****start fun connect*****")
                            stateAndSettings.vpn_state = enumStateVPN.VPN_CONNECTING
                            initialize()
                            if (networkSetting.LOG_DO_SAVE_LOG && logStream == null) {
                                prepareLog()
                            }
                            prepareLayers()
                            launchJobIncoming()
                            launchJobControl()
                        }
//                        waitConnect()
                        Log.i(TAG, "*****start fun waitConnect*****")
                        // ждем VPN_CONNECTED
                        val result = withTimeoutOrNull(10_000) {
                            while (isActive) {
//                if (vpnService.stateService != EnumStateService.SERVICE_START) {
//                    Log.i(TAG, "Service=Stop - exit!!!")
//                    return@withTimeoutOrNull
//                }
                                if (stateAndSettings.vpn_state == enumStateVPN.VPN_CONNECTING) {
                                    delay(1000)
                                    Log.i(
                                        TAG,
                                        "wait VPN_CONNECTED stateAndSettings.state=${stateAndSettings.vpn_state}"
                                    )
                                } else if (stateAndSettings.vpn_state == enumStateVPN.VPN_CONNECTED) {
                                    Log.i(TAG, "VPN_CONNECTED!!!")
                                    reconnectionSettings.resetCount()
                                    return@withTimeoutOrNull
//                } else {
//                    Log.i(TAG, "other state - exit!!!")
//                    return@withTimeoutOrNull
                                }
//                                if (queue.isNotEmpty()) {
//                                    return@withTimeoutOrNull true
//                                }
                            }
                        }
                        if (reconnectionSettings.isReconnection) {
                            val delay_ms: Long = reconnectionSettings.getDelay_ms()
                            Log.i(
                                TAG,
                                "***** delay before reconnect #${reconnectionSettings.currentCount} $delay_ms ms *****"
                            )
                            delay(delay_ms)
                        }
                        if (reconnectionSettings.isRetryable && ((result == null) || (stateAndSettings.vpn_state != enumStateVPN.VPN_CONNECTED))) {
                            Log.i(TAG, "Timeout VPN_CONNECTING -> tryReconnecting")
                            reconnectionSettings.consumeCount()
//                                if (queue==0) {
//                                queue = queue.inc()
//                                }
                            reconnectionSettings.setStartTime()
                        }
                    }
                    checkNetworks()
                } // while
                jobRun = null
                reconnectionSettings.resetCount()
                stateAndSettings.vpn_state = enumStateVPN.VPN_DISCONNECTED
//                checkNetworks()
                refreshStatus()
                Log.d(TAG, "*6* вышли из while **")
            })
        }

        Log.i(TAG, "***** STOP JOB !!!!!!!!!!!!!!!!")
    }

    private fun connect() {
        Log.i(TAG, "*****start fun connect*****")
        stateAndSettings.vpn_state = enumStateVPN.VPN_CONNECTING
        initialize()
        if (networkSetting.LOG_DO_SAVE_LOG && logStream == null) {
            prepareLog()
        }
//        inform("Establish VPN connection", null)
        prepareLayers()
        launchJobIncoming()
        launchJobControl()
    }

    private fun waitConnect() {
        launch(handler) {
            Log.i(TAG, "*****start fun waitConnect*****")
            // ждем VPN_CONNECTED
            val result = withTimeoutOrNull(10_000) {
                while (isActive) {
//                if (vpnService.stateService != EnumStateService.SERVICE_START) {
//                    Log.i(TAG, "Service=Stop - exit!!!")
//                    return@withTimeoutOrNull
//                }
                    if (stateAndSettings.vpn_state == enumStateVPN.VPN_CONNECTING) {
                        delay(1000)
                        Log.i(
                            TAG,
                            "wait VPN_CONNECTED stateAndSettings.state=${stateAndSettings.vpn_state}"
                        )
                    } else if (stateAndSettings.vpn_state == enumStateVPN.VPN_CONNECTED) {
                        Log.i(TAG, "VPN_CONNECTED!!!")
                        reconnectionSettings.resetCount()
                        return@withTimeoutOrNull
//                } else {
//                    Log.i(TAG, "other state - exit!!!")
//                    return@withTimeoutOrNull
                    }
                }
            }
            if ((result == null) || (stateAndSettings.vpn_state != enumStateVPN.VPN_CONNECTED)) {
                Log.i(TAG, "Timeout VPN_CONNECTING -> tryReconnecting")
                reconnectionSettings.consumeCount()
                reconnectionSettings.setStartTime()
            }
        }
    }

    private fun disconnect() {
        launch(handler) {
            Log.i(TAG, "*****start fun disconnect()*****")
            stateAndSettings.vpn_state = enumStateVPN.VPN_DISCONNECTING
            controlQueue.add(0)
            // release ConnectivityManager resource
            observer?.close() // смотри внутрь .close()
            // no more packets needed to be retrieved
            ipTerminal?.release()
            jobData?.cancel()
            jobEncapsulate?.cancel()
            // wait until SstpClient.sendLastGreeting() is invoked
            jobIncoming?.join()
            // wait until jobControl finishes sending messages
            try {
                withTimeout(10_000) {
                    while (isActive) {
                        if (jobControl?.isCompleted == false) {
                            delay(1)
                        } else break
                    }
                }
            } catch (ex: TimeoutCancellationException) {
                Log.e(TAG, "*****Timeout jobControl?.isCompleted in disconnect()*****")
            }
            // avoid jobControl being stuck with socket
            sslTerminal?.release()
            // ensure jobControl is completed
            jobControl?.cancel()
            Log.i(
                TAG,
                "*****end disconnect()***** stateAndSettings.vpn_state = ${stateAndSettings.vpn_state}"
            )
        }
    }

    private fun waitDisconnect() {
        launch(handler) {
            Log.i(TAG, "*****start fun waitDisconnect()*****")
            // ждем VPN_DISCONNECTED
            val result = withTimeoutOrNull(10_000) {
                while (isActive) {
//                if (vpnService.stateService != EnumStateService.SERVICE_START) {
//                    Log.i(TAG, "Service=Stop - exit!!!")
//                    return@withTimeoutOrNull
//                }
                    if (stateAndSettings.vpn_state == enumStateVPN.VPN_DISCONNECTING) {
                        Log.i(
                            TAG,
                            "wait VPN_DISCONNECTED stateAndSettings.state=${stateAndSettings.vpn_state}"
                        )
                        delay(1000)
                    } else if (stateAndSettings.vpn_state == enumStateVPN.VPN_DISCONNECTED) {
                        Log.i(TAG, "VPN_DISCONNECTED!!!")
                        return@withTimeoutOrNull
//                } else {
//                    Log.i(TAG, "other state - exit!!!")
//                    return@withTimeoutOrNull
                    }
                }
            }
            if (result == null) {
                Log.i(TAG, "Timeout VPN_DISCONNECTING")
                stateAndSettings.vpn_state = enumStateVPN.VPN_DISCONNECTED
            }
        }
    }

    private fun bye() {
        Log.i(TAG, "*****start bye*****")
        inform("Terminate VPN connection", null)
//        logStream?.close()
//        BoolPreference.HOME_CONNECTOR.setValue(prefs, false)
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
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vpnService.stopForeground(true)
//        }
//        vpnService.stopForeground(true)
//        vpnService.stopSelf()
        StatusPreference.STATUS.setValue(prefs, "")
        StatusPreference.CONNECTEDVIA.setValue(prefs, "")
//        stateAndSettings.isVPNConnected = false
//        BoolPreference.HOME_CONNECTOR.setEnabled(true)
    }

    private fun tryReconnection() {
        launch {
            reconnectionSettings.consumeCount()
            val str = reconnectionSettings.generateMessage()
            Log.d(TAG, "tryReconnection + $str")
            StatusPreference.STATUS.setValue(prefs, str)
            vpnService.helper.updateNotification(str)
            val startTime = System.currentTimeMillis()
            val result = withTimeoutOrNull(10_000) {
                while (true) {
//                    if (checkNetworks()) {
                    if (isAllJobCompleted) {
                        Log.e(TAG, "isAllJobCompleted == true")
                        return@withTimeoutOrNull true
                    } else {
                        Log.e(TAG, "isAllJobCompleted == false")
                        delay(200)
                    }
//                    } else return@withTimeoutOrNull false
                }
            }
            val totalTime = System.currentTimeMillis() - startTime
//            Log.e(TAG, "tryReconnection: result == $result")
//            Log.e(TAG, "measureTimeMillis: " + totalTime)
            if (result == null && !reconnectionSettings.isRetryable) {
                inform("The last session cannot be cleaned up", null)
//                makeNotification(NOTIFICATION_ID, "Failed to reconnect2")
                bye()
            } else {
//                NotificationManagerCompat.from(vpnService.applicationContext).also {
//                    it.cancel(NOTIFICATION_ID) // удалить notifi
//                }
                val delay_ms = reconnectionSettings.intervalMillis - totalTime
                Log.e(TAG, "stateAndSettings.state: " + stateAndSettings.vpn_state)
                Log.d(TAG, "delay_ms: " + delay_ms)
                if (delay_ms > 0) delay(delay_ms)
//                if (!isClosing)
                launchJobRun()
            }
        }
    }

    private fun launchJobRun() {
        Log.d(TAG, "start fun run()")
        Log.d(TAG, "stateAndSettings.networkTransport = " + stateAndSettings.networkTransport)

//        if ((stateAndSettings.networkTransport != CTransport.TRANSPORT_HOMEWIFI.value)
////            && (stateAndSettings.networkTransport != CTransport.TRANSPORT_NONE.value)
//        ) {
        initialize()
        Log.d(TAG, "run")
        if (networkSetting.LOG_DO_SAVE_LOG && logStream == null) {
            prepareLog()
        }
//        inform("Establish VPN connection", null)
//            prepareLayers()
        launchJobIncoming()
        launchJobControl()
//            Log.d(TAG, "checkNetworks: " + if (checkNetworks()) "true" else "false")
//            StatusPreference.STATUS.setValue(prefs, "")
//        } else {
//            stateAndSettings.state = enumStateVPN.VPN_DISCONNECTED
//            StatusPreference.STATUS.setValue(prefs, "Home Wi-Fi")
//        }

//        BoolPreference.HOME_CONNECTOR.setEnabled(true)
    }

//    fun launchJobService() {
//        Log.d(TAG, "***** start fun launchJobService isActive: " + if (isActive) "true" else "false")
//        jobService = launch(handler) {
//            Log.d(TAG, "launch in launchJobService")
//            Log.d(
//                TAG,
//                "run while in launchJobService isActive: " + if (isActive) "true" else "false"
//            )
//            while (isActive) {
////                Log.w(TAG, "while in launchJobService ***** ${status.sstp}")
//                val intent = Intent(enumStateService.TEST.name)
//                vpnService.onStartCommand(intent, 0, 0)
//                delay(5000)
//                Log.e(
//                    TAG,
//                    "!!!!!!!!!!!!!!!!! status.ppp = ${status.ppp}  status.sstp = ${status.sstp}"
//                )
//            }
//            Log.d(TAG, "stop while in launchJobService")
//        }
//    }

    private fun launchJobIncoming() {
        Log.d(
            TAG,
            "***** start fun launchJobIncoming isActive: " + if (isActive) "true" else "false"
        )
        jobIncoming = launch(handler) {
//            Log.d(TAG, "launch in launchJobIncoming")
//            Log.d(
//                TAG,
//                "run while in launchJobIncoming isActive: " + if (isActive) "true" else "false"
//            )
            while (isActive) {
//                Log.d(TAG, "while in launchJobIncoming1 $isActive")
                sstpClient.proceed()
//                Log.d(TAG, "while in launchJobIncoming2 $isActive")
                pppClient.proceed()
//                Log.d(TAG, "while in launchJobIncoming3 $isActive")
                if ((stateAndSettings.vpn_state == enumStateVPN.VPN_DISCONNECTING) || (stateAndSettings.vpn_state == enumStateVPN.VPN_DISCONNECTED)) {
                    Log.e(
                        TAG,
                        "***** launchJobIncoming CALL_DISCONNECT_IN_PROGRESS_1 ***** ${status.sstp}"
                    )
                    Log.e(
                        TAG,
                        "***** launchJobIncoming stateAndSettings.vpn_state ***** ${stateAndSettings.vpn_state}"
                    )
                    status.sstp = SstpStatus.CALL_DISCONNECT_IN_PROGRESS_1
                    if (stateAndSettings.vpn_state == enumStateVPN.VPN_DISCONNECTED) {
                        this.cancel()
                        Log.e(
                            TAG,
                            "***** launchJobIncoming this.cancel() *****"
                        )
                    }
                }
            }
//            Log.d(TAG, "stop while in launchJobIncoming")
        }
    }

    private fun launchJobControl() {
        Log.d(
            TAG,
            "***** start fun launchJobControl isActive: " + if (isActive) "true" else "false"
        )
        jobControl = launch(handler) {
//            Log.d(TAG, "launch in launchJobControl")
            val controlBuffer = ByteBuffer.allocate(CONTROL_BUFFER_SIZE)
//            Log.d(
//                TAG,
//                "run while in launchJobControl isActive: " + if (isActive) "true" else "false"
//            )
            while (isActive) {
//                Log.d(TAG, "while in launchJobControl")
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
                sslTerminal?.send(controlBuffer)
            }
//            Log.d(TAG, "stop while in launchJobControl")
        }
    }

    private fun launchJobEncapsulate(channel: Channel<ByteBuffer>) {
        Log.d(
            TAG,
            "***** start fun launchJobEncapsulate isActive: " + if (isActive) "true" else "false"
        )
        jobEncapsulate = launch(handler) { // buffer packets
//            Log.d(TAG, "launch in launchJobEncapsulate")
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
//            Log.d(
//                TAG,
//                "run while in launchJobEncapsulate isActive: " + if (isActive) "true" else "false"
//            )
            while (isActive) {
//                Log.d(TAG, "while in launchJobEncapsulate")
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
                sslTerminal?.send(dataBuffer)
            }
//            Log.d(TAG, "stop while in launchJobEncapsulate")
        }
    }

    internal fun launchJobData() {
        Log.d(TAG, "***** start fun launchJobData isActive: " + if (isActive) "true" else "false")
        jobData = launch(handler) {
//            Log.d(TAG, "launch in launchJobData")
            val channel = Channel<ByteBuffer>(0)
            val readBufferAlpha = ByteBuffer.allocate(networkSetting.currentMtu)
            val readBufferBeta = ByteBuffer.allocate(networkSetting.currentMtu)
            var isBlockingAlpha = true
            launchJobEncapsulate(channel)
            suspend fun read(dst: ByteBuffer) {
                dst.run {
                    clear()
                    ipTerminal?.ipInput?.read(
                        array(),
                        0,
                        networkSetting.currentMtu
                    )?.let {
                        position(
                            it
                        )
                    }
                    flip()
                }
                channel.send(dst)
            }

//            Log.d(
//                TAG,
//                "run while in launchJobData isActive: " + if (isActive) "true" else "false"
//            )
            while (isActive) {
//                Log.d(TAG, "while in launchJobData")
                isBlockingAlpha = if (isBlockingAlpha) {
                    read(readBufferAlpha)
                    false
                } else {
                    read(readBufferBeta)
                    true
                }
            }
//            Log.d(TAG, "stop while in launchJobData")
        }
    }

    internal fun attachNetworkObserver() {
        observer?.close()
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
            logStream =
                BufferedOutputStream(vpnService.contentResolver.openOutputStream(it!!.uri))
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
    fun checkNetworks() {
        var ssid = ""
        var wifi_ip = ""
        var vpn_net = "no"
//        var wifi_dns = ""
        var cellular = "no"
        var networkTransport: EnumTransport = EnumTransport.TRANSPORT_NONE
        val connectivityManager =
            vpnService.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val allNetworks = connectivityManager.allNetworks
        allNetworks.forEachIndexed { index, network ->
//            Log.d(
//                TAG,
//                "NetworkCapabilities: #$index - " + connectivityManager.getNetworkCapabilities(
//                    network
//                ).toString()
//            )
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val wifiManager =
                    vpnService.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val connectionInfo = wifiManager.connectionInfo
                ssid = connectionInfo.ssid.replace("\"", "")
                wifi_ip = ipToString(connectionInfo.ipAddress)
//                Log.d(TAG, "connectionInfo.ssid: ${ssid}")
                if (connectionInfo != null &&
                    !TextUtils.isEmpty(ssid)
                ) {
                    if (SetPreference.HOME_WIFI_SUITES.getValue(prefs).contains(ssid) &&
                        BoolPreference.SELECT_HOME_WIFI.getValue(prefs)
                    ) {
                        networkTransport = EnumTransport.TRANSPORT_HOMEWIFI
                    } else {
                        networkTransport = EnumTransport.TRANSPORT_WIFI
                    }
                }
            } else if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                cellular = "yes"
                if ((networkTransport != EnumTransport.TRANSPORT_HOMEWIFI) || (networkTransport != EnumTransport.TRANSPORT_WIFI)) {
                    networkTransport =
                        EnumTransport.TRANSPORT_CELLULAR
                }
            } else if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                vpn_net = "yes"
            }
        }
        stateAndSettings.vpn_net = vpn_net
        Log.d(TAG, "!!checkNetworks!! stateAndSettings.vpn_net = ${vpn_net}")
        stateAndSettings.wifi_ssid = ssid
        stateAndSettings.wifi_ip = wifi_ip
        stateAndSettings.cellular_state = cellular
        stateAndSettings.networkTransport = networkTransport
        refreshStatus()
    }

    private fun ipToString(i: Int): String {
        return (i and 0xFF).toString() + "." +
                (i shr 8 and 0xFF) + "." +
                (i shr 16 and 0xFF) + "." +
                (i shr 24 and 0xFF)

    }

    internal fun refreshStatus() {
        val summary = mutableListOf<String>()
        summary.add(SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date()))
        summary.add("")
        if (!BoolPreference.HOME_CONNECTOR.getValue(
                PreferenceManager.getDefaultSharedPreferences(
                    vpnService.applicationContext
                )
            )
        ) {
            summary.add("Service stopped")
        } else {
            summary.add("[Cellular] ${stateAndSettings.cellular_state}")
            summary.add("")

            if ((stateAndSettings.networkTransport == EnumTransport.TRANSPORT_HOMEWIFI)
                || (stateAndSettings.networkTransport == EnumTransport.TRANSPORT_WIFI)
            ) {
                summary.add("[${stateAndSettings.networkTransport.value}] ${stateAndSettings.wifi_ssid}")
                summary.add("[IP address] ${stateAndSettings.wifi_ip}")
                summary.add("[DNS servers] ${stateAndSettings.wifi_dns}")
            } else {
                summary.add("[WiFi] no")
            }
            summary.add("")

            summary.add("[Network transport] ${stateAndSettings.networkTransport.value}")

            summary.add("")

            summary.add("[VPN net] ${stateAndSettings.vpn_net}")
            if (reconnectionSettings.isReconnection) {
                summary.add("[VPN status] ${reconnectionSettings.generateMessage()}")

            } else {
                summary.add("[VPN status] ${stateAndSettings.vpn_state.value}")

            }
            summary.add("[VPN status] ${reconnectionSettings.generateMessage()}")


//            summary.add("[Assigned IP Address]")
//            properties.linkAddresses.forEach {
//                summary.add(it.address.hostAddress)
//            }
//            summary.add("")
//
//            summary.add("[DNS server]")
//            properties.dnsServers.forEach {
//                summary.add(it.hostAddress)
//            }
//            summary.add("")
//
//            summary.add("[Route]")
//            properties.routes.forEach {
//                summary.add(it.toString())
//            }
//            summary.add("")
//
//            summary.add("[SSL/TLS parameters]")
//            summary.add("PROTOCOL: ${parent.sslTerminal.socket.session.protocol}")
//            summary.add("SUITE: ${parent.sslTerminal.socket.session.cipherSuite}")

        }
        StatusPreference.STATUS.setValue(prefs, summary.reduce { acc, s -> acc + "\n" + s })
    }
}
