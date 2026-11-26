package com.mdpgroup08.scpd.reminders

import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.viewmodel.BookingRules

/** What the reminder shows instead of a greyed-out button, since notification buttons can't be disabled. */
enum class ReminderButton {
    /** Online and before the cut-off: "Can't make it" is shown (R4-S21 AC1). */
    CAN_CANCEL,
    /** Offline: no button, and "You are offline. Connect to the internet to cancel" (AC2). */
    OFFLINE,
    /** Past the cut-off: no button, and "Cancellation closed 2 hours before the event" (AC3). */
    TOO_LATE
}

/** One reminder that should exist. [remindAt] is 3 hours before the start. */
data class PlannedReminder(val eventId: String, val remindAt: Long)

/** Rules for the 3-hour reminder (R4-S21). Pure Kotlin, so every case is unit tested. */
object ReminderRules {
    const val REMIND_BEFORE_MS = 3 * 60 * 60 * 1000L

    /** A booking made inside the 3-hour window gets its reminder straight away, if it was made just now. */
    private const val JUST_BOOKED_MS = 10 * 60 * 1000L

    fun remindAt(event: Event): Long = event.startTime - REMIND_BEFORE_MS

    /** Which reminders should be scheduled for these bookings. */
    fun plan(bookings: List<Booking>, upcoming: List<Event>, now: Long): List<PlannedReminder> {
        val eventsById = upcoming.associateBy { it.id }
        return bookings
            .filter { it.status == BookingStatus.CONFIRMED }
            .mapNotNull { booking ->
                val event = eventsById[booking.eventId] ?: return@mapNotNull null
                // R5-S17 EX2: never remind about an event that has started
                if (event.status != EventStatus.APPROVED || event.startTime <= now) return@mapNotNull null
                val at = remindAt(event)
                val justBookedInsideWindow = booking.createdAt >= at && now - booking.createdAt < JUST_BOOKED_MS
                if (now < at || justBookedInsideWindow) PlannedReminder(event.id, at) else null
            }
    }

    /** Cut-off beats offline: reconnecting wouldn't help once cancellation has closed. */
    fun buttonFor(event: Event, online: Boolean, now: Long): ReminderButton = when {
        BookingRules.isBookingClosed(event, now) -> ReminderButton.TOO_LATE
        !online -> ReminderButton.OFFLINE
        else -> ReminderButton.CAN_CANCEL
    }
}