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
import kotlinx.coroutines.sync.Mutex
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
            "Reconnection: $triedCount/$initialCount"
        else
            "Reconnection: $currentCount"
    }
}

internal class StateAndSettings(prefs: SharedPreferences) {
    //    internal val sp = prefs
    internal var ssid: String = ""
    internal var networkTransport: CTransport = CTransport.TRANSPORT_NONE
    internal var state: enumStateVPN = enumStateVPN.VPN_DISCONNECTED
//        set(value) {
//            var str = value
//            if ((value == CTransport.TRANSPORT_WIFI.value) || (value == CTransport.TRANSPORT_HOMEWIFI.value))
//                str = value + ": " + ssid
//
////            if (StatusPreference.CONNECTEDVIA.getValue(sp) != str)
////                StatusPreference.CONNECTEDVIA.setValue(sp, str)
////            str = value.substringBefore(":")
////            }
//            field = value
//        }

//    internal var isVPNConnected: Boolean = false
//        set(data) {
//            stateChanged()
//        }
//    internal var isWIFIConnected: Boolean = false
//        set(data) {
//            stateChanged()
//        }
//
//    internal fun stateChanged() {
//        StatusPreference.CONNECTEDVIA.setValue(sp, NetworkTransport)
//    }

}

internal class ControlClient(internal val vpnService: MainService) :
    CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {
    private var TAG = "@!@ControlClient"
    private var prefs = PreferenceManager.getDefaultSharedPreferences(vpnService.applicationContext)

    //    private val queue: Queue<Throwable> = LinkedList()
    internal lateinit var networkSetting: NetworkSetting
    internal lateinit var status: DualClientStatus
    internal lateinit var builder: VpnService.Builder
    internal lateinit var incomingBuffer: IncomingBuffer
    private var observer: NetworkObserver? = null
    internal val controlQueue = LinkedBlockingQueue<Any>()
    internal var logStream: BufferedOutputStream? = null
    internal val reconnectionSettings = ReconnectionSettings(prefs)
    internal val stateAndSettings = StateAndSettings(prefs)

    internal lateinit var sslTerminal: SslTerminal
    private lateinit var sstpClient: SstpClient
    private lateinit var pppClient: PppClient
    internal lateinit var ipTerminal: IpTerminal

    var jobService: Job? = null
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

    private val handler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "CoroutineException " + exception)
//        if (stateAndSettings.isVPNConnected) {
        onCommand(exception)
//        }
    }

    init {
//        status = DualClientStatus()
        //        stateAndSettings.isVPNConnected = false
        initialize()
//        sslTerminal = SslTerminal(this)
//        sstpClient = SstpClient(this)
//        pppClient = PppClient(this)
//        ipTerminal = IpTerminal(this)
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

    internal fun onCommand(exception: Throwable?) {
        Log.e(
            TAG, "***** start fun " +
                    "onCommand {${exception?.localizedMessage}} " +
//                    "queue.size=${queue.size} " +
                    "mutex.isLocked={${mutex.isLocked}}"
        )
//        var exception: Throwable = Throwable("")
//        if (exception == null) { // не должно быть null
//            return
////            if (Exception != null) { // не должно быть null
////            exception = Exception
////            queue.add(exception)
//        }
//        if (/*!mutex.isLocked &&*/ queue.size > 0) {
//            exception = queue.poll()
//        } else return
//        Log.e(
//            TAG,
//            "!!!!!!!!!!!!!!!!! status.ppp = ${status.ppp}  status.sstp = ${status.sstp}"
//        )
        launch(handler) {
//            mutex.withLock {
//            Log.e(
//                TAG,
//                "*****exception: " + exception + " *****stateAndSettings.state: " + stateAndSettings.state
//            )
//            if (exception != null) {
//                Log.e(TAG, "*****exception.message: " + exception.message)
//                Log.e(TAG, "*****exception.localizedMessage: " + exception.localizedMessage)
//                Log.e(TAG, "*****exception.cause: " + exception.cause)
//            }
            Log.i(TAG, "***** start withLock")
            checkNetworks()
            Log.e(
                TAG, "onCommand: {${exception?.localizedMessage}} " +
                        "stateAndSettings.state: {${stateAndSettings.state}} " +
                        "stateAndSettings.networkTransport: {${stateAndSettings.networkTransport}}"
            )

            //1** если ACTION_DISCONNECT и curStateService=Any и (VPN_CONNECTED или VPN_CONNECTING) --- дисконнект-впн
            //2-- если ACTION_DISCONNECT и curStateService=Any и (VPN_DISCONNECTED или VPN_DISCONNECTING) --- ничего, итак остановлен
            //3** если ACTION_CONNECT и curStateService=Any и (VPN_CONNECTED или VPN_CONNECTING) и (TRANSPORT_HOMEWIFI или TRANSPORT_NONE) --- дисконнект-впн
            //4-- если ACTION_CONNECT и curStateService=Start и (VPN_CONNECTED или VPN_CONNECTING) и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- ничего, итак запущен
            //4.1** если ACTION_CONNECT и curStateService=Stop и (VPN_CONNECTED или VPN_CONNECTING) и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- дисконнект-впн
            //5-- если ACTION_CONNECT и curStateService=any и (VPN_DISCONNECTED или VPN_DISCONNECTING) и (TRANSPORT_HOMEWIFI или TRANSPORT_NONE) --- ничего, итак остановлен
            //6** если ACTION_CONNECT и curStateService=Start и (VPN_DISCONNECTED или VPN_DISCONNECTING) и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- коннект-впн
            //6.1-- если ACTION_CONNECT и curStateService=Stop и (VPN_DISCONNECTED или VPN_DISCONNECTING) и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- ничего, итак остановлен
            //7** если exception --- дисконнект-впн+коннект-впн

            //8**=3 если ACTION_WAIT и (VPN_CONNECTED или VPN_CONNECTING) и (TRANSPORT_HOMEWIFI или TRANSPORT_NONE) --- дисконнект-впн+VPN_DISCONNECTING+ACTION_WAIT (коннект-впн запустится автоматом)
            //9** если ACTION_WAIT и VPN_CONNECTING и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- таймер(ждем VPN_CONNECTED) потом Reconnect=0 иначе exception + Reconnect+=1
            //9.1-- если ACTION_WAIT и VPN_CONNECTED и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- ничего, итак запущен
            //10--=5 если ACTION_WAIT и (VPN_DISCONNECTED или VPN_DISCONNECTING) и (TRANSPORT_HOMEWIFI или TRANSPORT_NONE) --- ничего, итак остановлен
            //11** если ACTION_WAIT и VPN_DISCONNECTING и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- (таймер(ждем VPN_DISCONNECTED) потом)??? сразу ACTION_CONNECT
            //11.1-- если ACTION_WAIT и VPN_DISCONNECTED и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- ничего, итак остановлен

            if ( //1 если ACTION_DISCONNECT и curStateService=Any и (VPN_CONNECTED или VPN_CONNECTING) --- дисконнект-впн
                ((exception?.localizedMessage == VpnAction.ACTION_DISCONNECT.value)
                        && ((stateAndSettings.state == enumStateVPN.VPN_CONNECTED)
                        || (stateAndSettings.state == enumStateVPN.VPN_CONNECTING)))
                || //3 если ACTION_CONNECT и curStateService=Any и (VPN_CONNECTED или VPN_CONNECTING) и (TRANSPORT_HOMEWIFI или TRANSPORT_NONE) --- дисконнект-впн
                ((exception?.localizedMessage == VpnAction.ACTION_CONNECT.value)
                        && ((stateAndSettings.state == enumStateVPN.VPN_CONNECTED)
                        || (stateAndSettings.state == enumStateVPN.VPN_CONNECTING))
                        && ((stateAndSettings.networkTransport == CTransport.TRANSPORT_HOMEWIFI)
                        || (stateAndSettings.networkTransport == CTransport.TRANSPORT_NONE)))
                || //4.1 если ACTION_CONNECT и curStateService=Stop и (VPN_CONNECTED или VPN_CONNECTING) и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- дисконнект-впн
                ((exception?.localizedMessage == VpnAction.ACTION_CONNECT.value)
                        && (vpnService.curStateService != enumStateService.SERVICE_START)
                        && ((stateAndSettings.state == enumStateVPN.VPN_CONNECTED)
                        || (stateAndSettings.state == enumStateVPN.VPN_CONNECTING))
                        && ((stateAndSettings.networkTransport == CTransport.TRANSPORT_WIFI)
                        || (stateAndSettings.networkTransport == CTransport.TRANSPORT_CELLULAR)))
                || //7 если exception и (VPN_CONNECTED или VPN_CONNECTING)
                ((exception?.localizedMessage != VpnAction.ACTION_CONNECT.value)
                        && (exception?.localizedMessage != VpnAction.ACTION_CONNECT.value))
//                        && ((stateAndSettings.state == enumStateVPN.VPN_CONNECTED)
//                        || (stateAndSettings.state == enumStateVPN.VPN_CONNECTING))
            ) {
                stateAndSettings.state = enumStateVPN.VPN_DISCONNECTING
                if ((exception?.localizedMessage == VpnAction.ACTION_CONNECT.value) // если не exception // if #7
                    || (exception?.localizedMessage == VpnAction.ACTION_CONNECT.value)
                ) {
                    Log.i(TAG, "if #1 #3 #4.1")
                } else {
                    Log.i(TAG, "if #7")
                }
                Log.i(TAG, "дисконнект-впн()")
                disconnect()
                if ((exception?.localizedMessage == VpnAction.ACTION_CONNECT.value) // если не exception // if #7
                    || (exception?.localizedMessage == VpnAction.ACTION_CONNECT.value)
                ) {
                    // ждем VPN_DISCONNECTED
                    Log.i(TAG, "wait VPN_DISCONNECTED")
                    val result = withTimeoutOrNull(10_000) {
                        while (true) {
                            if (vpnService.curStateService != enumStateService.SERVICE_START) {
                                Log.i(TAG, "Service=Stop - exit!!!")
                                return@withTimeoutOrNull
                            }
                            if (stateAndSettings.state == enumStateVPN.VPN_DISCONNECTING) {
                                Log.i(
                                    TAG,
                                    "wait VPN_DISCONNECTED stateAndSettings.state=${stateAndSettings.state}"
                                )
                                delay(1000)
                            } else if (stateAndSettings.state == enumStateVPN.VPN_DISCONNECTED) {
                                Log.i(TAG, "VPN_DISCONNECTED!!!")
                                return@withTimeoutOrNull
                            } else {
                                Log.i(TAG, "other state - exit!!!")
                                return@withTimeoutOrNull
                            }
                        }
                    }
                    if (result == null) {
                        Log.i(TAG, "Timeout VPN_DISCONNECTING")
                    }
//                        return@withLock
//                        return@launch
                } else delay(1000)
            }
            if ( //6+7 если (ACTION_CONNECT или exception) и curStateService=Start и (VPN_DISCONNECTED или VPN_DISCONNECTING) и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- коннект-впн
                (((exception?.localizedMessage == VpnAction.ACTION_CONNECT.value)
                        || ((exception?.localizedMessage != VpnAction.ACTION_CONNECT.value)
                        && (exception?.localizedMessage != VpnAction.ACTION_CONNECT.value)))
                        && (vpnService.curStateService == enumStateService.SERVICE_START)
                        && ((stateAndSettings.state == enumStateVPN.VPN_DISCONNECTED)
                        || (stateAndSettings.state == enumStateVPN.VPN_DISCONNECTING))
                        && ((stateAndSettings.networkTransport == CTransport.TRANSPORT_WIFI)
                        || (stateAndSettings.networkTransport == CTransport.TRANSPORT_CELLULAR)))
            ) {
                stateAndSettings.state = enumStateVPN.VPN_CONNECTING
//                if((exception.localizedMessage == VpnAction.ACTION_CONNECT.value) // если не exception // if #7
//                    || (exception.localizedMessage == VpnAction.ACTION_CONNECT.value)){
//                    Log.i(TAG, "if #6")
//                } else {
//                    Log.i(TAG, "if #7")
//                }
                Log.i(TAG, "коннект-впн()")
                connect()
                // ждем VPN_CONNECTED
                Log.i(TAG, "wait VPN_CONNECTED else tryReconnecting")
                val result = withTimeoutOrNull(10_000) {
                    while (true) {
                        if (vpnService.curStateService != enumStateService.SERVICE_START) {
                            Log.i(TAG, "Service=Stop - exit!!!")
                            return@withTimeoutOrNull
                        }
                        if (stateAndSettings.state == enumStateVPN.VPN_CONNECTING) {
                            Log.i(
                                TAG,
                                "wait VPN_CONNECTED stateAndSettings.state=${stateAndSettings.state}"
                            )
                            delay(1000)
                        } else if (stateAndSettings.state == enumStateVPN.VPN_CONNECTED) {
                            Log.i(TAG, "VPN_CONNECTED!!!")
                            reconnectionSettings.resetCount()
                            return@withTimeoutOrNull
                        } else {
                            Log.i(TAG, "other state - exit!!!")
                            return@withTimeoutOrNull
                        }
                    }
                }
                if (result == null) {
                    Log.i(TAG, "Timeout VPN_CONNECTING -> tryReconnecting")
                    reconnectionSettings.consumeCount()
                    throw Exception("tryReconnecting")
                }
            }

//            return@withLock
            return@launch
            when (exception?.localizedMessage) {
                VpnAction.ACTION_CONNECT.value -> { // запуск VPN
                    if (stateAndSettings.state == enumStateVPN.VPN_DISCONNECTED) {
                        stateAndSettings.state = enumStateVPN.VPN_CONNECTING
                        run()

                        val result = withTimeoutOrNull(10_000) {
                            while (true) {
                                if (stateAndSettings.state == enumStateVPN.VPN_CONNECTED) {
                                    return@withTimeoutOrNull
                                } else {
                                    delay(200)
                                }
                            }
                        }
                        if (result == null) {
                            if (reconnectionSettings.isEnabled &&
                                BoolPreference.HOME_CONNECTOR.getValue(prefs)
                            ) {
                                if (reconnectionSettings.isRetryable) {
                                    tryReconnection()
//            return@withLock
                                    return@launch
                                } else {
                                    inform("Exhausted retry counts", null)
//                                makeNotification(
//                                    NOTIFICATION_ID,
//                                    "Failed to reconnect: Exhausted retry counts"
//                                )
                                    bye()
                                }
                            }
                        }
                    } else if (stateAndSettings.state == enumStateVPN.VPN_DISCONNECTING) {
                        onCommand(exception)
                    }
                }
                VpnAction.ACTION_DISCONNECT.value -> { // остановка VPN
                    if (stateAndSettings.state == enumStateVPN.VPN_CONNECTED) {
                        stateAndSettings.state = enumStateVPN.VPN_DISCONNECTING
//                            stateAndSettings.isVPNConnected = false
                        controlQueue.add(0)
                        if (exception != null && exception !is SuicideException) {
//                        observer?.close() // +22.12.2021
                            inform("An unexpected event occurred", exception)
                            Log.e(TAG, "!is SuicideException: " + exception)
                        }
                        if (exception != null)
                            when (exception.localizedMessage) {
                                "com.app.amigo.DISCONNECT", "No address associated with hostname"/*, "Kill this client as intended"*/ -> {
                                    StatusPreference.STATUS.setValue(prefs, "")
//                                observer?.close()
                                }
                            }
                        // release ConnectivityManager resource
                        observer?.close() // смотри внутрь .close()
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
                                    delay(1)
                                } else break
                            }
                        }
                        // avoid jobControl being stuck with socket
                        sslTerminal.release()
                        // ensure jobControl is completed
                        jobControl?.cancel()
                        bye()
                    } else if (stateAndSettings.state == enumStateVPN.VPN_CONNECTING) {
                        onCommand(exception)
                    }
                }
                else -> { // перезапуск VPN
                    stateAndSettings.state = enumStateVPN.VPN_DISCONNECTED
//                        stateAndSettings.isVPNConnected = false
                    Log.d(TAG, "An unexpected event occurred " + exception.toString())
//                if (!isClosing)
//                    isClosing = true
                    controlQueue.add(0)
                    if (exception != null && exception !is SuicideException) {
//                        observer?.close() // +22.12.2021
                        inform("An unexpected event occurred", exception)
                        Log.e(TAG, "!is SuicideException: " + exception)
                    }
                    if (exception != null)
                        when (exception.localizedMessage) {
                            "com.app.amigo.DISCONNECT", "No address associated with hostname"/*, "Kill this client as intended"*/ -> {
                                StatusPreference.STATUS.setValue(prefs, "")
//                                observer?.close()
                            }
                        }
                    // release ConnectivityManager resource
                    observer?.close() // смотри внутрь .close()
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
                                delay(1)
                            } else break
                        }
                    }
                    // avoid jobControl being stuck with socket
                    sslTerminal.release()
                    // ensure jobControl is completed
                    jobControl?.cancel()


                    if (reconnectionSettings.isEnabled &&
                        BoolPreference.HOME_CONNECTOR.getValue(prefs)
                    ) {
                        if (reconnectionSettings.isRetryable) {
                            tryReconnection()
//            return@withLock
                            return@launch
                        } else {
                            inform("Exhausted retry counts", null)
//                                makeNotification(
//                                    NOTIFICATION_ID,
//                                    "Failed to reconnect: Exhausted retry counts"
//                                )
                            bye()
                        }
                    }

                }
            }
//            }
        }
        Log.i(TAG, "***** end withLock")

        Log.e(
            TAG,
            "*****end fun onCommand: stateAndSettings.state: ${stateAndSettings.state}"
        )
    }

    private fun connect() {
        Log.e(TAG, "*****start connect*****")
        initialize()
        if (networkSetting.LOG_DO_SAVE_LOG && logStream == null) {
            prepareLog()
        }
//        inform("Establish VPN connection", null)
        prepareLayers()
        launchJobIncoming()
        launchJobControl()
    }

    private suspend fun disconnect() {
        Log.e(TAG, "*****start disconnect*****")
        controlQueue.add(0)
        // release ConnectivityManager resource
        observer?.close() // смотри внутрь .close()
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
    }

    private fun bye() {
        Log.e(TAG, "*****start bye*****")
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
                Log.e(TAG, "stateAndSettings.state: " + stateAndSettings.state)
                Log.d(TAG, "delay_ms: " + delay_ms)
                if (delay_ms > 0) delay(delay_ms)
//                if (!isClosing)
                run()
            }
        }
    }

    private fun run() {
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
//        Log.d(TAG, "run fun launchJobService isActive: " + if (isActive) "true" else "false")
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
        Log.d(TAG, "run fun launchJobIncoming isActive: " + if (isActive) "true" else "false")
        jobIncoming = launch(handler) {
            Log.d(TAG, "launch in launchJobIncoming")
            Log.d(
                TAG,
                "run while in launchJobIncoming isActive: " + if (isActive) "true" else "false"
            )
            while (isActive) {
//                Log.d(TAG, "while in launchJobIncoming")
                sstpClient.proceed()
                pppClient.proceed()
                if ((stateAndSettings.state == enumStateVPN.VPN_DISCONNECTING) || (stateAndSettings.state == enumStateVPN.VPN_DISCONNECTED)) {
                    Log.e(
                        TAG,
                        "***** launchJobIncoming CALL_DISCONNECT_IN_PROGRESS_1 ***** ${status.sstp}"
                    )
                    status.sstp = SstpStatus.CALL_DISCONNECT_IN_PROGRESS_1
                }
            }
            Log.d(TAG, "stop while in launchJobIncoming")
        }
    }

    private fun launchJobControl() {
        Log.d(TAG, "run fun launchJobControl isActive: " + if (isActive) "true" else "false")
        jobControl = launch(handler) {
            Log.d(TAG, "launch in launchJobControl")
            val controlBuffer = ByteBuffer.allocate(CONTROL_BUFFER_SIZE)
            Log.d(
                TAG,
                "run while in launchJobControl isActive: " + if (isActive) "true" else "false"
            )
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
                sslTerminal.send(controlBuffer)
            }
            Log.d(TAG, "stop while in launchJobControl")
        }
    }

    private fun launchJobEncapsulate(channel: Channel<ByteBuffer>) {
        Log.d(
            TAG,
            "run fun launchJobEncapsulate isActive: " + if (isActive) "true" else "false"
        )
        jobEncapsulate = launch(handler) { // buffer packets
            Log.d(TAG, "launch in launchJobEncapsulate")
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
            Log.d(
                TAG,
                "run while in launchJobEncapsulate isActive: " + if (isActive) "true" else "false"
            )
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
                sslTerminal.send(dataBuffer)
            }
            Log.d(TAG, "stop while in launchJobEncapsulate")
        }
    }

    internal fun launchJobData() {
        Log.d(TAG, "run fun launchJobData isActive: " + if (isActive) "true" else "false")
        jobData = launch(handler) {
            Log.d(TAG, "launch in launchJobData")
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

            Log.d(
                TAG,
                "run while in launchJobData isActive: " + if (isActive) "true" else "false"
            )
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
            Log.d(TAG, "stop while in launchJobData")
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
        var networkTransport: CTransport = CTransport.TRANSPORT_NONE
        val connectivityManager =
            vpnService.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val allNetworks = connectivityManager.allNetworks
        allNetworks.forEachIndexed { index, network ->
            Log.d(
                TAG,
                "NetworkCapabilities0: #$index - " + connectivityManager.getNetworkCapabilities(
                    network
                ).toString()
            )
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val wifiManager =
                    vpnService.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val connectionInfo = wifiManager.connectionInfo
                ssid = connectionInfo.ssid.replace("\"", "")
//                Log.d(TAG, "connectionInfo.ssid: ${ssid}")
                if (connectionInfo != null &&
                    !TextUtils.isEmpty(ssid)
                ) {
//                Log.d(
//                    TAG,
//                    "SetPreference.HOME_WIFI_SUITES.getValue: ${
//                        SetPreference.HOME_WIFI_SUITES.getValue(prefs)
//                    }"
//                )
                    if (SetPreference.HOME_WIFI_SUITES.getValue(prefs).contains(ssid) &&
                        BoolPreference.SELECT_HOME_WIFI.getValue(prefs)
                    ) {
                        networkTransport = CTransport.TRANSPORT_HOMEWIFI
                    } else {
                        networkTransport = CTransport.TRANSPORT_WIFI
                    }
                }
            } else if ((networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) && ((networkTransport != CTransport.TRANSPORT_HOMEWIFI) || (networkTransport != CTransport.TRANSPORT_WIFI))) {
                networkTransport =
                    CTransport.TRANSPORT_CELLULAR
            }
        }
        stateAndSettings.ssid = ssid
        stateAndSettings.networkTransport = networkTransport
    }
}
