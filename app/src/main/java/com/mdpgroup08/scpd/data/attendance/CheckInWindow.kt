package com.mdpgroup08.scpd.data.attendance

/**
 * The check-in window: from the event start to 15 min after the event ends (R3-S8, R4-S15).
 */
object CheckInWindow {

    const val GRACE_AFTER_END_MS = 15 * 60 * 1000L

    /** Offline records must reach the server within 72 h of the event ending (R4-S5). */
    const val SYNC_DEADLINE_MS = 72 * 60 * 60 * 1000L

    fun opensAt(startTime: Long): Long = startTime

    fun closesAt(endTime: Long): Long = endTime + GRACE_AFTER_END_MS

    fun isOpen(startTime: Long, endTime: Long, now: Long): Boolean =
        now >= opensAt(startTime) && now <= closesAt(endTime)

    /** True if a code's 15 s window lies inside the check-in window. */
    fun counterInside(counter: Long, startTime: Long, endTime: Long): Boolean {
        val codeStart = CheckInCodes.counterStart(counter)
        val codeEnd = codeStart + CheckInCodes.ROTATION_MS
        return codeEnd > opensAt(startTime) && codeStart <= closesAt(endTime)
    }

    fun syncDeadline(endTime: Long): Long = endTime + SYNC_DEADLINE_MS
}
