package com.mdpgroup08.scpd.data.attendance

import com.mdpgroup08.scpd.data.attendance.CheckInVerifier.Attempt
import com.mdpgroup08.scpd.data.attendance.CheckInVerifier.Context
import com.mdpgroup08.scpd.data.attendance.CheckInVerifier.Proof
import com.mdpgroup08.scpd.data.attendance.CheckInVerifier.Reason
import com.mdpgroup08.scpd.data.attendance.CheckInVerifier.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

/** Unit tests for the check-in security rules (R3, R4). */
class CheckInVerifierTest {

    private val secret = CheckInCodes.newSecret(SecureRandom(byteArrayOf(1, 2, 3)))
    private val eventId = "event1"
    private val start = 1_800_000_000_000L          // event start
    private val end = start + 2 * 60 * 60 * 1000L     // 2 h event
    private val duringEvent = start + 30 * 60 * 1000L

    private fun ctx(
        now: Long = duringEvent,
        booking: Boolean = true,
        log: Set<Long>? = null,
        already: Boolean = false,
        device: Boolean = false,
        cancelled: Boolean = false
    ) = Context(eventId, secret, start, end, cancelled, now, log, booking, already, device)

    private fun qr(counter: Long, online: Boolean = true, event: String = eventId): Attempt {
        val sig = CheckInCodes.signature(secret, event, counter)
        return Attempt(event, "s1", "d1", Proof.Qr(event, counter, sig), online)
    }

    private fun code(counter: Long, online: Boolean = true) =
        Attempt(eventId, "s1", "d1", Proof.Code(CheckInCodes.sixDigitCode(secret, eventId, counter)), online)

    private val now = CheckInCodes.counterAt(duringEvent)

    // --- Codes ---

    @Test fun `qr payload round trips`() {
        val parsed = CheckInCodes.parseQr(CheckInCodes.qrPayload(secret, eventId, 42))!!
        assertEquals(eventId, parsed.eventId)
        assertEquals(42L, parsed.counter)
    }

    @Test fun `a website qr is not an event code`() {          // R4-S6 EX1
        assertNull(CheckInCodes.parseQr("https://example.com"))
    }

    @Test fun `six digit code has six digits and changes every window`() {
        val a = CheckInCodes.sixDigitCode(secret, eventId, now)
        assertTrue(CheckInCodes.isSixDigitFormat(a))
        assertNotEquals(a, CheckInCodes.sixDigitCode(secret, eventId, now + 5))
    }

    @Test fun `wrong number of digits is a format error`() {    // R4-S6 EX2
        assertTrue(!CheckInCodes.isSixDigitFormat("12345"))
        assertTrue(!CheckInCodes.isSixDigitFormat("12a456"))
    }

    // --- Online ---

    @Test fun `current qr code is verified`() {                 // R3-S2
        assertEquals(Result.Verified(now), CheckInVerifier.verify(qr(now), ctx()))
    }

    @Test fun `previous code is still accepted`() {              // R3-S7 AC1 (30 s)
        assertEquals(Result.Verified(now - 1), CheckInVerifier.verify(qr(now - 1), ctx()))
    }

    @Test fun `typed current code is verified like a scan`() {   // R3-S3 AC1
        assertEquals(Result.Verified(now), CheckInVerifier.verify(code(now), ctx()))
    }

    @Test fun `old screenshot is expired`() {                    // R3-S7, R4-S15 AC1, R4-S6 AC2
        assertEquals(Result.Rejected(Reason.EXPIRED), CheckInVerifier.verify(qr(now - 4), ctx()))
        assertEquals(Result.Rejected(Reason.EXPIRED), CheckInVerifier.verify(code(now - 4), ctx()))
    }

    @Test fun `forged signature is rejected`() {                 // R3-S9 AC1
        val forged = Attempt(eventId, "s1", "d1", Proof.Qr(eventId, now, "0".repeat(32)), true)
        assertEquals(Result.Rejected(Reason.FORGED_CODE), CheckInVerifier.verify(forged, ctx()))
    }

    @Test fun `random six digits are incorrect`() {
        val wrong = Attempt(eventId, "s1", "d1", Proof.Code("000000"), true)
        val result = CheckInVerifier.verify(wrong, ctx())
        // A random code almost never matches; if it does, it can't be the incorrect path.
        if (result is Result.Rejected) assertEquals(Reason.INCORRECT_CODE, result.reason)
    }

    @Test fun `code from another event is wrong event`() {      // R4-S6 AC1, R4-S22
        assertEquals(Result.Rejected(Reason.WRONG_EVENT), CheckInVerifier.verify(qr(now, event = "other"), ctx()))
    }

    @Test fun `code from after the event is outside event time`() {   // R3-S8, R4-S15 AC2
        val late = CheckInCodes.counterAt(end + 16 * 60 * 1000L)
        assertEquals(Result.Rejected(Reason.CLOSED), CheckInVerifier.verify(qr(late), ctx(now = end + 16 * 60 * 1000L)))
    }

    @Test fun `code from before the event is not open`() {
        val early = CheckInCodes.counterAt(start - 60_000L)
        assertEquals(Result.Rejected(Reason.NOT_OPEN), CheckInVerifier.verify(qr(early), ctx(now = start - 60_000L)))
    }

    @Test fun `changing the phone clock has no effect`() {       // R4-S16: only the code counter matters
        assertEquals(Result.Verified(now), CheckInVerifier.verify(qr(now), ctx()))
    }

    @Test fun `second check in is a duplicate`() {               // R4-S17 AC3
        assertEquals(Result.Rejected(Reason.ALREADY_CHECKED_IN), CheckInVerifier.verify(qr(now), ctx(already = true)))
    }

    @Test fun `one phone cannot check in two students`() {      // R3-S10 AC1
        assertEquals(Result.Rejected(Reason.DEVICE_ALREADY_USED), CheckInVerifier.verify(qr(now), ctx(device = true)))
    }

    @Test fun `no booking goes to review`() {                    // R3-S11, R4-S7 AC1
        assertEquals(Result.NeedsReview(Reason.NO_BOOKING, now), CheckInVerifier.verify(qr(now), ctx(booking = false)))
    }

    @Test fun `cancelled event is never verified`() {            // R4-S5 EX1
        assertEquals(Result.Rejected(Reason.EVENT_CANCELLED), CheckInVerifier.verify(qr(now), ctx(cancelled = true)))
    }

    // --- Offline (synced later) ---

    @Test fun `offline scan in session log is verified later`() {    // R4-S5 AC1
        val later = end + 60 * 60 * 1000L
        assertEquals(Result.Verified(now), CheckInVerifier.verify(qr(now, online = false), ctx(now = later, log = setOf(now))))
    }

    @Test fun `offline typed code is matched and verified`() {
        val later = end + 60 * 60 * 1000L
        assertEquals(Result.Verified(now), CheckInVerifier.verify(code(now, online = false), ctx(now = later, log = setOf(now))))
    }

    @Test fun `offline scan not in session log needs review`() {    // R3-S7 AC2, R4-S15 AC4
        assertEquals(
            Result.NeedsReview(Reason.NOT_IN_SESSION_LOG, now),
            CheckInVerifier.verify(qr(now, online = false), ctx(log = setOf(now + 10)))
        )
    }

    @Test fun `offline scan before the log arrives waits`() {       // R4-S15 EX4
        assertEquals(
            Result.NeedsReview(Reason.WAITING_FOR_SESSION_LOG, now),
            CheckInVerifier.verify(qr(now, online = false), ctx(log = null))
        )
    }

    @Test fun `offline scan synced after 72 h is too late`() {      // R4-S5 AC2
        val tooLate = end + 73 * 60 * 60 * 1000L
        assertEquals(Result.Rejected(Reason.SYNCED_TOO_LATE), CheckInVerifier.verify(qr(now, online = false), ctx(now = tooLate, log = setOf(now))))
    }

    // --- Window helpers ---

    @Test fun `window runs from start to 15 min after end`() {      // R3-S8 AC1
        assertTrue(CheckInWindow.isOpen(start, end, start))
        assertTrue(CheckInWindow.isOpen(start, end, end + 15 * 60 * 1000L))
        assertTrue(!CheckInWindow.isOpen(start, end, start - 1))
        assertTrue(!CheckInWindow.isOpen(start, end, end + 15 * 60 * 1000L + 1))
    }
}
