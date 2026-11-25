package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Booking
import kotlinx.coroutines.flow.Flow

sealed class BookingException(message: String) : Exception(message) {
    object NotLoggedIn : BookingException("Not logged in")
    object EventNotFound : BookingException("Event not found")
    object EventNotApproved : BookingException("Event is not open for booking")
    object EventFull : BookingException("Event is full")
    object AlreadyBooked : BookingException("You already booked this event")
    /** Swap or cancel was asked for, but the student no longer has that booking. */
    object NotBooked : BookingException("You don't have a booking for that event")
    /** Tried to join the waitlist, but a place is free, so the student should book instead. */
    object NotFull : BookingException("A place is free, so you can book it")
    object AlreadyOnWaitlist : BookingException("You're already on the waitlist")
}

interface BookingRepository {

    /** uid of the signed-in student, used to find their place on a waitlist. */
    fun myId(): String?

    /**
     * R2-S4: book one place. Also removes the student from this event's waitlist and from
     * [leaveWaitlistsFor], the waitlists of events that overlap this one (R2-S5 EX1).
     */
    suspend fun book(eventId: String, leaveWaitlistsFor: List<String> = emptyList())

    /** R2-S7: cancel [fromEventId] and book [toEventId] in one step. The freed place goes to the waitlist. */
    suspend fun swap(fromEventId: String, toEventId: String)

    /** R2-S8: cancel a booking. The first student on the waitlist takes the place (R2-S5 AC2). */
    suspend fun cancel(eventId: String)

    /** R2-S5: join the waitlist of a full event. */
    suspend fun joinWaitlist(eventId: String)

    suspend fun leaveWaitlist(eventId: String)

    fun getMyBooking(eventId: String): Flow<Booking?>

    /** The signed-in student's confirmed bookings, used for clash checks (R2-S7). */
    fun getMyBookings(): Flow<List<Booking>>
}