package com.app.amigo

import amigo.getSSID
import android.Manifest
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.app.amigo.databinding.ActivityMainBinding
import com.app.amigo.fragment.HomeFragment
import com.app.amigo.fragment.MqttFragment
import com.app.amigo.fragment.SettingFragment
import com.google.android.gms.common.AccountPicker
import com.google.android.material.tabs.TabLayoutMediator


fun WifiManager.deviceName(): String = connectionInfo.ssid.run {
    if (this.contains("<unknown ssid>")) "UNKNOWN" else this
}


class MainActivity : AppCompatActivity() {
    private var TAG = "@!@MainActivity"
    private var wifiManager: WifiManager? = null
    var cm: ConnectivityManager? = null

    @SuppressLint("NewApi")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "HomeClient: ${BuildConfig.VERSION_NAME}"
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "start")




        object : FragmentStateAdapter(this) {
            private val homeFragment = HomeFragment()
            private val settingFragment = SettingFragment()
            private val mqttFragment = MqttFragment()

            //            override fun getItemCount() = 2
            override fun getItemCount() = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> homeFragment
                    1 -> settingFragment
                    2 -> mqttFragment
                    else -> throw NotImplementedError()
                }
            }
        }.also {
            binding.pager.adapter = it
        }

        TabLayoutMediator(binding.tabBar, binding.pager) { tab, position ->
            tab.text = when (position) {
                0 -> "HOME"
                1 -> "SETTING"
                2 -> "MQTT"
                else -> throw NotImplementedError()
            }
        }.attach()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
                    )
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.GET_ACCOUNTS,
                ), 0
            )
            //do something if have the permissions
        }
//        requestPermissions(arrayOf(Manifest.permission.GET_ACCOUNTS), 0)


//        public void pickUserAccount() {
//            /*This will list all available accounts on device without any filtering*/
//            Intent intent = AccountPicker.newChooseAccountIntent(null, null,
//            null, false, null, null, null, null);
//            startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
//        }
///*After manually selecting every app related account, I got its Account type using the code below*/
//        @Override
//        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//            if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
//                // Receiving a result from the AccountPicker
//                if (resultCode == RESULT_OK) {
//                    System.out.println(data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
//                    System.out.println(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
//                } else if (resultCode == RESULT_CANCELED) {
//                    Toast.makeText(this, R.string.pick_account, Toast.LENGTH_LONG).show();
//                }
//            }
//        }

        val aco = AccountPicker.AccountChooserOptions.Builder()
            .setAlwaysShowAccountPicker(true)
            .setAllowableAccountsTypes(listOf("com.google")) //
            .build()
        val intent = AccountPicker.newChooseAccountIntent(aco) as Intent
        startActivityForResult(intent, 1111);
//        val result: GoogleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(intent)
//        val acct: GoogleSignInAccount = result.signInAccount
//        val personName = acct.displayName
//        val personGivenName = acct.givenName
//        val personFamilyName = acct.familyName
//        val personEmail = acct.email
//        val personId = acct.id
//        val personPhoto: Uri = acct.photoUrl
//        Log.d(TAG, "personName: ${personName}")
//        Log.d(TAG, "personGivenName: ${personGivenName}")
//        Log.d(TAG, "personFamilyName: ${personFamilyName}")
//        Log.d(TAG, "personEmail: ${personEmail}")
//        Log.d(TAG, "personId: ${personId}")
//        Log.d(TAG, "personPhoto: ${personPhoto}")


        val am =
            getSystemService(Context.ACCOUNT_SERVICE) as AccountManager //AccountManager.get(applicationContext) // current Context
        val AccInfo = findViewById<View>(R.id.textView1) as TextView
        val accounts = am.accounts
//        tvInfo.text = accounts.toList().toString()
        for (account in accounts) {
            AccInfo.text = account.name+"!"
            if (account.type.equals("com.google", ignoreCase = true)) {
                //Что-то делаем
            }
        }

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        Log.d(TAG, "wifiManager.deviceName " + wifiManager!!.deviceName())
        Log.d(TAG, "getSSID " + getSSID(applicationContext))
        val wifiInfo = wifiManager!!.connectionInfo
        Toast.makeText(
            applicationContext,
            getSSID(applicationContext),
            Toast.LENGTH_LONG
        ).show()
        cm = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    val networkCallback = object : ConnectivityManager.NetworkCallback() {

        // Called when the framework connects and has declared a new network ready for use.
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "onAvailable: ${network}")
            //*******
            val connManager =
                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//                val networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            val linkProperties = connManager.getLinkProperties(network)
            Log.d(TAG, "LinkProperties $linkProperties")

            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wifiManager.connectionInfo
            if (connectionInfo != null) {
                Log.d(TAG, "connectionInfo: $connectionInfo")
            }
            //*******
            val capabilities = cm!!.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                Log.d(TAG, "NetworkCapabilities: TRANSPORT_WIFI")


                //                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    capabilities.transportInfo
//                }
            }
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                Log.d(TAG, "NetworkCapabilities: TRANSPORT_CELLULAR")
            }
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                Log.d(TAG, "NetworkCapabilities: TRANSPORT_VPN")
            }
        }

        // Called when a network disconnects or otherwise no longer satisfies this request or callback
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "onLost: ${network}")
        }
    }

    override fun onStart() {
        super.onStart()
        cm!!.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        cm!!.unregisterNetworkCallback(networkCallback)
    }
}
