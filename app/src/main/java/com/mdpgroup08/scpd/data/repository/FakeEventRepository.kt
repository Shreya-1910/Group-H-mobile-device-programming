package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEventRepository(initial: List<Event> = sampleEvents()) : EventRepository {

    private val store = MutableStateFlow(initial)

    override fun getApprovedUpcomingEvents(): Flow<List<Event>> = store.map { list ->
        val now = System.currentTimeMillis()
        list.filter { it.status == EventStatus.APPROVED && it.date >= now }.sortedBy { it.date }
    }

    override fun getEvent(id: String): Flow<Event?> = store.map { list -> list.find { it.id == id } }

    override fun getClubEvents(clubId: String): Flow<List<Event>> =
        store.map { list -> list.filter { it.clubId == clubId } }

    override suspend fun saveDraft(event: Event): String =
        upsert(event.copy(status = EventStatus.DRAFT, submittedAt = null))

    override suspend fun submit(event: Event): String =
        upsert(event.copy(status = EventStatus.PENDING, submittedAt = System.currentTimeMillis()))

    /** 給 FakeBookingRepository 同步名額用 */
    fun incrementBooked(eventId: String) {
        store.value = store.value.map {
            if (it.id == eventId) it.copy(bookedCount = it.bookedCount + 1) else it
        }
    }

    private fun upsert(event: Event): String {
        val id = event.id.ifBlank { "fake_${System.nanoTime()}" }
        val saved = event.copy(id = id)
        store.value = store.value.filter { it.id != id } + saved
        return id
    }

    companion object {
        private const val DAY = 24L * 60 * 60 * 1000

        fun sampleEvents(): List<Event> {
            val now = System.currentTimeMillis()
            return listOf(
                Event("1", "Hackathon 2026", "", "club_computing", "Lab 1", now + 2 * DAY, 10, 50, 25, EventStatus.APPROVED, now - DAY),
                Event("2", "Beach Clean-up", "", "club_green", "Port Dickson", now + 5 * DAY, 5, 30, 30, EventStatus.APPROVED, now - DAY),
                Event("3", "Career Talk: AI", "", "club_careers", "Hall A", now + 8 * DAY, 3, 100, 40, EventStatus.APPROVED, now - DAY),
                Event("4", "Charity Run", "", "club_sports", "Stadium", now + 12 * DAY, 8, 60, 48, EventStatus.APPROVED, now - DAY),
                Event("5", "Draft Workshop", "", "club_computing", "Lab 2", now + 15 * DAY, 4, 20, 0, EventStatus.DRAFT, null)
            )
        }
    }
}