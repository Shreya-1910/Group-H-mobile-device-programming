package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Whether a student can book an event right now. */
enum class Availability { OPEN, FULL, CLOSED }

/** One event card in the Marketplace (R2-S1 AC2). */
data class MarketplaceItem(
    val event: Event,
    /** Approved points: what students actually earn, not what the club asked for. */
    val points: Int,
    val placesLeft: Int,
    val availability: Availability
)

sealed interface MarketplaceList {
    data object Loading : MarketplaceList
    data class Loaded(val items: List<MarketplaceItem>) : MarketplaceList
    data class Error(val message: String) : MarketplaceList
}

data class MarketplaceUiState(
    val list: MarketplaceList = MarketplaceList.Loading,
    val isOffline: Boolean = false
)

/** Student Marketplace (R2-S1, R2-S9). All filtering and availability rules live here. */
class MarketplaceViewModel(
    private val eventRepository: EventRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState(isOffline = !networkMonitor.isOnline.value))
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> _uiState.update { it.copy(isOffline = !online) } }
        }
        load()
    }

    fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(list = MarketplaceList.Loading) }
        loadJob = viewModelScope.launch {
            eventRepository.getApprovedUpcomingEvents()
                .catch {
                    _uiState.update {
                        it.copy(list = MarketplaceList.Error("Couldn't load events. Check your connection and try again."))
                    }
                }
                .collect { events -> _uiState.update { it.copy(list = MarketplaceList.Loaded(toItems(events))) } }
        }
    }

    /** R2-S1 AC1: only Approved events that haven't started, soonest first. */
    private fun toItems(events: List<Event>): List<MarketplaceItem> {
        val now = clock()
        return events
            .filter { it.status == EventStatus.APPROVED && it.startTime > now }
            .sortedBy { it.startTime }
            .map { event ->
                MarketplaceItem(
                    event = event,
                    points = event.approvedPoints ?: event.requestedPoints,
                    placesLeft = event.seatsLeft,
                    availability = when {
                        BookingRules.isBookingClosed(event, now) -> Availability.CLOSED
                        event.isFull -> Availability.FULL
                        else -> Availability.OPEN
                    }
                )
            }
    }
}
