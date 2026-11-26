package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.BookingRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the student stands for one event. */
sealed interface StudentEventStatus {
    /** [fromWaitlist]: the place came from the waitlist (R2-S5 AC2). */
    data class Booked(val fromWaitlist: Boolean) : StudentEventStatus
    data class Waitlisted(val position: Int) : StudentEventStatus
}

data class StudentEventItem(val event: Event, val points: Int, val status: StudentEventStatus)

sealed interface StudentEventsList {
    data object Loading : StudentEventsList
    data class Loaded(val booked: List<StudentEventItem>, val waitlisted: List<StudentEventItem>) : StudentEventsList {
        val isEmpty: Boolean get() = booked.isEmpty() && waitlisted.isEmpty()
    }
    data class Error(val message: String) : StudentEventsList
}

data class StudentEventsUiState(
    val list: StudentEventsList = StudentEventsList.Loading,
    val isOffline: Boolean = false
)

/** Student "My events" (R4-S10): upcoming bookings and waitlists, readable offline from the saved data. */
class StudentEventsViewModel(
    private val bookingRepository: BookingRepository,
    private val eventRepository: EventRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentEventsUiState(isOffline = !networkMonitor.isOnline.value))
    val uiState: StateFlow<StudentEventsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> _uiState.update { it.copy(isOffline = !online) } }
        }
        load()
    }

    fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(list = StudentEventsList.Loading) }
        loadJob = viewModelScope.launch {
            combine(bookingRepository.getMyBookings(), eventRepository.getApprovedUpcomingEvents()) { bookings, events ->
                build(bookings, events)
            }
                .catch { emit(StudentEventsList.Error("Couldn't load your events. Check your connection and try again.")) }
                .collect { list -> _uiState.update { it.copy(list = list) } }
        }
    }

    private fun build(bookings: List<Booking>, events: List<Event>): StudentEventsList {
        val now = clock()
        val myId = bookingRepository.myId()
        val upcoming = events.filter { it.startTime > now }.sortedBy { it.startTime }
        val myBookings = bookings.filter { it.status == BookingStatus.CONFIRMED }.associateBy { it.eventId }

        val booked = upcoming.mapNotNull { event ->
            myBookings[event.id]?.let { booking ->
                item(event, StudentEventStatus.Booked(fromWaitlist = booking.fromWaitlist))
            }
        }
        val waitlisted = upcoming
            .filter { myId != null && myId in it.waitlist && it.id !in myBookings }
            .map { event -> item(event, StudentEventStatus.Waitlisted(event.waitlist.indexOf(myId) + 1)) }

        return StudentEventsList.Loaded(booked, waitlisted)
    }

    private fun item(event: Event, status: StudentEventStatus) =
        StudentEventItem(event, points = event.approvedPoints ?: event.requestedPoints, status = status)
}