package com.app.amigo.misc

import android.net.*
import android.os.Build
import androidx.preference.PreferenceManager
import com.app.amigo.ControlClient
import com.app.amigo.fragment.StatusPreference
import org.chromium.base.Log
import java.text.SimpleDateFormat
import java.util.*

internal class NetworkObserver(val parent: ControlClient) {
    private val manager = parent.vpnService.getSystemService(ConnectivityManager::class.java)
    private var TAG = "@!@NetworkObserver"

    private var callback: ConnectivityManager.NetworkCallback? = null

    private val prefs =
        PreferenceManager.getDefaultSharedPreferences(parent.vpnService.applicationContext)

    init {
        wipeStatus()
        val request = NetworkRequest.Builder().let {
            it.addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            it.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            it.build()
        }

        callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                Log.e(TAG, "onAvailable: ${network}")
                Log.d(TAG,
                    "NetworkCapabilities onAvailable: " + manager.getNetworkCapabilities(network)
                        .toString()
                )
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    manager.getLinkProperties(network)?.also { linkProperties ->
                        makeSummary(linkProperties).also {
                            prefs.edit().putString(StatusPreference.STATUS.name, it).apply()
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.e(TAG, "onLost: ${network}")
//            parent.checkNetworks()
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                makeSummary(linkProperties).also {
                    prefs.edit().putString(StatusPreference.STATUS.name, it).apply()
                }
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
        summary.add("PROTOCOL: ${parent.sslTerminal.socket.session.protocol}")
        summary.add("SUITE: ${parent.sslTerminal.socket.session.cipherSuite}")

        return summary.reduce { acc, s ->
            acc + "\n" + s
        }
    }

    private fun wipeStatus() {
        prefs.edit().putString(StatusPreference.STATUS.name, "").apply()
    }

    internal fun close() {
        Log.e("@!@observer", "callback " + callback)
        callback?.let { manager?.unregisterNetworkCallback(it) } // убрал - для автозапуска при загрузке Android
        callback = null
        wipeStatus()
    }
}
