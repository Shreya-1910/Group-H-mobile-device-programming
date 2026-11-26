package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventDecisionException
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds

/** One Pending event in the admin's approval queue, with its review deadline (R1-S4 AC3). */
data class PendingEventItem(val event: Event, val dueDate: LocalDate, val isOverdue: Boolean)

/** The Approve or Reject dialog the admin has open. */
sealed interface DecisionDialog {
    val event: Event
    val error: String?

    data class Approve(
        override val event: Event,
        val points: String,
        val note: String = "",
        override val error: String? = null
    ) : DecisionDialog

    data class Reject(
        override val event: Event,
        val reason: String = "",
        override val error: String? = null
    ) : DecisionDialog
}

sealed interface ApprovalQueue {
    data object Loading : ApprovalQueue
    data class Loaded(val items: List<PendingEventItem>) : ApprovalQueue
    data class Error(val message: String) : ApprovalQueue
}

data class AdminApprovalUiState(
    val queue: ApprovalQueue = ApprovalQueue.Loading,
    val dialog: DecisionDialog? = null,
    val isOffline: Boolean = false,
    val isSaving: Boolean = false,
    /** One-off message after a decision; the screen shows it, then calls messageShown(). */
    val message: String? = null
)

/** Admin approval queue (R1-S4, R1-S5). All rules live here; the security rules enforce them again on the server. */
class AdminApprovalViewModel(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val saveTimeoutMs: Long = SAVE_TIMEOUT_MS
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminApprovalUiState(isOffline = !networkMonitor.isOnline.value))
    val uiState: StateFlow<AdminApprovalUiState> = _uiState.asStateFlow()

    private var queueJob: Job? = null

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> _uiState.update { it.copy(isOffline = !online) } }
        }
        load()
    }

    fun load() {
        queueJob?.cancel()
        _uiState.update { it.copy(queue = ApprovalQueue.Loading) }
        queueJob = viewModelScope.launch {
            eventRepository.getPendingEvents()
                .catch {
                    _uiState.update {
                        it.copy(queue = ApprovalQueue.Error("Couldn't load the approval queue. Check your connection."))
                    }
                }
                .collect { events ->
                    // R1-S8: Pending events that have already started expire and leave the queue
                    val now = clock()
                    val (overdue, current) = events.partition { it.status == EventStatus.PENDING && it.startTime <= now }
                    overdue.forEach { expireQuietly(it.id) }
                    _uiState.update { it.copy(queue = ApprovalQueue.Loaded(toQueue(current))) }
                }
        }
    }

    /** Overdue first, then oldest submission first (R1-S4 AC3). */
    private fun toQueue(events: List<Event>): List<PendingEventItem> {
        val today = Instant.ofEpochMilli(clock()).atZone(zone).toLocalDate()
        return events
            .filter { it.status == EventStatus.PENDING }
            .map { event ->
                val due = ReviewDeadline.dueDate(event.submittedAt ?: clock(), zone)
                PendingEventItem(event, due, today.isAfter(due))
            }
            .sortedWith(
                compareByDescending<PendingEventItem> { it.isOverdue }
                    .thenBy { it.event.submittedAt ?: Long.MAX_VALUE }
            )
    }

    fun openApprove(event: Event) =
        _uiState.update { it.copy(dialog = DecisionDialog.Approve(event, points = event.requestedPoints.toString())) }

    fun openReject(event: Event) =
        _uiState.update { it.copy(dialog = DecisionDialog.Reject(event)) }

    fun dismissDialog() {
        if (!_uiState.value.isSaving) _uiState.update { it.copy(dialog = null) }
    }

    fun onPointsChange(points: String) =
        updateDialog { (it as? DecisionDialog.Approve)?.copy(points = points, error = null) ?: it }

    fun onNoteChange(note: String) =
        updateDialog { (it as? DecisionDialog.Approve)?.copy(note = note, error = null) ?: it }

    fun onReasonChange(reason: String) =
        updateDialog { (it as? DecisionDialog.Reject)?.copy(reason = reason, error = null) ?: it }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    fun confirm() {
        val dialog = _uiState.value.dialog ?: return
        if (_uiState.value.isOffline) { showError(OFFLINE_MESSAGE); return }
        validate(dialog)?.let { showError(it); return }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val adminId = authRepository.currentUserId()
                    ?: throw IllegalStateException("No admin signed in")

                val finished = withTimeoutOrNull(saveTimeoutMs.milliseconds) {
                    when (dialog) {
                        is DecisionDialog.Approve -> eventRepository.approve(
                            dialog.event.id, dialog.points.trim().toInt(), dialog.note.trim(), adminId
                        )
                        is DecisionDialog.Reject -> eventRepository.reject(
                            dialog.event.id, dialog.reason.trim(), adminId
                        )
                    }
                }
                if (finished == null) {
                    _uiState.update { it.copy(isSaving = false) }
                    showError(TIMEOUT_MESSAGE)
                    return@launch
                }

                val message = when (dialog) {
                    is DecisionDialog.Approve -> "Approved \"${dialog.event.title}\" for ${dialog.points.trim()} points"
                    is DecisionDialog.Reject -> "Rejected \"${dialog.event.title}\""
                }
                _uiState.update { it.copy(isSaving = false, dialog = null, message = message) }
            } catch (e: EventDecisionException) {
                val message = when (e) {
                    EventDecisionException.AlreadyDecided -> ALREADY_DECIDED_MESSAGE
                    EventDecisionException.NotFound -> "This event no longer exists."
                }
                _uiState.update { it.copy(isSaving = false, dialog = null, message = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                showError("Couldn't save the decision. Check your connection and try again.")
            }
        }
    }

    /** Returns an error message, or null if the decision is valid. */
    private fun validate(dialog: DecisionDialog): String? = when (dialog) {
        is DecisionDialog.Approve -> {
            val requested = dialog.event.requestedPoints
            val points = dialog.points.trim().toIntOrNull()
            when {
                dialog.event.startTime <= clock() -> "This event has already started, so it can't be approved."
                points == null || points !in 1..requested -> "Points must be between 1 and $requested"
                else -> null
            }
        }
        is DecisionDialog.Reject ->
            if (dialog.reason.isBlank()) "A reason is required so the club can fix the event." else null
    }

    private fun updateDialog(change: (DecisionDialog) -> DecisionDialog) =
        _uiState.update { state -> state.copy(dialog = state.dialog?.let(change)) }

    private fun showError(message: String) = updateDialog {
        when (it) {
            is DecisionDialog.Approve -> it.copy(error = message)
            is DecisionDialog.Reject -> it.copy(error = message)
        }
    }

    /** Best effort: if it fails (e.g. offline), the next load tries again. */
    private fun expireQuietly(eventId: String) {
        viewModelScope.launch {
            try { eventRepository.expire(eventId) } catch (e: Exception) { }
        }
    }

    companion object {
        const val SAVE_TIMEOUT_MS = 15_000L
        const val OFFLINE_MESSAGE = "You're offline. Connect to the internet to approve or reject."
        const val TIMEOUT_MESSAGE = "The server is taking too long to respond. Try again."
        const val ALREADY_DECIDED_MESSAGE = "Another admin already decided on this event."
    }
}