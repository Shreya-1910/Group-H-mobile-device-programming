package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** In-memory BookingRepository for tests, using the same rules as the Firebase transaction. */
class FakeBookingRepository(
    private val events: FakeEventRepository,
    private val currentUserId: String = "fake_student"
) : BookingRepository {

    private val bookings = MutableStateFlow<Map<String, Booking>>(emptyMap())

    override suspend fun book(eventId: String) {
        val event: Event = events.getEvent(eventId).first() ?: throw BookingException.EventNotFound
        val key = "${eventId}_$currentUserId"
        if (event.status != EventStatus.APPROVED) throw BookingException.EventNotApproved
        if (bookings.value[key]?.status == BookingStatus.CONFIRMED) throw BookingException.AlreadyBooked
        if (event.bookedCount >= event.capacity) throw BookingException.EventFull

        events.incrementBooked(eventId)
        bookings.value = bookings.value + (key to Booking(
            id = key, eventId = eventId, studentId = currentUserId,
            status = BookingStatus.CONFIRMED, createdAt = System.currentTimeMillis()
        ))
    }

    override fun getMyBooking(eventId: String): Flow<Booking?> =
        bookings.map { it["${eventId}_$currentUserId"] }

    override fun getMyBookings(): Flow<List<Booking>> = bookings.map { all ->
        all.values.filter { it.studentId == currentUserId && it.status == BookingStatus.CONFIRMED }
    }
}