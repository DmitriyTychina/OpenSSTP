package com.app.amigo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.TwoStatePreference
import com.app.amigo.fragment.BoolPreference
import com.app.amigo.fragment.SetPreference

class MainBroadcastReceiver(mode: Int = 0, fragment: PreferenceFragmentCompat? = null) :
    BroadcastReceiver() {
    private val Mode = mode
    private val settingFragment = fragment

    //    private val controlClient: ControlClient? = null
    private var TAG = "@!@MainBroadcastReceiver"
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

//    @SuppressLint("RestrictedApi")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive.action=$action")
        val service = Intent(context.applicationContext, MainService::class.java)
        val prefs =
            PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
//        val flagautostart = sharedPreferences.getBoolean("HOME_CONNECTOR", false)
        val autostart = BoolPreference.HOME_CONNECTOR.getValue(prefs)

//        Toast.makeText(context.applicationContext, "action=$action", Toast.LENGTH_LONG).show()
//        service.action = "broadcast"
        if (autostart && (action == "android.intent.action.BOOT_COMPLETED" ||
                    action == "android.intent.action.QUICKBOOT_POWERON" ||
                    action == "android.intent.action.MY_PACKAGE_REPLACED" || //+ 06.02.2022
                    action == "com.htc.intent.action.QUICKBOOT_POWERON")
        ) {
            // запускаем сервис
            VpnService.prepare(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.applicationContext.startForegroundService(
                    service.setAction(EnumStateService.SERVICE_START.name)
                )
            } else {
                context.applicationContext.startService(
                    service.setAction(EnumStateService.SERVICE_START.name)
                )
            }
        }

        if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
            Log.d(TAG, "WIFI_STATE_CHANGED_ACTION")

//            VpnService.prepare(context)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.applicationContext.startForegroundService(service.setAction(enumStateService.SERVICE_START.name))
//            } else {
//                context.applicationContext.startService(service.setAction(enumStateService.SERVICE_START.name))
//            }
        }

        if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            Log.d(TAG, "NETWORK_STATE_CHANGED_ACTION")

//            val nwInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
//            nwInfo.getState()
//            val info = wifiManager.connectionInfo
//
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

        if (action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            cnt++
            Log.d(TAG, "SCAN_RESULTS cnt $cnt")
            val scanResults = wifiManager.scanResults
//            val summary = mutableListOf<String>()
            val APlist = mutableListOf<String>()
//            val APlistlevel = mutableListOf<String>()
//            summary.add(SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(Date()))
            scanResults?.forEach {
//                summary.add(it.SSID + ": " + it.level)
                APlist.add(it.SSID)
//                APlistlevel.add(it.SSID + ": " + it.level)
            }
            Log.d(TAG, "Scan OK $APlist")
            if (Mode == 1) {
//                context.unregisterReceiver(this)
                settingFragment?.findPreference<TwoStatePreference>(BoolPreference.SELECT_HOME_WIFI.name)!!
                    .also {
                        if (it.isChecked) {
                            Log.d(TAG, "onPreferenceClickListener SELECT_HOME_WIFI = true")
                            val params =
                                SetPreference.HOME_WIFI_SUITES.getValue(prefs) + APlist
                            settingFragment.findPreference<MultiSelectListPreference>(SetPreference.HOME_WIFI_SUITES.name)!!
                                .also {
                                    it.isEnabled = true
                                    it.entries = params.toTypedArray()
                                    it.entryValues = params.toTypedArray()
                                }
                        } else {
                            Log.d(TAG, "onPreferenceClickListener SELECT_HOME_WIFI = false")
                            settingFragment.findPreference<MultiSelectListPreference>(SetPreference.HOME_WIFI_SUITES.name)!!
                                .also {
                                    it.isEnabled = false
                                }
                        }
                    }
            }
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