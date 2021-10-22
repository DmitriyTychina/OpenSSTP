package amigo

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.text.TextUtils
import android.util.Log
import com.app.amigo.deviceName

val TAG = "@!@wifi_kt"

@SuppressLint("MissingPermission")
fun getSSID(context: Context): String {
    val wifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val connectionInfo = wifiManager.connectionInfo
    Log.d(TAG+":getSSID", "wifiManager.deviceName " + wifiManager.deviceName())
    if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.ssid)) {
        Log.d(TAG+":getSSID", "connectionInfo.ssid: ${connectionInfo.ssid}")
        return connectionInfo.ssid
    }
    return "null"
}

fun getIPself(context: Context) {
}