package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface ClubCheckInUiState {
    data object Loading : ClubCheckInUiState
    data class Success(val events: List<Event>) : ClubCheckInUiState
    data class Error(val message: String) : ClubCheckInUiState
}

/** Club Check-in tab: this club's approved events, soonest first, to open check-in from. */
class ClubCheckInViewModel(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ClubCheckInUiState>(ClubCheckInUiState.Loading)
    val uiState: StateFlow<ClubCheckInUiState> = _uiState.asStateFlow()

    private var job: Job? = null

    init { load() }

    fun load() {
        job?.cancel()
        _uiState.value = ClubCheckInUiState.Loading
        job = viewModelScope.launch {
            val clubId = try { authRepository.currentClubId() } catch (e: Exception) { null }
            if (clubId == null) {
                _uiState.value = ClubCheckInUiState.Error("Couldn't find your club. Try again.")
                return@launch
            }
            eventRepository.getClubEvents(clubId)
                .catch { _uiState.value = ClubCheckInUiState.Error("Couldn't load your events. Check your connection.") }
                .collect { list ->
                    _uiState.value = ClubCheckInUiState.Success(
                        list.filter { it.status == EventStatus.APPROVED }.sortedBy { it.startTime }
                    )
                }
        }
    }
}
