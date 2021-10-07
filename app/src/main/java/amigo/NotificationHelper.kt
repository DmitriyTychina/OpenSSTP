package com.app.amigo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat


private const val CHANNEL_ID = "ASChannel"
private const val CHANNEL_NAME = "ASChannelName"
private const val CHANNEL_DESCRIPTION = "ASChannelDescription"

class NotificationHelper(private val context: Context) {

    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(context, CHANNEL_ID)
//            .setContentTitle(context.getString(R.string.app_name))
            .setSound(null)
//            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .setAutoCancel(true)
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

//    private val contentIntent by lazy {
//        PendingIntent.getActivity(
//            context,
//            0,
//            Intent(context, MainActivity::class.java),
//            PendingIntent.FLAG_UPDATE_CURRENT
//        )
//    }

    fun getNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(createChannel())
        }

        return notificationBuilder.build()
    }

    fun updateNotification(notificationText: String? = null) {
        notificationText?.let { notificationBuilder.setContentText(it) }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() =
        NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
            setSound(null, null)
        }

    companion object {
        const val NOTIFICATION_ID = 99
    }
}