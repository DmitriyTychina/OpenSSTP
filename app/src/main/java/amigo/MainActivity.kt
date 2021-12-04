package com.app.amigo

import amigo.fragment.HomeFragment
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.app.amigo.databinding.ActivityMainBinding
import com.app.amigo.fragment.MqttFragment
import com.app.amigo.fragment.SettingFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private var TAG = "@!@MainActivity"
    lateinit var cm: ConnectivityManager

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
                    || (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)
                    || (checkSelfPermission(Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
//                    || (checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)
                    )
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.GET_ACCOUNTS,
//                    Manifest.permission.CHANGE_WIFI_STATE,
                ), 0
            )
            //do something if have the permissions
        }
        cm = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    val networkCallback = object : ConnectivityManager.NetworkCallback() {

        // Called when the framework connects and has declared a new network ready for use.
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "onAvailable: $network")
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
            val capabilities = cm.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                Log.d(TAG, "NetworkCapabilities: TRANSPORT_WIFI")
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
        cm.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        cm.unregisterNetworkCallback(networkCallback)
    }
}
