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
import kotlinx.coroutines.delay
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.xml.sax.Parser
import java.io.IOException
import java.io.UnsupportedEncodingException

internal enum class EnumStateService(val value: String) {
    ENUM_NULL("com.app.amigo.NULL"),
    DIALOG_ACCOUNT("com.app.amigo.DIALOG_ACCOUNT"),
    DIALOG_VPN("com.app.amigo.DIALOG_VPN"),
    SERVICE_START("com.app.amigo.SERVICE_START"),
    SERVICE_STARTED("com.app.amigo.SERVICE_STARTED"),

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

internal enum class EnumStateTransport(val value: String) {
    STATE_TRANSPORT_NULL("NULL"),
    STATE_TRANSPORT_CONNECT("Connect"),
    STATE_TRANSPORT_LOST("Lost"),
//    STATE_TRANSPORT_NULL("NONE"),
}

internal class MainService : VpnService(), MqttCallback {
    private var TAG = "@!@MainService"
    private var broadcastReceiver: MainBroadcastReceiver? = null
    private var callbackRequestWIFI: ConnectivityManager.NetworkCallback? = null
    internal var stateWIFI: EnumStateTransport = EnumStateTransport.STATE_TRANSPORT_NULL
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
    private val clientId = "HomeClient"

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Log.d(TAG, "onCreate")
//        initNoti()
//        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        initSettings()
        initControlClient()
//        initMQTT()
        initReceiver()
//        initTTS()
//        initSensors()
        initNetworkRequest()
        BoolPreference.HOME_CONNECTOR.setEnabled(true)
    }

    private fun initSettings() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
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
        mqttIP = StrPreference.MQTT_HOST.getValue(prefs)
        mqttPort = IntPreference.MQTT_PORT.getValue(prefs).toString()
        mqttLogin = StrPreference.MQTT_USER.getValue(prefs)
        mqttPass = StrPreference.MQTT_PASS.getValue(prefs)
        mqttDevice = StrPreference.HOME_USER.getValue(prefs) // ???
        serverUri = "tcp://$mqttIP:$mqttPort"
        MQTTclient = MqttAndroidClient(application, serverUri, mqttDevice)
        options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = false
//        options.mqttVersion = 4

        if (mqttLogin.isNotBlank()) options.userName = mqttLogin
        if (mqttPass.isNotBlank()) options.password = mqttPass.toCharArray()

        MQTTclient!!.setCallback(this)


    }

    private fun initReceiver() {
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

        if (EnumStateService.SERVICE_START.name == intent?.action ?: false) {
            Log.d(TAG, "service start!!!")
            controlClient?.launchJobRun(0) // onExeption(Throwable(EnumAction.ACTION_DISCONNECT.value))
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                helper.getNotification()
            )
            Toast.makeText(applicationContext, "service start!!!", Toast.LENGTH_SHORT).show()
            return START_STICKY
        } else if (EnumStateService.SERVICE_STOPPING.name == intent?.action ?: false) {
            Log.d(TAG, "service stopping!!!")
//            MQTTclient!!.disconnect()
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
        } else if (EnumStateService.SERVICE_STARTED.name == intent?.action ?: false) {
            Log.d(TAG, "service started!!! start mqtt!!!")

//            val token = MQTTclient!!.connect(options)
//            token.actionCallback = object : IMqttActionListener {
//                override fun onSuccess(asyncActionToken: IMqttToken) {
//                    Log.d(TAG, "actionCallback-onSuccess: Connection")
//                    setSubscribe()
//                    pubOne()
//                    Log.d(TAG, "actionCallback-onSuccess: Connected")
////                    isStarted = true
//                }
//
//                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
//                    Log.d(TAG, "actionCallback-onFailure: Connection Failure")
//                    //                        toast = Toast.makeText(getApplicationContext(),
//                    //                                "Connection Failure", Toast.LENGTH_SHORT);
//                    //                        toast.show();
////                                            sendBrodecast("ConnectionFailure");
//                }
//            }

            return START_STICKY
        } else {
//            if (valHOME_CONNECTOR) {
                return START_STICKY
//            } else {
//                return START_NOT_STICKY
//            }
        }
    }

//    private val iCallbackClient: MqttCallbackExtended = object : MqttCallbackExtended {
//        override fun connectComplete(reconnect: Boolean, serverURI: String) {
//            Log.d(TAG,"Connect Complete.")
////            onConnect()
//        }
//
//        override fun deliveryComplete(token: IMqttDeliveryToken) {}
//        override fun connectionLost(cause: Throwable) {
//            Log.e(TAG, "connectionLost error")
////            scheduler.cancelMqttPing()
////            changeState(EndpointState.DISCONNECTED.withError(cause))
////            Log.d(TAG,"Releasing connectinglock")
////            connectingLock.release()
////            scheduler.scheduleMqttReconnect()
//        }
//
//        override fun messageArrived(topic: String, message: MqttMessage) {
//            Log.d(TAG, "topic: $topic value: $message")
//            Log.d(TAG, message.toString())
//
//        }
//    }


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
                controlClient?.launchJobRun(0) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
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
                controlClient?.launchJobRun(0) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
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

    override fun connectionLost(cause: Throwable?) {
        Log.d(TAG, "!!!!!!!!!!connectionLost: $cause")
    }

    //    @Throws(Exception::class)
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

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d(TAG, "!!!!!!!!!!deliveryComplete: $token")
    }

    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        if (MQTTclient!!.isConnected) {
            try {
                val message = MqttMessage()
                message.payload = msg.toByteArray()
                message.qos = qos
                message.isRetained = retained
                MQTTclient!!.publish(topic, message, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "$msg published to $topic")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d(TAG, "Failed to publish $msg to $topic")
                    }
                })
            } catch (e: MqttException) {
                e.printStackTrace()
            }
//            try {
//                val encodedPayload = payload.toByteArray(charset("UTF-8"))
//                val message = MqttMessage(encodedPayload)
//                MQTTclient!!.publish(clientId + "/" + mqttDevice + "/" + topic, message)
//            } catch (e: UnsupportedEncodingException) {
//                e.printStackTrace()
//            } catch (e: MqttPersistenceException) {
//                e.printStackTrace()
//            } catch (e: MqttException) {
//                e.printStackTrace()
//            }
        }
    }

    private fun pubOne() {
        publish("info/general/API", Build.VERSION.SDK_INT.toString())
        publish("info/general/BRAND", Build.BRAND)
        publish("info/general/BOARD", Build.BOARD)
        publish("info/general/DISPLAY", Build.DISPLAY)
        publish("info/general/FINGERPRINT", Build.FINGERPRINT)
        publish("info/general/HARDWARE", Build.HARDWARE)
        publish("info/general/HOST", Build.HOST)
        publish("info/general/ID", Build.ID)
        publish("info/general/BOOTLOADER", Build.BOOTLOADER)
        publish("info/general/DEVICE", Build.DEVICE)

        publish("info/general/MANUFACTURER", Build.MANUFACTURER)
        publish("info/general/USER", Build.USER)
        publish("info/general/MODEL", Build.MODEL)
        publish("info/general/PRODUCT", Build.PRODUCT)
        publish("info/general/TAGS", Build.TAGS)
        publish("info/general/TYPE", Build.TYPE)
        publish("info/general/UNKNOWN", Build.UNKNOWN)
        publish("info/general/SERIAL", Build.SERIAL)
        publish("info/general/BASE_OS", Build.VERSION.BASE_OS)
        publish("info/general/SECURITY_PATCH", Build.VERSION.SECURITY_PATCH)
    }

    private fun setSubscribe() {
        val qos = 1
        try {
            if (MQTTclient != null) {
                val subToken: IMqttToken =
                    MQTTclient!!.subscribe(clientId + "/" + mqttDevice + "/comm/*", qos)
                subToken.actionCallback = object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        Log.d(TAG, "setSubscribe-onSuccess: $asyncActionToken");
                        // The message was published
                    }

                    override fun onFailure(
                        asyncActionToken: IMqttToken,
                        exception: Throwable
                    ) {
                        Log.d(TAG, "setSubscribe-onFailure: $asyncActionToken @ $exception");
                    }
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

}