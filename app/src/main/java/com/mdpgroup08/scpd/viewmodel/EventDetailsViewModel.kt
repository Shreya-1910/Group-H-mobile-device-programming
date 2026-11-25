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

    /**
     * Clashes with a booking the student already has (R2-S7).
     * [canSwap] is true only if this event has a place and the other booking can still be cancelled.
     */
    data class Clashes(val other: Event, val canSwap: Boolean) : DetailsAction

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
        myOtherBookedEvents.firstOrNull { BookingRules.clashes(event, it) }?.let { other ->
            val canSwap = !event.isFull && !BookingRules.isBookingClosed(other, now)
            return DetailsAction.Clashes(other, canSwap)
        }
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
    /** True while a book or swap request is running, so buttons can't be tapped twice. */
    val isWorking: Boolean = false,
    /** Shows the "Keep or swap?" dialog (R2-S7). Kept here so it survives screen rotation. */
    val showSwapConfirm: Boolean = false,
    /** One-off message after an action; the screen shows it, then calls messageShown(). */
    val message: String? = null
)

/** Event details (R2-S3), booking (R2-S4, R2-S6) and swapping a clashing booking (R2-S7). */
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

    // ---------- Book (R2-S4, R2-S6) ----------

    fun book() {
        val loaded = readyFor<DetailsAction.Book>() ?: return
        run(successMessage = "You're booked for \"${loaded.event.title}\"") {
            bookingRepository.book(eventId)
        }
    }

    // ---------- Keep or swap (R2-S7) ----------

    fun requestSwap() {
        val clash = (currentAction() as? DetailsAction.Clashes) ?: return
        if (clash.canSwap) _uiState.update { it.copy(showSwapConfirm = true) }
    }

    /** "Keep my booking": nothing changes. */
    fun keepExisting() = _uiState.update { it.copy(showSwapConfirm = false) }

    fun confirmSwap() {
        _uiState.update { it.copy(showSwapConfirm = false) }
        val loaded = (_uiState.value.content as? DetailsContent.Loaded) ?: return
        val clash = (loaded.action as? DetailsAction.Clashes)?.takeIf { it.canSwap } ?: return
        if (_uiState.value.isOffline) {
            _uiState.update { it.copy(message = OFFLINE_MESSAGE) }
            return
        }
        run(successMessage = "Swapped: you're now booked for \"${loaded.event.title}\"") {
            bookingRepository.swap(fromEventId = clash.other.id, toEventId = eventId)
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    // ---------- Helpers ----------

    private fun currentAction(): DetailsAction? = (_uiState.value.content as? DetailsContent.Loaded)?.action

    /** Returns the loaded content only if the button currently shows [T], we're online and not already working. */
    private inline fun <reified T : DetailsAction> readyFor(): DetailsContent.Loaded? {
        val state = _uiState.value
        val loaded = state.content as? DetailsContent.Loaded ?: return null
        if (loaded.action !is T || state.isWorking) return null
        if (state.isOffline) {
            _uiState.update { it.copy(message = OFFLINE_MESSAGE) }
            return null
        }
        return loaded
    }

    /** Runs a booking action and turns every outcome into a clear message. */
    private fun run(successMessage: String, action: suspend () -> Unit) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val message = try {
                action()
                successMessage
            } catch (e: BookingException) {
                when (e) {
                    BookingException.EventFull -> JUST_FILLED_MESSAGE
                    BookingException.AlreadyBooked -> "You've already booked this event."
                    BookingException.EventNotApproved -> "This event is no longer open for booking."
                    BookingException.EventNotFound -> "This event no longer exists."
                    BookingException.NotLoggedIn -> "Please log in again."
                    BookingException.NotBooked -> "Your other booking has already changed. Nothing was swapped."
                }
            } catch (e: Exception) {
                "Couldn't complete that. Check your connection and try again."
            }
            _uiState.update { it.copy(isWorking = false, message = message) }
        }
    }

    companion object {
        const val OFFLINE_MESSAGE = "You're offline. Connect to the internet to book."
        const val JUST_FILLED_MESSAGE = "Just filled up. You can join the waitlist instead."
    }
}