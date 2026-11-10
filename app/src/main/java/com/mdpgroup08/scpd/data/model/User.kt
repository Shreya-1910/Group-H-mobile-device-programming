package com.mdpgroup08.scpd.data.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "STUDENT",         // 依 Shared rules 調整
    val clubId: String? = null
)