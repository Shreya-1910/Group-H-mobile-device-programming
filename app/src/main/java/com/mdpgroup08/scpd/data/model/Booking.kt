package com.mdpgroup08.scpd.data.model

import com.google.firebase.firestore.DocumentId

enum class BookingStatus { CONFIRMED, CANCELLED }

data class Booking(
    @DocumentId val id: String = "",      // = "{eventId}_{studentId}"
    val eventId: String = "",
    val studentId: String = "",
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val createdAt: Long = 0L
)