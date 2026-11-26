package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.network.FakeNetworkMonitor
import com.mdpgroup08.scpd.data.repository.FakeBookingRepository
import com.mdpgroup08.scpd.data.repository.FakeEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Student "My events" (R4-S10). */
@OptIn(ExperimentalCoroutinesApi::class)
class StudentEventsViewModelTest {

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = System.currentTimeMillis()

    private fun event(id: String, startsIn: Long, capacity: Int = 5, booked: Int = 0) = Event(
        id = id, title = "Event $id", clubName = "Club", status = EventStatus.APPROVED,
        startTime = now + startsIn, endTime = now + startsIn + 2 * hour,
        requestedPoints = 8, approvedPoints = 5, capacity = capacity, bookedCount = booked
    )

    private lateinit var events: FakeEventRepository
    private lateinit var me: FakeBookingRepository

    private fun viewModel(online: Boolean = true) =
        StudentEventsViewModel(me, events, FakeNetworkMonitor(online), clock = { now })

    private fun StudentEventsViewModel.loaded() = uiState.value.list as StudentEventsList.Loaded

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `booked events are listed soonest first with approved points`() = runTest {
        events = FakeEventRepository(listOf(event("later", 5 * day), event("soon", day)))
        me = FakeBookingRepository(events, currentUserId = "me")
        me.book("later")
        me.book("soon")

        val booked = viewModel().loaded().booked
        assertEquals(listOf("soon", "later"), booked.map { it.event.id })
        assertEquals(5, booked.first().points)
    }

    @Test
    fun `cancelled bookings are not listed`() = runTest {
        events = FakeEventRepository(listOf(event("a", day)))
        me = FakeBookingRepository(events, currentUserId = "me")
        me.book("a")
        me.cancel("a")

        assertTrue(viewModel().loaded().isEmpty)
    }

    @Test
    fun `waitlisted events show my position`() = runTest {
        events = FakeEventRepository(listOf(event("full", day, capacity = 1, booked = 1)))
        me = FakeBookingRepository(events, currentUserId = "me")
        me.asUser("ahead").joinWaitlist("full")
        me.joinWaitlist("full")

        val item = viewModel().loaded().waitlisted.single()
        assertEquals(StudentEventStatus.Waitlisted(position = 2), item.status)
    }

    @Test
    fun `a place from the waitlist is labelled`() = runTest {
        events = FakeEventRepository(listOf(event("a", day, capacity = 1)))
        me = FakeBookingRepository(events, currentUserId = "me")
        val other = me.asUser("other")
        other.book("a")
        me.joinWaitlist("a")
        other.cancel("a")   // my turn

        val item = viewModel().loaded().booked.single()
        assertEquals(StudentEventStatus.Booked(fromWaitlist = true), item.status)
    }

    @Test
    fun `offline still shows the saved list with the offline flag`() = runTest {
        events = FakeEventRepository(listOf(event("a", day)))
        me = FakeBookingRepository(events, currentUserId = "me")
        me.book("a")

        val vm = viewModel(online = false)
        assertTrue(vm.uiState.value.isOffline)
        assertEquals(1, vm.loaded().booked.size)
    }
}