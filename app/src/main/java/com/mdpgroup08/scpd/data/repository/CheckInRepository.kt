
package com.mdpgroup08.scpd.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.google.firebase.auth.FirebaseAuth
import com.mdpgroup08.scpd.data.local.CheckInDao
import com.mdpgroup08.scpd.data.local.CheckInMethod
import com.mdpgroup08.scpd.data.local.CheckInRecord
import com.mdpgroup08.scpd.data.local.CheckInStatus
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed interface SaveCheckInResult {

    data class Saved(
        val record: CheckInRecord
    ) : SaveCheckInResult

    data object AlreadyExists : SaveCheckInResult

    data object NotLoggedIn : SaveCheckInResult

    data class Failed(
        val message: String
    ) : SaveCheckInResult
}

class CheckInRepository(
    private val dao: CheckInDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        private val SYNC_DEADLINE_MS =
            TimeUnit.HOURS.toMillis(72)
    }

    /**
     * Saves a check-in locally.
     *
     * This function does not require an internet connection.
     * Firebase is not contacted here.
     */
    suspend fun saveCheckInOffline(
        eventId: String,
        codeCounter: Long,
        method: CheckInMethod,
        signature: String
    ): SaveCheckInResult {

        val uid = auth.currentUser?.uid
            ?: return SaveCheckInResult.NotLoggedIn

        if (eventId.isBlank()) {
            return SaveCheckInResult.Failed(
                "Event ID cannot be empty."
            )
        }

        if (codeCounter < 0) {
            return SaveCheckInResult.Failed(
                "Invalid QR code counter."
            )
        }

        if (signature.isBlank()) {
            return SaveCheckInResult.Failed(
                "Check-in signature cannot be empty."
            )
        }

        val now = System.currentTimeMillis()

        val record = CheckInRecord(
            id = UUID.randomUUID().toString(),

            // Firebase UID identifies the account.
            accountId = uid,
            studentId = uid,

            eventId = eventId,
            codeCounter = codeCounter,

            scannedAtLocal = now,

            method = method,
            status = CheckInStatus.PENDING,

            reason = null,
            signature = signature,

            attemptCount = 0,
            lastAttemptAt = null,

            // Existing booking policy uses a 72-hour
            // check-in deadline.
            syncDeadlineAt = now + SYNC_DEADLINE_MS,

            createdAt = now
        )

        return try {
            dao.insert(record)

            SaveCheckInResult.Saved(record)

        } catch (e: SQLiteConstraintException) {

            // Unique accountId + eventId index prevents
            // duplicate check-ins.
            SaveCheckInResult.AlreadyExists

        } catch (e: Exception) {

            SaveCheckInResult.Failed(
                e.message ?: "Unable to save check-in."
            )
        }
    }
}
