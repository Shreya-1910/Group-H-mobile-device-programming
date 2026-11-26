package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** What My events screen shows. */
sealed interface MyEventsUiState {
    data object Loading : MyEventsUiState
    data class Success(val events: List<ClubEventItem>) : MyEventsUiState
    data class Error(val message: String) : MyEventsUiState
}

/** Club "My events" list (R1-S3, R1-S8). Newest event first. */
class MyEventsViewModel(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyEventsUiState>(MyEventsUiState.Loading)
    val uiState: StateFlow<MyEventsUiState> = _uiState.asStateFlow()

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
                    // R1-S8: an event still Pending at its start time becomes Expired, so the club doesn't advertise it
                    val now = System.currentTimeMillis()
                    list.filter { it.status == EventStatus.PENDING && it.startTime <= now }
                        .forEach { expireQuietly(it.id) }

                    _uiState.value = MyEventsUiState.Success(
                        list.sortedByDescending { it.startTime }.map { ClubEventStatus.toItem(it) }
                    )
                }
        }
    }

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
}