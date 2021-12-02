package com.app.amigo.fragment

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.app.amigo.R





private val mqttPreferences = arrayOf<PreferenceWrapper<*>>(
    StrPreference.MQTT_HOST,
    IntPreference.MQTT_PORT,
    StrPreference.MQTT_USER,
    StrPreference.MQTT_PASS,
    BoolPreference.MQTT_CONNECTOR,
    StatusPreference.MQTT_STATUS,
)

class MqttFragment : PreferenceFragmentCompat() {
    private var TAG = "@!@MqttFragment"

//    private lateinit var sharedPreferenceListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(TAG, "onCreatePreferences")
        setPreferencesFromResource(R.xml.mqtt, rootKey)
        mqttPreferences.forEach {
            it.initPreference(this, preferenceManager.sharedPreferences)
        }
//        attachSharedPreferenceListener()
        attachConnectorListener()

//        if (preferenceManager.sharedPreferences.getBoolean("MQTT_CONNECTOR", false)){
//            Log.d(TAG, "startVPN")
//            startVPN()
//        }
//        else
//            preferenceManager.sharedPreferences.edit().putString(StatusPreference.STATUS.name, "").apply()

    }

    //    @SuppressLint("LongLogTag")
//    private fun attachSharedPreferenceListener() {
//        // for updating by both user and system
//        sharedPreferenceListener =
//            SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
//                when (key) {
//                    BoolPreference.MQTT_CONNECTOR.name -> {
//                        BoolPreference.MQTT_CONNECTOR.also {
//                            it.setValue(this, it.getValue(prefs))
//                        }
//                    }
//                    StatusPreference.MQTT_STATUS.name -> {
//                        StatusPreference.MQTT_STATUS.also {
//                            it.setValue(this, it.getValue(prefs))
//                        }
//                    }
//                }
//            }
//        preferenceManager.sharedPreferences
//            .registerOnSharedPreferenceChangeListener(sharedPreferenceListener)
//    }

    //    fun startVPN(): Boolean {
//        if (!checkPreferences()) {
////            BoolPreference.MQTT_CONNECTOR.setValue(this,false)
//            Log.d(TAG, "checkPreferences=false")
//            return false
//        }
//        val intent = VpnService.prepare(context)
//        if (intent != null) {
//            startActivityForResult(intent, 0)
//        } else {
//            onActivityResult(0, Activity.RESULT_OK, null)
//        }
////        Log.d(TAG, "startVpnService")
////        startVpnService(VpnAction.ACTION_CONNECT)
//
//        return true
//    }
//    sealed class WifiConnector constructor(
////        systemService: Any?) {
////    private class WifiConnector(
//        protected val wifiManager: WifiManager,
//        protected val connectivityManager: ConnectivityManager
//    ) {
//        val currentSsid: String
//            get() = wifiManager.connectionInfo
//                ?.takeIf { it.supplicantState == SupplicantState.COMPLETED }
//                ?.ssid
//                ?.replace("\"", "")
//                ?: "<None>"
//
////    @ExperimentalCoroutinesApi
////    fun connectivityFlow(): Flow<String>> = callbackFlow {
////
////        val callback = object : ConnectivityManager.NetworkCallback() {
////            override fun onAvailable(network: Network) {
////                super.onAvailable(network)
////                sendBlocking(currentSsid)
////            }
////
////            override fun onLost(network: Network) {
////                super.onLost(network)
////                sendBlocking(currentSsid)
////            }
////        }
////        val request = NetworkRequest.Builder()
////            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
////            .build()
////        connectivityManager.registerNetworkCallback(request, callback)
////
////        awaitClose {
////            connectivityManager.unregisterNetworkCallback(callback)
////        }
////    }
////    .
////    .
////    .
//    }
    private fun attachConnectorListener() {
//        // for disconnecting by user in MqttFragment
//        findPreference<SwitchPreferenceCompat>(BoolPreference.MQTT_CONNECTOR.name)!!.also {
//            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newState ->
//                val intent = VpnService.prepare(context)
//                if (newState == true) {
////                    val ok: Boolean = true
//////                        intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
//////                    val wifiManager = context?.getSystemService(WIFI_SERVICE)
//////                    val wifiInfo = wifiManager.is
//////                    val ssid = wifiInfo.ssid
////                    if (ok) {
////                        val summary = mutableListOf<String>()
////                        var wifissid = this.WifiConnector.currentSsid
////                        summary.add("[Application.WIFI_SERVICE.length.toString]")
////                        summary.add()
////                        summary.add("")
////                        Log.d(TAG, "Scan OK")
//////                        val list: List<ScanResult> = wifiManager.getScanResults()
//////                        this@MainActivity.showNetworks(list)
//////                        this@MainActivity.showNetworksDetails(list)
////                        preferenceManager.sharedPreferences.edit().putString(
////                            StatusPreference.MQTT_STATUS.name,
////                            summary.toString()
////                        ).apply()
////                    } else {
////                        Log.d(TAG, "Scan not OK")
////                    }
//
//                    return@OnPreferenceChangeListener false
//                } else {
////                    startVpnService(VpnAction.ACTION_DISCONNECT)
//                }
//                true
//            }
//        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (resultCode == Activity.RESULT_OK) {
////            startVpnService(VpnAction.ACTION_CONNECT)
//        }
//    }


    private fun makeToast(cause: String) {
        Toast.makeText(context, "INVALID SETTING: $cause", Toast.LENGTH_LONG).show()
    }

    private fun checkPreferences(): Boolean {
//        val prefs = PreferenceManager.getDefaultSharedPreferences(this.context)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
//            preferenceManager.sharedPreferences

//        StrPreference.MQTT_HOST.getValue(prefs).also {
//            if (TextUtils.isEmpty(it)) {
//                makeToast("Host is missing")
//                return false
//            }
//        }
        return true
    }
}