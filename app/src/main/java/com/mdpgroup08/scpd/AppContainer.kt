

package com.mdpgroup08.scpd

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mdpgroup08.scpd.data.local.AppDatabase
import com.mdpgroup08.scpd.data.network.AndroidNetworkMonitor
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.AdminRepository
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.CheckInRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import com.mdpgroup08.scpd.data.repository.FirebaseAdminRepository
import com.mdpgroup08.scpd.data.repository.FirebaseAuthRepository
import com.mdpgroup08.scpd.data.repository.FirebaseEventRepository
import com.mdpgroup08.scpd.data.repository.KeystoreHelper

/**
 * Shared dependency container providing repositories,
 * database access and helpers to ViewModels.
 */
class AppContainer(context: Context) {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val keystoreHelper: KeystoreHelper by lazy {
        KeystoreHelper()
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(auth, firestore, keystoreHelper)
    }

    val eventRepository: EventRepository by lazy {
        FirebaseEventRepository(firestore)
    }

    val adminRepository: AdminRepository by lazy {
        FirebaseAdminRepository(firestore)
    }

    val networkMonitor: NetworkMonitor by lazy {
        AndroidNetworkMonitor(context)
    }

    // Local Room database
    val appDatabase: AppDatabase by lazy {
        AppDatabase.get(context)
    }

    // Local check-in DAO
    val checkInDao by lazy {
        appDatabase.checkInDao()
    }

    // Repository for saving and retrieving offline check-ins
    val checkInRepository: CheckInRepository by lazy {
        CheckInRepository(
            dao = checkInDao,
            auth = auth
        )
    }
}

