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

enum class EnumStateService(val value: String) {
    ENUM_NULL("com.app.amigo.NULL"),
    DIALOG_ACCOUNT("com.app.amigo.DIALOG_ACCOUNT"),
    DIALOG_VPN("com.app.amigo.DIALOG_VPN"),
    SERVICE_START("com.app.amigo.SERVICE_START"),
    SERVICE_STARTED("com.app.amigo.SERVICE_STARTED"),

    PUB("com.app.amigo.PUB"),

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

internal class MainService : VpnService() {
    private var TAG = "@!@MainService"
    private var broadcastReceiver: MainBroadcastReceiver? = null
    private var callbackRequestWIFI: ConnectivityManager.NetworkCallback? = null
    internal var stateWIFI: EnumStateTransport = EnumStateTransport.STATE_TRANSPORT_NULL
    private var callbackRequestCELLULAR: ConnectivityManager.NetworkCallback? = null

    //    private var builder: NotificationCompat.Builder? = null
//    internal val CHANNEL_ID = "HomeClient"
    var ccVPN: ControlClientVPN? = null
    internal var ccMQTT: ControlClientMQTT? = null

    //    internal var stateService = EnumStateService.ENUM_NULL
    internal val helper by lazy { NotificationHelper(this) }

//    lateinit var cm: ConnectivityManager
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Log.d(TAG, "onCreate")
//        initNoti()
//        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        initSettings()
        init_ccVPN()
        init_ccMQTT()
        initReceiver()
//        initTTS()
//        initSensors()
        initNetworkRequest()
        BoolPreference.HOME_CONNECTOR.setEnabled(true)
    }

    private fun initSettings() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
    }

    private fun init_ccVPN() {
        if (ccVPN == null) {
            Log.d(TAG, "new ccVPN")
            ccVPN = ControlClientVPN(this).also {
//                helper.updateNotification("update")
//                beForegrounded() // уведомление о работе vpn
//                Log.d(TAG, "beForegrounded!!!!!")
                it.initJobRun()
            }
        }
    }

    private fun init_ccMQTT() {
        if (ccMQTT == null) {
            Log.d(TAG, "new ccMQTT")
            ccMQTT = ControlClientMQTT(this)
        }
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
//            filter.addAction("android.provider.Telephony.SMS_RECEIVED")
//            filter.addAction("android.intent.action.BATTERY_LOW")
            filter.addAction(Intent.ACTION_POWER_CONNECTED)
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
            filter.addAction(Intent.ACTION_BATTERY_CHANGED)
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
            ccVPN?.launchJobRun(0) // onExeption(Throwable(EnumAction.ACTION_DISCONNECT.value))
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                helper.getNotification()
            )
            Toast.makeText(applicationContext, "service start!!!", Toast.LENGTH_SHORT).show()
            return START_STICKY
        } else if (EnumStateService.SERVICE_STOPPING.name == intent?.action ?: false) {
            Log.d(TAG, "service stopping!!!")
//            ccMQTT?.stop()
            ccVPN?.launchJobRun(4) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            return START_STICKY
        } else if (EnumStateService.SERVICE_STOPPED.name == intent?.action ?: false) {
            Log.d(TAG, "service stopped!!!")
//            ccMQTT?.stop()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true)
            }
            stopSelf()
            Toast.makeText(applicationContext, "service stop!!!", Toast.LENGTH_SHORT).show()
            return START_NOT_STICKY
        } else if (EnumStateService.SERVICE_STARTED.name == intent?.action ?: false) {
            Log.d(TAG, "service started!!! start mqtt!!!")
//            ccMQTT?.start()

            return START_STICKY
        } else if (EnumStateService.PUB.name == intent?.action ?: false) {

            if (intent != null && intent.extras != null) {
                val key = intent.getStringExtra("key")
                val value = intent.getStringExtra("value")
                Log.d(TAG, "service COMMAND: $key = $value")
                when(key) {
                   "screen" -> ccMQTT?.publish("info/display/on", value!!)
                }
            }


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
        ccVPN?.logStream?.close()
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
                ccVPN?.checkNetworks()
                ccVPN?.launchJobRun(0) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.e(TAG, "onLost WiFi: ${network}")
                ccVPN?.checkNetworks()
//                controlClient?.launchJobRun(6) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                Log.e(TAG, "onLinkPropertiesChanged WiFi: ${network} ---- ${linkProperties}")
                if (ccVPN != null) {
                    if (ccVPN!!.stateAndSettings.wifi_dns != linkProperties.dnsServers.toString()) {
                        ccVPN!!.stateAndSettings.wifi_dns =
                            linkProperties.dnsServers.toString()
                        ccVPN!!.refreshStatus()
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
                ccVPN?.checkNetworks()
                ccVPN?.launchJobRun(0) // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.e(TAG, "onLost CELLULAR: ${network}")
                ccVPN?.checkNetworks()
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