package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.BookingException
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

/** What the main button on the details screen shows (R2-S3 AC1). */
sealed interface DetailsAction {
    data object Book : DetailsAction
    data object Booked : DetailsAction
    data object JoinWaitlist : DetailsAction
    data class Clashes(val other: Event) : DetailsAction
    data object BookingClosed : DetailsAction
    /** Not approved, cancelled or already started: can't be booked at all. */
    data object Unavailable : DetailsAction
}

/** Picks the main button. A pure function, so every case is unit tested. */
object EventDetailsRules {
    fun actionFor(event: Event, myBooking: Booking?, myOtherBookedEvents: List<Event>, now: Long): DetailsAction {
        if (myBooking?.status == BookingStatus.CONFIRMED) return DetailsAction.Booked
        if (event.status != EventStatus.APPROVED || event.startTime <= now) return DetailsAction.Unavailable
        if (BookingRules.isBookingClosed(event, now)) return DetailsAction.BookingClosed
        myOtherBookedEvents.firstOrNull { BookingRules.clashes(event, it) }?.let { return DetailsAction.Clashes(it) }
        if (event.isFull) return DetailsAction.JoinWaitlist
        return DetailsAction.Book
    }
}

sealed interface DetailsContent {
    data object Loading : DetailsContent
    data object NotFound : DetailsContent
    data class Error(val message: String) : DetailsContent
    data class Loaded(
        val event: Event,
        val points: Int,
        val placesLeft: Int,
        val action: DetailsAction
    ) : DetailsContent
}

data class EventDetailsUiState(
    val content: DetailsContent = DetailsContent.Loading,
    val isOffline: Boolean = false,
    val isBooking: Boolean = false,
    /** One-off message after an action; the screen shows it, then calls messageShown(). */
    val message: String? = null
)

/** Event details (R2-S3) and booking from the details screen (R2-S4, R2-S6). */
class EventDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val bookingRepository: BookingRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    /** Comes from the navigation route "student_event/{eventId}". */
    private val eventId: String = checkNotNull(savedStateHandle["eventId"]) { "eventId is missing" }

    private val _uiState = MutableStateFlow(EventDetailsUiState(isOffline = !networkMonitor.isOnline.value))
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> _uiState.update { it.copy(isOffline = !online) } }
        }
        load()
    }

    /** Live: the button updates if the event fills up or another booking changes while the screen is open. */
    fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(content = DetailsContent.Loading) }
        loadJob = viewModelScope.launch {
            combine(
                eventRepository.getEvent(eventId),
                bookingRepository.getMyBooking(eventId),
                bookingRepository.getMyBookings(),
                eventRepository.getApprovedUpcomingEvents()
            ) { event, myBooking, myBookings, upcoming ->
                if (event == null) {
                    DetailsContent.NotFound
                } else {
                    val bookedIds = myBookings.map { it.eventId }.toSet()
                    val myOtherEvents = upcoming.filter { it.id in bookedIds && it.id != event.id }
                    DetailsContent.Loaded(
                        event = event,
                        points = event.approvedPoints ?: event.requestedPoints,
                        placesLeft = event.seatsLeft,
                        action = EventDetailsRules.actionFor(event, myBooking, myOtherEvents, clock())
                    )
                }
            }
                .catch {
                    emit(DetailsContent.Error("Couldn't load this event. Check your connection and try again."))
                }
                .collect { content -> _uiState.update { it.copy(content = content) } }
        }
    }

    /** R2-S4: book in one tap. The server transaction makes sure capacity is never exceeded (R2-S6). */
    fun book() {
        val state = _uiState.value
        val loaded = state.content as? DetailsContent.Loaded ?: return
        if (loaded.action != DetailsAction.Book || state.isBooking) return
        if (state.isOffline) {
            _uiState.update { it.copy(message = OFFLINE_MESSAGE) }
            return
        }

        _uiState.update { it.copy(isBooking = true) }
        viewModelScope.launch {
            val message = try {
                bookingRepository.book(eventId)
                "You're booked for \"${loaded.event.title}\""
            } catch (e: BookingException) {
                when (e) {
                    BookingException.EventFull -> JUST_FILLED_MESSAGE
                    BookingException.AlreadyBooked -> "You've already booked this event."
                    BookingException.EventNotApproved -> "This event is no longer open for booking."
                    BookingException.EventNotFound -> "This event no longer exists."
                    BookingException.NotLoggedIn -> "Please log in again to book."
                }
            } catch (e: Exception) {
                "Couldn't book. Check your connection and try again."
            }
            _uiState.update { it.copy(isBooking = false, message = message) }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    companion object {
        const val OFFLINE_MESSAGE = "You're offline. Connect to the internet to book."
        const val JUST_FILLED_MESSAGE = "Just filled up. You can join the waitlist instead."
    }
}