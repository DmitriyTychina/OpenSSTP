package com.app.amigo

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.*
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.amigo.fragment.StatusPreference


internal enum class VpnAction(val value: String) {
    ACTION_CONNECT("com.app.amigo.CONNECT"),
    ACTION_DISCONNECT("com.app.amigo.DISCONNECT"),
}

internal enum class BCAction(val value: String) {
    ACTION_WIFI_STATE_CHANGED("com.app.amigo.WIFI_STATE_CHANGED"),
}

internal enum class CTransport(val value: String) {
    TRANSPORT_WIFI("WIFI"),
    TRANSPORT_HOMEWIFI("HOME WIFI"),
    TRANSPORT_CELLULAR("CELLULAR"),
    TRANSPORT_VPN("VPN"),
    TRANSPORT_NONE("NONE"),
}

internal class SstpVpnService : VpnService() {
    private var TAG = "@!@SstpVpnService"
    private var broadcastReceiver: MainBroadcastReceiver? = null

    //    private var builder: NotificationCompat.Builder? = null
//    internal val CHANNEL_ID = "HomeClient"
    var controlClient: ControlClient? = null
    private var state = false
    internal val helper by lazy { NotificationHelper(this) }

//    lateinit var cm: ConnectivityManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
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
//        cm =
//            applicationContext.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
//        cm.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand intent= " + intent?.action)
        Log.d(
            TAG,
            "intent?.action= " + intent?.action + "  state= " + state + "  controlClient= " + controlClient
        )

        val status = intent?.getStringExtra("action")
        Log.d(TAG, "intent?.getStringExtra: status :" + status)
        when (status) {
            BCAction.ACTION_WIFI_STATE_CHANGED.value -> {
                controlClient?.isNetworkConnected()
//                if (hasConnection()) {
//                    startMQTT()
//                }
                return START_STICKY
            }
        }

        return if (VpnAction.ACTION_CONNECT.value == intent?.action ?: false) {
            if (!state) {
                Toast.makeText(applicationContext, "service start!!!", Toast.LENGTH_LONG).show()
                Log.d(TAG, "fStartService")
                state = true
//                controlClient?.kill(Throwable("com.app.amigo.CONNECT"))
                if (controlClient == null) {
                    Log.d(TAG, "new ControlClient")
                    controlClient = ControlClient(this).also {
                        startForeground(
                            NotificationHelper.NOTIFICATION_ID,
                            helper.getNotification()
                        )
//        helper.updateNotification("update")
//                beForegrounded() // уведомление о работе vpn
//                Log.d(TAG, "beForegrounded!!!!!")
                    }
                }
                controlClient!!.run()
                START_STICKY
            } else {
                START_NOT_STICKY
            }
        } else if (VpnAction.ACTION_DISCONNECT.value == intent?.action ?: false) {
            if (state) {
                Log.d(TAG, "fStopService")
                state = false
                Log.d(TAG, "ACTION_DISCONNECT")
                controlClient?.kill(Throwable("com.app.amigo.DISCONNECT"))
                controlClient = null // 21.12.2021
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    stopForeground(true)
                }
                stopSelf()
            }
            START_NOT_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
//        cm.unregisterNetworkCallback(networkCallback)
        unregisterReceiver(broadcastReceiver)
        controlClient?.kill(null)
//        controlClient = null
        Toast.makeText(applicationContext, "service stop!!!", Toast.LENGTH_LONG).show()
    }

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
