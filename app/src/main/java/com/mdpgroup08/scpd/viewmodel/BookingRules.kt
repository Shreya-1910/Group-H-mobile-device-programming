package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Event

/** Shared booking rules (R2-S4, R2-S5, R2-S8), kept in one place so every screen agrees. */
object BookingRules {
    /** Booking, waitlist and cancellation all close 2 hours before the start (Requirements assumption). */
    const val CUTOFF_MS = 2 * 60 * 60 * 1000L

    fun isBookingClosed(event: Event, now: Long): Boolean = now >= event.startTime - CUTOFF_MS
}