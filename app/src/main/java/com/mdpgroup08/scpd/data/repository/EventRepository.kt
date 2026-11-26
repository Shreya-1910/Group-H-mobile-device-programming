package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Event
import kotlinx.coroutines.flow.Flow

/** Why an admin decision couldn't be saved. */
sealed class EventDecisionException(message: String) : Exception(message) {
    /** The event is no longer Pending, e.g. another admin already decided (R6-S21 EX3). */
    object AlreadyDecided : EventDecisionException("Event already decided")
    object NotFound : EventDecisionException("Event not found")
}

interface EventRepository {
    fun getApprovedUpcomingEvents(): Flow<List<Event>>
    fun getEvent(id: String): Flow<Event?>
    fun getClubEvents(clubId: String): Flow<List<Event>>

    /** Pending events for the admin approval queue (R1-S4). */
    fun getPendingEvents(): Flow<List<Event>>

    /** A new, unused event ID, reserved before the first save so a retry can't create a duplicate. */
    fun newId(): String

    /** Saves as DRAFT and returns document ID */
    suspend fun saveDraft(event: Event): String

    /** Sets status to PENDING, submittedAt = now, and returns document ID */
    suspend fun submit(event: Event): String

    /**
     * Admin approves a Pending event with the confirmed points (R1-S4).
     * Updates the event and writes the audit entry together. Throws EventDecisionException.
     */
    suspend fun approve(eventId: String, approvedPoints: Int, note: String, adminId: String)

    /** Admin rejects a Pending event with a reason (R1-S5). Same rules as approve. */
    suspend fun reject(eventId: String, reason: String, adminId: String)

    /** R1-S8: a Pending event that has reached its start time becomes EXPIRED. Does nothing otherwise. */
    suspend fun expire(eventId: String)
}