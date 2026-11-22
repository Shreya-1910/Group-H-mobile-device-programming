package com.mdpgroup08.scpd.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Immutable audit entry written whenever an admin action or security event occurs (R6-S6).
 */
data class AuditEntry(
    @DocumentId val id: String = "",
    val actorId: String = "",        // UID of admin or system action executor
    val action: String = "",         // e.g. "EVENT_APPROVED", "APPEAL_DECIDED", "ACCOUNT_DISABLED", "PHONE_RELEASED"
    val targetId: String = "",       // Target event, student account, or appeal ID
    val reason: String = "",         // Required justification (R6-S6 AC1)
    val verified2Fa: Boolean = true, // Admin 2-step verification confirmed
    val timestamp: Long = System.currentTimeMillis()
)
