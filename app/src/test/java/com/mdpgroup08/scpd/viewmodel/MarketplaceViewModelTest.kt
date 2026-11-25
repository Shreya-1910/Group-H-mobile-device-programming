package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.network.FakeNetworkMonitor
import com.mdpgroup08.scpd.data.repository.EventRepository
import com.mdpgroup08.scpd.data.repository.FakeEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Marketplace browse (R2-S1) and the booking cut-off (R2-S4). */
@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModelTest {

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = System.currentTimeMillis()

    private fun event(
        id: String, startsIn: Long, status: EventStatus = EventStatus.APPROVED,
        requested: Int = 8, approved: Int? = 5, capacity: Int = 50, booked: Int = 10
    ) = Event(
        id = id, title = "Event $id", clubName = "Club", status = status,
        startTime = now + startsIn, endTime = now + startsIn + 2 * hour,
        requestedPoints = requested, approvedPoints = approved,
        capacity = capacity, bookedCount = booked
    )

    /** A repository whose Marketplace query always fails. */
    private class FailingRepository : EventRepository by FakeEventRepository(emptyList()) {
        override fun getApprovedUpcomingEvents(): Flow<List<Event>> = flow { throw RuntimeException("No network") }
    }

    private fun viewModel(
        events: List<Event>,
        network: FakeNetworkMonitor = FakeNetworkMonitor()
    ) = MarketplaceViewModel(FakeEventRepository(events), network, clock = { now })

    private fun MarketplaceViewModel.items() = (uiState.value.list as MarketplaceList.Loaded).items

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `only approved upcoming events are shown, soonest first`() {
        val vm = viewModel(
            listOf(
                event("later", startsIn = 5 * day),
                event("soon", startsIn = 1 * day),
                event("pending", startsIn = 2 * day, status = EventStatus.PENDING),
                event("draft", startsIn = 2 * day, status = EventStatus.DRAFT),
                event("past", startsIn = -1 * day)
            )
        )
        assertEquals(listOf("soon", "later"), vm.items().map { it.event.id })
    }

    @Test
    fun `cards show approved points, not requested`() {
        val vm = viewModel(listOf(event("a", startsIn = day, requested = 8, approved = 5)))
        assertEquals(5, vm.items().single().points)
    }

    @Test
    fun `open event shows places left`() {
        val item = viewModel(listOf(event("a", startsIn = day, capacity = 50, booked = 48))).items().single()
        assertEquals(Availability.OPEN, item.availability)
        assertEquals(2, item.placesLeft)
    }

    @Test
    fun `full event is marked full`() {
        val item = viewModel(listOf(event("a", startsIn = day, capacity = 30, booked = 30))).items().single()
        assertEquals(Availability.FULL, item.availability)
        assertEquals(0, item.placesLeft)
    }

    @Test
    fun `event starting within 2 hours is closed for booking`() {
        val item = viewModel(listOf(event("a", startsIn = 90 * 60 * 1000L))).items().single()
        assertEquals(Availability.CLOSED, item.availability)
    }

    @Test
    fun `cut-off is exactly 2 hours before the start`() {
        val e = event("a", startsIn = 2 * hour)
        assertTrue(BookingRules.isBookingClosed(e, now))
        assertFalse(BookingRules.isBookingClosed(e, now - 1))
    }

    @Test
    fun `no events gives an empty list, not an error`() {
        assertTrue(viewModel(emptyList()).items().isEmpty())
    }

    @Test
    fun `load failure shows an error with retry`() {
        val vm = MarketplaceViewModel(FailingRepository(), FakeNetworkMonitor(), clock = { now })
        assertTrue(vm.uiState.value.list is MarketplaceList.Error)
    }

    @Test
    fun `offline shows the offline flag`() {
        val vm = viewModel(listOf(event("a", startsIn = day)), network = FakeNetworkMonitor(online = false))
        assertTrue(vm.uiState.value.isOffline)
        assertEquals(1, vm.items().size)  // saved events still shown (R2-S9)
    }
}