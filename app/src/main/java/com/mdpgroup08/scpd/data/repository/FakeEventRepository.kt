package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory EventRepository for tests and for running the app without Firebase. */
class FakeEventRepository(initial: List<Event> = sampleEvents()) : EventRepository {

    private val store = MutableStateFlow(initial)

    override fun getApprovedUpcomingEvents(): Flow<List<Event>> = store.map { list ->
        val now = System.currentTimeMillis()
        list.filter { it.status == EventStatus.APPROVED && it.startTime >= now }
            .sortedBy { it.startTime }
    }

    override fun getEvent(id: String): Flow<Event?> = store.map { list -> list.find { it.id == id } }

    override fun getClubEvents(clubId: String): Flow<List<Event>> =
        store.map { list -> list.filter { it.clubId == clubId } }

    override suspend fun saveDraft(event: Event): String =
        upsert(event.copy(status = EventStatus.DRAFT, submittedAt = null))

    override suspend fun submit(event: Event): String =
        upsert(event.copy(status = EventStatus.PENDING, submittedAt = System.currentTimeMillis()))

    /** Used by FakeBookingRepository to keep the booked count in step. */
    fun incrementBooked(eventId: String) {
        store.value = store.value.map {
            if (it.id == eventId) it.copy(bookedCount = it.bookedCount + 1) else it
        }
    }

    private fun upsert(event: Event): String {
        val id = event.id.ifBlank { "fake_${System.nanoTime()}" }
        store.value = store.value.filter { it.id != id } + event.copy(id = id)
        return id
    }

    companion object {
        private const val HOUR = 60L * 60 * 1000
        private const val DAY = 24 * HOUR

        /** Five sample events; "Beach Clean-up" is full so the full-event state can be tested. */
        fun sampleEvents(): List<Event> {
            val now = System.currentTimeMillis()
            return listOf(
                approved("1", "Hackathon 2026", "Technology", "club_computing", "Computing Club",
                    "Lab 1", now + 2 * DAY, 10, 50, 25, now),
                approved("2", "Beach Clean-up", "Community", "club_green", "Green Society",
                    "Port Dickson", now + 5 * DAY, 5, 30, 30, now),
                approved("3", "Career Talk: AI", "Career", "club_careers", "Careers Club",
                    "Hall A", now + 8 * DAY, 3, 100, 40, now),
                approved("4", "Charity Run", "Sports", "club_sports", "Sports Club",
                    "Stadium", now + 12 * DAY, 8, 60, 48, now),
                Event(
                    id = "5", title = "Draft Workshop", category = "Technology",
                    clubId = "club_computing", clubName = "Computing Club", location = "Lab 2",
                    startTime = now + 15 * DAY, endTime = now + 15 * DAY + 2 * HOUR,
                    requestedPoints = 4, capacity = 20,
                    status = EventStatus.DRAFT, createdAt = now - DAY
                )
            )
        }

        private fun approved(
            id: String, title: String, category: String, clubId: String, clubName: String,
            location: String, start: Long, points: Int, capacity: Int, booked: Int, now: Long
        ) = Event(
            id = id, title = title, category = category,
            clubId = clubId, clubName = clubName, location = location,
            startTime = start, endTime = start + 2 * HOUR,
            requestedPoints = points, approvedPoints = points,
            capacity = capacity, bookedCount = booked,
            status = EventStatus.APPROVED,
            createdAt = now - 2 * DAY, submittedAt = now - DAY
        )
    }
}