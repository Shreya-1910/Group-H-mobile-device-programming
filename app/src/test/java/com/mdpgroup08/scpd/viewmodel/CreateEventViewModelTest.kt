package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.model.UserRole
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset

/** Validation and saving for Create event (R1-S1, R1-S2). */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateEventViewModelTest {

    private class FakeClubAuth(private val clubId: String? = "club_robotics") : AuthRepository {
        override suspend fun signIn(email: String, password: String) = UserRole.CLUB
        override suspend fun currentUserRole() = UserRole.CLUB
        override suspend fun currentClubId() = clubId
        override fun signOut() {}
    }

    // 1 Jan 2026 00:00 UTC, so "the future" is predictable
    private val now = 1_767_225_600_000L
    private val utc = ZoneOffset.UTC

    private val valid = EventForm(
        title = "Intro to Arduino", location = "Lab B2",
        date = "05/11/2026", time = "14:00", capacity = "60", points = "3"
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

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
    fun `capacity of 0 is rejected`() {
        val e = EventValidation.validate(valid.copy(capacity = "0"), now, utc)
        assertEquals("Capacity must be at least 1", e.capacity)
    }

    @Test
    fun `points of 11 are rejected`() {
        val e = EventValidation.validate(valid.copy(points = "11"), now, utc)
        assertEquals("Points must be between 1 and 10", e.points)
    }

    @Test
    fun `invalid form is not saved`() = runTest {
        val repo = FakeEventRepository(emptyList())
        val vm = CreateEventViewModel(repo, FakeClubAuth(), clock = { now })
        vm.onFormChange(valid.copy(points = "11"))
        vm.saveDraft()

        assertTrue(repo.getClubEvents("club_robotics").first().isEmpty())
        assertNotNull(vm.uiState.value.errors.points)
    }

    @Test
    fun `save draft stores a DRAFT for this club`() = runTest {
        val repo = FakeEventRepository(emptyList())
        val vm = CreateEventViewModel(repo, FakeClubAuth(), clock = { now })
        vm.onFormChange(valid)
        vm.saveDraft()

        val saved = repo.getClubEvents("club_robotics").first().single()
        assertEquals(EventStatus.DRAFT, saved.status)
        assertNull(saved.submittedAt)
        assertEquals("Draft saved", vm.uiState.value.successMessage)
    }

    @Test
    fun `submit asks for confirmation then stores PENDING`() = runTest {
        val repo = FakeEventRepository(emptyList())
        val vm = CreateEventViewModel(repo, FakeClubAuth(), clock = { now })
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
        val vm = CreateEventViewModel(FakeEventRepository(emptyList()), FakeClubAuth(clubId = null), clock = { now })
        vm.onFormChange(valid)
        vm.saveDraft()

        assertNotNull(vm.uiState.value.saveError)
        assertEquals(SaveAction.DRAFT, vm.uiState.value.lastAction)
    }
}
