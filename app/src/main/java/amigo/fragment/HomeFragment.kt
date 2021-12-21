package amigo.fragment

import amigo.unit.setPacketSettings
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.preference.*
import com.app.amigo.*
import com.app.amigo.R
import com.app.amigo.fragment.*
import com.google.android.gms.common.AccountPicker

private val homePreferences = arrayOf<PreferenceWrapper<*>>(
    StatusPreference.ACCOUNT,
    BoolPreference.HOME_CONNECTOR,
    StatusPreference.CONNECTEDVIA,
    StatusPreference.STATUS,
)

class HomeFragment : PreferenceFragmentCompat() {
    private var TAG = "@!@HomeFragment"
//    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
//    init {
//        prefs = preferenceManager.sharedPreferences
//        homePreferences.forEach {
//            it.initPreference(this, prefs /*preferenceManager.sharedPreferences*/)
//        }
//        startVPN()
//    }

    private lateinit var sharedPreferenceListener: SharedPreferences.OnSharedPreferenceChangeListener // for avoiding GC

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(TAG, "onCreatePreferences")
        setPreferencesFromResource(R.xml.home, rootKey)
        homePreferences.forEach {
            it.initPreference(this, preferenceManager.sharedPreferences)
        }
        attachSharedPreferenceChangeListener()
        attachConnectorChangeListener()
        attachAccountClickListener()
//        val linearLayout = findViewById<PreferenceScreen>(R.id.SwitchPreferenceCompat)
        if (BoolPreference.HOME_CONNECTOR.getValue(preferenceManager.sharedPreferences)) {
            Log.d(TAG, "startVPN")
            startVPN()
        }
//        else
//            preferenceManager.sharedPreferences.edit().putString(StatusPreference.STATUS.name, "").apply()

    }

    //    @SuppressLint("LongLogTag")
    private fun attachSharedPreferenceChangeListener() {
        // for updating by both user and system
        sharedPreferenceListener =
            SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    BoolPreference.HOME_CONNECTOR.name -> {
                        BoolPreference.HOME_CONNECTOR.also {
                            it.setValueFragment(it.getValue(prefs))
//  delete                      prefs.edit().putBoolean(BoolPreference.HOME_CONNECTOR.name, it.getValue(prefs)).apply()
                        }
                    }
                    StatusPreference.STATUS.name -> {
                        StatusPreference.STATUS.also {
                            it.setValueFragment(it.getValue(prefs))
                        }
                    }
                    StatusPreference.ACCOUNT.name -> {
                        StatusPreference.ACCOUNT.also {
                            it.setValueFragment(it.getValue(prefs))
                        }
                    }
                    StatusPreference.CONNECTEDVIA.name -> {
                        StatusPreference.CONNECTEDVIA.also {
                            it.setValueFragment(it.getValue(prefs))
                        }
                    }
                }
            }
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(
            sharedPreferenceListener
        )
    }

    private fun startVPN(): Boolean {
        if (!checkPreferences()) {
            Log.d(TAG, "checkPreferences=false")
            StatusPreference.STATUS.setValue(
                preferenceManager.sharedPreferences,
                "Неверно заданы настройки"
            )
            return false
        }
        val intent = VpnService.prepare(context)
        if (intent != null) {
            Log.d(TAG, "VpnService уже запущен")
            startActivityForResult(intent, 0)
        } else {
            Log.d(TAG, "startVpnService")
//            StatusPreference.STATUS.setValue(preferenceManager.sharedPreferences,  "")
            onActivityResult(0, Activity.RESULT_OK, null)
        }
        return true
    }

    private fun attachConnectorChangeListener() {
        // for disconnecting by user in HomeFragment
        Log.d(TAG, "attachConnectorChangeListener")
        findPreference<TwoStatePreference>(BoolPreference.HOME_CONNECTOR.name)!!.also {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newState ->
                if (newState == true) {
                    return@OnPreferenceChangeListener startVPN()
                } else {
                    startVpnService(VpnAction.ACTION_DISCONNECT)
                }
                true
            }
        }
    }

    //    @SuppressLint("WrongConstant")
    private fun attachAccountClickListener() {
        Log.d(TAG, "attachAccountListener")
        findPreference<Preference>(StatusPreference.ACCOUNT.name)!!.also {
//            it.shouldDisableView = false
//            it.isEnabled = false
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val aco = AccountPicker.AccountChooserOptions.Builder()
                    .setAlwaysShowAccountPicker(true)
                    .setAllowableAccountsTypes(listOf("com.google"))
                    .build()
                val intent = AccountPicker.newChooseAccountIntent(aco) as Intent
                startActivityForResult(intent, 1111);
                true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        Log.d(TAG, "requestCode = $requestCode")
        if (requestCode == 1111) {
            // Receiving a result from the AccountPicker
            if (resultCode == Activity.RESULT_OK) {
                val account = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (account != null) {
//                    if (checkPreferences()){
//                        //диалог подтверждающий замену рабочих настроек
                    setPacketSettings(
                        account,
                        PreferenceManager.getDefaultSharedPreferences(context)
                    ) // загружаем настройки для опреденного аккаунта гугл
//                    }
                }
//                System.out.println(data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
//                System.out.println(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "AccountPicker   Activity.RESULT_CANCELED")
//                    Toast.makeText(this, R.string.pick_account, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            startVpnService(VpnAction.ACTION_CONNECT)
        }
    }

    private fun startVpnService(action: VpnAction) {
//        context?.startService(Intent(context, SstpVpnService::class.java).setAction(action.value))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(
                Intent(context, SstpVpnService::class.java).setAction(
                    action.value
                )
            )
        } else {
            context?.startService(
                Intent(
                    context,
                    SstpVpnService::class.java
                ).setAction(action.value)
            )
        }
    }

    private fun makeToast(cause: String) {
        Toast.makeText(context, "INVALID SETTING: $cause", Toast.LENGTH_LONG).show()
    }

    private fun checkPreferences(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        StrPreference.HOME_HOST.getValue(prefs).also {
            if (TextUtils.isEmpty(it)) {
                makeToast("Host is missing")
                return false
            }
        }

        val SelectHomeWiFi = BoolPreference.SELECT_HOME_WIFI.getValue(prefs)
        val homeWiFisuites = SetPreference.HOME_WIFI_SUITES.getValue(prefs)
        if (SelectHomeWiFi && homeWiFisuites.isEmpty()) {
            makeToast("No Home WiFi suite was selected")
            return false
        }

        IntPreference.SSL_PORT.getValue(prefs).also {
            if (it !in 0..65535) {
                makeToast("The given port is out of 0-65535")
                return false
            }
        }

        val doAddCerts = BoolPreference.SSL_DO_ADD_CERT.getValue(prefs)
        val version = StrPreference.SSL_VERSION.getValue(prefs)
        if (doAddCerts && version == "DEFAULT") {
            makeToast("Adding trusted certificates needs SSL version to be specified")
            return false
        }

        val certDir = DirPreference.SSL_CERT_DIR.getValue(prefs)
        if (doAddCerts && certDir.isEmpty()) {
            makeToast("No certificates directory was selected")
            return false
        }

        val doSelectSuites = BoolPreference.SSL_DO_SELECT_SUITES.getValue(prefs)
        val suites = SetPreference.SSL_SUITES.getValue(prefs)
        if (doSelectSuites && suites.isEmpty()) {
            makeToast("No cipher suite was selected")
            return false
        }

        val mru = IntPreference.PPP_MRU.getValue(prefs).also {
            if (it !in MIN_MRU..MAX_MRU) {
                makeToast("The given MRU is out of $MIN_MRU-$MAX_MRU")
                return false
            }
        }

        val mtu = IntPreference.PPP_MTU.getValue(prefs).also {
            if (it !in MIN_MTU..MAX_MTU) {
                makeToast("The given MRU is out of $MIN_MTU-$MAX_MTU")
                return false
            }
        }

        val isIpv4Enabled = BoolPreference.PPP_IPv4_ENABLED.getValue(prefs)
        val isIpv6Enabled = BoolPreference.PPP_IPv6_ENABLED.getValue(prefs)
        if (!isIpv4Enabled && !isIpv6Enabled) {
            makeToast("No network protocol was enabled")
            return false
        }

        val isPapEnabled = BoolPreference.PPP_PAP_ENABLED.getValue(prefs)
        val isMschapv2Enabled = BoolPreference.PPP_MSCHAPv2_ENABLED.getValue(prefs)
        if (!isPapEnabled && !isMschapv2Enabled) {
            makeToast("No authentication protocol was enabled")
            return false
        }

        IntPreference.IP_PREFIX.getValue(prefs).also {
            if (it !in 0..32) {
                makeToast("The given address prefix length is out of 0-32")
                return false
            }
        }

        IntPreference.RECONNECTION_COUNT.getValue(prefs).also {
            if (it < 0) {
                makeToast("Retry Count must be a positive integer")
                return false
            }
        }

        IntPreference.BUFFER_INCOMING.getValue(prefs).also {
            if (it < 2 * mru) {
                makeToast("Incoming Buffer Size must be >= 2 * MRU")
                return false
            }
        }

        IntPreference.BUFFER_OUTGOING.getValue(prefs).also {
            if (it < 2 * mtu) {
                makeToast("Outgoing Buffer Size must be >= 2 * MTU")
                return false
            }
        }

        val doSaveLog = BoolPreference.LOG_DO_SAVE_LOG.getValue(prefs)
        val logDir = DirPreference.LOG_DIR.getValue(prefs)
        if (doSaveLog && logDir.isEmpty()) {
            makeToast("No log directory was selected")
            return false
        }
        return true
    }
}
