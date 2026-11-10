package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.UserRole
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.SignInError
import com.mdpgroup08.scpd.data.repository.SignInException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the login screen needs to draw itself. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    /** True while checking someone is already signed in, when the app starts. */
    val isCheckingSession: Boolean = true,
    val errorMessage: String? = null,
    /** Set when sign-in succeeds; the screen then moves to that role's tabs. */
    val signedInRole: UserRole? = null,
) {
    /** The Log in button only works when both fields are filled and nothing is loading. */
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}


class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // If the user is already signed in, skip the login screen
        viewModelScope.launch {
            val role = authRepository.currentUserRole()
            _uiState.update { it.copy(isCheckingSession = false, signedInRole = role) }
        }
    }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun signIn() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val role = authRepository.signIn(state.email, state.password)
                _uiState.update { it.copy(isLoading = false, signedInRole = role) }
            } catch (e: SignInException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = messageFor(e.error)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = messageFor(SignInError.UNKNOWN)) }
            }
        }
    }

    companion object {
        /** Messages shown to the user (wording from R6-S1). */
        fun messageFor(error: SignInError): String = when (error) {
            SignInError.WRONG_CREDENTIALS -> "The email or password is incorrect"
            SignInError.TOO_MANY_ATTEMPTS -> "Too many attempts. Please wait a few minutes and try again."
            SignInError.NO_CONNECTION -> "No internet connection. Check your connection and try again."
            SignInError.NO_ROLE -> "This account has no role yet. Please contact the s-CPD office."
            SignInError.UNKNOWN -> "Something went wrong. Please try again."
        }
    }
}