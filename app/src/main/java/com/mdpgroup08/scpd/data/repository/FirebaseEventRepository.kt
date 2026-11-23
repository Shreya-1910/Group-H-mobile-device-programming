package com.mdpgroup08.scpd.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mdpgroup08.scpd.data.model.AuditEntry
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
            // Client-side date filtering and sorting to avoid requiring Firestore composite index
            .map { list ->
                val now = System.currentTimeMillis()
                list.filter { it.startTime >= now }.sortedBy { it.startTime }
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
            .map { list -> list.sortedByDescending { it.startTime } }

    override fun getPendingEvents(): Flow<List<Event>> =
        events.whereEqualTo("status", EventStatus.PENDING.name)
            .snapshotFlow()
            .map { list -> list.sortedBy { it.submittedAt ?: it.createdAt } }

    override suspend fun saveDraft(event: Event): String =
        write(event.copy(status = EventStatus.DRAFT, submittedAt = null))

    override suspend fun submit(event: Event): String =
        write(event.copy(status = EventStatus.PENDING, submittedAt = System.currentTimeMillis()))

    override suspend fun approveEvent(
        eventId: String,
        approvedPoints: Int,
        reason: String,
        adminUid: String
    ) {
        val now = System.currentTimeMillis()
        // 1. Update event status in Firestore
        events.document(eventId).update(
            mapOf(
                "status" to EventStatus.APPROVED.name,
                "approvedPoints" to approvedPoints
            )
        ).await()

        // 2. Write audit log document: auditLog/event_{id}_decision (R1-S4 / R6-S6)
        val decisionMap = mapOf(
            "actorId" to adminUid,
            "action" to "EVENT_APPROVED",
            "targetId" to eventId,
            "approvedPoints" to approvedPoints,
            "reason" to reason,
            "timestamp" to now
        )
        db.collection("auditLog").document("event_${eventId}_decision").set(decisionMap).await()

        // Also record in audit_logs collection if available
        try {
            db.collection("audit_logs").add(
                AuditEntry(
                    actorId = adminUid,
                    action = "EVENT_APPROVED",
                    targetId = eventId,
                    reason = "Approved $approvedPoints points: $reason",
                    timestamp = now
                )
            ).await()
        } catch (e: Exception) {
            // Secondary log fallback
        }
    }

    override suspend fun rejectEvent(
        eventId: String,
        reason: String,
        adminUid: String
    ) {
        val now = System.currentTimeMillis()
        // 1. Update event status in Firestore
        events.document(eventId).update(
            mapOf(
                "status" to EventStatus.REJECTED.name,
                "rejectionReason" to reason
            )
        ).await()

        // 2. Write audit log document: auditLog/event_{id}_decision (R1-S5 / R6-S6)
        val decisionMap = mapOf(
            "actorId" to adminUid,
            "action" to "EVENT_REJECTED",
            "targetId" to eventId,
            "rejectionReason" to reason,
            "reason" to reason,
            "timestamp" to now
        )
        db.collection("auditLog").document("event_${eventId}_decision").set(decisionMap).await()

        // Also record in audit_logs collection if available
        try {
            db.collection("audit_logs").add(
                AuditEntry(
                    actorId = adminUid,
                    action = "EVENT_REJECTED",
                    targetId = eventId,
                    reason = reason,
                    timestamp = now
                )
            ).await()
        } catch (e: Exception) {
            // Secondary log fallback
        }
    }

    override fun newId(): String = events.document().id

    private suspend fun write(event: Event): String {
        val doc = if (event.id.isBlank()) events.document() else events.document(event.id)
        doc.set(event).await()
        return doc.id
    }
}
