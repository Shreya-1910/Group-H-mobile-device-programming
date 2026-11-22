package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.model.StudyDetails
import com.mdpgroup08.scpd.data.model.User
import com.mdpgroup08.scpd.data.model.UserRole
import com.mdpgroup08.scpd.data.network.FakeNetworkMonitor
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
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

/** Validation, saving and offline handling for Create event (R1-S1, R1-S2). */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateEventViewModelTest {

    private class FakeClubAuth(private val clubId: String? = "club_robotics") : AuthRepository {
        override suspend fun signIn(email: String, password: String) = UserRole.CLUB
        override suspend fun registerStudent(
            email: String,
            password: String,
            name: String,
            pdpaAccepted: Boolean,
            studyDetails: StudyDetails
        ) {}
        override suspend fun currentUserRole() = UserRole.CLUB
        override suspend fun currentUser(): User? = null
        override suspend fun currentClubId() = clubId
        override suspend fun verifyCheckInDevice(): Boolean = true
        override fun signOut() {}
    }

    /** Saves the event but reports a failure the first time, like a write whose reply was lost. */
    private class LostReplyRepository(private val inner: FakeEventRepository) : EventRepository by inner {
        private var failed = false
        override suspend fun saveDraft(event: Event): String {
            val id = inner.saveDraft(event)
            if (!failed) { failed = true; throw RuntimeException("Reply lost") }
            return id
        }
    }

    // 1 Jan 2026 00:00 UTC, so "the future" is predictable
    private val now = 1_767_225_600_000L
    private val utc = ZoneOffset.UTC

    private val valid = EventForm(
        title = "Intro to Arduino", category = "Technology", location = "Lab B2",
        date = "05/11/2026", time = "14:00", endTime = "16:00", capacity = "60", points = "3"
    )

    private fun viewModel(
        repo: EventRepository = FakeEventRepository(emptyList()),
        auth: AuthRepository = FakeClubAuth(),
        network: FakeNetworkMonitor = FakeNetworkMonitor()
    ) = CreateEventViewModel(repo, auth, network, clock = { now })

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // ---------- Validation (R1-S1) ----------

    @Test
    fun `valid event passes`() {
        assertFalse(EventValidation.validate(valid, now, utc).hasErrors)
    }

    @Test
    fun `missing title and location are rejected`() {
        val e = EventValidation.validate(valid.copy(title = " ", location = ""), now, utc)
        assertEquals("Title is required", e.title)
        assertEquals("Location is required", e.location)
    }

    @Test
    fun `missing category is rejected`() {
        val e = EventValidation.validate(valid.copy(category = ""), now, utc)
        assertEquals("Choose a category", e.category)
    }

    @Test
    fun `start time in the past is rejected`() {
        val e = EventValidation.validate(valid.copy(date = "01/06/2025"), now, utc)
        assertEquals("The start time must be in the future", e.dateTime)
    }

    @Test
    fun `badly formatted date is rejected`() {
        val e = EventValidation.validate(valid.copy(date = "2026-11-05"), now, utc)
        assertEquals("Use the format dd/MM/yyyy and HH:mm", e.dateTime)
    }

    @Test
    fun `missing end time is rejected`() {
        val e = EventValidation.validate(valid.copy(endTime = ""), now, utc)
        assertEquals("End time is required", e.endTime)
    }

    @Test
    fun `end time before start is rejected`() {
        val e = EventValidation.validate(valid.copy(endTime = "13:00"), now, utc)
        assertEquals("End time must be after the start time", e.endTime)
    }

    @Test
    fun `end time equal to start is rejected`() {
        val e = EventValidation.validate(valid.copy(endTime = "14:00"), now, utc)
        assertEquals("End time must be after the start time", e.endTime)
    }

    @Test
    fun `badly formatted end time is rejected`() {
        val e = EventValidation.validate(valid.copy(endTime = "4pm"), now, utc)
        assertEquals("Use the format HH:mm", e.endTime)
    }

    @Test
    fun `capacity of 0 is rejected`() {
        val e = EventValidation.validate(valid.copy(capacity = "0"), now, utc)
        assertEquals("Capacity must be at least 1", e.capacity)
    }

    @Test
    fun `points of 11 are rejected`() {
        val e = EventValidation.validate(valid.copy(points = "11"), now, utc)
        assertEquals("Points must be between 1 and 10", e.points)
    }

    // ---------- Saving (R1-S1, R1-S2) ----------

    @Test
    fun `invalid form is not saved`() = runTest {
        val repo = FakeEventRepository(emptyList())
        val vm = viewModel(repo = repo)
        vm.onFormChange(valid.copy(points = "11"))
        vm.saveDraft()

        assertTrue(repo.getClubEvents("club_robotics").first().isEmpty())
        assertNotNull(vm.uiState.value.errors.points)
    }

    @Test
    fun `save draft stores a DRAFT for this club`() = runTest {
        val repo = FakeEventRepository(emptyList())
        val vm = viewModel(repo = repo)
        vm.onFormChange(valid)
        vm.saveDraft()

        val saved = repo.getClubEvents("club_robotics").first().single()
        assertEquals(EventStatus.DRAFT, saved.status)
        assertNull(saved.submittedAt)
        assertEquals("Draft saved", vm.uiState.value.successMessage)
    }

    @Test
    fun `saved event has end time after start and the category`() = runTest {
        val repo = FakeEventRepository(emptyList())
        val vm = viewModel(repo = repo)
        vm.onFormChange(valid)
        vm.saveDraft()

        val saved = repo.getClubEvents("club_robotics").first().single()
        assertTrue(saved.endTime > saved.startTime)
        assertEquals("Technology", saved.category)
        assertEquals(now, saved.createdAt)
    }

    @Test
    fun `submit asks for confirmation then stores PENDING`() = runTest {
        val repo = FakeEventRepository(emptyList())
        val vm = viewModel(repo = repo)
        vm.onFormChange(valid)
        vm.requestSubmit()
        assertTrue(vm.uiState.value.showSubmitConfirm)

        vm.confirmSubmit()
        val saved = repo.getClubEvents("club_robotics").first().single()
        assertEquals(EventStatus.PENDING, saved.status)
        assertNotNull(saved.submittedAt)
    }

    @Test
    fun `save failure shows an error with retry`() = runTest {
        val vm = viewModel(auth = FakeClubAuth(clubId = null))
        vm.onFormChange(valid)
        vm.saveDraft()

        assertNotNull(vm.uiState.value.saveError)
        assertEquals(SaveAction.DRAFT, vm.uiState.value.lastAction)
    }

    // ---------- Offline and retry (R1-S2 AC2) ----------

    @Test
    fun `offline submit is blocked and the form is kept`() = runTest {
        val repo = FakeEventRepository(emptyList())
        val vm = viewModel(repo = repo, network = FakeNetworkMonitor(online = false))
        vm.onFormChange(valid)
        vm.requestSubmit()
        vm.confirmSubmit()

        assertTrue(repo.getClubEvents("club_robotics").first().isEmpty())
        assertEquals(CreateEventViewModel.OFFLINE_MESSAGE, vm.uiState.value.saveError)
        assertEquals(valid, vm.uiState.value.form)
    }

    @Test
    fun `offline draft save is blocked`() = runTest {
        val repo = FakeEventRepository(emptyList())
        val vm = viewModel(repo = repo, network = FakeNetworkMonitor(online = false))
        vm.onFormChange(valid)
        vm.saveDraft()

        assertTrue(repo.getClubEvents("club_robotics").first().isEmpty())
        assertEquals(CreateEventViewModel.OFFLINE_MESSAGE, vm.uiState.value.saveError)
    }

    @Test
    fun `offline banner clears when the connection returns`() = runTest {
        val network = FakeNetworkMonitor(online = false)
        val vm = viewModel(network = network)
        assertTrue(vm.uiState.value.isOffline)

        network.isOnline.value = true
        assertFalse(vm.uiState.value.isOffline)
    }

    @Test
    fun `retry after a lost reply does not create a duplicate event`() = runTest {
        val inner = FakeEventRepository(emptyList())
        val vm = viewModel(repo = LostReplyRepository(inner))
        vm.onFormChange(valid)
        vm.saveDraft()
        assertNotNull(vm.uiState.value.saveError)

        vm.retry()
        assertEquals(1, inner.getClubEvents("club_robotics").first().size)
        assertEquals("Draft saved", vm.uiState.value.successMessage)
    }
}