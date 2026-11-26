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
import java.time.ZoneOffset

/** Admin approval queue (R1-S4, R1-S5, R1-S8, R6-S21 EX3). */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminApprovalViewModelTest {

    // Thu 1 Jan 2026 00:00 UTC
    private val now = 1_767_225_600_000L
    private val day = 24L * 60 * 60 * 1000
    private val utc = ZoneOffset.UTC

    private fun pending(id: String, submittedDaysAgo: Int, requested: Int = 8) = Event(
        id = id, title = "Event $id", clubId = "club_a", status = EventStatus.PENDING,
        requestedPoints = requested, capacity = 50,
        startTime = now + 20 * day, endTime = now + 20 * day + 2 * 60 * 60 * 1000,
        submittedAt = now - submittedDaysAgo * day
    )

    private lateinit var repo: FakeEventRepository

    private fun viewModel(network: FakeNetworkMonitor = FakeNetworkMonitor()) =
        AdminApprovalViewModel(repo, FakeAuthRepository(UserRole.ADMIN, uid = "admin_1"), network, clock = { now }, zone = utc)

    private fun AdminApprovalViewModel.items() = (uiState.value.queue as ApprovalQueue.Loaded).items
    private fun AdminApprovalViewModel.event(id: String) = items().first { it.event.id == id }.event

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeEventRepository(
            listOf(pending("recent", submittedDaysAgo = 1), pending("old", submittedDaysAgo = 10),
                Event(id = "draft", status = EventStatus.DRAFT))
        )
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `queue shows only pending events, overdue first`() {
        val vm = viewModel()
        assertEquals(listOf("old", "recent"), vm.items().map { it.event.id })
        assertTrue(vm.items()[0].isOverdue)
        assertFalse(vm.items()[1].isOverdue)
    }

    @Test
    fun `approve sets approved points and writes the audit entry`() = runTest {
        val vm = viewModel()
        vm.openApprove(vm.event("recent"))
        vm.onPointsChange("5")
        vm.confirm()

        val saved = repo.getEvent("recent").first()!!
        assertEquals(EventStatus.APPROVED, saved.status)
        assertEquals(5, saved.approvedPoints)
        val audit = repo.auditLog.single()
        assertEquals("admin_1", audit.actorId)
        assertEquals("EVENT_APPROVED", audit.action)
        assertNull(vm.uiState.value.dialog)
        assertEquals(listOf("old"), vm.items().map { it.event.id })
    }

    @Test
    fun `approving more points than requested is refused`() = runTest {
        val vm = viewModel()
        vm.openApprove(vm.event("recent"))
        vm.onPointsChange("9")
        vm.confirm()

        assertEquals("Points must be between 1 and 8", vm.uiState.value.dialog?.error)
        assertEquals(EventStatus.PENDING, repo.getEvent("recent").first()!!.status)
        assertTrue(repo.auditLog.isEmpty())
    }

    @Test
    fun `approving 0 points is refused`() {
        val vm = viewModel()
        vm.openApprove(vm.event("recent"))
        vm.onPointsChange("0")
        vm.confirm()
        assertEquals("Points must be between 1 and 8", vm.uiState.value.dialog?.error)
    }

    @Test
    fun `reject without a reason is refused`() = runTest {
        val vm = viewModel()
        vm.openReject(vm.event("recent"))
        vm.confirm()

        assertNotNull(vm.uiState.value.dialog?.error)
        assertEquals(EventStatus.PENDING, repo.getEvent("recent").first()!!.status)
    }

    @Test
    fun `reject stores the reason and writes the audit entry`() = runTest {
        val vm = viewModel()
        vm.openReject(vm.event("recent"))
        vm.onReasonChange("Venue not booked")
        vm.confirm()

        val saved = repo.getEvent("recent").first()!!
        assertEquals(EventStatus.REJECTED, saved.status)
        assertEquals("Venue not booked", saved.rejectionReason)
        assertEquals("Venue not booked", repo.auditLog.single().reason)
    }

    @Test
    fun `event decided by another admin shows a clear message`() = runTest {
        val vm = viewModel()
        vm.openApprove(vm.event("recent"))
        repo.reject("recent", "Duplicate event", adminId = "admin_2")  // the other admin was faster
        vm.confirm()

        assertEquals(AdminApprovalViewModel.ALREADY_DECIDED_MESSAGE, vm.uiState.value.message)
        assertNull(vm.uiState.value.dialog)
        assertEquals(EventStatus.REJECTED, repo.getEvent("recent").first()!!.status)
    }

    @Test
    fun `offline decision is blocked`() = runTest {
        val vm = viewModel(network = FakeNetworkMonitor(online = false))
        vm.openApprove(vm.event("recent"))
        vm.confirm()

        assertEquals(AdminApprovalViewModel.OFFLINE_MESSAGE, vm.uiState.value.dialog?.error)
        assertEquals(EventStatus.PENDING, repo.getEvent("recent").first()!!.status)
    }

    @Test
    fun `event that has already started cannot be approved`() = runTest {
        // According to R1-S8, an event that has already started is filtered out and marked EXPIRED quietly.
        // So the Admin queue shouldn't even show it, and attempting to find it will fail.
        repo = FakeEventRepository(listOf(pending("late", submittedDaysAgo = 1).copy(startTime = now - 1000)))
        val vm = viewModel()
        
        // Ensure that the overdue event is automatically expired and removed from the queue
        val items = (vm.uiState.value.queue as ApprovalQueue.Loaded).items
        assertTrue("Overdue event should not be in the queue", items.isEmpty())
        assertEquals(EventStatus.EXPIRED, repo.getEvent("late").first()!!.status)
    }
}