package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** What the My events screen shows. */
sealed interface MyEventsUiState {
    data object Loading : MyEventsUiState
    data class Success(val events: List<Event>) : MyEventsUiState
    data class Error(val message: String) : MyEventsUiState
}

/** Club "My events" list (R1-S3). Newest event first. */
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
                .catch { _uiState.value = MyEventsUiState.Error("Couldn't load your events. Check your connection.") }
                .collect { list ->
                    _uiState.value = MyEventsUiState.Success(list.sortedByDescending { it.date })
                }
        }
    }
}
