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

/**
 * Shared dependency container providing repositories and helpers to ViewModels.
 */
class AppContainer {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val keystoreHelper: KeystoreHelper by lazy { KeystoreHelper() }
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(auth, firestore, keystoreHelper) }
    val eventRepository: EventRepository by lazy { FirebaseEventRepository(firestore) }
    val adminRepository: AdminRepository by lazy { FirebaseAdminRepository(firestore) }
}
