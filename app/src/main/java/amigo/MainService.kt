package com.app.amigo

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.app.amigo.fragment.BoolPreference

internal enum class EnumStateService(val value: String) {
    ENUM_NULL("com.app.amigo.NULL"),
    DIALOG_ACCOUNT("com.app.amigo.DIALOG_ACCOUNT"),
    DIALOG_VPN("com.app.amigo.DIALOG_VPN"),
    SERVICE_START("com.app.amigo.SERVICE_START"),

    TEST("com.app.amigo.TEST"),

    SERVICE_STOP("com.app.amigo.SERVICE_STOP"),
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

internal class MainService : VpnService() {
    private var TAG = "@!@MainService"
    private var broadcastReceiver: MainBroadcastReceiver? = null
    private var callbackRequestWIFI: ConnectivityManager.NetworkCallback? = null
    private var callbackRequestCELLULAR: ConnectivityManager.NetworkCallback? = null

    //    private var builder: NotificationCompat.Builder? = null
//    internal val CHANNEL_ID = "HomeClient"
    var controlClient: ControlClient? = null

    //    internal var stateService = EnumStateService.ENUM_NULL
    internal val helper by lazy { NotificationHelper(this) }

//    lateinit var cm: ConnectivityManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
//        initNoti()
//        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        initSettings()
        initControlClient()
//        initMQTT()
        initBroadReceiver()
//        initTTS()
//        initSensors()
        initNetworkRequest()
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
                controlClient?.launchJobRun() // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.e(TAG, "onLost WiFi: ${network}")
                controlClient?.checkNetworks()
                controlClient?.launchJobRun() // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
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
                controlClient?.launchJobRun() // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.e(TAG, "onLost CELLULAR: ${network}")
                controlClient?.checkNetworks()
                controlClient?.launchJobRun() // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
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

    private fun initControlClient() {
        if (controlClient == null) {
            Log.d(TAG, "new ControlClient")
            controlClient = ControlClient(this).also {
//                helper.updateNotification("update")
//                beForegrounded() // уведомление о работе vpn
//                Log.d(TAG, "beForegrounded!!!!!")
                it.initJobRun()
            }
        }
    }
//        cm =
//            applicationContext.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
//        cm.registerNetworkCallback(networkRequest, networkCallback)

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
            BoolPreference.HOME_CONNECTOR.getValue(
                PreferenceManager.getDefaultSharedPreferences(applicationContext)
            )
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
            controlClient?.launchJobRun() // onExeption(Throwable(EnumAction.ACTION_DISCONNECT.value))
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                helper.getNotification()
            )
            Toast.makeText(applicationContext, "service start!!!", Toast.LENGTH_SHORT).show()
            return START_STICKY
        } else if (EnumStateService.SERVICE_STOP.name == intent?.action ?: false) {
            Log.d(TAG, "service stop!!!")
            controlClient?.launchJobRun() // onExeption(Throwable(EnumAction.ACTION_CONNECT.value))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true)
            }
            stopSelf()
            Toast.makeText(applicationContext, "service stop!!!", Toast.LENGTH_SHORT).show()
            return STOP_FOREGROUND_REMOVE
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
}
