package com.mdpgroup08.scpd.data.model

import com.google.firebase.firestore.DocumentId

/**
 * User data model storing role, study details, device key, and account status (R6).
 */
data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "STUDENT",                 // R6-S2: "STUDENT", "CLUB", "ADMIN"
    val clubId: String? = null,
    val pdpaAccepted: Boolean = false,            // R6-S1: Privacy notice acceptance
    val accountStatus: String = AccountStatus.ACTIVE.name, // R6-S1, R6-S4, R6-S5
    val registeredAt: Long = System.currentTimeMillis(),

    // R6-S3: One phone per account (Keystore public key & device ID)
    val publicKey: String? = null,
    val deviceId: String? = null,

    // R6-S4: Study details
    val currentYear: Int? = null,
    val degreeName: String? = null,
    val degreeLength: Int? = null,
    val startYear: Int? = null,
    val expectedEndYear: Int? = null,              // Calculated: startYear + degreeLength
    val extensionEndDate: Long? = null,           // Admin extension end date (R6-S4 EX2)

    // R6-S5: Deactivation & email bounce tracking
    val emailBounceCount: Int = 0,
    val deactivatedAt: Long? = null
)
