
package com.mdpgroup08.scpd.data.validation

import android.os.SystemClock

/**
 * Handles the manual check-in code attempt limit.
 *
 * Five incorrect attempts trigger a 60-second lockout.
 */
class CheckInCodeLockoutManager {

    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 60_000L
    }

    private var failedAttempts = 0
    private var lockedUntil = 0L

    val attemptsRemaining: Int
        get() = (MAX_ATTEMPTS - failedAttempts).coerceAtLeast(0)

    val isLocked: Boolean
        get() = getRemainingLockoutSeconds() > 0

    /**
     * Records an incorrect code attempt.
     * Returns true when the lockout is activated.
     */
    fun registerFailedAttempt(): Boolean {

        if (isLocked) return false

        failedAttempts++

        if (failedAttempts >= MAX_ATTEMPTS) {
            lockedUntil =
                SystemClock.elapsedRealtime() + LOCKOUT_DURATION_MS

            failedAttempts = 0

            return true
        }

        return false
    }

    /**
     * Returns remaining lockout time in seconds.
     */
    fun getRemainingLockoutSeconds(): Int {

        val remaining =
            lockedUntil - SystemClock.elapsedRealtime()

        if (remaining <= 0) {
            lockedUntil = 0L
            return 0
        }

        return ((remaining + 999) / 1000).toInt()
    }

    /**
     * Resets failed attempts after a successful validation.
     */
    fun resetAttempts() {
        failedAttempts = 0
    }
}
