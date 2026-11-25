package com.mdpgroup08.scpd.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/** Event lifecycle (R1): DRAFT → PENDING → APPROVED / REJECTED. APPROVED can become CANCELLED; PENDING becomes EXPIRED if not reviewed by the start time. */
enum class EventStatus { DRAFT, PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED }

/** An event created by a club (R1) and listed in the Marketplace once approved (R2). Times are epoch milliseconds. */
data class Event(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val clubId: String = "",
    val clubName: String = "",            // stored here so Marketplace cards need no extra lookup (R2-S1)
    val location: String = "",
    val venueLat: Double? = null,         // optional, used by the location check (R3-S12)
    val venueLng: Double? = null,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val requestedPoints: Int = 0,         // 1–10, set by the club (R1-S1)
    val approvedPoints: Int? = null,      // set by the admin on approval, ≤ requestedPoints (R1-S4)
    val capacity: Int = 0,
    val bookedCount: Int = 0,
    val status: EventStatus = EventStatus.DRAFT,
    val rejectionReason: String? = null,      // required when rejected (R1-S5)
    val cancellationReason: String? = null, // required when cancelled (R1-S9)
    val waitlist: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val submittedAt: Long? = null             // review due 5 working days after this (R1-S4)
) {
    @get:Exclude
    val seatsLeft: Int get() = (capacity - bookedCount).coerceAtLeast(0)

    @get:Exclude
    val isFull: Boolean get() = seatsLeft == 0
}