package com.mdpgroup08.scpd.data.attendance

/**
 * R4-S18: who can decide a Needs review check-in.
 * Pure Kotlin, so it can be unit tested.
 */
object ReviewRules {

    enum class Who {
        /** AC1: reasons the club can judge itself, so it gets Approve and Reject. */
        CLUB,
        /** Not ready yet: the organiser's phone still has to sync its session log. */
        WAITING,
        /** EX5: appeals, tampering, organiser flags or clock problems go to the s-CPD admin. */
        ADMIN
    }

    private val clubCanDecide = setOf(
        CheckInVerifier.Reason.NO_BOOKING.message,
        CheckInVerifier.Reason.NOT_IN_SESSION_LOG.message
    )

    fun whoDecides(reason: String?): Who = when (reason) {
        in clubCanDecide -> Who.CLUB
        CheckInVerifier.Reason.WAITING_FOR_SESSION_LOG.message -> Who.WAITING
        else -> Who.ADMIN
    }

    /** What a final rejection tells the student (organiser decision or synced too late). */
    const val APPEAL_STEP = "You can appeal this decision."

    /**
     * True when a rejection is a final decision (by the organiser, or synced too late), so the
     * student should appeal instead of sending a new code. Other rejections (wrong or expired
     * code) can simply be tried again.
     */
    fun isFinalRejection(status: String?, nextStep: String?): Boolean =
        status == "REJECTED" && nextStep == APPEAL_STEP

    /** EX3: a rejection needs a reason the student can read. */
    const val MIN_REJECT_REASON = 3

    fun isValidRejectReason(text: String): Boolean = text.trim().length >= MIN_REJECT_REASON
}
