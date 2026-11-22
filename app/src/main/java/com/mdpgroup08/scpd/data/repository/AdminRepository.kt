package com.mdpgroup08.scpd.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mdpgroup08.scpd.data.model.AccountStatus
import com.mdpgroup08.scpd.data.model.AuditEntry
import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.User
import com.mdpgroup08.scpd.data.model.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class AuditLogWriteException(message: String) : Exception(message)
class UnauthorizedAdminException(message: String = "Non-admin user access denied") : Exception(message)

interface AdminRepository {
    /** Executes an admin action with 2-step verification and writes an immutable audit entry (R6-S6). */
    suspend fun performAdminAction(
        adminUid: String,
        actionName: String,
        targetId: String,
        reason: String,
        verified2Step: Boolean,
        actionTask: suspend () -> Unit
    )

    /** Runs daily graduation check for student accounts (R6-S5). */
    suspend fun runDailyGraduationCheck(): List<User>

    /** Confirms deactivation of a student account (R6-S5 EX1 & EX2). */
    suspend fun deactivateStudentAccount(adminUid: String, studentId: String, reason: String, verified2Step: Boolean)

    /** Extends study end date for resit/extension accounts (R6-S4 EX2). */
    suspend fun updateStudentEndYear(adminUid: String, studentId: String, newEndYear: Int, reason: String, verified2Step: Boolean)

    /** Releases registered phone key for a student (R6-S3/R6-S6). */
    suspend fun releaseStudentPhone(adminUid: String, studentId: String, reason: String, verified2Step: Boolean)

    /** Gets list of audit logs (R6-S6). */
    fun getAuditLogs(): Flow<List<AuditEntry>>
}

class FirebaseAdminRepository(
    private val firestore: FirebaseFirestore
) : AdminRepository {

    override suspend fun performAdminAction(
        adminUid: String,
        actionName: String,
        targetId: String,
        reason: String,
        verified2Step: Boolean,
        actionTask: suspend () -> Unit
    ) {
        // 1. Target & reason required (R6-S6 AC1)
        if (targetId.isBlank()) throw IllegalArgumentException("Target ID is required")
        if (reason.isBlank()) throw IllegalArgumentException("Reason is required for admin actions")
        if (!verified2Step) throw IllegalArgumentException("2-Step admin verification required")

        // 2. Check if adminUid is an ADMIN (R6-S6 EX1)
        val adminDoc = firestore.collection("users").document(adminUid).get().await()
        val roleStr = adminDoc.getString("role")
        if (UserRole.fromString(roleStr) != UserRole.ADMIN) {
            // EX1: Log unauthorized attempt & deny access
            writeAuditEntry(
                AuditEntry(
                    actorId = adminUid,
                    action = "UNAUTHORIZED_ADMIN_ACCESS_ATTEMPT",
                    targetId = targetId,
                    reason = "Denied: non-admin attempted action $actionName",
                    verified2Fa = false
                )
            )
            throw UnauthorizedAdminException("Access denied: Admin role required")
        }

        // 3. Create audit entry
        val auditEntry = AuditEntry(
            actorId = adminUid,
            action = actionName,
            targetId = targetId,
            reason = reason,
            verified2Fa = verified2Step
        )

        // 4. Try writing audit entry first (R6-S6 EX2). If audit log can't be written, action is not carried out.
        try {
            writeAuditEntry(auditEntry)
        } catch (e: Exception) {
            throw AuditLogWriteException("Audit log could not be written. Action cancelled.")
        }

        // 5. Audit log succeeded -> execute admin action
        actionTask()
    }

    private suspend fun writeAuditEntry(entry: AuditEntry) {
        val docRef = firestore.collection("audit_logs").document()
        docRef.set(entry.copy(id = docRef.id)).await()
    }

    override suspend fun runDailyGraduationCheck(): List<User> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val snapshot = firestore.collection("users")
            .whereEqualTo("role", UserRole.STUDENT.name)
            .get().await()

        val updatedUsers = mutableListOf<User>()

        for (doc in snapshot.documents) {
            val user = doc.toObject(User::class.java) ?: continue
            val endYear = user.expectedEndYear ?: continue

            // R6-S5 AC1: Daily check flags accounts near/past end year
            if (currentYear >= endYear && user.accountStatus == AccountStatus.ACTIVE.name) {
                val newStatus = if (user.emailBounceCount >= 3) {
                    AccountStatus.FLAGGED.name // EX1: Email bounces 3 times -> admin confirmation before deactivation
                } else {
                    AccountStatus.WAITING_FOR_APPROVAL.name // EX2: Waiting for admin to set new end date
                }

                firestore.collection("users").document(user.id)
                    .update("accountStatus", newStatus).await()

                updatedUsers.add(user.copy(accountStatus = newStatus))
            }
        }
        return updatedUsers
    }

    override suspend fun deactivateStudentAccount(
        adminUid: String,
        studentId: String,
        reason: String,
        verified2Step: Boolean
    ) {
        performAdminAction(adminUid, "DISABLE_ACCOUNT", studentId, reason, verified2Step) {
            // 1. Deactivate student account
            firestore.collection("users").document(studentId).update(
                mapOf(
                    "accountStatus" to AccountStatus.DEACTIVATED.name,
                    "deactivatedAt" to System.currentTimeMillis()
                )
            ).await()

            // 2. R6-S5 EX2: Future bookings cancelled and places released
            val now = System.currentTimeMillis()
            val bookingsSnapshot = firestore.collection("bookings")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", BookingStatus.CONFIRMED.name)
                .get().await()

            for (bDoc in bookingsSnapshot.documents) {
                val booking = bDoc.toObject(Booking::class.java) ?: continue

                // Cancel booking
                firestore.collection("bookings").document(booking.id).update(
                    mapOf(
                        "status" to BookingStatus.CANCELLED.name,
                        "cancelledAt" to now
                    )
                ).await()

                // Release place on event (decrement bookedCount)
                val eventDoc = firestore.collection("events").document(booking.eventId).get().await()
                val event = eventDoc.toObject(Event::class.java)
                if (event != null && event.bookedCount > 0) {
                    firestore.collection("events").document(booking.eventId)
                        .update("bookedCount", event.bookedCount - 1).await()
                }
            }
        }
    }

    override suspend fun updateStudentEndYear(
        adminUid: String,
        studentId: String,
        newEndYear: Int,
        reason: String,
        verified2Step: Boolean
    ) {
        performAdminAction(adminUid, "UPDATE_END_DATE", studentId, reason, verified2Step) {
            firestore.collection("users").document(studentId).update(
                mapOf(
                    "expectedEndYear" to newEndYear,
                    "accountStatus" to AccountStatus.ACTIVE.name
                )
            ).await()
        }
    }

    override suspend fun releaseStudentPhone(
        adminUid: String,
        studentId: String,
        reason: String,
        verified2Step: Boolean
    ) {
        performAdminAction(adminUid, "RELEASE_PHONE", studentId, reason, verified2Step) {
            firestore.collection("users").document(studentId).update(
                mapOf(
                    "publicKey" to null,
                    "deviceId" to null
                )
            ).await()
        }
    }

    override fun getAuditLogs(): Flow<List<AuditEntry>> = callbackFlow {
        val listener = firestore.collection("audit_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull { it.toObject(AuditEntry::class.java) } ?: emptyList()
                trySend(entries)
            }
        awaitClose { listener.remove() }
    }
}
