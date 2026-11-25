package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Booking
import kotlinx.coroutines.flow.Flow

sealed class BookingException(message: String) : Exception(message) {
    object NotLoggedIn : BookingException("Not logged in")
    object EventNotFound : BookingException("Event not found")
    object EventNotApproved : BookingException("Event is not open for booking")
    object EventFull : BookingException("Event is full")
    object AlreadyBooked : BookingException("You already booked this event")
    /** Swap was asked for, but the student no longer has a booking on the old event. */
    object NotBooked : BookingException("You don't have a booking for that event")
}

interface BookingRepository {

    suspend fun book(eventId: String)

    /**
     * R2-S7: cancel the booking on [fromEventId] and book [toEventId] in one step.
     * Either both happen or neither does, so the student never loses a place by accident.
     */
    suspend fun swap(fromEventId: String, toEventId: String)

    fun getMyBooking(eventId: String): Flow<Booking?>

    /** The signed-in student's confirmed bookings, used for clash checks (R2-S7). */
    fun getMyBookings(): Flow<List<Booking>>
}