package com.mdpgroup08.scpd.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

enum class EventStatus { DRAFT, PENDING, APPROVED, REJECTED, CANCELLED }

// Firestore 的 toObject() 需要所有欄位都有預設值
data class Event(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val clubId: String = "",
    val location: String = "",
    val date: Long = 0L,              // 活動開始時間 (epoch millis)
    val points: Int = 0,
    val capacity: Int = 0,
    val bookedCount: Int = 0,
    val status: EventStatus = EventStatus.DRAFT,
    val submittedAt: Long? = null
) {
    // 計算屬性：必須加 @get:Exclude，否則會被寫進 Firestore
    @get:Exclude
    val seatsLeft: Int get() = capacity - bookedCount
}