package com.mdpgroup08.scpd.data.model

/**
 * The role stored in Firestore at users/{uid}.role.
 * Must match exactly: "STUDENT", "CLUB" or "ADMIN".
 */
enum class UserRole {
    STUDENT,
    CLUB,
    ADMIN;

    companion object {
        /** Turns the text from Firestore into a UserRole, or null if it's missing or unknown. */
        fun fromString(value: String?): UserRole? =
            entries.firstOrNull { it.name == value }
    }
}