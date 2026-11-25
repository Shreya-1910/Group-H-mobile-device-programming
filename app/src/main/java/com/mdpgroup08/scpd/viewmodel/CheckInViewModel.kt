
package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.local.CheckInMethod
import com.mdpgroup08.scpd.data.repository.CheckInRepository
import com.mdpgroup08.scpd.data.repository.SaveCheckInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckInUiState(
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class CheckInViewModel(
    private val repository: CheckInRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    /**
     * Save a check-in locally.
     * No internet connection is required.
     */
    fun saveCheckIn(
        eventId: String,
        codeCounter: Long,
        signature: String,
        method: CheckInMethod = CheckInMethod.QR
    ) {
        if (_uiState.value.isSaving) return

        viewModelScope.launch {

            _uiState.value = CheckInUiState(isSaving = true)

            val result = repository.saveCheckInOffline(
                eventId = eventId,
                codeCounter = codeCounter,
                method = method,
                signature = signature
            )

            _uiState.value = when (result) {

                is SaveCheckInResult.Saved ->
                    CheckInUiState(
                        successMessage =
                            "Check-in saved offline. It is waiting to sync."
                    )

                SaveCheckInResult.AlreadyExists ->
                    CheckInUiState(
                        errorMessage =
                            "You have already checked in to this event."
                    )

                SaveCheckInResult.NotLoggedIn ->
                    CheckInUiState(
                        errorMessage =
                            "Please sign in before checking in."
                    )

                is SaveCheckInResult.Failed ->
                    CheckInUiState(
                        errorMessage = result.message
                    )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = CheckInUiState()
    }
}
