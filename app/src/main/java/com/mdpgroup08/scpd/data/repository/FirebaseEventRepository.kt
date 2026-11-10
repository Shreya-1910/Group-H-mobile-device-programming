package com.mdpgroup08.scpd.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseEventRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EventRepository {

    private val events = db.collection("events")

    private fun Query.snapshotFlow(): Flow<List<Event>> = callbackFlow {
        val reg = addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObjects(Event::class.java).orEmpty())
        }
        awaitClose { reg.remove() }
    }

    override fun getApprovedUpcomingEvents(): Flow<List<Event>> =
        events.whereEqualTo("status", EventStatus.APPROVED.name)
            .snapshotFlow()
            // 在客戶端過濾日期並排序，避免需要建立 Firestore 複合索引
            .map { list ->
                val now = System.currentTimeMillis()
                list.filter { it.date >= now }.sortedBy { it.date }
            }

    override fun getEvent(id: String): Flow<Event?> = callbackFlow {
        val reg = events.document(id).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(Event::class.java))
        }
        awaitClose { reg.remove() }
    }

    override fun getClubEvents(clubId: String): Flow<List<Event>> =
        events.whereEqualTo("clubId", clubId)
            .snapshotFlow()
            .map { list -> list.sortedByDescending { it.date } }

    override suspend fun saveDraft(event: Event): String =
        write(event.copy(status = EventStatus.DRAFT, submittedAt = null))

    override suspend fun submit(event: Event): String =
        write(event.copy(status = EventStatus.PENDING, submittedAt = System.currentTimeMillis()))

    private suspend fun write(event: Event): String {
        val doc = if (event.id.isBlank()) events.document() else events.document(event.id)
        doc.set(event).await()
        return doc.id
    }
}