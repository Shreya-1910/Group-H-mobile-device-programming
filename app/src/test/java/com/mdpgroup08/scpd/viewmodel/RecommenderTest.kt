package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
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

/** "For you" recommendations (R2-S11). */
@OptIn(ExperimentalCoroutinesApi::class)
class RecommenderTest {

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val now = System.currentTimeMillis()

    private fun event(
        id: String, category: String = "Technology", club: String = "Computing Club",
        startsIn: Long = 3 * day, capacity: Int = 10, booked: Int = 0, waitlist: List<String> = emptyList()
    ) = Event(
        id = id, title = "Event $id", category = category, clubName = club, status = EventStatus.APPROVED,
        startTime = now + startsIn, endTime = now + startsIn + 2 * hour,
        requestedPoints = 5, approvedPoints = 5, capacity = capacity, bookedCount = booked, waitlist = waitlist
    )

    private fun booking(eventId: String) = Booking(eventId = eventId, studentId = "me", status = BookingStatus.CONFIRMED)

    private fun ids(recs: List<Recommendation>) = recs.map { it.item.event.id }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `recommends events in categories I have booked, with the reason`() {
        val past = event("past", category = "Sports", club = "Sports Club", startsIn = -10 * day)
        val upcoming = listOf(
            event("run", category = "Sports", club = "Other Club", startsIn = 4 * day),
            event("talk", category = "Career", club = "Careers Club")
        )
        val recs = Recommender.recommend(upcoming, listOf(booking("past")), listOf(past), "me", now)

        assertEquals(listOf("run"), ids(recs))
        assertEquals("Because you booked 1 Sports event", recs.single().reason)
    }

    @Test
    fun `a club I keep booking ranks above a category match`() {
        val history = listOf(
            event("p1", category = "Career", club = "Robotics Club", startsIn = -20 * day),
            event("p2", category = "Sports", club = "Other Club", startsIn = -10 * day)
        )
        val upcoming = listOf(
            event("sports", category = "Sports", club = "Another Club", startsIn = 2 * day),
            event("robotics", category = "Technology", club = "Robotics Club", startsIn = 5 * day)
        )
        val recs = Recommender.recommend(upcoming, listOf(booking("p1"), booking("p2")), history, "me", now)

        assertEquals(listOf("robotics", "sports"), ids(recs))
        assertEquals("From Robotics Club, which you've booked before", recs.first().reason)
    }

    @Test
    fun `never recommends events I cannot book`() {
        val past = event("past", startsIn = -10 * day)
        val mine = event("mine", startsIn = 3 * day)
        val upcoming = listOf(
            mine,                                                    // already booked
            event("full", capacity = 5, booked = 5),                 // full
            event("closed", startsIn = hour),                        // past the cut-off
            event("waiting", waitlist = listOf("me")),               // on its waitlist
            event("clash", startsIn = 3 * day + hour),               // overlaps "mine"
            event("ok", startsIn = 6 * day)
        )
        val recs = Recommender.recommend(upcoming, listOf(booking("past"), booking("mine")), listOf(past, mine), "me", now)

        assertEquals(listOf("ok"), ids(recs))
    }

    @Test
    fun `a new student sees popular events instead`() {
        val upcoming = listOf(event("quiet", booked = 1), event("busy", booked = 8))
        val recs = Recommender.recommend(upcoming, emptyList(), emptyList(), "me", now)

        assertEquals(listOf("busy", "quiet"), ids(recs))
        assertEquals("Popular: 80% of places taken", recs.first().reason)
    }

    @Test
    fun `at most 5 recommendations`() {
        val upcoming = (1..8).map { event("e$it", startsIn = it * day) }
        assertEquals(Recommender.MAX, Recommender.recommend(upcoming, emptyList(), emptyList(), "me", now).size)
    }

    @Test
    fun `booking a recommended event removes it from the row`() = runTest {
        val events = FakeEventRepository(listOf(event("a"), event("b", startsIn = 6 * day)))
        val me = FakeBookingRepository(events, currentUserId = "me")
        val vm = ForYouViewModel(events, me, clock = { now })
        assertTrue("a" in ids(vm.recommendations.value))

        me.book("a")
        assertTrue("a" !in ids(vm.recommendations.value))
    }
}