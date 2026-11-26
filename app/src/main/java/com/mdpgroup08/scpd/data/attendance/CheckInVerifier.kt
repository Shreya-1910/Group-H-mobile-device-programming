package com.mdpgroup08.scpd.data.attendance

/**
 * The server-side check-in rules (R3-S7 to R3-S11, R4-S5, R4-S6, R4-S7, R4-S15 to R4-S17).
 *
 * This is the single source of truth for whether a check-in is Verified, Rejected or
 * Needs review. It is pure Kotlin so every rule is unit tested, and the same logic
 * runs wherever the trusted verification happens (Cloud Function or club device).
 * The phone clock of the student is never used (R4-S16).
 */
object CheckInVerifier {

    /** What the student sent: a scanned QR (counter + signature) or a typed 6-digit code. */
    sealed interface Proof {
        data class Qr(val eventId: String, val counter: Long, val signature: String) : Proof
        data class Code(val sixDigits: String) : Proof
    }

    data class Attempt(
        val eventId: String,
        val studentId: String,
        val deviceId: String,
        val proof: Proof,
        /** True if the scan was checked live; false if it was saved offline and synced later. */
        val online: Boolean
    )

    /** Everything the trusted side knows when it judges the attempt. */
    data class Context(
        val eventId: String,
        val secret: String,
        val startTime: Long,
        val endTime: Long,
        val eventCancelled: Boolean,
        /** Server time, already adjusted by the organiser device's clock offset (R4-S19). */
        val serverNow: Long,
        /** Counters shown in the club's session log, or null if the log hasn't been uploaded yet. */
        val sessionLogCounters: Set<Long>?,
        val hasBooking: Boolean,
        val studentAlreadyCheckedIn: Boolean,
        /** This phone already checked in a different student for this event (R3-S10 AC1). */
        val deviceUsedByAnotherStudent: Boolean
    )

    enum class Reason(val message: String, val nextStep: String) {
        WRONG_EVENT("This code is not valid for this event", "Scan the code shown for this event."),
        INCORRECT_CODE("Invalid code, please try again", "Type the 6 digits shown on the organiser's screen right now."),
        FORGED_CODE("This code is not valid", "Scan the code on the organiser's screen."),
        EXPIRED("Code timed out, please try again", "The code changes every 15 seconds. Type the one shown now."),
        NOT_OPEN("Check-in hasn't opened yet", "Try again when the event starts."),
        CLOSED("Check-in is closed: outside event time", "Ask the organiser if you think this is wrong."),
        EVENT_CANCELLED("Event cancelled", "No check-in is needed for a cancelled event."),
        SYNCED_TOO_LATE("Synced too late", "You can appeal this decision."),
        ALREADY_CHECKED_IN("You're already checked in", "Nothing else to do."),
        DEVICE_ALREADY_USED("This phone was already used to check in someone else", "Check in on your own registered phone."),
        NO_BOOKING("No booking", "Sent for review. The club decides whether points are given."),
        NOT_IN_SESSION_LOG("Code not in session log", "Sent for review. The club will check it."),
        WAITING_FOR_SESSION_LOG("Waiting for session log", "This is checked again when the organiser's phone syncs.")
    }

    sealed interface Result {
        data class Verified(val counter: Long) : Result
        data class Rejected(val reason: Reason) : Result
        data class NeedsReview(val reason: Reason, val counter: Long) : Result
    }

    fun verify(attempt: Attempt, ctx: Context): Result {
        // R4-S6 AC1: a code from another event is rejected straight away.
        if (attempt.eventId != ctx.eventId) return Result.Rejected(Reason.WRONG_EVENT)
        if (attempt.proof is Proof.Qr && attempt.proof.eventId != ctx.eventId)
            return Result.Rejected(Reason.WRONG_EVENT)

        // R4-S5 EX1
        if (ctx.eventCancelled) return Result.Rejected(Reason.EVENT_CANCELLED)

        // R4-S5 AC2: offline records must sync within 72 h of the event ending.
        if (!attempt.online && ctx.serverNow > CheckInWindow.syncDeadline(ctx.endTime))
            return Result.Rejected(Reason.SYNCED_TOO_LATE)

        // Find which window counter the proof belongs to, and check its signature (R3-S9).
        val counter = when (val p = attempt.proof) {
            is Proof.Qr -> {
                if (CheckInCodes.signature(ctx.secret, ctx.eventId, p.counter) != p.signature)
                    return Result.Rejected(Reason.FORGED_CODE)
                p.counter
            }
            is Proof.Code -> matchSixDigit(p.sixDigits, attempt.online, ctx)
                ?: return Result.Rejected(Reason.INCORRECT_CODE)
        }

        // R3-S8, R4-S15 AC2, R4-S16: judged on the code's own counter, never the phone clock.
        if (CheckInCodes.counterStart(counter) + CheckInCodes.ROTATION_MS <= CheckInWindow.opensAt(ctx.startTime))
            return Result.Rejected(Reason.NOT_OPEN)
        if (!CheckInWindow.counterInside(counter, ctx.startTime, ctx.endTime))
            return Result.Rejected(Reason.CLOSED)

        // Duplicates and one device per event (R4-S17 AC1/AC3, R3-S10 AC1).
        if (ctx.studentAlreadyCheckedIn) return Result.Rejected(Reason.ALREADY_CHECKED_IN)
        if (ctx.deviceUsedByAnotherStudent) return Result.Rejected(Reason.DEVICE_ALREADY_USED)

        if (attempt.online) {
            // R3-S7 AC1, R4-S6 AC2: only the current or previous code at server time.
            if (!isFresh(counter, ctx.serverNow)) return Result.Rejected(Reason.EXPIRED)
        } else {
            // R3-S7 AC2, R4-S15 AC4/EX4: offline scans must match a code the club actually showed.
            val log = ctx.sessionLogCounters
                ?: return Result.NeedsReview(Reason.WAITING_FOR_SESSION_LOG, counter)
            if (counter !in log) return Result.NeedsReview(Reason.NOT_IN_SESSION_LOG, counter)
        }

        // R3-S11, R4-S7: unbooked check-ins are held for the club to decide.
        if (!ctx.hasBooking) return Result.NeedsReview(Reason.NO_BOOKING, counter)

        return Result.Verified(counter)
    }

    fun isFresh(counter: Long, serverNow: Long): Boolean {
        val current = CheckInCodes.counterAt(serverNow)
        return counter <= current && counter > current - CheckInCodes.ACCEPTED_COUNTERS
    }

    /**
     * Finds the counter a typed code belongs to.
     * Online: the current and previous codes first. If neither matches, any code from the
     * check-in window is still matched so the student sees "expired" rather than "incorrect".
     * Offline: any counter in the check-in window (the session log is checked afterwards).
     */
    private fun matchSixDigit(code: String, online: Boolean, ctx: Context): Long? {
        if (!CheckInCodes.isSixDigitFormat(code)) return null
        val current = CheckInCodes.counterAt(ctx.serverNow)
        if (online) {
            for (c in current downTo current - CheckInCodes.ACCEPTED_COUNTERS + 1)
                if (CheckInCodes.sixDigitCode(ctx.secret, ctx.eventId, c) == code) return c
        }
        val first = CheckInCodes.counterAt(CheckInWindow.opensAt(ctx.startTime))
        val last = minOf(CheckInCodes.counterAt(CheckInWindow.closesAt(ctx.endTime)), current)
        for (c in last downTo first)
            if (CheckInCodes.sixDigitCode(ctx.secret, ctx.eventId, c) == code) return c
        return null
    }
}
