package com.mdpgroup08.scpd

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mdpgroup08.scpd.data.repository.AdminRepository
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import com.mdpgroup08.scpd.data.repository.FirebaseAdminRepository
import com.mdpgroup08.scpd.data.repository.FirebaseAuthRepository
import com.mdpgroup08.scpd.data.repository.FirebaseEventRepository
import com.mdpgroup08.scpd.data.repository.KeystoreHelper
import android.content.Context
import com.mdpgroup08.scpd.data.network.AndroidNetworkMonitor
import com.mdpgroup08.scpd.data.network.NetworkMonitor

/**
 * Shared dependency container providing repositories and helpers to ViewModels.
 */
class AppContainer(context: Context) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val keystoreHelper: KeystoreHelper by lazy { KeystoreHelper() }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(auth, firestore, keystoreHelper) }
    val eventRepository: EventRepository by lazy { FirebaseEventRepository(firestore) }
    val adminRepository: AdminRepository by lazy { FirebaseAdminRepository(firestore) }

    val networkMonitor: NetworkMonitor by lazy { AndroidNetworkMonitor(context) }
}
