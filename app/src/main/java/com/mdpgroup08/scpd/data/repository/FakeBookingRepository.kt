package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** In-memory BookingRepository for tests, using the same rules as the Firebase transactions. */
class FakeBookingRepository(
    private val events: FakeEventRepository,
    private val currentUserId: String = "fake_student",
    /** Shared between users, so a booking made for someone else (waitlist promotion) is visible to them. */
    private val store: MutableStateFlow<Map<String, Booking>> = MutableStateFlow(emptyMap())
) : BookingRepository {

    /** The same fake bookings, seen as another student. */
    fun asUser(userId: String) = FakeBookingRepository(events, userId, store)

    private fun key(eventId: String, user: String = currentUserId) = "${eventId}_$user"
    private suspend fun event(id: String): Event = events.getEvent(id).first() ?: throw BookingException.EventNotFound
    private fun put(booking: Booking) { store.value = store.value + (booking.id to booking) }
    private fun now() = System.currentTimeMillis()

    override fun myId(): String = currentUserId

    override suspend fun book(eventId: String, leaveWaitlistsFor: List<String>) {
        val event = event(eventId)
        if (event.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
        if (store.value[key(eventId)]?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
        if (event.bookedCount >= event.capacity) throw BookingException.EventFull

        events.update(eventId) { it.copy(bookedCount = it.bookedCount + 1, waitlist = it.waitlist - currentUserId) }
        leaveWaitlistsFor.forEach { id -> events.update(id) { it.copy(waitlist = it.waitlist - currentUserId) } }
        put(newBooking(eventId, currentUserId))
    }

    /** Checks everything before changing anything, like the Firebase transaction. */
    override suspend fun swap(fromEventId: String, toEventId: String) {
        val toEvent = event(toEventId)
        val fromEvent = event(fromEventId)
        val fromBooking = store.value[key(fromEventId)]

        if (fromBooking?.status != BookingStatus.CONFIRMED) throw BookingException.NotBooked
        if (toEvent.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
        if (store.value[key(toEventId)]?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
        if (toEvent.bookedCount >= toEvent.capacity) throw BookingException.EventFull

        put(fromBooking.copy(status = BookingStatus.CANCELLED, cancelledAt = now()))
        releasePlace(fromEvent)
        events.update(toEventId) { it.copy(bookedCount = it.bookedCount + 1, waitlist = it.waitlist - currentUserId) }
        put(newBooking(toEventId, currentUserId))
    }

    override suspend fun cancel(eventId: String) {
        val event = event(eventId)
        val mine = store.value[key(eventId)]
        if (mine?.status != BookingStatus.CONFIRMED) throw BookingException.NotBooked

        put(mine.copy(status = BookingStatus.CANCELLED, cancelledAt = now()))
        releasePlace(event)
    }

    override suspend fun joinWaitlist(eventId: String) {
        val event = event(eventId)
        if (event.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
        if (store.value[key(eventId)]?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
        if (currentUserId in event.waitlist) throw BookingException.AlreadyOnWaitlist
        if (event.bookedCount < event.capacity) throw BookingException.NotFull

        events.update(eventId) { it.copy(waitlist = it.waitlist + currentUserId) }
    }

    override suspend fun leaveWaitlist(eventId: String) {
        events.update(eventId) { it.copy(waitlist = it.waitlist - currentUserId) }
    }

    override fun getMyBooking(eventId: String): Flow<Booking?> =
        store.map { it[key(eventId)] }

    override fun getMyBookings(): Flow<List<Booking>> = store.map { all ->
        all.values.filter { it.studentId == currentUserId && it.status == BookingStatus.CONFIRMED }
    }

    /** R2-S5 AC2: the first student in line takes the freed place; otherwise the place is released. */
    private fun releasePlace(event: Event) {
        val next = event.waitlist.firstOrNull()
        if (next != null && store.value[key(event.id, next)]?.status != BookingStatus.CONFIRMED) {
            put(newBooking(event.id, next, fromWaitlist = true))
            events.update(event.id) { it.copy(waitlist = it.waitlist - next) }
        } else {
            events.update(event.id) {
                it.copy(
                    bookedCount = (it.bookedCount - 1).coerceAtLeast(0),
                    waitlist = if (next != null) it.waitlist - next else it.waitlist
                )
            }
        }
    }

    private fun newBooking(eventId: String, studentId: String, fromWaitlist: Boolean = false) = Booking(
        id = key(eventId, studentId), eventId = eventId, studentId = studentId,
        status = BookingStatus.CONFIRMED, createdAt = now(), fromWaitlist = fromWaitlist
    )
}