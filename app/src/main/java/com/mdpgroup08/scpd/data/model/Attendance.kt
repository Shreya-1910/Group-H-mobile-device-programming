package com.mdpgroup08.scpd.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/** Result of a check-in (R3-S2). Pending while it waits to be checked. */
enum class AttendanceStatus { PENDING, VERIFIED, REJECTED, NEEDS_REVIEW }

/** How the student proved they were there: scanned QR, typed 6-digit code, or marked by the club (R3-S6). */
enum class AttendanceMethod { QR, CODE, MANUAL }

/**
 * One student's check-in for one event: checkIns/{eventId}_{studentId}.
 * The fixed ID means a student can only ever have one check-in per event (R4-S17).
 * The student writes it as PENDING; only the event's club can change the result.
 */
data class Attendance(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val clubId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val deviceId: String = "",
    val method: AttendanceMethod = AttendanceMethod.CODE,
    val sixDigits: String? = null,
    val qrCounter: Long? = null,
    val qrSignature: String? = null,
    /** False if the phone had no connection when the student checked in (R4-S1). */
    val online: Boolean = true,
    /** Phone time, for display only. Never trusted for the rules (R4-S16). */
    val submittedAt: Long = 0L,
    /** Set by Firebase when the check-in reaches the server: the trusted time for "expired" (R3-S7). */
    val serverSubmittedAt: Timestamp? = null,
    val status: AttendanceStatus = AttendanceStatus.PENDING,
    /** Why it was rejected or held for review, shown to the student (R3-S4). */
    val reason: String? = null,
    val nextStep: String? = null,
    /** The 15 s code window the proof belongs to, set when checked. */
    val counter: Long? = null,
    val decidedAt: Long? = null,
    val decidedBy: String? = null,
    /** Required when the club marks a student present by hand (R3-S6). */
    val manualReason: String? = null
) {
    companion object {
        fun idFor(eventId: String, studentId: String) = "${eventId}_$studentId"
    }
}
