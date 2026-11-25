package com.mdpgroup08.scpd.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_in_records",
    indices = [
        Index(
            value = ["accountId", "eventId"],
            unique = true
        )
    ]
)
data class CheckInRecord(

    @PrimaryKey
    val id: String,

    val accountId: String,

    val studentId: String,

    val eventId: String,

    val codeCounter: Long,

    val scannedAtLocal: Long,

    val method: CheckInMethod,

    val status: CheckInStatus,

    val reason: String? = null,

    val signature: String,

    val attemptCount: Int = 0,

    val lastAttemptAt: Long? = null,

    val syncDeadlineAt: Long,

    val createdAt: Long
)