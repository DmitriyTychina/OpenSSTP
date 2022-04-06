package com.app.amigo

import android.os.Build
import androidx.preference.PreferenceManager
import com.app.amigo.fragment.IntPreference
import com.app.amigo.fragment.StrPreference
import kotlinx.coroutines.*
import org.chromium.base.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

internal enum class enumStateMQTT(val value: String) {
    MQTT_STOP("com.app.amigo.MQTT_STOP"),
    MQTT_CONNECTING("com.app.amigo.MQTT_CONNECTING"),
    MQTT_CONNECTED("com.app.amigo.MQTT_CONNECTED"),
    MQTT_DISCONNECTING("com.app.amigo.MQTT_DISCONNECTING"),
    MQTT_DISCONNECTED("com.app.amigo.MQTT_DISCONNECTED"),

    TEST("com.app.amigo.TEST"),
}


internal class ControlClientMQTT(internal val vpnService: MainService) :
    CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()), MqttCallback {

    private var cntConnection = 0
    val TAG = "@!@ControlClientMQTT"
    private var prefs = PreferenceManager.getDefaultSharedPreferences(vpnService.applicationContext)

    private var MQTTclient: MqttAndroidClient? = null
    private lateinit var options: MqttConnectOptions
    private var serverUri: String? = null
    private var mqttIP: String = ""
    private var mqttPort: String = ""
    private var mqttLogin: String = ""
    private var mqttPass: String = ""
    private var mqttDevice: String = ""
    private val clientId = "HomeClient"
//    private var token: IMqttToken? = null
    private var mainTopic: String = ""
    private var stateMQTT = enumStateMQTT.MQTT_DISCONNECTED
//    private val handler = CoroutineExceptionHandler { _, exception ->
//        Log.e(TAG, "***** start exception exception.localizedMessage ${exception.localizedMessage}")
//        ccmqtt_launchJobRun()
//    }

    init {
        initialize()
    }

    fun initialize() {
        Log.d(TAG, "initialize")
        mqttIP = StrPreference.MQTT_HOST.getValue(prefs)
        mqttPort = IntPreference.MQTT_PORT.getValue(prefs).toString()
        mqttLogin = StrPreference.MQTT_USER.getValue(prefs)
        mqttPass = StrPreference.MQTT_PASS.getValue(prefs)
        mqttDevice = StrPreference.HOME_USER.getValue(prefs) // ???
        mainTopic = clientId + "/" + mqttDevice + "/"
        serverUri = "tcp://$mqttIP:$mqttPort"
        MQTTclient = MqttAndroidClient(vpnService.applicationContext, serverUri, mqttDevice)
        options = MqttConnectOptions()
//        options.isAutomaticReconnect = true
//        options.isCleanSession = false
//        options.mqttVersion = 4

        if (mqttLogin.isNotBlank()) options.userName = mqttLogin
        if (mqttPass.isNotBlank()) options.password = mqttPass.toCharArray()

        MQTTclient!!.setCallback(this)
    }

    private fun startConnection() {
        stateMQTT = enumStateMQTT.MQTT_DISCONNECTED
        launch {
//            if(stateMQTT == enumStateMQTT.MQTT_DISCONNECTED){
            while ((MQTTclient != null) && (stateMQTT != enumStateMQTT.MQTT_CONNECTED) && (MQTTclient?.isConnected != true)) {
                Log.e(TAG, "startConnection()")
                stateMQTT = enumStateMQTT.MQTT_CONNECTING
                val token = MQTTclient?.connect(options)
                token?.actionCallback = object : IMqttActionListener {

                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        Log.d(TAG, "actionCallback-onSuccess: Connected")
                        stateMQTT = enumStateMQTT.MQTT_CONNECTED
//                        setSubscribe()
                        pubOne()
//                    isStarted = true
                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        Log.e(TAG, "actionCallback-onFailure: Connection Failure")
                        stateMQTT = enumStateMQTT.MQTT_DISCONNECTED
//                        startConnection()
                    }
                }
                delay(5000)
                Log.e(TAG, "re-startConnection()")
            }
        }
    }

    internal fun ccmqtt_launchJobRun() {
        launch {

        }
    }

    internal fun initJobRun() {
        Log.e(
            TAG,
            "***** initJobRun() *****"
        )
    }

    override fun connectionLost(cause: Throwable?) {
        Log.d(TAG, "!!!!!!!!!!connectionLost: $cause")
//        stateMQTT = enumStateMQTT.MQTT_DISCONNECTED
        startConnection()
    }

    //    @Throws(Exception::class)
    override fun messageArrived(topic: String, message: MqttMessage) {
        Log.d(TAG, "messageArrived topic: $topic value: $message")
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
//        Log.d(TAG, "!!!!!!!!!!deliveryComplete: $token")
    }

    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        launch {
            Log.d(TAG, "MQTTclient?.isConnected " + MQTTclient?.isConnected)

            if (MQTTclient?.isConnected == true) {
                try {
                    val message = MqttMessage()
                    message.payload = msg.toByteArray()
                    message.qos = qos
                    message.isRetained = retained
                    val token =
                        MQTTclient!!.publish(
                            mainTopic + topic,
                            message,
                            null,
                            object : IMqttActionListener {
                                override fun onSuccess(asyncActionToken: IMqttToken?) {
                                    Log.d(TAG, "$msg published to $topic")
                                }

                                override fun onFailure(
                                    asyncActionToken: IMqttToken?,
                                    exception: Throwable?
                                ) {
                                    Log.e(TAG, "Failed to publish $msg to $topic")
                                }
                            })
//            token.waitForCompletion(1000)
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
    }

    private fun pubOne() { // публиковать всегда при запуске
        publish("info/connection/count", cntConnection.toString())

        if(cntConnection == 0) { // первый запуск
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
        cntConnection++
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
                        Log.e(TAG, "setSubscribe-onFailure: $asyncActionToken @ $exception");
                    }
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun stop() {
        Log.i(TAG, "stop()")
        MQTTclient?.disconnect()
        MQTTclient = null
    }

    fun start() {
        Log.i(TAG, "start()")
        if ((MQTTclient != null) && (MQTTclient?.isConnected != true)) {
//            stateMQTT = enumStateMQTT.MQTT_DISCONNECTED
            startConnection()
        }
    }
}
