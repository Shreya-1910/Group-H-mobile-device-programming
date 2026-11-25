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
import java.time.ZoneId

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

    /** items = events matching the search and filters; total = all upcoming events before filtering. */
    data class Loaded(val items: List<MarketplaceItem>, val total: Int) : MarketplaceList

    data class Error(val message: String) : MarketplaceList
}

data class MarketplaceUiState(
    val list: MarketplaceList = MarketplaceList.Loading,
    val isOffline: Boolean = false,
    val filters: MarketplaceFilters = MarketplaceFilters(),
    /** Clubs that have upcoming events, for the Club filter. */
    val clubs: List<String> = emptyList()
)

/** Student Marketplace (R2-S1, R2-S2, R2-S9). All filtering and availability rules live here. */
class MarketplaceViewModel(
    private val eventRepository: EventRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState(isOffline = !networkMonitor.isOnline.value))
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    /** Every upcoming event before search and filters are applied. */
    private var allItems: List<MarketplaceItem> = emptyList()
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
                .collect { events ->
                    allItems = toItems(events)
                    _uiState.update { state ->
                        state.copy(
                            list = filtered(state.filters),
                            clubs = allItems.map { it.event.clubName }.filter { it.isNotBlank() }.distinct().sorted()
                        )
                    }
                }
        }
    }

    // ---------- Search and filters (R2-S2) ----------

    fun onQueryChange(query: String) = updateFilters { it.copy(query = query) }
    fun setCategory(category: String?) = updateFilters { it.copy(category = category) }
    fun setDate(date: DateFilter) = updateFilters { it.copy(date = date) }
    fun setMinPoints(points: Int?) = updateFilters { it.copy(minPoints = points) }
    fun setClub(club: String?) = updateFilters { it.copy(club = club) }
    fun setAvailableOnly(on: Boolean) = updateFilters { it.copy(availableOnly = on) }

    /** R2-S2 AC2: one tap to clear everything when nothing matches. */
    fun clearFilters() = updateFilters { MarketplaceFilters() }

    private fun updateFilters(change: (MarketplaceFilters) -> MarketplaceFilters) {
        _uiState.update { state ->
            val filters = change(state.filters)
            // Only re-filter once the events have loaded; leave Loading and Error alone
            val list = if (state.list is MarketplaceList.Loaded) filtered(filters) else state.list
            state.copy(filters = filters, list = list)
        }
    }

    private fun filtered(filters: MarketplaceFilters) = MarketplaceList.Loaded(
        items = MarketplaceSearch.apply(allItems, filters, clock(), zone),
        total = allItems.size
    )

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