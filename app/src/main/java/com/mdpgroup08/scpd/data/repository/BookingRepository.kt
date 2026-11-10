package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Booking
import kotlinx.coroutines.flow.Flow

sealed class BookingException(message: String) : Exception(message) {
    object NotLoggedIn : BookingException("Not logged in")
    object EventNotFound : BookingException("Event not found")
    object EventNotApproved : BookingException("Event is not open for booking")
    object EventFull : BookingException("Event is full")
    object AlreadyBooked : BookingException("You already booked this event")
}

interface BookingRepository {

    suspend fun book(eventId: String)
    fun getMyBooking(eventId: String): Flow<Booking?>
}