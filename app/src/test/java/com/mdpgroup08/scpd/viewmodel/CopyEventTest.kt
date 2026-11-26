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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Copy a rejected event into a new draft (R1-S11). */
@OptIn(ExperimentalCoroutinesApi::class)
class CopyEventTest {

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = System.currentTimeMillis()

    private fun event(id: String, status: EventStatus, startsIn: Long = 10 * day) = Event(
        id = id, title = "Robotics Workshop", description = "Build a robot", category = "Technology",
        clubId = "club_a", clubName = "Club A", location = "Lab 2", status = status,
        startTime = now + startsIn, endTime = now + startsIn + 2 * hour,
        requestedPoints = 6, capacity = 40, rejectionReason = "Venue not booked"
    )

    private val clubAuth = FakeAuthRepository(UserRole.CLUB, clubId = "club_a")

    private fun createViewModel(repo: FakeEventRepository) =
        CreateEventViewModel(repo, clubAuth, FakeNetworkMonitor(), clock = { now })

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `copy fills the form and shows the admin's reason`() {
        val repo = FakeEventRepository(listOf(event("rej", EventStatus.REJECTED)))
        val vm = createViewModel(repo)
        vm.loadCopy("rej")

        val state = vm.uiState.value
        assertEquals("Robotics Workshop", state.form.title)
        assertEquals("6", state.form.points)
        assertEquals("", state.editingId)                       // a new event, not an edit
        assertEquals("Venue not booked", state.copiedFrom?.reason)
    }

    @Test
    fun `saving the copy creates a new draft and leaves the original rejected`() = runTest {
        val repo = FakeEventRepository(listOf(event("rej", EventStatus.REJECTED)))
        val vm = createViewModel(repo)
        vm.loadCopy("rej")
        vm.saveDraft()

        val all = repo.getClubEvents("club_a").first()
        assertEquals(2, all.size)
        assertEquals(EventStatus.REJECTED, all.first { it.id == "rej" }.status)
        assertEquals(EventStatus.DRAFT, all.first { it.id != "rej" }.status)
    }

    @Test
    fun `a date that has passed is cleared so a new one must be chosen`() {
        val repo = FakeEventRepository(listOf(event("old", EventStatus.EXPIRED, startsIn = -day)))
        val vm = createViewModel(repo)
        vm.loadCopy("old")

        assertEquals("", vm.uiState.value.form.date)
        assertEquals("Robotics Workshop", vm.uiState.value.form.title)
    }

    @Test
    fun `pending and approved events cannot be copied`() {
        val repo = FakeEventRepository(listOf(event("p", EventStatus.PENDING), event("a", EventStatus.APPROVED)))
        val vm = createViewModel(repo)
        vm.loadCopy("p")
        vm.loadCopy("a")

        assertNull(vm.uiState.value.copiedFrom)
        assertEquals("", vm.uiState.value.form.title)
    }

    @Test
    fun `My events offers copy only for rejected, cancelled and expired`() {
        val repo = FakeEventRepository(
            listOf(event("rej", EventStatus.REJECTED), event("ok", EventStatus.APPROVED))
        )
        val vm = MyEventsViewModel(repo, clubAuth, FakeNetworkMonitor(), clock = { now })
        val items = (vm.uiState.value as MyEventsUiState.Success).events

        assertTrue(items.first { it.event.id == "rej" }.canCopy)
        assertFalse(items.first { it.event.id == "ok" }.canCopy)
    }
}