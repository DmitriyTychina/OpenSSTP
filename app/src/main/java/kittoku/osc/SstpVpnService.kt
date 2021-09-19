package kittoku.osc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat


internal enum class VpnAction(val value: String) {
    ACTION_CONNECT("kittoku.osc.CONNECT"),
    ACTION_DISCONNECT("kittoku.osc.DISCONNECT"),
}

internal class SstpVpnService : VpnService() {
    private var broadcastReceiver: BroadcastReceiver? = null

    internal val CHANNEL_ID = "OpenSSTPClient"
    private var controlClient: ControlClient? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val filter = IntentFilter()

        filter.addAction("android.net.wifi.STATE_CHANGE")
        filter.addAction("android.intent.action.PHONE_STATE")
        filter.addAction("android.intent.action.PHONE_STATE")
        filter.addAction("android.provider.Telephony.SMS_RECEIVED")
        filter.addAction("android.intent.action.BATTERY_LOW")
        filter.addAction(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)

        broadcastReceiver = MainReceiver()
        registerReceiver(broadcastReceiver, filter)
        (broadcastReceiver as MainReceiver).initialize(this)

        return if (VpnAction.ACTION_DISCONNECT.value == intent?.action ?: false) {
            controlClient?.kill(Throwable("kittoku.osc.DISCONNECT"))
            controlClient = null
            Service.START_NOT_STICKY
//            prefs.edit().putString(StatusPreference.STATUS.name, "").apply()

        } else {
            controlClient?.kill(Throwable("kittoku.osc.CONNECT"))
            controlClient = ControlClient(this).also {
                beForegrounded() // уведомление о работе vpn
                Log.e("@!@", "beForegrounded!!!!!")
                it.run()
            }
//            Service.START_STICKY
            Service.START_STICKY_COMPATIBILITY
        }
    }

    private fun beForegrounded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_MIN
            ) //IMPORTANCE_MIN - без звука
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

//        val intent = Intent(
//            applicationContext,
//            SstpVpnService::class.java
//        )//.setAction(VpnAction.ACTION_DISCONNECT.value)
//        val pendingIntent = PendingIntent.getService(applicationContext, 0, intent, 0)
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID).also {
            it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
//            it.setContentText("Disconnect SSTP connection")
//            it.priority = NotificationCompat.PRIORITY_LOW
//            it.setContentIntent(pendingIntent)
//            it.setAutoCancel(true)
//            it.setSound(null)
        }

        startForeground(1, builder.build())
    }

    override fun onDestroy() {
        controlClient?.kill(null)
    }
}
