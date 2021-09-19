package kittoku.osc

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MainReceiver : BroadcastReceiver() { var TAG = "@!@AppReceiver"
    var context: Context? = null

    var notiID = 2
    private var vpnService: SstpVpnService? = null

    internal fun initialize(vpn: SstpVpnService) {
        vpnService = vpn
        Log.i(TAG, "@!@initialize MainReceiver")
        makeNotification(++notiID, "initialize MainReceiver")
    }

    //    Intent i;
    var settings: SharedPreferences? = null

    //    var general_startBoot: Boolean? = null
//    var general_wifi: Boolean? = null
//    var event_call: Boolean? = null
//    var event_sms: Boolean? = null
//    var event_battery: Boolean? = null
//    var connection_list: String? = null
    override fun onReceive(context: Context, intent: Intent) {
//        settings = PreferenceManager.getDefaultSharedPreferences(context);
//        general_startBoot = settings.getBoolean("general_auto_start", false);
//        general_wifi = settings.getBoolean("general_wifi", false);
//        event_call = settings.getBoolean("event_all", false);
//        event_sms = settings.getBoolean("event_sms", false);
//        connection_list = settings.getString("connection_list", "");
//        event_battery = settings.getBoolean("event_battery", false);
        val action = intent.action
        Log.i(TAG, "@!@Action : $action")
        makeNotification(++notiID,"Action : $action")
        if (action == null) {
            return
        }

//        if(connection_list.equals("0")){
//            i = new Intent(context,WebServerService.class);
//
//        }else{
//            i = new Intent(context,MQTTService.class);
//        }
        if (/*general_startBoot!! &&*/ (action == "android.intent.action.BOOT_COMPLETED" || action == "android.intent.action.QUICKBOOT_POWERON" || action == "com.htc.intent.action.QUICKBOOT_POWERON")) {
//            i.putExtra("init","start");
//            context.startService(i);
            Toast.makeText(context, "Start action: $action", Toast.LENGTH_LONG).show()
            makeNotification(++notiID, action)
        }

//        if (action.equals("android.intent.action.SCREEN_ON")
//                ||action.equals("android.intent.action.SCREEN_OFF")){
//            i.putExtra("init","screen");
//            i.putExtra("state",action.equals("android.intent.action.SCREEN_ON") ? "true" : "false");
//            context.startService(i);
//        }

//        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
//            if ("android.provider.Telephony.SMS_RECEIVED".compareToIgnoreCase(intent.getAction()) == 0) {
//                i.putExtra("init","sms");
//                Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
//                SmsMessage[] messages = new SmsMessage[pduArray.length];
//                String sms = "";
//                for (int i = 0; i < pduArray.length; i++) {
//                    messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
//                    sms = sms+messages[i].getDisplayMessageBody();
//                }
//                String phoneNumber = messages[0].getDisplayOriginatingAddress();
//                Log.d(TAG,"sms  :  " + sms);
//                i.putExtra("number",phoneNumber);
//                i.putExtra("text",sms);
//                context.startService(i);
//            }
//        }

//        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
//            i.putExtra("init","call");
//            String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
//            if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
//                i.putExtra("type","ringing");
//                String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
//                i.putExtra("number",phoneNumber);
//                context.startService(i);
//                Log.d(TAG,"Show window: " + phoneNumber);
//            } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
//                i.putExtra("type","connection");
//                Log.d(TAG,"EXTRA_STATE_OFFHOOK.");
//                context.startService(i);
//            } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
//                i.putExtra("type","disconnection");
//                Log.d(TAG,"EXTRA_STATE_IDLE.");
//                context.startService(i);
//            }
//        }
        if (action == "android.net.wifi.STATE_CHANGE") {
            val typeConn = hasConnection(context)
            Log.d(TAG, "typeConn = $typeConn")
            makeNotification(++notiID, action)

//            if(typeConn.equals("wifi")){
//                i.putExtra("init",typeConn);
//                context.startService(i);
//        }
        }

//        if (action.equals("android.intent.action.BATTERY_CHANGED")){
//            i.putExtras(intent);
//            i.putExtra("init","batteryInfo");
//            context.startService(i);
//        }

//        if ((action.equals("android.intent.action.ACTION_POWER_CONNECTED") || action.equals("android.intent.action.ACTION_POWER_DISCONNECTED"))){
//            i.putExtra("init","power");
//            i.putExtra("power",action.equals("android.intent.action.ACTION_POWER_CONNECTED") ? "connected" : "disconnected");
//            context.startService(i);
//        }
    }

    @SuppressLint("MissingPermission")
    fun hasConnection(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (wifiInfo != null && wifiInfo.isConnected) {
            Log.e(TAG, "Network Connection is TYPE_WIFI")
            return "wifi"
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
        if (wifiInfo != null && wifiInfo.isConnected) {
            Log.e(TAG, "Network Connection is TYPE_MOBILE")
            return "mobile"
        }
        return "false"
    }

    private fun makeNotification(id: Int, message: String) {
        val builder =
            vpnService?.let {
                NotificationCompat.Builder(it.applicationContext, vpnService!!.CHANNEL_ID).also {
                    it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
                    it.setContentText(message)
                    it.priority = NotificationCompat.PRIORITY_LOW
                    it.setAutoCancel(true)
                    //                it.setSound(null)
                }
            }
        vpnService?.let {
            NotificationManagerCompat.from(it.applicationContext).also {
                if (builder != null) {
                    it.notify(id, builder.build())
                }
            }
        }
    }
}