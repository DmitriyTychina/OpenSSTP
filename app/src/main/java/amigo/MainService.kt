package com.app.amigo

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.*
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.app.amigo.fragment.BoolPreference
import com.app.amigo.fragment.IntPreference
import com.app.amigo.fragment.StrPreference
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

internal enum class EnumStateService(val value: String) {
    ENUM_NULL("com.app.amigo.NULL"),
    DIALOG_ACCOUNT("com.app.amigo.DIALOG_ACCOUNT"),
    DIALOG_VPN("com.app.amigo.DIALOG_VPN"),
    SERVICE_START("com.app.amigo.SERVICE_START"),

    TEST("com.app.amigo.TEST"),

    SERVICE_STOPPING("com.app.amigo.SERVICE_STOPPING"),
    SERVICE_STOPPED("com.app.amigo.SERVICE_STOPPED"),
}

internal enum class EnumAction(val value: String) {
    ACTION_CONNECT("com.app.amigo.CONNECT"),
    ACTION_DISCONNECT("com.app.amigo.DISCONNECT"),
}

internal enum class EnumTransport(val value: String) {
    TRANSPORT_WIFI("WIFI"),
    TRANSPORT_HOMEWIFI("HOME WIFI"),
    TRANSPORT_CELLULAR("CELLULAR"),
    TRANSPORT_NONE("NONE"),
}


internal class MainService : VpnService(), MqttCallback {
    private var TAG = "@!@MainService"
    private var broadcastReceiver: MainBroadcastReceiver? = null
    private var callbackRequestWIFI: ConnectivityManager.NetworkCallback? = null
    private var callbackRequestCELLULAR: ConnectivityManager.NetworkCallback? = null

    //    private var builder: NotificationCompat.Builder? = null
//    internal val CHANNEL_ID = "HomeClient"
    var controlClient: ControlClientVPN? = null

    //    internal var stateService = EnumStateService.ENUM_NULL
    internal val helper by lazy { NotificationHelper(this) }

//    lateinit var cm: ConnectivityManager

    private var MQTTclient: MqttAndroidClient? = null
    private lateinit var options: MqttConnectOptions
    private var serverUri: String? = null
    private lateinit var prefs: SharedPreferences
    private var mqttIP: String = ""
    private var mqttPort: String = ""
    private var mqttLogin: String = ""
    private var mqttPass: String = ""
    private var mqttDevice: String = ""

    override fun connectionLost(cause: Throwable?) {}

    @Throws(Exception::class)
    override fun messageArrived(topic: String, message: MqttMessage) {
        Log.d(TAG, "topic: $topic value: $message")
        Log.d(TAG, message.toString())
//        if (message.toString() == "") {
//            return
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/tts/request") {
//            if (speakOut(message.toString())) publish("comm/tts/request", "")
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/tts/stop") {
//            if (isTrue(message.toString()) == 1) {
//                if (speakStop()) publish("comm/tts/stop", "")
//            }
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/display/brightness") {
//            if (isNumber(message.toString())) {
//                if (setBrightness(message.toString().toInt())) {
//                    publish("comm/display/brightness", "")
//                    publish("info/display/brightness", message.toString())
//                    publish("info/display/mode", "manual")
//                }
//            }
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/display/mode") {
//            val num = isTrue(message.toString())
//            if (num == 1 || num == 2) {
//                if (setBrightnessMode(if (num == 1) "auto" else "manual")) {
//                    publish("comm/display/mode", "")
//                    publish("info/display/mode", if (num == 1) "auto" else "manual")
//                }
//            }
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/display/timeOff") {
//            if (isNumber(message.toString())) {
//                if (setTimeScreenOff(message.toString().toInt())) publish(
//                    "comm/display/timeOff",
//                    ""
//                )
//            }
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/display/toWake") {
//            val num = isTrue(message.toString())
//            if (num == 1 || num == 2) {
////                set(num);
//            }
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/call/number") {
//            if (isNumber(message.toString())) {
//                if (setCall(message.toString())) publish("comm/call/number", "")
//            }
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/call/end") {
//            if (isTrue(message.toString()) == 1) {
//                if (disconnectCall()) publish("comm/call/end", "")
//            }
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/other/home") {
//            if (isTrue(message.toString()) == 1) {
//                if (setHome()) publish("comm/other/home", "")
//            }
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/other/openURL") {
//            if (openURL(message.toString())) publish("comm/other/openURL", "")
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/other/openURL") {
//            if (openURL(message.toString())) publish("comm/other/openURL", "")
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/display/turnOnOff") {
//            if (isTrue(message.toString()) == 1) {
//                turnOnScreen()
//                publish("comm/display/turnOnOff", "")
//            } else if (isTrue(message.toString()) == 2) {
//                turnOffScreen()
//                publish("comm/display/turnOnOff", "")
//            }
//        }
//        if (topic == clientId + "/" + mqttDevice + "/comm/other/vibrate") {
//            if (isNumber(message.toString())) {
//                if (vibrate(message.toString().toInt())) publish("comm/other/vibrate", "")
//            }
//        }
//        if (topic.contains(clientId + "/" + mqttDevice + "/comm/audio/")) {
////            Log.i(TAG,"TOPIC : "+topic);
//            val key: String =
//                topic.replace(clientId + "/" + mqttDevice + "/comm/audio/".toRegex(), "")
//            //            Log.i(TAG,key);
//            if (isNumber(message.toString())) {
//                if (setVolume(message.toString().toInt(), key)) {
//                    publish("comm/audio/$key", "")
//                    publish("info/audio/$key", message.toString())
//                }
//            }
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {}

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Log.d(TAG, "onCreate")
//        initNoti()
//        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        initSettings()
        initControlClient()
        initMQTT()
        initBroadReceiver()
//        initTTS()
//        initSensors()
        initNetworkRequest()
        BoolPreference.HOME_CONNECTOR.setEnabled(true)
    }

    private fun initSettings() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        mqttIP = StrPreference.MQTT_HOST.getValue(prefs)
        mqttPort = IntPreference.MQTT_PORT.getValue(prefs).toString()
        mqttLogin = StrPreference.MQTT_USER.getValue(prefs)
        mqttPass = StrPreference.MQTT_PASS.getValue(prefs)
        mqttDevice = StrPreference.HOME_USER.getValue(prefs) // ???
        serverUri = "tcp://$mqttIP:$mqttPort"
    }

    private fun initControlClient() {
        if (controlClient == null) {
            Log.d(TAG, "new ControlClient")
            controlClient = ControlClientVPN(this).also {
//                helper.updateNotification("update")
//                beForegrounded() // уведомление о работе vpn
//                Log.d(TAG, "beForegrounded!!!!!")
                it.initJobRun()
            }
        }
    }

    private fun initMQTT() {
        Log.i(TAG, "Start initMQTT")
//        MQTTclient = MqttAndroidClient(application, serverUri, mqttDevice)
//        options = MqttConnectOptions()
//        options.isAutomaticReconnect = true
//        options.isCleanSession = false
//        if (!mqttLogin.isNullOrBlank()) options.userName = mqttLogin
//        if (!mqttPass.isNullOrBlank()) options.password = mqttPass.toCharArray()
//        MQTTclient!!.setCallback(this)
    }

    private fun initBroadReceiver() {
//        val filter = IntentFilter()
//
//        if (settings.getBoolean(
//                "event_wifi",
//                false
//            )
//        ) filter.addAction("android.net.wifi.STATE_CHANGE")
//        if (settings.getBoolean(
//                "event_call",
//                false
//            )
//        ) filter.addAction("android.intent.action.PHONE_STATE")
////        if(settings.getBoolean("event_call", false)) filter.addAction("android.intent.action.PHONE_STATE");
//        //        if(settings.getBoolean("event_call", false)) filter.addAction("android.intent.action.PHONE_STATE");
//        if (settings.getBoolean(
//                "event_sms",
//                false
//            )
//        ) filter.addAction("android.provider.Telephony.SMS_RECEIVED")
//
//        if (settings.getBoolean("event_battery", false)) {
//            filter.addAction("android.intent.action.BATTERY_LOW")
//            filter.addAction(Intent.ACTION_POWER_CONNECTED)
//            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
//            filter.addAction(Intent.ACTION_BATTERY_CHANGED)
//        }
//        if (settings.getBoolean("event_display", false)) {
//            filter.addAction(Intent.ACTION_SCREEN_ON)
//            filter.addAction(Intent.ACTION_SCREEN_OFF)
//        }
//
//        broadcastReceiver = MainReceiver()
//        registerReceiver(broadcastReceiver, filter)

        if (broadcastReceiver == null) {
            val filter = IntentFilter()
//            filter.addAction("android.net.wifi.STATE_CHANGE")
//            filter.addAction("android.intent.action.PHONE_STATE")
//            filter.addAction("android.intent.action.PHONE_STATE")
//            filter.addAction("android.provider.Telephony.SMS_RECEIVED")
//            filter.addAction("android.intent.action.BATTERY_LOW")
//            filter.addAction(Intent.ACTION_POWER_CONNECTED)
//            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
//            filter.addAction(Intent.ACTION_BATTERY_CHANGED)
//            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            broadcastReceiver = MainBroadcastReceiver()
            registerReceiver(broadcastReceiver, filter)
        }
    }

    @SuppressLint("WrongConstant")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val valHOME_CONNECTOR =
            BoolPreference.HOME_CONNECTOR.getValue(prefs)
//        var newState: EnumStateService
////        val old_state = state
////        Log.d(TAG, "intent = " + intent)
        Log.d(TAG, "intent.action = " + intent?.action)
//        if (intent != null && intent.action != null) intent.action.let {
//            Log.d(TAG, "intent.action = " + it)
//            newState = EnumStateService.valueOf(it!!)
//            //                state = it
//            Log.d(TAG, "newState = " + newState)
//        } else {
//            Log.e(TAG, "intent.action = null")
//            newState = EnumStateService.ENUM_NULL
//        }

//        if (!valHOME_CONNECTOR) {
////        } else {
////            if (controlClient != null) {
////                controlClient?.jobService?.cancel()
////                controlClient?.onCommand(null)
////                controlClient = null
////            }
//        }

//        if (newState != EnumStateService.ENUM_NULL) {
////            queue.add(newState)
//            Log.d(TAG, "valHOME_CONNECTOR = " + valHOME_CONNECTOR)
//        }
//        StatusPreference.CONNECTEDVIA.setValue(
//            PreferenceManager.getDefaultSharedPreferences(
//                applicationContext
//            ), queue.size.toString()
//        )

//        stateService = newState

        if (EnumStateService.SERVICE_START.name == intent?.action ?: false) {
            Log.d(TAG, "service start!!!")
            controlClient?.launchJobRun(3) // onExeption(Throwable(EnumAction.ACTION_DISCONNECT.value))
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                helper.getNotification()
            )
            Toast.makeText(applicationContext, "service start!!!", Toast.LENGTH_SHORT).show()
            return START_STICKY
        } else if (EnumStateService.SERVICE_STOPPING.name == intent?.action ?: false) {
            Log.d(TAG, "service stopping!!!")
            controlClient?.launchJobRun(4) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            return START_STICKY
        } else if (EnumStateService.SERVICE_STOPPED.name == intent?.action ?: false) {
            Log.d(TAG, "service stopped!!!")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true)
            }
            stopSelf()
            Toast.makeText(applicationContext, "service stop!!!", Toast.LENGTH_SHORT).show()
            return START_NOT_STICKY
        } else {
            if (valHOME_CONNECTOR) {
                return START_STICKY
            } else {
                return START_NOT_STICKY
            }
        }

//        val status = intent?.getStringExtra("action")
//        Log.d(TAG, "intent?.getStringExtra: status :" + status)
//        when (status) {
//            BCAction.ACTION_WIFI_STATE_CHANGED.value -> {
//                controlClient?.checkNetworks()
////                if (hasConnection()) {
////                    startMQTT()
////                }
//                return START_STICKY
//            }
//        }

//        if ((enumStateService.SERVICE_START.value == intent?.action ?: false) ||
//            (VpnAction.ACTION_WIFI_STATE_CHANGED.value == intent?.action ?: false)
//        ) {
////            controlClient?.checkNetworks()
//            if (!state ||
//                (VpnAction.ACTION_WIFI_STATE_CHANGED.value == intent?.action ?: false)
//            ) {
//                Toast.makeText(applicationContext, "service start!!!", Toast.LENGTH_LONG).show()
//                Log.d(TAG, "fStartService")
//                state = true
////                controlClient?.onCommand(Throwable("com.app.amigo.CONNECT"))
//                if (controlClient == null) {
//                    Log.d(TAG, "new ControlClient")
//                    controlClient = ControlClient(this).also {
//                        startForeground(
//                            NotificationHelper.NOTIFICATION_ID,
//                            helper.getNotification()
//                        )
////        helper.updateNotification("update")
////                beForegrounded() // уведомление о работе vpn
////                Log.d(TAG, "beForegrounded!!!!!")
//                    }
//                }
//                controlClient!!.run()
//                return START_STICKY
//            } else {
//                return START_NOT_STICKY
//            }
//        } else if (VpnAction.ACTION_DISCONNECT.value == intent?.action ?: false) {
//            if (state) {
//                Log.d(TAG, "fStopService")
//                state = false
//                Log.d(TAG, "ACTION_DISCONNECT")
//                controlClient?.onCommand(Throwable("com.app.amigo.DISCONNECT"))
////                controlClient = null // 21.12.2021+
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    stopForeground(true)
//                }
//                stopSelf()
//            }
//            return START_NOT_STICKY
//        } else {
//            return START_NOT_STICKY
//        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        callbackRequestWIFI?.let {
            getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(
                it
            )
        }
        callbackRequestWIFI = null
        callbackRequestCELLULAR?.let {
            getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(
                it
            )
        }
        callbackRequestCELLULAR = null
//        cm.unregisterNetworkCallback(networkCallback)
        unregisterReceiver(broadcastReceiver)
//        controlClient?.onCommand(null)
//        controlClient = null
        Toast.makeText(applicationContext, "service stop!!!", Toast.LENGTH_LONG).show()
        controlClient?.logStream?.close()
        BoolPreference.HOME_CONNECTOR.setEnabled(true)
    }

//    fun proceed() {
//        TODO("Not yet implemented")
//    }

//    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
//        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//        .build()
//    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
//
//        // Called when the framework connects and has declared a new network ready for use.
//        override fun onAvailable(network: Network) {
//            super.onAvailable(network)
//            Log.d(TAG, "onAvailable: $network")
//            //*******
//            val connManager =
//                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
////                val networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
//            val linkProperties = connManager.getLinkProperties(network)
//            Log.d(TAG, "LinkProperties $linkProperties")
//
//            val wifiManager =
//                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//            val connectionInfo = wifiManager.connectionInfo
//            if (connectionInfo != null) {
//                Log.d(TAG, "connectionInfo: $connectionInfo")
//            }
//            //*******
//            val capabilities = cm.getNetworkCapabilities(network)
//            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
//                controlClient?.stateAndSettings?.NetworkTransport = CTransport.TRANSPORT_WIFI.value
//            } else if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
//                controlClient?.stateAndSettings?.NetworkTransport =
//                    CTransport.TRANSPORT_CELLULAR.value
//            } else if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
//                controlClient?.stateAndSettings?.NetworkTransport = CTransport.TRANSPORT_VPN.value
//            } else {
//                controlClient?.stateAndSettings?.NetworkTransport = CTransport.TRANSPORT_NONE.value
//            }
//            Log.d(TAG, "NetworkCapabilities: " + controlClient?.stateAndSettings?.NetworkTransport)
//        }
//
//        // Called when a network disconnects or otherwise no longer satisfies this request or callback
//        override fun onLost(network: Network) {
//            super.onLost(network)
//            Log.d(TAG, "onLost: ${network}")
//        }
//    }

    private fun initNetworkRequest() {
        callbackRequestWIFI = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.e(TAG, "onAvailable WiFi: ${network}")
                Log.d(
                    TAG,
                    "NetworkCapabilities onAvailable: " + getSystemService(ConnectivityManager::class.java).getNetworkCapabilities(
                        network
                    )
                        .toString()
                )
                controlClient?.checkNetworks()
//                controlClient?.launchJobRun(5) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.e(TAG, "onLost WiFi: ${network}")
                controlClient?.checkNetworks()
//                controlClient?.launchJobRun(6) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                Log.e(TAG, "onLinkPropertiesChanged WiFi: ${network} ---- ${linkProperties}")
                if (controlClient != null) {
                    if (controlClient!!.stateAndSettings.wifi_dns != linkProperties.dnsServers.toString()) {
                        controlClient!!.stateAndSettings.wifi_dns =
                            linkProperties.dnsServers.toString()
                        controlClient!!.refreshStatus()
                    }
                }
            }
        }

        callbackRequestCELLULAR = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.e(TAG, "onAvailable CELLULAR: ${network}")
                Log.d(
                    TAG,
                    "NetworkCapabilities onAvailable: " + getSystemService(ConnectivityManager::class.java).getNetworkCapabilities(
                        network
                    )
                        .toString()
                )
                controlClient?.checkNetworks()
//                controlClient?.launchJobRun(7) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.e(TAG, "onLost CELLULAR: ${network}")
                controlClient?.checkNetworks()
//                controlClient?.launchJobRun(8) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                Log.e(TAG, "onLinkPropertiesChanged CELLULAR: ${network} ---- ${linkProperties}")
            }
        }

        val requestWIFI = NetworkRequest.Builder().let {
            it.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            it.build()
        }
        callbackRequestWIFI?.let {
            getSystemService(ConnectivityManager::class.java).registerNetworkCallback(
                requestWIFI,
                it
            )
        }

        val requestCELLULAR = NetworkRequest.Builder().let {
            it.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            it.build()
        }
        callbackRequestCELLULAR?.let {
            getSystemService(ConnectivityManager::class.java).registerNetworkCallback(
                requestCELLULAR,
                it
            )
        }
    }


}
