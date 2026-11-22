package com.mdpgroup08.scpd.data.model

import com.google.firebase.firestore.DocumentId

/** One admin or check-in action, written once and never edited (R6). */
data class AuditEntry(
    @DocumentId val id: String = "",
    val actorId: String = "",     // uid of who did it
    val action: String = "",      // e.g. "EVENT_APPROVED", "EVENT_REJECTED"
    val targetId: String = "",    // the event, user or appeal it was done to
    val reason: String = "",
    val timestamp: Long = 0L
)