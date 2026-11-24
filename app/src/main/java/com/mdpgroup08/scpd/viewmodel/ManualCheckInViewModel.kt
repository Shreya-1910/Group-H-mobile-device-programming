
package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.validation.CheckInCodeLockoutManager
import com.mdpgroup08.scpd.data.validation.CheckInCodeValidator
import com.mdpgroup08.scpd.data.validation.CodeValidationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManualCheckInUiState(
    val message: String? = null,
    val isSuccessful: Boolean = false,
    val isLocked: Boolean = false,
    val attemptsRemaining: Int = 5,
    val lockoutSeconds: Int = 0
)

class ManualCheckInViewModel : ViewModel() {

    companion object {
        // Temporary demonstration code.
        // Replace this with the event's actual accepted code later.
        private const val DEMO_CODE = "123456"
    }

    private val lockoutManager = CheckInCodeLockoutManager()

    private val _uiState = MutableStateFlow(
        ManualCheckInUiState()
    )

    val uiState: StateFlow<ManualCheckInUiState> =
        _uiState.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * Validates a manually entered six-digit code.
     */
    fun submitCode(enteredCode: String) {

        if (_uiState.value.isLocked) return

        when (
            CheckInCodeValidator.validate(
                enteredCode = enteredCode,
                acceptedCodes = setOf(DEMO_CODE)
            )
        ) {

            CodeValidationResult.InvalidFormat -> {
                _uiState.value = _uiState.value.copy(
                    message = "Enter a valid six-digit numeric code.",
                    isSuccessful = false
                )
            }

            CodeValidationResult.IncorrectCode -> {
                handleIncorrectCode()
            }

            CodeValidationResult.Valid -> {
                lockoutManager.resetAttempts()
                countdownJob?.cancel()

                _uiState.value = ManualCheckInUiState(
                    message = "Code accepted successfully!",
                    isSuccessful = true,
                    attemptsRemaining = 5
                )
            }
        }
    }

    /**
     * Handles an incorrect code attempt.
     */
    private fun handleIncorrectCode() {

        val lockoutActivated =
            lockoutManager.registerFailedAttempt()

        if (lockoutActivated) {

            _uiState.value = ManualCheckInUiState(
                message = "Too many incorrect attempts. Please wait.",
                isLocked = true,
                attemptsRemaining = 0,
                lockoutSeconds = 60
            )

            startLockoutCountdown()

        } else {

            _uiState.value = ManualCheckInUiState(
                message = "Incorrect code. Try again.",
                attemptsRemaining = lockoutManager.attemptsRemaining
            )
        }
    }

    /**
     * Updates the remaining lockout time every second.
     */
    private fun startLockoutCountdown() {

        countdownJob?.cancel()

        countdownJob = viewModelScope.launch {

            while (true) {

                val remaining =
                    lockoutManager.getRemainingLockoutSeconds()

                if (remaining <= 0) {
                    _uiState.value = ManualCheckInUiState(
                        message = "You can try entering the code again.",
                        attemptsRemaining = 5
                    )
                    break
                }

                _uiState.value = _uiState.value.copy(
                    isLocked = true,
                    lockoutSeconds = remaining
                )

                delay(1000)
            }
        }
    }

    /**
     * Clears the current message.
     */
    fun clearMessage() {
        if (!_uiState.value.isLocked) {
            _uiState.value = _uiState.value.copy(
                message = null,
                isSuccessful = false
            )
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
