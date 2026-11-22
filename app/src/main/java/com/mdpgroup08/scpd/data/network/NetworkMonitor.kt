package com.mdpgroup08.scpd.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tells ViewModels whether the phone is online. Shared by the whole app. */
interface NetworkMonitor {
    /** True while the phone has a working internet connection. */
    val isOnline: StateFlow<Boolean>
}

/** Real implementation using Android's ConnectivityManager. Created once in AppContainer. */
class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val _isOnline = MutableStateFlow(currentlyOnline())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                _isOnline.value = caps.hasInternet()
            }
            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        })
    }

    private fun currentlyOnline(): Boolean =
        connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.hasInternet() ?: false

    private fun NetworkCapabilities.hasInternet() =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}