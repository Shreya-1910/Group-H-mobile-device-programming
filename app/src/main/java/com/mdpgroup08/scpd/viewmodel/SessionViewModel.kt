package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.AccountStatus
import com.mdpgroup08.scpd.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/** Handles logging out, so the navigation code never calls Firebase directly. */
class SessionViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _isDeactivated = MutableStateFlow(false)
    val isDeactivated: StateFlow<Boolean> = _isDeactivated.asStateFlow()

    init {
        monitorAccountStatus()
    }

    private fun monitorAccountStatus() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L) // Poll every 5 minutes (R6-S11 AC1)
                try {
                    val user = authRepository.currentUser()
                    if (user?.accountStatus == AccountStatus.DEACTIVATED.name) {
                        _isDeactivated.value = true
                    }
                } catch (e: Exception) {
                    // Ignore offline errors
                }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}