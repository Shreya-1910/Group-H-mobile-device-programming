package com.mdpgroup08.scpd.data.model

import com.google.firebase.firestore.DocumentId

enum class AppealStatus { PENDING, GRANTED, DENIED }

/** A student's appeal against an attendance or points decision (R5, R6). */
data class Appeal(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val targetType: String = "",  // "ATTENDANCE" or "POINTS"
    val targetId: String = "",
    val reason: String = "",
    val status: AppealStatus = AppealStatus.PENDING,
    val decidedBy: String? = null,
    val createdAt: Long = 0L
)
