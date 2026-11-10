package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getApprovedUpcomingEvents(): Flow<List<Event>>
    fun getEvent(id: String): Flow<Event?>
    fun getClubEvents(clubId: String): Flow<List<Event>>

    /** 儲存為 DRAFT，回傳文件 id */
    suspend fun saveDraft(event: Event): String

    /** 狀態設為 PENDING，submittedAt = 現在，回傳文件 id */
    suspend fun submit(event: Event): String
}