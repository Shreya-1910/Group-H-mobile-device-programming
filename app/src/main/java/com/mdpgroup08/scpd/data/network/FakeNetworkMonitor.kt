package com.mdpgroup08.scpd.data.network

import kotlinx.coroutines.flow.MutableStateFlow

/** Lets tests switch the connection on and off. */
class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    override val isOnline = MutableStateFlow(online)
}