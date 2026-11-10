package com.mdpgroup08.scpd

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import com.mdpgroup08.scpd.data.repository.FirebaseAuthRepository
import com.mdpgroup08.scpd.data.repository.FirebaseEventRepository
import kotlin.getValue

/**
 * Creates each repository once and shares it with ViewModels.
 * Member 4 (Mark) : add EventRepository and BookingRepository here.
 */

class AppContainer {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val authRepository: AuthRepository by lazy { FirebaseAuthRepository(auth, firestore) }
    val eventRepository: EventRepository by lazy { FirebaseEventRepository(firestore) }
}