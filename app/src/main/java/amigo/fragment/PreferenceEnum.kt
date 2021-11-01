package com.app.amigo.fragment

import android.content.SharedPreferences
import android.net.Uri
import android.text.InputType
import android.text.TextUtils
import androidx.preference.*
import com.app.amigo.DEFAULT_MRU
import com.app.amigo.DEFAULT_MTU

private var TAG = "@!@PreferenceEnum"

internal const val TYPE_PASSWORD =
    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

internal interface PreferenceWrapper<T> {
    val name: String

    val defaultValue: T

    fun getValue(prefs: SharedPreferences): T

    fun setValue(
        fragment: PreferenceFragmentCompat,
        value: T
    ) // Use Preference to ensure its summary is updated.

    fun initValue(fragment: PreferenceFragmentCompat, prefs: SharedPreferences) {
        if (!prefs.contains(name)) {
            setValue(fragment, defaultValue)
        }
    }

    fun initPreference(fragment: PreferenceFragmentCompat, prefs: SharedPreferences)

    fun restoreDefault(fragment: PreferenceFragmentCompat) {
        setValue(fragment, defaultValue)
    }
}

private fun EditTextPreference.setInputType(type: Int) {
    setOnBindEditTextListener { editText ->
        editText.inputType = type
    }
}

internal enum class StrPreference(override val defaultValue: String) : PreferenceWrapper<String> {
    HOME_HOST(""),
    HOME_USER(""),
    HOME_PASS(""),
//    MQTT_HOST(""),
//    MQTT_USER(""),
//    MQTT_PASS(""),
    SSL_VERSION("DEFAULT");

    override fun getValue(prefs: SharedPreferences): String {
        return prefs.getString(name, defaultValue)!!
    }

    override fun setValue(fragment: PreferenceFragmentCompat, value: String) {
        if (this == SSL_VERSION) {
            fragment.findPreference<DropDownPreference>(name)!!.also {
                it.value = value
            }
//        } else if (this == ACCOUNT){
//            Log.d(TAG, "setValue ACCOUNT")
//            fragment.findPreference<EditTextPreference>(name)!!.also {
//                this.(clickAccount())
//            }
        } else {
            fragment.findPreference<EditTextPreference>(name)!!.also {
                it.text = value
            }
        }
    }

    override fun initPreference(fragment: PreferenceFragmentCompat, prefs: SharedPreferences) {
        if (this == SSL_VERSION) {
            fragment.findPreference<DropDownPreference>(name)!!.also {
                it.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                initValue(fragment, prefs)
            }
        } else {
            fragment.findPreference<EditTextPreference>(name)!!.also {
                if (this == HOME_PASS || this == HOME_HOST /*|| this == MQTT_PASS || this == MQTT_USER*/) {
                    it.summaryProvider = passwordSummaryProvider
                    it.setInputType(TYPE_PASSWORD)
                } else {
                    it.summaryProvider = normalSummaryProvider
                    it.setInputType(InputType.TYPE_CLASS_TEXT)
                }

                initValue(fragment, prefs)
            }
        }
    }
}

internal enum class DirPreference(override val defaultValue: String) : PreferenceWrapper<String> {
    SSL_CERT_DIR(""),
    LOG_DIR("");

    override fun getValue(prefs: SharedPreferences): String {
        return prefs.getString(name, defaultValue)!!
    }

    override fun setValue(fragment: PreferenceFragmentCompat, value: String) {
        fragment.findPreference<Preference>(name)!!.also {
            it.sharedPreferences.edit().also { editor ->
                editor.putString(name, value)
                editor.apply()
            }

            it.provideSummary(value)
        }
    }

    override fun initPreference(fragment: PreferenceFragmentCompat, prefs: SharedPreferences) {
        fragment.findPreference<Preference>(name)!!.also {
            it.provideSummary(getValue(it.sharedPreferences))
            initValue(fragment, prefs)
        }
    }

    private fun Preference.provideSummary(uri: String) {
        summary = if (TextUtils.isEmpty(uri)) {
            "[No Directory Selected]"
        } else {
            Uri.parse(uri).path
        }
    }
}

internal enum class StatusPreference(override val defaultValue: String) :
    PreferenceWrapper<String> {
    ACCOUNT(""),
    STATUS("");
//    MQTT_STATUS("");

    override fun getValue(prefs: SharedPreferences): String {
        return prefs.getString(name, defaultValue)!!
    }

    override fun setValue(fragment: PreferenceFragmentCompat, value: String) {
        fragment.findPreference<Preference>(name)!!.also {
            it.sharedPreferences.edit().also { editor ->
                editor.putString(name, value)
                editor.apply()
            }
            it.provideSummary(value)
        }
    }

    override fun initPreference(fragment: PreferenceFragmentCompat, prefs: SharedPreferences) {
        fragment.findPreference<Preference>(name)!!.also {
            it.provideSummary(getValue(it.sharedPreferences))
            initValue(fragment, prefs)
        }
    }

    private fun Preference.provideSummary(value: String) {
        summary = if (TextUtils.isEmpty(value)) {
            "[No Connection Established]"
        } else {
            value
        }
    }
}

internal enum class SetPreference(override val defaultValue: Set<String>) :
    PreferenceWrapper<Set<String>> {
    SSL_SUITES(setOf<String>());
//    HOME_WIFI_SUITES(setOf<String>());

    override fun getValue(prefs: SharedPreferences): Set<String> {
        return prefs.getStringSet(name, defaultValue)!!
    }

    override fun setValue(fragment: PreferenceFragmentCompat, value: Set<String>) {
        fragment.findPreference<MultiSelectListPreference>(name)!!.also {
            it.values = value
        }
    }

    override fun initPreference(fragment: PreferenceFragmentCompat, prefs: SharedPreferences) {
        fragment.findPreference<MultiSelectListPreference>(name)!!.also {
            it.summaryProvider = suitesSummaryProvider
            initValue(fragment, prefs)
        }
    }
}

internal enum class BoolPreference(override val defaultValue: Boolean) :
    PreferenceWrapper<Boolean> {
    HOME_CONNECTOR(false),
//    MQTT_CONNECTOR(false),
//    SELECT_HOME_WIFI(false),
    SSL_DO_VERIFY(true),
    SSL_DO_ADD_CERT(false),
    SSL_DO_SELECT_SUITES(false),
    PPP_PAP_ENABLED(false),
    PPP_MSCHAPv2_ENABLED(true),
    PPP_IPv4_ENABLED(true),
    PPP_IPv6_ENABLED(false),
    IP_ONLY_LAN(true),
    IP_ONLY_ULA(false),
    RECONNECTION_ENABLED(true),
    LOG_DO_SAVE_LOG(false);

    override fun getValue(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(name, defaultValue)
    }

    override fun setValue(fragment: PreferenceFragmentCompat, value: Boolean) {
        fragment.findPreference<TwoStatePreference>(name)!!.also {
            it.isChecked = value
        }
    }

    //    @SuppressLint("LongLogTag")
    override fun initPreference(fragment: PreferenceFragmentCompat, prefs: SharedPreferences) {
        fragment.findPreference<TwoStatePreference>(name)!!.also {
            if (this == HOME_CONNECTOR /*|| this == MQTT_CONNECTOR*/) {
                it.callChangeListener(getValue(it.sharedPreferences)) // запускает при старте если было запущено
            }
//            Log.d(TAG, "initPreference BoolPreference $fragment::$prefs::$this")
            initValue(fragment, prefs)
            it.isSingleLineTitle = false
        }
    }
}

internal enum class IntPreference(override val defaultValue: Int) : PreferenceWrapper<Int> {
    SSL_PORT(443),
//    MQTT_PORT(1883),
    PPP_MRU(DEFAULT_MRU),
    PPP_MTU(DEFAULT_MTU),
    IP_PREFIX(0),
    RECONNECTION_COUNT(0),
    RECONNECTION_INTERVAL(10),
    BUFFER_INCOMING(16384),
    BUFFER_OUTGOING(16384);

    override fun getValue(prefs: SharedPreferences): Int {
        return prefs.getString(name, defaultValue.toString())!!.toIntOrNull() ?: defaultValue
    }

    override fun setValue(fragment: PreferenceFragmentCompat, value: Int) {
        fragment.findPreference<EditTextPreference>(name)!!.also {
            it.text = value.toString()
        }
    }

    override fun initPreference(fragment: PreferenceFragmentCompat, prefs: SharedPreferences) {
        fragment.findPreference<EditTextPreference>(name)!!.also {
            it.summaryProvider = if (this == IP_PREFIX) {
                it.dialogMessage = "0 means prefix length will be inferred"
                zeroDefaultSummaryProvider
            } else {
                numSummaryProvider
            }

            it.setInputType(InputType.TYPE_CLASS_NUMBER)
            initValue(fragment, prefs)
        }
    }
}
