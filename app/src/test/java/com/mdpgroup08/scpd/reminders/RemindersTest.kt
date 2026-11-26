package com.mdpgroup08.scpd.reminders

import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.network.FakeNetworkMonitor
import com.mdpgroup08.scpd.data.repository.FakeBookingRepository
import com.mdpgroup08.scpd.data.repository.FakeEventRepository
import com.mdpgroup08.scpd.viewmodel.StudentRemindersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The 3-hour reminder and "Can't make it" (R4-S21). */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderTest {

    private val minute = 60L * 1000
    private val hour = 60 * minute
    private val now = System.currentTimeMillis()

    private fun event(id: String, startsIn: Long, capacity: Int = 5) = Event(
        id = id, title = "Event $id", clubName = "Club", status = EventStatus.APPROVED,
        startTime = now + startsIn, endTime = now + startsIn + 2 * hour,
        requestedPoints = 5, approvedPoints = 5, capacity = capacity
    )

    private fun booking(eventId: String, createdAgo: Long = 24 * hour, status: BookingStatus = BookingStatus.CONFIRMED) =
        Booking(eventId = eventId, studentId = "me", status = status, createdAt = now - createdAgo)

    /** Records what would have been scheduled, instead of using WorkManager. */
    private class FakeScheduler : ReminderScheduler {
        val scheduled = mutableMapOf<String, Long>()
        val reposts = mutableListOf<String>()
        override fun schedule(eventId: String, remindAt: Long) { scheduled[eventId] = remindAt }
        override fun cancel(eventId: String) { scheduled.remove(eventId) }
        override fun repostWhenOnline(eventId: String) { reposts += eventId }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // ---------- When to remind ----------

    @Test
    fun `reminder is 3 hours before the start`() {
        val e = event("a", startsIn = 24 * hour)
        val plan = ReminderRules.plan(listOf(booking("a")), listOf(e), now)
        assertEquals(listOf(PlannedReminder("a", e.startTime - 3 * hour)), plan)
    }

    @Test
    fun `booked just now inside the 3-hour window gets a reminder straight away`() {
        val e = event("a", startsIn = 150 * minute)
        val plan = ReminderRules.plan(listOf(booking("a", createdAgo = minute)), listOf(e), now)
        assertEquals(1, plan.size)
    }

    @Test
    fun `old booking whose reminder time has passed is not reminded again`() {
        val e = event("a", startsIn = 150 * minute)
        assertTrue(ReminderRules.plan(listOf(booking("a", createdAgo = 24 * hour)), listOf(e), now).isEmpty())
    }

    @Test
    fun `no reminder for cancelled bookings or events that have started`() {
        val cancelled = ReminderRules.plan(
            listOf(booking("a", status = BookingStatus.CANCELLED)), listOf(event("a", startsIn = 24 * hour)), now
        )
        val started = ReminderRules.plan(listOf(booking("b")), listOf(event("b", startsIn = -minute)), now)
        assertTrue(cancelled.isEmpty())
        assertTrue(started.isEmpty())
    }

    // ---------- Which button ----------

    @Test
    fun `online before the cut-off shows Can't make it`() {
        assertEquals(ReminderButton.CAN_CANCEL, ReminderRules.buttonFor(event("a", startsIn = 3 * hour), true, now))
    }

    @Test
    fun `offline shows no button`() {
        assertEquals(ReminderButton.OFFLINE, ReminderRules.buttonFor(event("a", startsIn = 3 * hour), false, now))
    }

    @Test
    fun `after the cut-off shows cancellation closed, even offline`() {
        assertEquals(ReminderButton.TOO_LATE, ReminderRules.buttonFor(event("a", startsIn = hour), false, now))
    }

    // ---------- Tapping Can't make it ----------

    @Test
    fun `can't make it cancels the booking and gives the place to the waitlist`() = runTest {
        val events = FakeEventRepository(listOf(event("a", startsIn = 3 * hour, capacity = 1)))
        val me = FakeBookingRepository(events, currentUserId = "me")
        me.book("a")
        val waiting = me.asUser("waiting")
        waiting.joinWaitlist("a")

        val result = CantMakeItHandler(me, events, FakeNetworkMonitor(online = true), clock = { now }).handle("a")

        assertTrue(result is CantMakeItResult.Cancelled)
        assertEquals(BookingStatus.CANCELLED, me.getMyBooking("a").first()!!.status)
        assertEquals(BookingStatus.CONFIRMED, waiting.getMyBooking("a").first()!!.status)
    }

    @Test
    fun `offline does nothing and keeps the booking`() = runTest {
        val events = FakeEventRepository(listOf(event("a", startsIn = 3 * hour)))
        val me = FakeBookingRepository(events, currentUserId = "me")
        me.book("a")

        val result = CantMakeItHandler(me, events, FakeNetworkMonitor(online = false), clock = { now }).handle("a")

        assertEquals(CantMakeItResult.Offline, result)
        assertEquals(BookingStatus.CONFIRMED, me.getMyBooking("a").first()!!.status)
    }

    @Test
    fun `after the cut-off it is too late and the booking stays`() = runTest {
        val events = FakeEventRepository(listOf(event("a", startsIn = hour)))
        val me = FakeBookingRepository(events, currentUserId = "me")
        me.book("a")

        val result = CantMakeItHandler(me, events, FakeNetworkMonitor(), clock = { now }).handle("a")

        assertEquals(CantMakeItResult.TooLate, result)
        assertEquals(BookingStatus.CONFIRMED, me.getMyBooking("a").first()!!.status)
    }

    @Test
    fun `tapping twice cancels only once`() = runTest {
        val events = FakeEventRepository(listOf(event("a", startsIn = 3 * hour)))
        val me = FakeBookingRepository(events, currentUserId = "me")
        me.book("a")
        val handler = CantMakeItHandler(me, events, FakeNetworkMonitor(), clock = { now })

        handler.handle("a")
        assertEquals(CantMakeItResult.NotBooked, handler.handle("a"))
        assertEquals(0, events.getEvent("a").first()!!.bookedCount)
    }

    // ---------- Keeping reminders in step with bookings ----------

    @Test
    fun `booking schedules a reminder and cancelling removes it`() = runTest {
        val events = FakeEventRepository(listOf(event("a", startsIn = 24 * hour)))
        val me = FakeBookingRepository(events, currentUserId = "me")
        val scheduler = FakeScheduler()
        StudentRemindersViewModel(me, events, scheduler, clock = { now })

        me.book("a")
        assertTrue("a" in scheduler.scheduled)

        me.cancel("a")
        assertTrue(scheduler.scheduled.isEmpty())
    }
}