package kittoku.osc

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kittoku.osc.databinding.ActivityMainBinding
import kittoku.osc.fragment.HomeFragment
import kittoku.osc.fragment.SettingFragment

class MainActivity : AppCompatActivity() {
    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "HomeClient: ${BuildConfig.VERSION_NAME}"
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val builder = NotificationCompat.Builder(this, "channelID")
//            .setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
//            .setContentTitle("Напоминание")
//            .setContentText("Пора покормить кота")
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//
//        with(NotificationManagerCompat.from(this)) {
//            notify(111, builder.build()) // посылаем уведомление
//        }
        Log.e("@!@", "start!!!!!!!!!!!!!!!")

        //        summary.add("*****************************************")

//        val connectivityManager =
//            getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val capabilities =
//            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
//        if (capabilities != null) {
//            when {
//                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
//                    Log.i("@!@Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
//                }
//                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
//                    Log.i("@!@Internet", "NetworkCapabilities.TRANSPORT_WIFI")
//                }
//                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
//                    Log.i("@!@Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
//                }
//            }
//        }
//        if (capabilities != null) {
//            Log.i("@!@Internetыыыыы", capabilities.getTransportInfo()..toString())
//        }
//        getCurrentSsid(getApplicationContext())?.let { Log.i("@!@wifi", it) }
//        WifiManager wifiManager =
//        (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        tvWifiEnabled.setText("isWifiEnabled(): " + wifiManager.isWifiEnabled());
//        tvWifiState.setText(readtvWifiState(wifiManager));
//
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        if(wifiInfo == null){
//            tvWifiInfo.setText("wifiInfo == null !!!");
//
//            val myWiFiList: MutableList<MWiFi> = ArrayList<MWiFi>()
//            val wifiManager = getApplicationContext().getSystemService(WIFI_SERVICE)
//
//            val wifiInfo = wifiManager.javaClass
//            if (wifiInfo != null) {
//                if (wifiInfo.nnetworkId != -1) {
//                    val mWiFi = MWiFi()
//                    mWiFi.name = wifiInfo.ssid
//                    mWiFi.networkID = wifiInfo.networkId
//                    mWiFi.enabled = true
//                    myWiFiList.add(mWiFi)
//                }
//            }
//
//            // List stored networks
//
//            // List stored networks
//            val configs = wifiManager.configuredNetworks
//
//            if (configs != null) {
//                for (config in configs) {
//                    if (config.networkId != wifiInfo!!.networkId) {
//                        val mWiFi = MWiFi()
//                        mWiFi.name = config.SSID
//                        mWiFi.networkID = config.networkId
//                        mWiFi.enabled = true
//                        myWiFiList.add(mWiFi)
//                    }
//                }
//            }
//        Log.e("@!@HOME_CONNECTOR",it.getValue(prefs).toString())

        // ********
//        var ssid: String = null
//        ConnectivityManager connManager =(ConnectivityManager) context . getSystemService (Context.CONNECTIVITY_SERVICE);
//        val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
//        if (networkInfo.isConnected()) {
//            final WifiManager wifiManager =
//                (WifiManager) context . getSystemService (Context.WIFI_SERVICE);
//            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
//            if (connectionInfo != null && !(connectionInfo.getSSID().equals(""))) {
//                //if (connectionInfo != null && !StringUtil.isBlank(connectionInfo.getSSID())) {
//                ssid = connectionInfo.getSSID();
//            }
//
////        private fun isNetworkConnected(): Boolean {
//            //1
//            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            //2
//            val activeNetwork = cm.activeNetwork
//            //3
//            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
//            //4
////            return networkCapabilities != null &&
////                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//        Toast.makeText(
//            applicationContext,
//            "networkCapabilities: " + if(networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) "1" else "2",
//            Toast.LENGTH_LONG
//        ).show()
//

        //
//        Toast.makeText(
//            applicationContext,
//            "id: " + Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME),
//            Toast.LENGTH_LONG
//        ).show()

        object : FragmentStateAdapter(this) {
            private val homeFragment = HomeFragment()

            private val settingFragment = SettingFragment()
//            private val mqttFragment = HomeFragment()

            override fun getItemCount() = 2
//            override fun getItemCount() = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> homeFragment
                    1 -> settingFragment
//                    2 -> mqttFragment
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
//                2 -> "MQTT"
                else -> throw NotImplementedError()
            }
        }.attach()
    }
}

//fun getCurrentSsid(context: Context): String? {
//    var ssid: String? = null
//    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
////    val networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
////    if (networkInfo!!.isConnected) {
//        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        val connectionInfo = wifiManager.connectionInfo
////        if (connectionInfo != null && connectionInfo.ssid != "") {
//            //if (connectionInfo != null && !StringUtil.isBlank(connectionInfo.getSSID())) {
//            ssid = connectionInfo.ssid
////        }
////    }
//    return ssid
//}