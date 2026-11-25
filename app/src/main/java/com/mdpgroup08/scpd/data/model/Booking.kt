package com.mdpgroup08.scpd.data.model

import com.google.firebase.firestore.DocumentId

/** NO_SHOW is set only after the 72 h sync deadline passes with no check-in (R4-S26). */
enum class BookingStatus { CONFIRMED, CANCELLED, NO_SHOW }

/** A student's place on an event (R2-S4). The ID "{eventId}_{studentId}" allows only one booking per student per event. */
data class Booking(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val studentId: String = "",
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val createdAt: Long = 0L,
    val cancelledAt: Long? = null,
    /** True when the place came from the waitlist, so the student can be told (R2-S5 AC2, R5-S17). */
    val fromWaitlist: Boolean = false
)