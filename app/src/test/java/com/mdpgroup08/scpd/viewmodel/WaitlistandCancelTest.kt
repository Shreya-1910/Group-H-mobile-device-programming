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

/** Waitlist (R2-S5) and cancelling a booking (R2-S8). */
@OptIn(ExperimentalCoroutinesApi::class)
class WaitlistAndCancelTest {

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = System.currentTimeMillis()

    private fun event(id: String, startsIn: Long = 3 * day, capacity: Int = 2, booked: Int = 0) = Event(
        id = id, title = "Event $id", clubName = "Club", status = EventStatus.APPROVED,
        startTime = now + startsIn, endTime = now + startsIn + 2 * hour,
        requestedPoints = 5, approvedPoints = 5, capacity = capacity, bookedCount = booked
    )

    private lateinit var events: FakeEventRepository
    private lateinit var me: FakeBookingRepository

    private fun setUpEvents(vararg list: Event) {
        events = FakeEventRepository(list.toList())
        me = FakeBookingRepository(events, currentUserId = "me")
    }

    private fun details(id: String, as_: FakeBookingRepository = me, online: Boolean = true) = EventDetailsViewModel(
        SavedStateHandle(mapOf("eventId" to id)), events, as_, FakeNetworkMonitor(online), clock = { now }
    )

    private fun EventDetailsViewModel.loaded() = uiState.value.content as DetailsContent.Loaded

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // ---------- Waitlist (R2-S5) ----------

    @Test
    fun `full event lets me join the waitlist and shows my position`() = runTest {
        setUpEvents(event("a", capacity = 1, booked = 1))
        me.asUser("ahead_of_me").joinWaitlist("a")

        val vm = details("a")
        assertEquals(DetailsAction.JoinWaitlist, vm.loaded().action)

        vm.joinWaitlist()
        assertEquals(DetailsAction.OnWaitlist(position = 2, isClosed = false), vm.loaded().action)
    }

    @Test
    fun `leaving the waitlist removes me`() = runTest {
        setUpEvents(event("a", capacity = 1, booked = 1))
        me.joinWaitlist("a")

        val vm = details("a")
        vm.leaveWaitlist()
        assertTrue(events.getEvent("a").first()!!.waitlist.isEmpty())
        assertEquals(DetailsAction.JoinWaitlist, vm.loaded().action)
    }

    @Test
    fun `waitlist closes at the cut-off`() {
        setUpEvents(event("a", startsIn = hour, capacity = 1, booked = 1))
        assertEquals(DetailsAction.BookingClosed, details("a").loaded().action)
    }

    @Test
    fun `booking an overlapping event takes me off that waitlist`() = runTest {
        setUpEvents(event("full", capacity = 1, booked = 1), event("other", startsIn = 3 * day + hour, capacity = 5))
        me.joinWaitlist("full")

        val vm = details("other")
        assertEquals(listOf("full"), vm.loaded().leavesWaitlists.map { it.id })

        vm.book()
        assertTrue(events.getEvent("full").first()!!.waitlist.isEmpty())
        assertEquals(DetailsAction.Booked, vm.loaded().action)
    }

    // ---------- Cancel (R2-S8) ----------

    @Test
    fun `cancelling with nobody waiting frees the place`() = runTest {
        setUpEvents(event("a", capacity = 5))
        me.book("a")

        val vm = details("a")
        assertTrue(vm.loaded().canCancel)
        vm.requestCancel()
        assertTrue(vm.uiState.value.showCancelConfirm)
        vm.confirmCancel()

        assertEquals(BookingStatus.CANCELLED, me.getMyBooking("a").first()!!.status)
        assertEquals(0, events.getEvent("a").first()!!.bookedCount)
        assertEquals(DetailsAction.Book, vm.loaded().action)
    }

    @Test
    fun `cancelling gives my place to the first student in line`() = runTest {
        setUpEvents(event("a", capacity = 1))
        me.book("a")
        val first = me.asUser("first")
        val second = me.asUser("second")
        first.joinWaitlist("a")
        second.joinWaitlist("a")

        val vm = details("a")
        vm.requestCancel()
        vm.confirmCancel()

        val promoted = first.getMyBooking("a").first()!!
        assertEquals(BookingStatus.CONFIRMED, promoted.status)
        assertTrue(promoted.fromWaitlist)
        val e = events.getEvent("a").first()!!
        assertEquals(1, e.bookedCount)           // one left, one joined
        assertEquals(listOf("second"), e.waitlist)
        assertEquals(DetailsAction.OnWaitlist(1, false), details("a", as_ = second).loaded().action)
    }

    @Test
    fun `keep booking in the dialog changes nothing`() = runTest {
        setUpEvents(event("a", capacity = 5))
        me.book("a")
        val vm = details("a")
        vm.requestCancel()
        vm.keepBooking()

        assertFalse(vm.uiState.value.showCancelConfirm)
        assertEquals(BookingStatus.CONFIRMED, me.getMyBooking("a").first()!!.status)
    }

    @Test
    fun `cancel is not allowed after the cut-off`() = runTest {
        setUpEvents(event("a", startsIn = hour, capacity = 5))
        me.book("a")

        val vm = details("a")
        assertFalse(vm.loaded().canCancel)
        vm.requestCancel()
        assertFalse(vm.uiState.value.showCancelConfirm)
        vm.confirmCancel()
        assertEquals(BookingStatus.CONFIRMED, me.getMyBooking("a").first()!!.status)
    }

    @Test
    fun `I can book again after cancelling`() = runTest {
        setUpEvents(event("a", capacity = 5))
        me.book("a")
        me.cancel("a")

        val vm = details("a")
        vm.book()
        assertEquals(DetailsAction.Booked, vm.loaded().action)
        assertEquals(1, events.getEvent("a").first()!!.bookedCount)
    }

    @Test
    fun `offline cancel is blocked`() = runTest {
        setUpEvents(event("a", capacity = 5))
        me.book("a")

        val vm = details("a", online = false)
        vm.requestCancel()
        vm.confirmCancel()
        assertEquals(EventDetailsViewModel.OFFLINE_MESSAGE, vm.uiState.value.message)
        assertEquals(BookingStatus.CONFIRMED, me.getMyBooking("a").first()!!.status)
    }

    // ---------- Swap moves the waitlist too ----------

    @Test
    fun `swapping away gives the old place to the waitlist`() = runTest {
        setUpEvents(event("old", capacity = 1), event("new", startsIn = 3 * day + hour, capacity = 5))
        me.book("old")
        val waiting = me.asUser("waiting")
        waiting.joinWaitlist("old")

        val vm = details("new")
        vm.requestSwap()
        vm.confirmSwap()

        assertEquals(BookingStatus.CONFIRMED, waiting.getMyBooking("old").first()!!.status)
        assertEquals(1, events.getEvent("old").first()!!.bookedCount)
    }
}