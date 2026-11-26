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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What My events screen shows. */
sealed interface MyEventsUiState {
    data object Loading : MyEventsUiState
    data class Success(val events: List<ClubEventItem>) : MyEventsUiState
    data class Error(val message: String) : MyEventsUiState
}

/** The "Cancel event" dialog (R1-S9). Kept in the ViewModel so it survives screen rotation. */
data class CancelEventDialog(val event: Event, val reason: String = "", val error: String? = null)

/** Club "My events" (R1-S3, R1-S8, R1-S9, R2-S13). Newest event first. */
class MyEventsViewModel(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyEventsUiState>(MyEventsUiState.Loading)
    val uiState: StateFlow<MyEventsUiState> = _uiState.asStateFlow()

    private val _cancelDialog = MutableStateFlow<CancelEventDialog?>(null)
    val cancelDialog: StateFlow<CancelEventDialog?> = _cancelDialog.asStateFlow()

    private val _isCancelling = MutableStateFlow(false)
    val isCancelling: StateFlow<Boolean> = _isCancelling.asStateFlow()

    /** One-off message after cancelling; the screen shows it, then calls messageShown(). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, !networkMonitor.isOnline.value)

    private var job: Job? = null

    init { load() }

    fun load() {
        job?.cancel()
        _uiState.value = MyEventsUiState.Loading
        job = viewModelScope.launch {
            val clubId = try { authRepository.currentClubId() } catch (e: Exception) { null }
            if (clubId == null) {
                _uiState.value = MyEventsUiState.Error("Couldn't find your club. Try again.")
                return@launch
            }
            eventRepository.getClubEvents(clubId)
                .catch {
                    // Keep the list already shown if there is one; otherwise show the error
                    if (_uiState.value !is MyEventsUiState.Success) {
                        _uiState.value = MyEventsUiState.Error("Couldn't load your events. Check your connection.")
                    }
                }
                .collect { list ->
                    val now = clock()
                    // R1-S8: an event still Pending at its start time becomes Expired, so the club doesn't advertise it
                    list.filter { it.status == EventStatus.PENDING && it.startTime <= now }
                        .forEach { expireQuietly(it.id) }

                    _uiState.value = MyEventsUiState.Success(
                        list.sortedByDescending { it.startTime }.map { toItem(it, now) }
                    )
                }
        }
    }

    // ---------- Cancel an approved event (R1-S9) ----------

    fun openCancel(event: Event) {
        if (canCancel(event, clock())) _cancelDialog.value = CancelEventDialog(event)
    }

    fun onCancelReasonChange(reason: String) =
        _cancelDialog.update { it?.copy(reason = reason, error = null) }

    fun dismissCancel() {
        if (!_isCancelling.value) _cancelDialog.value = null
    }

    fun confirmCancel() {
        val dialog = _cancelDialog.value ?: return
        val error = when {
            dialog.reason.isBlank() -> "A reason is required so booked students know why."
            isOffline.value -> OFFLINE_MESSAGE
            !canCancel(dialog.event, clock()) -> "This event has already started, so it can't be cancelled."
            else -> null
        }
        if (error != null) {
            _cancelDialog.update { it?.copy(error = error) }
            return
        }

        _isCancelling.value = true
        viewModelScope.launch {
            try {
                eventRepository.cancelEvent(dialog.event.id, dialog.reason.trim())
                _cancelDialog.value = null
                _message.value = "\"${dialog.event.title}\" is cancelled. Booked students will see your reason."
            } catch (e: EventDecisionException) {
                _cancelDialog.value = null
                _message.value = "This event has already changed, so nothing was cancelled."
            } catch (e: Exception) {
                _cancelDialog.update { it?.copy(error = "Couldn't cancel. Check your connection and try again.") }
            } finally {
                _isCancelling.value = false
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }

    // ---------- Helpers ----------

    private fun toItem(event: Event, now: Long): ClubEventItem =
        ClubEventStatus.toItem(event).copy(
            canCancel = canCancel(event, now),
            bookingSummary = if (event.status == EventStatus.APPROVED) bookingSummary(event) else null,
            canCopy = event.status in CreateEventViewModel.COPYABLE
        )

    /** Best effort: if it fails (e.g. offline), the next load tries again. */
    private fun expireQuietly(eventId: String) {
        viewModelScope.launch {
            try {
                eventRepository.expire(eventId)
            } catch (e: Exception) {
                // Ignored on purpose: the event stays Pending until the next successful load
            }
        }
    }

    companion object {
        const val OFFLINE_MESSAGE = "You're offline. Connect to the internet to cancel the event."

        /** R1-S9: only an approved event that hasn't started can be cancelled. */
        fun canCancel(event: Event, now: Long): Boolean =
            event.status == EventStatus.APPROVED && now < event.startTime

        /** R2-S13: how full the event is, for the organiser. */
        fun bookingSummary(event: Event): String = buildString {
            append("Booked ${event.bookedCount} of ${event.capacity}")
            if (event.waitlist.isNotEmpty()) append(" · ${event.waitlist.size} on the waitlist")
        }
    }
}