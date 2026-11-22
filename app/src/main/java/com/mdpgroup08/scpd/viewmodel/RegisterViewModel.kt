package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.StudyDetails
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.StudyDetailsMismatchException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val pdpaAccepted: Boolean = false,
    val currentYear: String = "1",
    val degreeName: String = "",
    val degreeLength: String = "3",
    val startYear: String = "2023",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                degreeName.isNotBlank() &&
                pdpaAccepted &&
                !isLoading
}

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun onPdpaChange(value: Boolean) = _uiState.update { it.copy(pdpaAccepted = value, errorMessage = null) }
    fun onCurrentYearChange(value: String) = _uiState.update { it.copy(currentYear = value, errorMessage = null) }
    fun onDegreeNameChange(value: String) = _uiState.update { it.copy(degreeName = value, errorMessage = null) }
    fun onDegreeLengthChange(value: String) = _uiState.update { it.copy(degreeLength = value, errorMessage = null) }
    fun onStartYearChange(value: String) = _uiState.update { it.copy(startYear = value, errorMessage = null) }

    fun register() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val studyDetails = StudyDetails(
                    currentYear = state.currentYear.toIntOrNull() ?: 1,
                    degreeName = state.degreeName,
                    degreeLength = state.degreeLength.toIntOrNull() ?: 3,
                    startYear = state.startYear.toIntOrNull() ?: 2023
                )

                authRepository.registerStudent(
                    email = state.email,
                    password = state.password,
                    name = state.name,
                    pdpaAccepted = state.pdpaAccepted,
                    studyDetails = studyDetails
                )

                _uiState.update { it.copy(isLoading = false, isRegistered = true) }
            } catch (e: StudyDetailsMismatchException) {
                // R6-S4 EX1 requirement error message
                _uiState.update { it.copy(isLoading = false, errorMessage = "These details do not match") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Registration failed") }
            }
        }
    }
}
