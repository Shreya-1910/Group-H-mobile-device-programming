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
    private val currentUserId: String = "fake_student"
) : BookingRepository {

    private val bookings = MutableStateFlow<Map<String, Booking>>(emptyMap())

    private fun key(eventId: String) = "${eventId}_$currentUserId"

    override suspend fun book(eventId: String) {
        val event: Event = events.getEvent(eventId).first() ?: throw BookingException.EventNotFound
        if (event.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
        if (bookings.value[key(eventId)]?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
        if (event.bookedCount >= event.capacity) throw BookingException.EventFull

        events.incrementBooked(eventId)
        bookings.value = bookings.value + (key(eventId) to newBooking(eventId))
    }

    /** Checks everything before changing anything, like the Firebase transaction. */
    override suspend fun swap(fromEventId: String, toEventId: String) {
        val toEvent = events.getEvent(toEventId).first() ?: throw BookingException.EventNotFound
        events.getEvent(fromEventId).first() ?: throw BookingException.EventNotFound
        val fromBooking = bookings.value[key(fromEventId)]

        if (fromBooking?.status != BookingStatus.CONFIRMED) throw BookingException.NotBooked
        if (toEvent.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
        if (bookings.value[key(toEventId)]?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
        if (toEvent.bookedCount >= toEvent.capacity) throw BookingException.EventFull

        events.decrementBooked(fromEventId)
        events.incrementBooked(toEventId)
        bookings.value = bookings.value +
                (key(fromEventId) to fromBooking.copy(status = BookingStatus.CANCELLED, cancelledAt = System.currentTimeMillis())) +
                (key(toEventId) to newBooking(toEventId))
    }

    override fun getMyBooking(eventId: String): Flow<Booking?> =
        bookings.map { it[key(eventId)] }

    override fun getMyBookings(): Flow<List<Booking>> = bookings.map { all ->
        all.values.filter { it.studentId == currentUserId && it.status == BookingStatus.CONFIRMED }
    }

    private fun newBooking(eventId: String) = Booking(
        id = key(eventId), eventId = eventId, studentId = currentUserId,
        status = BookingStatus.CONFIRMED, createdAt = System.currentTimeMillis()
    )
}