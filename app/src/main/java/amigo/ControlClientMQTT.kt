package com.app.amigo

import android.content.SharedPreferences
import android.net.*
import androidx.preference.PreferenceManager
import com.app.amigo.fragment.BoolPreference
import com.app.amigo.fragment.IntPreference
import com.app.amigo.fragment.StatusPreference
import com.app.amigo.layer.*
import com.app.amigo.misc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.sync.Mutex
import org.chromium.base.Log
import java.io.BufferedOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

internal enum class enumStateMQTT(val value: String) {
    MQTT_STOP("com.app.amigo.MQTT_STOP"),
    MQTT_CONNECTING("com.app.amigo.MQTT_CONNECTING"),
    MQTT_CONNECTED("com.app.amigo.MQTT_CONNECTED"),
    MQTT_DISCONNECTING("com.app.amigo.MQTT_DISCONNECTING"),
    MQTT_DISCONNECTED("com.app.amigo.MQTT_DISCONNECTED"),

    TEST("com.app.amigo.TEST"),
}


internal class ControlClientMQTT(internal val vpnService: MainService) :
    CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    class ReconnectionSettings(prefs: SharedPreferences) {
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

    class StateAndSettings() {
        internal var wifi_ssid: String = ""
        internal var wifi_ip: String = ""
        internal var wifi_dns: String = ""
        internal var cellular_state: String = ""
        internal var networkTransport: EnumTransport = EnumTransport.TRANSPORT_NONE
        internal var vpn_state: enumStateMQTT = enumStateMQTT.MQTT_DISCONNECTED
        internal var vpn_net: String = "no"
        internal var vpn_ip: String = ""
        internal var vpn_dns: String = ""
    }
    val TAG = "@!@ControlClientMQTT"
    private var prefs = PreferenceManager.getDefaultSharedPreferences(vpnService.applicationContext)

    internal val ccmqtt_channel =
        Channel<Int>(CONFLATED) // создает канал с размером буфера равный 1. Повторный вызов offer или send перезаписывает текущее значение в буфере, при этом приостановка корутины не происходит. Поэтому ресивер будет считывать всегда самое последнее значение из канала.

//    var number: Int = 0
//    @Synchronized
//    fun numberAdd(x: Int = 1) {
//        number = number + x
//    }

    internal lateinit var networkSetting: NetworkSetting
    internal lateinit var status: DualClientStatus
    internal lateinit var builder: VpnService.Builder
    internal lateinit var incomingBuffer: IncomingBuffer
    private var observer: NetworkObserver? = null
    internal val controlQueue = LinkedBlockingQueue<Any>()
    internal var logStream: BufferedOutputStream? = null
    internal val reconnectionSettings = ReconnectionSettings(prefs)
    internal val stateAndSettings = StateAndSettings()

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

    private val handler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "***** start exception exception.localizedMessage ${exception.localizedMessage}")
//        if (stateAndSettings.isMQTTConnected) {
//        Log.d(TAG, "INFO jobIncoming: ${jobIncoming}")
//        Log.d(TAG, "INFO jobControl: ${jobControl}")
//        }
        ccmqtt_launchJobRun()
    }

    init {
        initialize()
    }

    fun initialize() {
//        launchJobService()
        Log.d(TAG, "initialize")
//        prefs = PreferenceManager.getDefaultSharedPreferences(vpnService.applicationContext)
//        networkSetting = NetworkSetting(prefs)
//        status = DualClientStatus()
//        builder = vpnService.Builder()
//        incomingBuffer = IncomingBuffer(networkSetting.BUFFER_INCOMING, this)
//        controlQueue.clear()
//        isClosing = false
//        if(jobService == null)
//        launchJobService()
    }

    // 1 если stateService=Start и (MQTT_DISCONNECTED или MQTT_DISCONNECTING) и (TRANSPORT_WIFI или TRANSPORT_CELLULAR) --- коннект-впн
    // 2 если stateService=Start и (MQTT_CONNECTED или MQTT_CONNECTING) и (TRANSPORT_HOMEWIFI или TRANSPORT_NONE) --- дисконнект-впн
    // 3 если stateService=Stop и (MQTT_CONNECTED или MQTT_CONNECTING) --- дисконнект-впн
    internal fun ccmqtt_launchJobRun() {
        launch {
            // отправляем данные через канал
//            mutex.withLock {
            ccmqtt_channel.send(0)
//            }
        }
    }

    internal fun initJobRun() {
        Log.e(
            TAG,
            "***** ***** ***** ***** ***** ***** initJobRun()"
        )

        if ((getJobRun() == null)/* || (jobStateMachine?.isCompleted == true) || (jobStateMachine?.isCancelled == true)*/) {
//            reconnectionSettings.resetCount()
//            Log.d(TAG, "START JOB0")
            var stop = false
//            Log.d(TAG, "START JOB1")
            setJobRun(launch() {
                Log.d(TAG, "START JOB")
                if (ccmqtt_channel.isEmpty) ccmqtt_channel.send(0)
//                Log.d(TAG, "START JOB3")
                while (!stop) {
//                    Log.d(TAG, "START JOB4")
//                    if (!reconnectionSettings.isReconnection) channel.receive()
                    ccmqtt_channel.receive()
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



                    checkNetworks()
                } // while
                jobRun = null
                reconnectionSettings.resetCount()
                stateAndSettings.vpn_state = enumStateMQTT.MQTT_DISCONNECTED
//                checkNetworks()
                refreshStatus()
                Log.d(TAG, "*6* вышли из while **")
            })
        }

        Log.i(TAG, "***** STOP JOB !!!!!!!!!!!!!!!!")
    }

    fun checkNetworks() {

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

            summary.add("[MQTT net] ${stateAndSettings.vpn_net}")
            var MQTTstatus = ""
            if (reconnectionSettings.isReconnection) {
                summary.add("[MQTT status] ${reconnectionSettings.generateMessage()}")

            } else {
                when (stateAndSettings.vpn_state) {
                    enumStateMQTT.MQTT_CONNECTING -> MQTTstatus = "Подключение..."
                    enumStateMQTT.MQTT_CONNECTED -> MQTTstatus = "Подключено!!!"
                    enumStateMQTT.MQTT_DISCONNECTING -> MQTTstatus = "Отключение..."
                    enumStateMQTT.MQTT_DISCONNECTED -> MQTTstatus = "Отключено"
                }
                summary.add("[MQTT status] $MQTTstatus")
                if (stateAndSettings.vpn_state == enumStateMQTT.MQTT_CONNECTED) {
                    summary.add("[IP address] ${stateAndSettings.vpn_ip}")
                    summary.add("[DNS servers] ${stateAndSettings.vpn_dns}")
                }
            }
        }
        StatusPreference.STATUS.setValue(prefs, summary.reduce { acc, s -> acc + "\n" + s })
    }
}
