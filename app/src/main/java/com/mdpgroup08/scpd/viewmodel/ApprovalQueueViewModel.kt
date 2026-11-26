package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApprovalQueueUiState(
    val pendingEvents: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val selectedEventForApprove: Event? = null,
    val selectedEventForReject: Event? = null,
    val approvedPointsInput: String = "",
    val approveReasonInput: String = "",
    val rejectReasonInput: String = "",
    val approveError: String? = null,
    val rejectError: String? = null,
    val isSaving: Boolean = false,
    val statusMessage: String? = null
)

class ApprovalQueueViewModel(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApprovalQueueUiState())
    val uiState: StateFlow<ApprovalQueueUiState> = _uiState.asStateFlow()

    init {
        // Monitor offline/online status (Airplane mode)
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }

        // Observe pending events queue
        viewModelScope.launch {
            eventRepository.getPendingEvents()
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { events ->
                    _uiState.update { it.copy(pendingEvents = events, isLoading = false) }
                }
        }
    }

    fun openApproveDialog(event: Event) {
        _uiState.update {
            it.copy(
                selectedEventForApprove = event,
                approvedPointsInput = event.requestedPoints.toString(),
                approveReasonInput = "",
                approveError = null
            )
        }
    }

    fun onApprovedPointsChange(value: String) =
        _uiState.update { it.copy(approvedPointsInput = value, approveError = null) }

    fun onApproveReasonChange(value: String) =
        _uiState.update { it.copy(approveReasonInput = value, approveError = null) }

    fun dismissApproveDialog() =
        _uiState.update { it.copy(selectedEventForApprove = null, approveError = null) }

    fun confirmApprove() {
        val state = _uiState.value
        val event = state.selectedEventForApprove ?: return
        val points = state.approvedPointsInput.trim().toIntOrNull()

        // Validation 1: Approved points cannot exceed requested points
        if (points == null || points < 1) {
            _uiState.update { it.copy(approveError = "Points must be at least 1") }
            return
        }
        if (points > event.requestedPoints) {
            _uiState.update { it.copy(approveError = "Approved points cannot exceed requested points (${event.requestedPoints})") }
            return
        }

        _uiState.update { it.copy(isSaving = true, approveError = null) }
        viewModelScope.launch {
            try {
                val adminUser = authRepository.currentUser()
                val adminUid = adminUser?.id ?: ""
                eventRepository.approve(
                    eventId = event.id,
                    approvedPoints = points,
                    note = state.approveReasonInput.ifBlank { "Approved" },
                    adminId = adminUid
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        selectedEventForApprove = null,
                        statusMessage = "Event '${event.title}' approved for $points points"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, approveError = e.message ?: "Couldn't save the decision. Check your connection.")
                }
            }
        }
    }

    fun openRejectDialog(event: Event) {
        _uiState.update {
            it.copy(
                selectedEventForReject = event,
                rejectReasonInput = "",
                rejectError = null
            )
        }
    }

    fun onRejectReasonChange(value: String) =
        _uiState.update { it.copy(rejectReasonInput = value, rejectError = null) }

    fun dismissRejectDialog() =
        _uiState.update { it.copy(selectedEventForReject = null, rejectError = null) }

    fun confirmReject() {
        val state = _uiState.value
        val event = state.selectedEventForReject ?: return
        val reason = state.rejectReasonInput.trim()

        // Validation 2: Reject reason is required
        if (reason.isBlank()) {
            _uiState.update { it.copy(rejectError = "Reason is required") }
            return
        }

        _uiState.update { it.copy(isSaving = true, rejectError = null) }
        viewModelScope.launch {
            try {
                val adminUser = authRepository.currentUser()
                val adminUid = adminUser?.id ?: ""
                eventRepository.reject(
                    eventId = event.id,
                    reason = reason,
                    adminId = adminUid
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        selectedEventForReject = null,
                        statusMessage = "Event '${event.title}' rejected"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, rejectError = e.message ?: "Couldn't save the decision. Check your connection.")
                }
            }
        }
    }
}
