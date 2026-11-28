package com.mdpgroup08.scpd.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.mdpgroup08.scpd.data.attendance.CheckInCodes
import com.mdpgroup08.scpd.data.attendance.ReviewRules
import com.mdpgroup08.scpd.data.model.Attendance
import com.mdpgroup08.scpd.data.model.AttendanceMethod
import com.mdpgroup08.scpd.data.model.AttendanceStatus
import com.mdpgroup08.scpd.data.model.Event
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await

/** R4-S18 EX1: someone else already decided this check-in. [decidedBy] is their user id. */
class CheckInAlreadyDecidedException(val decidedBy: String?) :
    Exception("This check-in was already decided.")

/** R4-S18 EX2: approving or rejecting needs a connection; decisions are never saved only on the phone. */
class ReviewNeedsConnectionException :
    Exception("You need a connection to approve or reject. Try again when you're online.")

/** Firebase refused access (security rules), so this account can't run check-in for the event. */
class CheckInNotAllowedException :
    Exception("You don't have permission to run check-in for this event. Make sure you're signed in as its club.")

/** The event was never opened online, so its secret isn't on this phone (R4-S14 AC2). */
class CheckInNotLoadedException :
    Exception("Open this event while online before the event starts, then you can show the code offline.")

/** One Show code ... Hide code period from the session log (R4-S23 AC3). */
data class CodeDisplay(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val firstCounter: Long,
    val lastCounter: Long
)

/**
 * Secure attendance data (Member 2).
 *
 * Firestore:
 *   eventSecrets/{eventId}            clubId, secret, createdAt  (only the owning club, R3-S9 AC2)
 *   codeDisplays/{auto id}            the session log of Show / Hide code (R4-S23)
 *   checkIns/{eventId}_{studentId}    one check-in per student per event (R3-S2, R4-S17)
 */
interface AttendanceRepository {

    // ---- Club: codes and session log ----

    /** The event's secret. Created once if missing; read from the phone's cache when offline (R4-S14). */
    suspend fun getOrCreateSecret(eventId: String, clubId: String): String

    /** Logs the start of a code display and returns its ID. Works offline: the write is queued. */
    fun startDisplay(eventId: String, clubId: String, organiserId: String, counter: Long, now: Long): String

    fun updateDisplay(displayId: String, lastCounter: Long)

    fun endDisplay(displayId: String, lastCounter: Long, now: Long)

    /** The event's session log, oldest first. */
    fun observeDisplays(eventId: String, clubId: String): Flow<List<CodeDisplay>>

    // ---- Club: checking and the live list ----

    fun observeEventCheckIns(eventId: String, clubId: String): Flow<List<Attendance>>

    suspend fun hasConfirmedBooking(eventId: String, studentId: String): Boolean

    /** Students with a confirmed booking. Empty if they can't be loaded. */
    suspend fun bookedStudentIds(eventId: String): List<String>

    suspend fun studentName(studentId: String): String

    /** Saves the result of checking a Pending check-in. */
    suspend fun decide(
        checkInId: String, status: AttendanceStatus, reason: String?, nextStep: String?,
        counter: Long?, decidedBy: String, now: Long
    )

    /** R3-S6: the club marks a booked student present by hand, with a reason, and audits it. */
    suspend fun markPresent(event: Event, studentId: String, studentName: String, reason: String, organiserId: String, now: Long)

    /**
     * R4-S18: the club approves or rejects a Needs review check-in. Runs as a transaction so the
     * first decision wins (EX1), needs a connection (EX2), and is written to the audit log (AC2).
     */
    suspend fun review(checkInId: String, approve: Boolean, reason: String, organiserId: String, now: Long)

    // ---- Student ----

    /** Sends a check-in. Not awaited: offline, Firestore keeps it and uploads it later (R4-S1). */
    fun submitCheckIn(checkIn: Attendance, onRejected: (Exception) -> Unit = {})

    fun observeMyCheckIn(eventId: String, studentId: String): Flow<Attendance?>
}

class FirebaseAttendanceRepository(
    private val db: FirebaseFirestore
) : AttendanceRepository {

    private val secrets = db.collection("eventSecrets")
    private val displays = db.collection("codeDisplays")
    private val checkIns = db.collection("checkIns")

    override suspend fun getOrCreateSecret(eventId: String, clubId: String): String {
        val doc = secrets.document(eventId)
        return try {
            // A transaction, so two organiser phones can never create two different secrets.
            db.runTransaction { t ->
                t.get(doc).getString("secret") ?: CheckInCodes.newSecret().also { secret ->
                    t.set(doc, mapOf(
                        "clubId" to clubId,
                        "secret" to secret,
                        "createdAt" to System.currentTimeMillis()
                    ))
                }
            }.await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) throw CheckInNotAllowedException()
            if (e.code != FirebaseFirestoreException.Code.UNAVAILABLE) throw e
            // Offline: transactions need a connection, so use the copy cached when the event was opened.
            try {
                doc.get(Source.CACHE).await().getString("secret")
            } catch (cacheMiss: Exception) {
                null
            } ?: throw CheckInNotLoadedException()
        }
    }

    // Display writes are not awaited: Firestore queues them while offline and uploads
    // the session log when the phone reconnects (R4-S14 EX2).
    override fun startDisplay(eventId: String, clubId: String, organiserId: String, counter: Long, now: Long): String {
        val doc = displays.document()
        doc.set(mapOf(
            "eventId" to eventId,
            "clubId" to clubId,
            "organiserId" to organiserId,
            "startedAt" to now,
            "endedAt" to null,
            "firstCounter" to counter,
            "lastCounter" to counter
        ))
        return doc.id
    }

    override fun updateDisplay(displayId: String, lastCounter: Long) {
        displays.document(displayId).update("lastCounter", lastCounter)
    }

    override fun endDisplay(displayId: String, lastCounter: Long, now: Long) {
        displays.document(displayId).update(mapOf("lastCounter" to lastCounter, "endedAt" to now))
    }

    override fun observeDisplays(eventId: String, clubId: String): Flow<List<CodeDisplay>> = callbackFlow {
        // clubId is in the query so the security rules can allow it.
        val reg = displays.whereEqualTo("eventId", eventId).whereEqualTo("clubId", clubId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents.orEmpty().mapNotNull { d ->
                    CodeDisplay(
                        id = d.id,
                        startedAt = d.getLong("startedAt") ?: return@mapNotNull null,
                        endedAt = d.getLong("endedAt"),
                        firstCounter = d.getLong("firstCounter") ?: return@mapNotNull null,
                        lastCounter = d.getLong("lastCounter") ?: return@mapNotNull null
                    )
                }.sortedBy { it.startedAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }.keepListening()

    override fun observeEventCheckIns(eventId: String, clubId: String): Flow<List<Attendance>> = callbackFlow {
        val reg = checkIns.whereEqualTo("eventId", eventId).whereEqualTo("clubId", clubId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(Attendance::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }.keepListening()

    override suspend fun hasConfirmedBooking(eventId: String, studentId: String): Boolean = try {
        db.collection("bookings").document("${eventId}_$studentId").get().await()
            .getString("status") == "CONFIRMED"
    } catch (e: Exception) {
        false
    }

    override suspend fun bookedStudentIds(eventId: String): List<String> = try {
        db.collection("bookings")
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("status", "CONFIRMED")
            .get().await()
            .documents.mapNotNull { it.getString("studentId") }
    } catch (e: Exception) {
        emptyList()
    }

    override suspend fun studentName(studentId: String): String = try {
        db.collection("users").document(studentId).get().await()
            .getString("name").orEmpty().ifBlank { "Student" }
    } catch (e: Exception) {
        "Student"
    }

    override suspend fun decide(
        checkInId: String, status: AttendanceStatus, reason: String?, nextStep: String?,
        counter: Long?, decidedBy: String, now: Long
    ) {
        checkIns.document(checkInId).update(mapOf(
            "status" to status.name,
            "reason" to reason,
            "nextStep" to nextStep,
            "counter" to counter,
            "decidedBy" to decidedBy,
            "decidedAt" to now
        )).await()
    }

    override suspend fun markPresent(
        event: Event, studentId: String, studentName: String, reason: String, organiserId: String, now: Long
    ) {
        val batch = db.batch()
        batch.set(checkIns.document(Attendance.idFor(event.id, studentId)), mapOf(
            "eventId" to event.id,
            "clubId" to event.clubId,
            "studentId" to studentId,
            "studentName" to studentName,
            "deviceId" to "",
            "method" to AttendanceMethod.MANUAL.name,
            "online" to true,
            "submittedAt" to now,
            "status" to AttendanceStatus.VERIFIED.name,
            "reason" to "Marked present by the organiser",
            "manualReason" to reason,
            "decidedBy" to organiserId,
            "decidedAt" to now
        ))
        // R3-S6 AC1: every manual change goes to the audit log.
        batch.set(db.collection("audit_logs").document(), mapOf(
            "actorId" to organiserId,
            "action" to "CHECK_IN_MARKED_PRESENT",
            "targetId" to Attendance.idFor(event.id, studentId),
            "reason" to reason,
            "timestamp" to now
        ))
        batch.commit().await()
    }

    override suspend fun review(checkInId: String, approve: Boolean, reason: String, organiserId: String, now: Long) {
        val doc = checkIns.document(checkInId)
        try {
            db.runTransaction { t ->
                val snap = t.get(doc)
                if (snap.getString("status") != AttendanceStatus.NEEDS_REVIEW.name) {
                    throw CheckInAlreadyDecidedException(snap.getString("decidedBy"))
                }
                t.update(doc, mapOf(
                    "status" to (if (approve) AttendanceStatus.VERIFIED else AttendanceStatus.REJECTED).name,
                    "reason" to (if (approve) "Approved by the organiser" else reason),
                    "nextStep" to (if (approve) null else ReviewRules.APPEAL_STEP),
                    "decidedBy" to organiserId,
                    "decidedAt" to now
                ))
                // AC2: every decision goes to the audit log, with who, when and why.
                t.set(db.collection("audit_logs").document(), mapOf(
                    "actorId" to organiserId,
                    "action" to if (approve) "CHECK_IN_APPROVED" else "CHECK_IN_REJECTED",
                    "targetId" to checkInId,
                    "reason" to reason,
                    "timestamp" to now
                ))
                null
            }.await()
        } catch (e: Exception) {
            // Firestore may wrap our exception, so unwrap it for the ViewModel
            val cause = (e as? CheckInAlreadyDecidedException) ?: (e.cause as? CheckInAlreadyDecidedException)
            if (cause != null) throw cause
            val code = (e as? FirebaseFirestoreException)?.code ?: (e.cause as? FirebaseFirestoreException)?.code
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) throw ReviewNeedsConnectionException()
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) throw CheckInNotAllowedException()
            throw e
        }
    }

    override fun submitCheckIn(checkIn: Attendance, onRejected: (Exception) -> Unit) {
        checkIns.document(Attendance.idFor(checkIn.eventId, checkIn.studentId)).set(mapOf(
            "eventId" to checkIn.eventId,
            "clubId" to checkIn.clubId,
            "studentId" to checkIn.studentId,
            "studentName" to checkIn.studentName,
            "deviceId" to checkIn.deviceId,
            "method" to checkIn.method.name,
            "sixDigits" to checkIn.sixDigits,
            "qrCounter" to checkIn.qrCounter,
            "qrSignature" to checkIn.qrSignature,
            "online" to checkIn.online,
            "submittedAt" to checkIn.submittedAt,
            "serverSubmittedAt" to FieldValue.serverTimestamp(),
            "status" to AttendanceStatus.PENDING.name,
            "reason" to null,
            "nextStep" to null,
            "counter" to null,
            "decidedAt" to null,
            "decidedBy" to null
        )).addOnFailureListener { e ->
            // Offline the write waits in the queue; this only fires if the server refuses it.
            android.util.Log.w("SCPD-CheckIn", "Check-in refused", e)
            onRejected(e)
        }
    }

    override fun observeMyCheckIn(eventId: String, studentId: String): Flow<Attendance?> = callbackFlow {
        val reg = checkIns.document(Attendance.idFor(eventId, studentId)).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(if (snap?.exists() == true) snap.toObject(Attendance::class.java) else null)
        }
        awaitClose { reg.remove() }
    }.keepListening()

    /**
     * If a listener fails (no permission for a moment, network drop, rules being re-published),
     * start it again after 3 s instead of stopping for good. Without this, the club phone could
     * stop checking codes and the student would wait forever.
     */
    private fun <T> Flow<T>.keepListening(): Flow<T> = retryWhen { cause, _ ->
        android.util.Log.w("SCPD-CheckIn", "Listener stopped, retrying in 3 s", cause)
        delay(3_000)
        true
    }
}
