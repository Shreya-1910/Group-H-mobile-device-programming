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
import org.junit.Before
import org.junit.Test

/** R1-S8: a Pending event still unreviewed at its start time becomes Expired. */
@OptIn(ExperimentalCoroutinesApi::class)
class EventExpiryTest {

    private val hour = 60L * 60 * 1000
    private val now = System.currentTimeMillis()

    private fun pending(id: String, startsIn: Long) = Event(
        id = id, title = "Event $id", clubId = "club_a", status = EventStatus.PENDING,
        requestedPoints = 5, capacity = 10,
        startTime = now + startsIn, endTime = now + startsIn + 2 * hour,
        submittedAt = now - 48 * hour
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `started pending event expires and leaves the admin queue`() = runTest {
        val repo = FakeEventRepository(listOf(pending("late", startsIn = -hour), pending("ok", startsIn = 24 * hour)))
        val vm = AdminApprovalViewModel(
            repo, FakeAuthRepository(UserRole.ADMIN, uid = "admin_1"), FakeNetworkMonitor(), clock = { now }
        )

        assertEquals(EventStatus.EXPIRED, repo.getEvent("late").first()!!.status)
        assertEquals(listOf("ok"), (vm.uiState.value.queue as ApprovalQueue.Loaded).items.map { it.event.id })
    }

    @Test
    fun `club sees its unreviewed event as expired`() = runTest {
        val repo = FakeEventRepository(listOf(pending("late", startsIn = -hour)))
        val vm = MyEventsViewModel(repo, FakeAuthRepository(UserRole.CLUB, clubId = "club_a"))

        val item = (vm.uiState.value as MyEventsUiState.Success).events.single()
        assertEquals(EventStatus.EXPIRED, item.event.status)
    }

    @Test
    fun `pending event that has not started is left alone`() = runTest {
        val repo = FakeEventRepository(listOf(pending("future", startsIn = 24 * hour)))
        repo.expire("future")
        assertEquals(EventStatus.PENDING, repo.getEvent("future").first()!!.status)
    }

    @Test
    fun `approved event is never expired`() = runTest {
        val repo = FakeEventRepository(listOf(pending("a", startsIn = -hour).copy(status = EventStatus.APPROVED)))
        repo.expire("a")
        assertEquals(EventStatus.APPROVED, repo.getEvent("a").first()!!.status)
    }
}

