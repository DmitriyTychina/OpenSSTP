package amigo.fragment

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat


class WiFiSuitesHomeFragment : PreferenceFragmentCompat() {
    private var TAG = "@!@WiFiSuitesHomeFragment"
//    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
//    init {
//        prefs = preferenceManager.sharedPreferences
//        homePreferences.forEach {
//            it.initPreference(this, prefs /*preferenceManager.sharedPreferences*/)
//        }
//        startVPN()
//    }

//    private lateinit var sharedPreferenceListener: SharedPreferences.OnSharedPreferenceChangeListener // for avoiding GC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = preferenceManager.createPreferenceScreen(activity)
        preferenceScreen = screen
        val preference = EditTextPreference(screen.context)
        preference.key = "EditTextPreference"
        preference.title = "Edit Text Preference1"
        preference.summary = "Click the preference to edit text."
        screen.addPreference(preference)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//        Log.d(TAG, "onCreatePreferences")
//        setPreferencesFromResource(R.xml.home, rootKey)
//        homePreferences.forEach {
//            it.initPreference(this, preferenceManager.sharedPreferences)
//        }
//        attachSharedPreferenceChangeListener()
//        attachConnectorChangeListener()
//        attachAccountClickListener()
////        val linearLayout = findViewById<PreferenceScreen>(R.id.SwitchPreferenceCompat)
//        if (BoolPreference.HOME_CONNECTOR.getValue(preferenceManager.sharedPreferences)) {
//            Log.d(TAG, "startVPN")
//            startVPN()
//        }
////        else
////            preferenceManager.sharedPreferences.edit().putString(StatusPreference.STATUS.name, "").apply()
//
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        Log.d(TAG, "requestCode = $requestCode")
//        if (requestCode == 1111) {
//            // Receiving a result from the AccountPicker
//            if (resultCode == Activity.RESULT_OK) {
//                val account = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
//                if (account != null) {
////                    if (checkPreferences()){
////                        //диалог подтверждающий замену рабочих настроек
//                    setPacketSettings(account,  PreferenceManager.getDefaultSharedPreferences(context)) // загружаем настройки для опреденного аккаунта гугл
////                    }
//                }
////                System.out.println(data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
////                System.out.println(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
//            } else if (resultCode == Activity.RESULT_CANCELED) {
//                Log.d(TAG, "AccountPicker   Activity.RESULT_CANCELED")
////                    Toast.makeText(this, R.string.pick_account, Toast.LENGTH_LONG).show();
//            }
//        } else if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
//            startVpnService(VpnAction.ACTION_CONNECT)
//        }
    }

    private fun makeToast(cause: String) {
        Toast.makeText(context, "INVALID SETTING: $cause", Toast.LENGTH_LONG).show()
    }
}

