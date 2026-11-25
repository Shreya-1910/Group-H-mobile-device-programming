package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.SavedStateHandle
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

/** Keep or swap a clashing booking (R2-S7) and back-to-back events (R4-S22 EX1). */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingSwapTest {

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = System.currentTimeMillis()

    private fun event(id: String, startsIn: Long, capacity: Int = 50, booked: Int = 10) = Event(
        id = id, title = "Event $id", clubName = "Club", status = EventStatus.APPROVED,
        startTime = now + startsIn, endTime = now + startsIn + 2 * hour,
        requestedPoints = 5, approvedPoints = 5, capacity = capacity, bookedCount = booked
    )

    private lateinit var events: FakeEventRepository
    private lateinit var bookings: FakeBookingRepository

    /** Student is booked on "old"; "new" overlaps it by one hour. */
    private fun setUpClash(newCapacity: Int = 50, newBooked: Int = 10) = runTest {
        events = FakeEventRepository(
            listOf(event("old", startsIn = 3 * day), event("new", startsIn = 3 * day + hour, newCapacity, newBooked))
        )
        bookings = FakeBookingRepository(events)
        bookings.book("old")
    }

    private fun viewModel(online: Boolean = true) = EventDetailsViewModel(
        SavedStateHandle(mapOf("eventId" to "new")), events, bookings, FakeNetworkMonitor(online), clock = { now }
    )

    private fun EventDetailsViewModel.action() = (uiState.value.content as DetailsContent.Loaded).action

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `clash offers a swap when the new event has a place`() {
        setUpClash()
        val action = viewModel().action()
        assertTrue(action is DetailsAction.Clashes && action.canSwap)
    }

    @Test
    fun `swap cancels the old booking and books the new one`() = runTest {
        setUpClash()
        val vm = viewModel()
        vm.requestSwap()
        assertTrue(vm.uiState.value.showSwapConfirm)

        vm.confirmSwap()
        assertEquals(BookingStatus.CANCELLED, bookings.getMyBooking("old").first()!!.status)
        assertEquals(BookingStatus.CONFIRMED, bookings.getMyBooking("new").first()!!.status)
        assertEquals(10, events.getEvent("old").first()!!.bookedCount)  // 11 after booking, back to 10
        assertEquals(11, events.getEvent("new").first()!!.bookedCount)
        assertEquals(DetailsAction.Booked, vm.action())
    }

    @Test
    fun `keep my booking changes nothing`() = runTest {
        setUpClash()
        val vm = viewModel()
        vm.requestSwap()
        vm.keepExisting()

        assertFalse(vm.uiState.value.showSwapConfirm)
        assertEquals(BookingStatus.CONFIRMED, bookings.getMyBooking("old").first()!!.status)
        assertEquals(null, bookings.getMyBooking("new").first())
    }

    @Test
    fun `cannot hold both overlapping bookings`() = runTest {
        setUpClash()
        viewModel().confirmSwap()  // swap without asking first does nothing
        val confirmed = bookings.getMyBookings().first().map { it.eventId }
        assertEquals(listOf("old"), confirmed)
    }

    @Test
    fun `no swap offered when the new event is full`() {
        setUpClash(newCapacity = 20, newBooked = 20)
        val action = viewModel().action()
        assertTrue(action is DetailsAction.Clashes && !action.canSwap)
    }

    @Test
    fun `new event filling up during the swap keeps the old booking`() = runTest {
        setUpClash(newCapacity = 11, newBooked = 10)
        val vm = viewModel()
        vm.requestSwap()

        // Someone else takes the last place on "new" before the student confirms
        FakeBookingRepository(events, currentUserId = "other_student").book("new")
        vm.confirmSwap()

        assertEquals(BookingStatus.CONFIRMED, bookings.getMyBooking("old").first()!!.status)
        assertEquals(null, bookings.getMyBooking("new").first())
    }

    @Test
    fun `no swap offered when the old booking is past its cut-off`() {
        events = FakeEventRepository(
            listOf(event("old", startsIn = hour), event("new", startsIn = 2 * hour))  // old starts in 1 h
        )
        bookings = FakeBookingRepository(events)
        runTest { bookings.book("old") }

        val action = EventDetailsRules.actionFor(
            event("new", startsIn = 3 * hour), null, listOf(event("old", startsIn = 2 * hour)), now
        )
        assertTrue(action is DetailsAction.Clashes && !action.canSwap)
    }

    @Test
    fun `offline swap is blocked`() = runTest {
        setUpClash()
        val vm = viewModel(online = false)
        vm.requestSwap()
        vm.confirmSwap()

        assertEquals(EventDetailsViewModel.OFFLINE_MESSAGE, vm.uiState.value.message)
        assertEquals(BookingStatus.CONFIRMED, bookings.getMyBooking("old").first()!!.status)
    }
}