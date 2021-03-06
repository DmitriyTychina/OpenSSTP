package com.app.amigo.misc

import android.net.*
import androidx.preference.PreferenceManager
import com.app.amigo.ControlClientVPN
import com.app.amigo.fragment.StatusPreference
import org.chromium.base.Log
import java.text.SimpleDateFormat
import java.util.*

internal class NetworkObserver(val parent: ControlClientVPN) {
    private val manager = parent.vpnService.getSystemService(ConnectivityManager::class.java)
    private var TAG = "@!@NetworkObserver"

    private var callback: ConnectivityManager.NetworkCallback? = null

    private val prefs =
        PreferenceManager.getDefaultSharedPreferences(parent.vpnService.applicationContext)

    init {
//        wipeStatus()
        val request = NetworkRequest.Builder().let {
            it.addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            it.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            it.build()
        }

        callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.e(TAG, "onAvailable VPN: ${network}")
                Log.d(
                    TAG,
                    "NetworkCapabilities onAvailable: " + manager.getNetworkCapabilities(network)
                        .toString()
                )
//                parent.checkNetworks()
//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
//                    manager.getLinkProperties(network)?.also { linkProperties ->
//                        makeSummary(linkProperties).also {
//                            prefs.edit().putString(StatusPreference.STATUS.name, it).apply()
//                        }
//                    }
//                }
//                parent?.launchJobRun(1)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.e(TAG, "onLost VPN: ${network}")
//                parent.checkNetworks()
//                parent?.launchJobRun(2)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                parent.refreshStatus()
                Log.e(
                    TAG,
                    "onLinkPropertiesChanged VPN: ${network} linkProperties: $linkProperties"
                )
                parent.stateAndSettings.vpn_ip = linkProperties.linkAddresses.toString()
                parent.stateAndSettings.vpn_dns = linkProperties.dnsServers.toString()
                parent.refreshStatus()
                //                makeSummary(linkProperties).also {
//                    prefs.edit().putString(StatusPreference.STATUS.name, it).apply()
//                }
            }
        }

//        manager.registerNetworkCallback(request, callback)
        callback?.let { manager.registerNetworkCallback(request, it) }
    }

    //    @SuppressLint("ServiceCast")
    private fun makeSummary(properties: LinkProperties): String {
        val summary = mutableListOf<String>()
        summary.add(SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault()).format(Date()))
        summary.add("")

        summary.add("[Assigned IP Address]")
        properties.linkAddresses.forEach {
            summary.add(it.address.hostAddress)
        }
        summary.add("")

        summary.add("[DNS server]")
        properties.dnsServers.forEach {
            summary.add(it.hostAddress)
        }
        summary.add("")

        summary.add("[Route]")
        properties.routes.forEach {
            summary.add(it.toString())
        }
        summary.add("")

        summary.add("[SSL/TLS parameters]")
        summary.add("PROTOCOL: ${parent.sslTerminal?.socket?.session?.protocol}")
        summary.add("SUITE: ${parent.sslTerminal?.socket?.session?.cipherSuite}")

        return summary.reduce { acc, s ->
            acc + "\n" + s
        }
    }

    private fun wipeStatus() {
        prefs.edit().putString(StatusPreference.STATUS.name, "").apply()
    }

    internal fun close() {
        Log.e("@!@observer", "unregisterNetworkCallback " + callback)
        callback?.let { manager?.unregisterNetworkCallback(it) } // ?????????????????? ???????????????????? ?????? ???????????????? Android
        callback = null
//        wipeStatus()
    }
}
