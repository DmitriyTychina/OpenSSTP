package com.app.amigo

import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat


internal enum class VpnAction(val value: String) {
    ACTION_CONNECT("com.app.amigo.CONNECT"),
    ACTION_DISCONNECT("com.app.amigo.DISCONNECT"),
    ACTION_STOP("com.app.amigo.STOP"),
}

internal class SstpVpnService : VpnService() {
    private var TAG = "@!@SstpVpnService"
    private var broadcastReceiver: MainBroadcastReceiver? = null
    private var builder: NotificationCompat.Builder? = null
    internal val CHANNEL_ID = "HomeClient"
    var controlClient: ControlClient? = null
    private var state = false
    internal val helper by lazy { NotificationHelper(this) }

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
//            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            broadcastReceiver = MainBroadcastReceiver()
            registerReceiver(broadcastReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand intent= " + intent?.action)
        super.onStartCommand(intent, flags, startId)
        Log.d(
            TAG,
            "intent?.action= " + intent?.action + "  state= " + state + "  controlClient= " + controlClient
        )

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
                        it.run()
                    }
                }
                START_STICKY
            } else {
                START_NOT_STICKY
            }
        } else {
            if (state) {
                Log.d(TAG, "fStopService")
                state = false
                if (VpnAction.ACTION_DISCONNECT.value == intent?.action ?: false) {
                    Log.d(TAG, "ACTION_DISCONNECT")
                    controlClient?.kill(Throwable("com.app.amigo.DISCONNECT"))
                    controlClient = null
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    stopForeground(true)
                }
                stopSelf()
            }
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        controlClient?.kill(null)
//        controlClient = null
        Toast.makeText(applicationContext, "service stop!!!", Toast.LENGTH_LONG).show()
    }
}
