package com.mdpgroup08.scpd.data.attendance

import com.mdpgroup08.scpd.data.attendance.CheckInVerifier.Reason
import com.mdpgroup08.scpd.data.attendance.ReviewRules.Who
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRulesTest {

    @Test
    fun clubDecidesNoBookingAndNotInSessionLog() {
        assertEquals(Who.CLUB, ReviewRules.whoDecides(Reason.NO_BOOKING.message))
        assertEquals(Who.CLUB, ReviewRules.whoDecides(Reason.NOT_IN_SESSION_LOG.message))
    }

    @Test
    fun waitingForSessionLogCannotBeDecidedYet() {
        assertEquals(Who.WAITING, ReviewRules.whoDecides(Reason.WAITING_FOR_SESSION_LOG.message))
    }

    @Test
    fun anythingElseGoesToTheAdmin() {
        assertEquals(Who.ADMIN, ReviewRules.whoDecides("Possible tampering"))
        assertEquals(Who.ADMIN, ReviewRules.whoDecides(null))
    }

    @Test
    fun rejectNeedsAReason() {
        assertFalse(ReviewRules.isValidRejectReason(""))
        assertFalse(ReviewRules.isValidRejectReason("  a "))
        assertTrue(ReviewRules.isValidRejectReason("Not at the event"))
    }

    @Test
    fun onlyAppealableRejectionsAreFinal() {
        assertTrue(ReviewRules.isFinalRejection("REJECTED", ReviewRules.APPEAL_STEP))
        assertFalse(ReviewRules.isFinalRejection("REJECTED", Reason.INCORRECT_CODE.nextStep))
        assertFalse(ReviewRules.isFinalRejection("VERIFIED", null))
    }

    @Test
    fun syncedTooLateUsesTheSameAppealText() {
        assertEquals(ReviewRules.APPEAL_STEP, Reason.SYNCED_TOO_LATE.nextStep)
    }
}
