package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One event on the club's My events list, with the note explaining its status. */
/** One event on the club's My events list, with the note explaining its status. */
data class ClubEventItem(
    val event: Event,
    val note: String?,
    val noteIsWarning: Boolean = false,
    /** R1-S9: approved and not started yet. */
    val canCancel: Boolean = false,
    /** R2-S13: e.g. "Booked 18 of 30 · 4 on the waitlist", for approved events only. */
    val bookingSummary: String? = null,
    /** R1-S11: rejected, cancelled or expired events can be copied into a new draft. */
    val canCopy: Boolean = false
)

/** Review deadline for Pending events: 5 working days after submission (R1-S4). Weekends are skipped; public holidays are not (assumption). */
object ReviewDeadline {
    const val WORKING_DAYS = 5

    fun dueDate(submittedAt: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        var date = Instant.ofEpochMilli(submittedAt).atZone(zone).toLocalDate()
        var added = 0
        while (added < WORKING_DAYS) {
            date = date.plusDays(1)
            if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) added++
        }
        return date
    }
}

/** Turns an event's status into the message the club sees (R1-S3, R1-S6, R1-S8). */
object ClubEventStatus {
    private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.ENGLISH)
    private const val LOCKED = "Submitted events can't be edited. To make a change, cancel it and create a new event."

    fun toItem(event: Event, zone: ZoneId = ZoneId.systemDefault()): ClubEventItem = when (event.status) {
        EventStatus.DRAFT -> ClubEventItem(event, null)

        EventStatus.PENDING -> {
            val due = event.submittedAt?.let { ReviewDeadline.dueDate(it, zone).format(dateFormat) }
            val waiting = if (due != null) "Waiting for s-CPD review, decision due by $due." else "Waiting for s-CPD review."
            ClubEventItem(event, "$waiting $LOCKED")
        }

        EventStatus.APPROVED -> ClubEventItem(
            event,
            "Approved for ${event.approvedPoints ?: event.requestedPoints} points. $LOCKED"
        )

        EventStatus.REJECTED -> ClubEventItem(
            event,
            "Rejected: ${event.rejectionReason ?: "no reason given"}. Tap Copy to new draft to fix it and submit again.",
            noteIsWarning = true
        )

        EventStatus.CANCELLED -> ClubEventItem(
            event,
            "Cancelled: ${event.cancellationReason ?: "no reason given"}.",
            noteIsWarning = true
        )

        EventStatus.EXPIRED -> ClubEventItem(
            event,
            "Not reviewed before the start time, so it was not published. Don't advertise this event.",
            noteIsWarning = true
        )
    }
}