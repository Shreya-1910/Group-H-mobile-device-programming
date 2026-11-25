package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.network.FakeNetworkMonitor
import com.mdpgroup08.scpd.data.repository.FakeBookingRepository
import com.mdpgroup08.scpd.data.repository.FakeEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Event details main button (R2-S3), clashes (R2-S7) and booking from details (R2-S4, R2-S6). */
@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailsViewModelTest {

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = System.currentTimeMillis()

    private fun event(
        id: String, startsIn: Long = 3 * day, length: Long = 2 * hour,
        capacity: Int = 50, booked: Int = 10, status: EventStatus = EventStatus.APPROVED
    ) = Event(
        id = id, title = "Event $id", clubName = "Club", status = status,
        startTime = now + startsIn, endTime = now + startsIn + length,
        requestedPoints = 8, approvedPoints = 5, capacity = capacity, bookedCount = booked
    )

    private fun confirmed(eventId: String) =
        Booking(eventId = eventId, studentId = "s1", status = BookingStatus.CONFIRMED)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // ---------- Which button (pure rules) ----------

    @Test
    fun `open event shows Book`() {
        assertEquals(DetailsAction.Book, EventDetailsRules.actionFor(event("a"), null, emptyList(), now))
    }

    @Test
    fun `my booking shows Booked, even after the cut-off`() {
        val soon = event("a", startsIn = hour)
        assertEquals(DetailsAction.Booked, EventDetailsRules.actionFor(soon, confirmed("a"), emptyList(), now))
    }

    @Test
    fun `full event shows Join waitlist`() {
        val full = event("a", capacity = 30, booked = 30)
        assertEquals(DetailsAction.JoinWaitlist, EventDetailsRules.actionFor(full, null, emptyList(), now))
    }

    @Test
    fun `within 2 hours of the start shows Booking closed`() {
        val soon = event("a", startsIn = 90 * 60 * 1000L)
        assertEquals(DetailsAction.BookingClosed, EventDetailsRules.actionFor(soon, null, emptyList(), now))
    }

    @Test
    fun `overlapping booking shows Clashes with that event`() {
        val a = event("a", startsIn = 3 * day)
        val b = event("b", startsIn = 3 * day + hour)  // starts while a is still running
        assertEquals(DetailsAction.Clashes(b, canSwap = true), EventDetailsRules.actionFor(a, null, listOf(b), now))
    }

    @Test
    fun `back-to-back events do not clash`() {
        val a = event("a", startsIn = 3 * day, length = 2 * hour)
        val b = event("b", startsIn = 3 * day + 2 * hour)  // starts exactly when a ends
        assertFalse(BookingRules.clashes(a, b))
        assertEquals(DetailsAction.Book, EventDetailsRules.actionFor(a, null, listOf(b), now))
    }

    @Test
    fun `pending or started events are unavailable`() {
        assertEquals(
            DetailsAction.Unavailable,
            EventDetailsRules.actionFor(event("a", status = EventStatus.PENDING), null, emptyList(), now)
        )
        assertEquals(
            DetailsAction.Unavailable,
            EventDetailsRules.actionFor(event("b", startsIn = -hour), null, emptyList(), now)
        )
    }

    // ---------- In the ViewModel ----------

    private lateinit var events: FakeEventRepository
    private lateinit var bookings: FakeBookingRepository

    private fun viewModel(id: String, online: Boolean = true) = EventDetailsViewModel(
        SavedStateHandle(mapOf("eventId" to id)), events, bookings, FakeNetworkMonitor(online), clock = { now }
    )

    private fun EventDetailsViewModel.action() = (uiState.value.content as DetailsContent.Loaded).action

    @Test
    fun `booking changes the button to Booked`() = runTest {
        events = FakeEventRepository(listOf(event("a")))
        bookings = FakeBookingRepository(events)
        val vm = viewModel("a")
        assertEquals(DetailsAction.Book, vm.action())

        vm.book()
        assertEquals(DetailsAction.Booked, vm.action())
        assertEquals(11, events.getEvent("a").first()!!.bookedCount)
    }

    @Test
    fun `existing booking on an overlapping event shows the clash`() = runTest {
        events = FakeEventRepository(listOf(event("a"), event("b", startsIn = 3 * day + hour)))
        bookings = FakeBookingRepository(events)
        bookings.book("b")

        val vm = viewModel("a")
        assertTrue(vm.action() is DetailsAction.Clashes)
    }

    @Test
    fun `last place taken by someone else shows Just filled up`() = runTest {
        events = FakeEventRepository(listOf(event("a", capacity = 1, booked = 0)))
        bookings = FakeBookingRepository(events)
        val vm = viewModel("a")

        // Another student takes the last place while this screen is open
        FakeBookingRepository(events, currentUserId = "other_student").book("a")
        assertEquals(DetailsAction.JoinWaitlist, vm.action())
    }

    @Test
    fun `offline booking is blocked with a message`() = runTest {
        events = FakeEventRepository(listOf(event("a")))
        bookings = FakeBookingRepository(events)
        val vm = viewModel("a", online = false)

        vm.book()
        assertEquals(EventDetailsViewModel.OFFLINE_MESSAGE, vm.uiState.value.message)
        assertEquals(10, events.getEvent("a").first()!!.bookedCount)
    }

    @Test
    fun `unknown event shows not found`() {
        events = FakeEventRepository(emptyList())
        bookings = FakeBookingRepository(events)
        assertEquals(DetailsContent.NotFound, viewModel("missing").uiState.value.content)
    }
}