package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.model.User
import com.mdpgroup08.scpd.data.model.UserRole
import com.mdpgroup08.scpd.data.network.FakeNetworkMonitor
import com.mdpgroup08.scpd.data.repository.AuthRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApprovalQueueViewModelTest {

    private class FakeAdminAuth : AuthRepository {
        override suspend fun signIn(email: String, password: String) = UserRole.ADMIN
        override suspend fun currentUserRole() = UserRole.ADMIN
        override suspend fun currentUser(): User = User(id = "admin_123", role = "ADMIN")
        override suspend fun registerStudent(email: String, password: String, name: String, pdpaAccepted: Boolean, studyDetails: com.mdpgroup08.scpd.data.model.StudyDetails) {}
        override suspend fun verifyCheckInDevice(): Boolean = true
        override suspend fun sendPasswordResetEmail(email: String) {}
        override fun signOut() {}
    }

    private val now = System.currentTimeMillis()
    private val pendingEvent = Event(
        id = "pending_1",
        title = "AI Workshop",
        category = "Technology",
        clubId = "club_tech",
        clubName = "Tech Club",
        location = "Lab 3",
        startTime = now + 86400000L,
        endTime = now + 90000000L,
        requestedPoints = 8,
        capacity = 50,
        status = EventStatus.PENDING,
        submittedAt = now - 10000L
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `approve with 9 points when 8 requested gives error and nothing saved`() = runTest {
        val repo = FakeEventRepository(listOf(pendingEvent))
        val vm = ApprovalQueueViewModel(repo, FakeAdminAuth(), FakeNetworkMonitor(true))

        vm.openApproveDialog(pendingEvent)
        vm.onApprovedPointsChange("9")
        vm.confirmApprove()

        val state = vm.uiState.value
        assertEquals("Approved points cannot exceed requested points (8)", state.approveError)
        // Check that event status in repo is still PENDING
        assertEquals(EventStatus.PENDING, repo.getEvent("pending_1").first()?.status)
    }

    @Test
    fun `reject with no reason gives error`() = runTest {
        val repo = FakeEventRepository(listOf(pendingEvent))
        val vm = ApprovalQueueViewModel(repo, FakeAdminAuth(), FakeNetworkMonitor(true))

        vm.openRejectDialog(pendingEvent)
        vm.onRejectReasonChange("")
        vm.confirmReject()

        val state = vm.uiState.value
        assertEquals("Reason is required", state.rejectError)
        assertEquals(EventStatus.PENDING, repo.getEvent("pending_1").first()?.status)
    }

    @Test
    fun `reject with reason updates event status and removes from queue`() = runTest {
        val repo = FakeEventRepository(listOf(pendingEvent))
        val vm = ApprovalQueueViewModel(repo, FakeAdminAuth(), FakeNetworkMonitor(true))

        vm.openRejectDialog(pendingEvent)
        vm.onRejectReasonChange("Duplicated event topic")
        vm.confirmReject()

        val saved = repo.getEvent("pending_1").first()
        assertEquals(EventStatus.REJECTED, saved?.status)
        assertEquals("Duplicated event topic", saved?.rejectionReason)
    }

    @Test
    fun `airplane mode sets isOffline to true`() = runTest {
        val repo = FakeEventRepository(listOf(pendingEvent))
        val vm = ApprovalQueueViewModel(repo, FakeAdminAuth(), FakeNetworkMonitor(false))

        assertTrue(vm.uiState.value.isOffline)
    }
}
