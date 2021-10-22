package com.app.amigo

import amigo.getSSID
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.app.amigo.fragment.StatusPreference


class MainBroadcastReceiver : BroadcastReceiver() {
    private var TAG = "@!@AppReceiver"
    private var cnt = 0
//    private final var wifiManager: WifiManager? = null
//    var context: Context? = null

//    var notiID = 1

    //    Intent i;
//    var settings: SharedPreferences? = null

    //    var general_startBoot: Boolean? = null
//    var general_wifi: Boolean? = null
//    var event_call: Boolean? = null
//    var event_sms: Boolean? = null
//    var event_battery: Boolean? = null
//    var connection_list: String? = null

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive.action=$action")
        val service = Intent(context.applicationContext, SstpVpnService::class.java)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val flagautostart = sharedPreferences.getBoolean("HOME_CONNECTOR", false)

//        Toast.makeText(context.applicationContext, "action=$action", Toast.LENGTH_LONG).show()
//        service.action = "broadcast"
        if (flagautostart && (action == "android.intent.action.BOOT_COMPLETED" ||
                    action == "android.intent.action.QUICKBOOT_POWERON" ||
                    action == "com.htc.intent.action.QUICKBOOT_POWERON")
        ) {
            VpnService.prepare(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.applicationContext.startForegroundService(
                    service.setAction(VpnAction.ACTION_CONNECT.value)
                )
            } else {
                context.applicationContext.startService(
                    service.setAction(VpnAction.ACTION_CONNECT.value)
                )
            }
        }

        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

//            val nwInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
//            nwInfo.getState()
//            val info = wifiManager.connectionInfo
            Log.d(TAG, "findSSIDForWifiInfo " + getSSID(context.applicationContext))
//            val summary = mutableListOf<String>()
//                    summary.add(findSSIDForWifiInfo(wifiManager))
//            sharedPreferences.edit().putString(
//                StatusPreference.MQTT_STATUS.name,
//                getSSID(context.applicationContext)
////                        summary.toString()
//            ).apply()

//            wifiManager =
//                getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager?
        }

        if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            cnt++
            Log.d(TAG, "cnt $cnt")
            val scanResults = wifiManager?.scanResults
            val summary = mutableListOf<String>()
            if (scanResults != null) {
                scanResults.forEach {
                    summary.add(it.SSID + ": " + it.level)
                }
            }

            Log.d(TAG, "Scan OK $summary")
//            val wifiState = wifiManager?.wifiState
//            summary.forEach() {
//                sharedPreferences.edit().putString(StatusPreference.MQTT_STATUS.name, it)
//                    .apply()
//            }
            sharedPreferences.edit().putString(
                StatusPreference.MQTT_STATUS.name,
//                wifiState.toString()
                summary.reduce { acc, s ->
                    acc + "\n" + s
                }
            ).apply()
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
//        if (action == "android.net.wifi.STATE_CHANGE") {
//            val typeConn = hasConnection(context)
//            Log.d(TAG, "typeConn = $typeConn")
//
////            if(typeConn.equals("wifi")){
////                i.putExtra("init",typeConn);
////                context.startService(i);
////        }
//        }

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

//    @SuppressLint("MissingPermission")
//    fun hasConnection(context: Context): String {
//        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        var wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
//        if (wifiInfo != null && wifiInfo.isConnected) {
//            Log.d(TAG, "Network Connection is TYPE_WIFI")
//            return "wifi"
//        }
//        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
//        if (wifiInfo != null && wifiInfo.isConnected) {
//            Log.d(TAG, "Network Connection is TYPE_MOBILE")
//            return "mobile"
//        }
//        return "false"
//    }
}