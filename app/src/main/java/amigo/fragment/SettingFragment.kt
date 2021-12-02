package com.app.amigo.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceClickListener
import com.app.amigo.MainBroadcastReceiver
import com.app.amigo.R
import javax.net.ssl.SSLContext

private const val CERT_DIR_REQUEST_CODE: Int = 0
private const val LOG_DIR_REQUEST_CODE: Int = 1

private val settingPreferences = arrayOf<PreferenceWrapper<*>>(
    StrPreference.HOME_HOST,
    StrPreference.HOME_USER,
    StrPreference.HOME_PASS,
    BoolPreference.SELECT_HOME_WIFI,
    SetPreference.HOME_WIFI_SUITES,
    IntPreference.SSL_PORT,
    StrPreference.SSL_VERSION,
    BoolPreference.SSL_DO_VERIFY,
    BoolPreference.SSL_DO_ADD_CERT,
    DirPreference.SSL_CERT_DIR,
    BoolPreference.SSL_DO_SELECT_SUITES,
    SetPreference.SSL_SUITES,
    IntPreference.PPP_MRU,
    IntPreference.PPP_MTU,
    BoolPreference.PPP_PAP_ENABLED,
    BoolPreference.PPP_MSCHAPv2_ENABLED,
    BoolPreference.PPP_IPv4_ENABLED,
    BoolPreference.PPP_IPv6_ENABLED,
    IntPreference.IP_PREFIX,
    BoolPreference.IP_ONLY_LAN,
    BoolPreference.IP_ONLY_ULA,
    BoolPreference.RECONNECTION_ENABLED,
    IntPreference.RECONNECTION_COUNT,
    IntPreference.RECONNECTION_INTERVAL,
    IntPreference.BUFFER_INCOMING,
    IntPreference.BUFFER_OUTGOING,
    BoolPreference.LOG_DO_SAVE_LOG,
    DirPreference.LOG_DIR,
)

internal class SettingFragment : PreferenceFragmentCompat() {
    private var TAG = "@!@SettingFragment"
    private var broadcastReceiver: MainBroadcastReceiver? = null
    private var mManager: WifiManager? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(TAG, "onCreatePreferences")
        setPreferencesFromResource(R.xml.settings, rootKey)
        settingPreferences.forEach {
            it.initPreference(this, preferenceManager.sharedPreferences)
        }
        attachWiFiSuitesClickListener()
        initSelectHomeWiFi()

        initSSLPreferences()
        setCertDirListener()
        setLogDirListener()
    }

    private fun initSelectHomeWiFi() {
        findPreference<MultiSelectListPreference>(SetPreference.HOME_WIFI_SUITES.name)!!.also {
            it.isEnabled = false
        }
        findPreference<TwoStatePreference>(BoolPreference.SELECT_HOME_WIFI.name)!!.also {
            if (it.isChecked) {
//                        Log.d(TAG, "onPreferenceClickListener SELECT_HOME_WIFI = true")
                if (context != null) {
                    if (broadcastReceiver == null) {
                        val filter = IntentFilter()
                        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
//                                filter.addAction(WifiManager.ACTION_WIFI_SCAN_AVAILABILITY_CHANGED)
//            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
//            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                        broadcastReceiver = MainBroadcastReceiver(
                            1,
                            this
                        )
                        requireContext().registerReceiver(broadcastReceiver, filter)
                        Log.d(TAG, "registerReceiver")
                    }
                }
                if (Build.VERSION.SDK_INT < 28) {
//                    getApplicationContext().getSystemService(WIFI_SERVICE).startScan()
                    mManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    mManager?.startScan()
                }
//                findPreference<MultiSelectListPreference>(SetPreference.HOME_WIFI_SUITES.name)!!.also {
//                    it.isEnabled = true
//                }
            } else {
//                        Log.d(TAG, "onPreferenceClickListener SELECT_HOME_WIFI = false")
//                findPreference<MultiSelectListPreference>(SetPreference.HOME_WIFI_SUITES.name)!!.also {
//                    it.isEnabled = false
//                }
                if (context != null) {
                    if (broadcastReceiver != null) {
                        requireContext().unregisterReceiver(broadcastReceiver)
                        broadcastReceiver = null
                        Log.d(TAG, "unregisterReceiver")
                    }
                }
            }
        }
    }

    private fun initSSLPreferences() {
        val params = SSLContext.getDefault().supportedSSLParameters

        findPreference<DropDownPreference>(StrPreference.SSL_VERSION.name)!!.also {
            val versions = arrayOf("DEFAULT") + params.protocols

            it.entries = versions
            it.entryValues = versions
        }

        findPreference<MultiSelectListPreference>(SetPreference.SSL_SUITES.name)!!.also {
            it.entries = params.cipherSuites
            it.entryValues = params.cipherSuites
        }
    }

    private fun attachWiFiSuitesClickListener() {
        Log.d(TAG, "attachHomeWiFiListener")
//        val frag = this
        findPreference<TwoStatePreference>(BoolPreference.SELECT_HOME_WIFI.name)!!.also {
            it.onPreferenceClickListener = object : OnPreferenceClickListener {
                override fun onPreferenceClick(preference: Preference?): Boolean {
//                    Log.d(TAG, "onPreferenceClickListener SELECT_HOME_WIFI")
                    initSelectHomeWiFi()
                    return true
                }
            }
        }
        findPreference<MultiSelectListPreference>(SetPreference.HOME_WIFI_SUITES.name)!!.also {
            it.onPreferenceClickListener = object : OnPreferenceClickListener {
                override fun onPreferenceClick(preference: Preference?): Boolean {
                    Log.d(TAG, "onPreferenceClickListener HOME_WIFI_SUITES")
//                initHomeWiFiSuites()
//                    if (context != null) {
//                        if (broadcastReceiver == null) {
//                            val filter = IntentFilter()
//                            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
////            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
////            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
//                            broadcastReceiver = MainBroadcastReceiver(1,
//                                frag)
//                            requireContext().registerReceiver(broadcastReceiver, filter)
//                            Log.d(TAG, "registerReceiver")
//                            val params =
//                                /*SetPreference.HOME_WIFI_SUITES.getValue(preferenceManager.sharedPreferences) +*/ setOf<String>()
////                            findPreference<MultiSelectListPreference>(SetPreference.HOME_WIFI_SUITES.name)!!.also {
//                                it.entries = params.toTypedArray()
//                                it.entryValues = params.toTypedArray()
////                            }
//                            return false
//                        } else {
//                            requireContext().unregisterReceiver(broadcastReceiver)
//                            broadcastReceiver = null
//                            Log.d(TAG, "unregisterReceiver")
//                            val params =
//                                SetPreference.HOME_WIFI_SUITES.getValue(preferenceManager.sharedPreferences) + setOf<String>(
//                                    "11",
//                                    "22",
//                                    "33")
//
////                            findPreference<MultiSelectListPreference>(SetPreference.HOME_WIFI_SUITES.name)!!.also {
//                                it.entries = params.toTypedArray()
//                                it.entryValues = params.toTypedArray()
////                            }
//                            return true
//                        }
//                    }
                    return false
//                true
                }
//            it.onPreferenceChangeListener =
//                Preference.OnPreferenceChangeListener { preference, newValue ->
////                return@OnPreferenceChangeListener newState as Boolean
////                if (newState == true) {
//                    Log.d(TAG, "onPreferenceChangeListener")
//                    if (context != null) {
//                        if (broadcastReceiver != null) {
//                            requireContext().unregisterReceiver(broadcastReceiver)
//                            broadcastReceiver = null
//                            Log.d(TAG, "unregisterReceiver")
//                        }
//                    }
////                } else {
////                }
//                    true
//                }

            }
//            it.onPrepareDialogBuilder = object : OnPreferenceClickListener {
//            }
//        findPreference<SwitchPreferenceCompat>(BoolPreference.SELECT_HOME_WIFI.name)!!.also {
//            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newState ->
//                if (newState == true) {
//                    true
//                } else {
//                    false
//                }
//            }
//        }
        }
    }

//    private fun initHomeWiFiSuites() {
//
//    }

    private fun setCertDirListener() {
        findPreference<Preference>(DirPreference.SSL_CERT_DIR.name)!!.also {
            it.onPreferenceClickListener = OnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivityForResult(intent, CERT_DIR_REQUEST_CODE)
                true
            }
        }
    }

    private fun setLogDirListener() {
        findPreference<Preference>(DirPreference.LOG_DIR.name)!!.also {
            it.onPreferenceClickListener = OnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                startActivityForResult(intent, LOG_DIR_REQUEST_CODE)
                true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        when (requestCode) {
            CERT_DIR_REQUEST_CODE -> {
                val uri = if (resultCode == Activity.RESULT_OK) resultData?.data?.also {
                    context?.contentResolver?.takePersistableUriPermission(
                        it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } else null

                DirPreference.SSL_CERT_DIR.setValue(
                    preferenceManager.sharedPreferences,
                    uri?.toString() ?: ""
                )
            }

            LOG_DIR_REQUEST_CODE -> {
                val uri = if (resultCode == Activity.RESULT_OK) resultData?.data?.also {
                    context?.contentResolver?.takePersistableUriPermission(
                        it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } else null

                DirPreference.LOG_DIR.setValue(
                    preferenceManager.sharedPreferences,
                    uri?.toString() ?: ""
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initSelectHomeWiFi()
    }

    override fun onPause() {
        super.onPause()
        if (context != null) {
            if (broadcastReceiver != null) {
                requireContext().unregisterReceiver(broadcastReceiver)
                broadcastReceiver = null
                Log.d(TAG, "unregisterReceiver")
            }
        }
    }
}
