package com.app.amigo

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.text.TextUtils
import android.util.Log

val TAG = "@!@wifi_kt"

//fun WifiManager.deviceName(): String = connectionInfo.ssid.run {                  *
//    if (this.contains("<unknown ssid>")) "UNKNOWN" else this                      *
//}                                                                                 *

//fun hasConnection(context: Context): String? {
//    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//    var wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
//    if (wifiInfo != null && wifiInfo.isConnected) {
//        Log.e(TAG, "Network Connection is TYPE_WIFI")
//        return "wifi"
//    }
//    wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
//    if (wifiInfo != null && wifiInfo.isConnected) {
//        Log.e(TAG, "Network Connection is TYPE_MOBILE")
//        return "mobile"
//    }
//    return "false"
//}

@SuppressLint("MissingPermission")
fun getSSID(context: Context): String {
    val wifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val connectionInfo = wifiManager.connectionInfo
//    Log.d(TAG+":getSSID", "wifiManager.deviceName " + wifiManager.deviceName())   *
    if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.ssid)) {
        Log.d(TAG+":getSSID", "connectionInfo.ssid: ${connectionInfo.ssid}")
        return connectionInfo.ssid
    }
    return "null"
}

fun getIPself(context: Context) {
}


// из mainactivity
//private var wifiManager: WifiManager? = null
// oncreate
//        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        Log.d(TAG, "wifiManager.deviceName " + wifiManager!!.deviceName())
//        Log.d(TAG, "getSSID " + getSSID(applicationContext))
//        val wifiInfo = wifiManager!!.connectionInfo
//        Toast.makeText(
//            applicationContext,
//            getSSID(applicationContext),
//            Toast.LENGTH_LONG
//        ).show()
//        cm = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
