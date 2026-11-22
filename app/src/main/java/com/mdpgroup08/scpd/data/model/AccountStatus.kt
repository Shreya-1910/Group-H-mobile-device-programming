package com.mdpgroup08.scpd.data.model

/**
 * Account lifecycle statuses for R6-S1, R6-S4, R6-S5.
 */
enum class AccountStatus {
    /** Registered, awaiting 24-hour email verification and PDPA acceptance. */
    PENDING_VERIFICATION,

    /** Active account with full privileges for its role. */
    ACTIVE,

    /** Study period ended; awaiting admin decision on resit/extension (R6-S4 EX2). */
    WAITING_FOR_APPROVAL,

    /** End date within 30 days or passed; warning sent (R6-S5 AC1). */
    FLAGGED,

    /** Account deactivated after 30-day grace period; future bookings cancelled, records retained (R6-S5 EX2). */
    DEACTIVATED;

    companion object {
        fun fromString(value: String?): AccountStatus =
            entries.firstOrNull { it.name == value } ?: PENDING_VERIFICATION
    }
}
