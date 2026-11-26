package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.model.UserRole
import com.mdpgroup08.scpd.data.network.FakeNetworkMonitor
import com.mdpgroup08.scpd.data.repository.FakeAuthRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Club cancels an approved event (R1-S9) and sees booking counts (R2-S13). */
@OptIn(ExperimentalCoroutinesApi::class)
class ClubCancelEventTest {

    private val hour = 60L * 60 * 1000
    private val now = System.currentTimeMillis()

    private fun event(
        id: String, status: EventStatus = EventStatus.APPROVED, startsIn: Long = 48 * hour,
        booked: Int = 3, waitlist: List<String> = emptyList()
    ) = Event(
        id = id, title = "Event $id", clubId = "club_a", clubName = "Club A", status = status,
        startTime = now + startsIn, endTime = now + startsIn + 2 * hour,
        requestedPoints = 5, approvedPoints = 5, capacity = 10, bookedCount = booked, waitlist = waitlist
    )

    private lateinit var repo: FakeEventRepository

    private fun viewModel(online: Boolean = true) = MyEventsViewModel(
        repo, FakeAuthRepository(UserRole.CLUB, clubId = "club_a"), FakeNetworkMonitor(online), clock = { now }
    )

    private fun MyEventsViewModel.item(id: String) =
        (uiState.value as MyEventsUiState.Success).events.first { it.event.id == id }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // ---------- R2-S13: booking counts ----------

    @Test
    fun `approved event shows bookings and waitlist`() {
        repo = FakeEventRepository(listOf(event("a", booked = 10, waitlist = listOf("s1", "s2"))))
        assertEquals("Booked 10 of 10 · 2 on the waitlist", viewModel().item("a").bookingSummary)
    }

    @Test
    fun `draft has no booking summary`() {
        repo = FakeEventRepository(listOf(event("a", status = EventStatus.DRAFT)))
        assertNull(viewModel().item("a").bookingSummary)
    }

    // ---------- R1-S9: cancel ----------

    @Test
    fun `only approved events that have not started can be cancelled`() {
        repo = FakeEventRepository(
            listOf(
                event("ok"),
                event("pending", status = EventStatus.PENDING),
                event("started", startsIn = -hour)
            )
        )
        val vm = viewModel()
        assertTrue(vm.item("ok").canCancel)
        assertFalse(vm.item("pending").canCancel)
        assertFalse(vm.item("started").canCancel)
    }

    @Test
    fun `cancelling needs a reason`() = runTest {
        repo = FakeEventRepository(listOf(event("a")))
        val vm = viewModel()
        vm.openCancel(vm.item("a").event)
        vm.confirmCancel()

        assertNotNull(vm.cancelDialog.value?.error)
        assertEquals(EventStatus.APPROVED, repo.getEvent("a").first()!!.status)
    }

    @Test
    fun `cancel with a reason removes it from the Marketplace`() = runTest {
        repo = FakeEventRepository(listOf(event("a")))
        val vm = viewModel()
        vm.openCancel(vm.item("a").event)
        vm.onCancelReasonChange("Speaker unavailable")
        vm.confirmCancel()

        val saved = repo.getEvent("a").first()!!
        assertEquals(EventStatus.CANCELLED, saved.status)
        assertEquals("Speaker unavailable", saved.cancellationReason)
        assertTrue(repo.getApprovedUpcomingEvents().first().none { it.id == "a" })
        assertNull(vm.cancelDialog.value)
        assertNotNull(vm.message.value)
    }

    @Test
    fun `offline cancel is blocked and the dialog stays open`() = runTest {
        repo = FakeEventRepository(listOf(event("a")))
        val vm = viewModel(online = false)
        vm.openCancel(vm.item("a").event)
        vm.onCancelReasonChange("Speaker unavailable")
        vm.confirmCancel()

        assertEquals(MyEventsViewModel.OFFLINE_MESSAGE, vm.cancelDialog.value?.error)
        assertEquals(EventStatus.APPROVED, repo.getEvent("a").first()!!.status)
    }

    @Test
    fun `event already changed elsewhere shows a message and cancels nothing`() = runTest {
        repo = FakeEventRepository(listOf(event("a")))
        val vm = viewModel()
        vm.openCancel(vm.item("a").event)
        repo.cancelEvent("a", "Cancelled on another phone")
        vm.onCancelReasonChange("Speaker unavailable")
        vm.confirmCancel()

        assertNull(vm.cancelDialog.value)
        assertEquals("Cancelled on another phone", repo.getEvent("a").first()!!.cancellationReason)
    }
}