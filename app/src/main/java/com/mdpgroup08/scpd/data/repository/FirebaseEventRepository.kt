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
import com.mdpgroup08.scpd.data.model.AuditEntry

class FirebaseEventRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EventRepository {

    private val events = db.collection("events")
    private val auditLog = db.collection("auditLog")

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

    override fun getPendingEvents(): Flow<List<Event>> =
        events.whereEqualTo("status", EventStatus.PENDING.name)
            .snapshotFlow()
            .map { list -> list.sortedBy { it.submittedAt ?: Long.MAX_VALUE } }

    override suspend fun approve(eventId: String, approvedPoints: Int, note: String, adminId: String) =
        decide(
            eventId, adminId, "EVENT_APPROVED", note,
            mapOf("status" to EventStatus.APPROVED.name, "approvedPoints" to approvedPoints)
        )

    override suspend fun reject(eventId: String, reason: String, adminId: String) =
        decide(
            eventId, adminId, "EVENT_REJECTED", reason,
            mapOf("status" to EventStatus.REJECTED.name, "rejectionReason" to reason)
        )

    /**
     * One transaction: check the event is still Pending, update it, and write the audit entry.
     * The audit entry has a fixed ID so the security rules can require it (R6-S20).
     */
    private suspend fun decide(
        eventId: String, adminId: String, action: String, reason: String, changes: Map<String, Any>
    ) {
        val eventRef = events.document(eventId)
        val auditRef = auditLog.document("event_${eventId}_decision")
        try {
            db.runTransaction { tx ->
                val snap = tx.get(eventRef)
                if (!snap.exists()) throw EventDecisionException.NotFound
                if (snap.getString("status") != EventStatus.PENDING.name) throw EventDecisionException.AlreadyDecided
                tx.update(eventRef, changes)
                tx.set(
                    auditRef,
                    AuditEntry(
                        actorId = adminId, action = action, targetId = eventId,
                        reason = reason, timestamp = System.currentTimeMillis()
                    )
                )
            }.await()
        } catch (e: Exception) {
            // Firestore may wrap our exception, so unwrap it for the ViewModel
            throw (e as? EventDecisionException) ?: (e.cause as? EventDecisionException) ?: e
        }
    }

    private suspend fun write(event: Event): String {
        val doc = if (event.id.isBlank()) events.document() else events.document(event.id)
        doc.set(event).await()
        return doc.id
    }
}
