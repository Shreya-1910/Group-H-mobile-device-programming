package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getApprovedUpcomingEvents(): Flow<List<Event>>
    fun getEvent(id: String): Flow<Event?>
    fun getClubEvents(clubId: String): Flow<List<Event>>
    /** A new, unused event ID, reserved before the first save so a retry can't create a duplicate. */
    fun newId(): String

    /** Saves as DRAFT and returns document ID */
    suspend fun saveDraft(event: Event): String

    /** Sets status to PENDING, submittedAt = now, and returns document ID */
    suspend fun submit(event: Event): String
}
