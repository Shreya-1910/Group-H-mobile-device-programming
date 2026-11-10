package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import com.mdpgroup08.scpd.data.repository.AuthRepository

/** Handles logging out, so the navigation code never calls Firebase directly. */
class SessionViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    fun signOut() = authRepository.signOut()
}