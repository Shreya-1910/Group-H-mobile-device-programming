package com.mdpgroup08.scpd.data.attendance

import com.mdpgroup08.scpd.data.model.Attendance
import com.mdpgroup08.scpd.data.model.AttendanceMethod
import com.mdpgroup08.scpd.data.model.AttendanceStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.repository.AttendanceRepository
import com.mdpgroup08.scpd.data.repository.CodeDisplay
import kotlinx.coroutines.flow.combine

/**
 * Runs on the club's phone while its check-in or attendance screen is open.
 * It watches Pending check-ins for the event and judges each one with [CheckInVerifier],
 * using the event secret that only this club can read (R3-S9).
 *
 * Checking on the club phone means the app works on Firebase's free plan (no Cloud Functions).
 * The trade-off: check-ins wait as Pending until the club's screen is open.
 */
class ClubCheckInProcessor(
    private val repository: AttendanceRepository,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    /** Check-ins already handled, so a list refresh doesn't check the same submission twice. */
    private val handled = mutableSetOf<String>()

    suspend fun run(event: Event, secret: String, organiserId: String) {
        combine(
            repository.observeEventCheckIns(event.id, event.clubId),
            repository.observeDisplays(event.id, event.clubId)
        ) { checkIns, displays -> checkIns to displays }
            .collect { (checkIns, displays) ->
                val log = sessionCounters(displays)
                checkIns.filter(::needsChecking).forEach { checkIn ->
                    val key = "${checkIn.id}:${checkIn.submittedAt}:${log != null}"
                    if (handled.add(key)) {
                        try {
                            process(checkIn, checkIns, log, event, secret, organiserId)
                        } catch (e: Exception) {
                            android.util.Log.w("SCPD-CheckIn", "Couldn't check ${checkIn.id}", e)
                            handled.remove(key)   // try again on the next update
                        }
                    }
                }
            }
    }

    private fun needsChecking(c: Attendance) =
        c.status == AttendanceStatus.PENDING ||
            (c.status == AttendanceStatus.NEEDS_REVIEW &&
                c.reason == CheckInVerifier.Reason.WAITING_FOR_SESSION_LOG.message)

    private suspend fun process(
        checkIn: Attendance, all: List<Attendance>, log: Set<Long>?,
        event: Event, secret: String, organiserId: String
    ) {
        val proof = when (checkIn.method) {
            AttendanceMethod.QR -> CheckInVerifier.Proof.Qr(
                checkIn.eventId, checkIn.qrCounter ?: -1L, checkIn.qrSignature.orEmpty()
            )
            AttendanceMethod.CODE -> CheckInVerifier.Proof.Code(checkIn.sixDigits.orEmpty())
            AttendanceMethod.MANUAL -> return   // already decided by the club
        }
        val attempt = CheckInVerifier.Attempt(
            eventId = checkIn.eventId,
            studentId = checkIn.studentId,
            deviceId = checkIn.deviceId,
            proof = proof,
            online = checkIn.online
        )
        val context = CheckInVerifier.Context(
            eventId = event.id,
            secret = secret,
            startTime = event.startTime,
            endTime = event.endTime,
            eventCancelled = event.status == EventStatus.CANCELLED,
            // Judged at the moment the check-in reached the server, so a club screen opened
            // a minute later doesn't turn a fresh code into "expired" (R3-S7, R4-S16).
            serverNow = checkIn.serverSubmittedAt?.toDate()?.time ?: clock(),
            sessionLogCounters = log,
            hasBooking = repository.hasConfirmedBooking(event.id, checkIn.studentId),
            studentAlreadyCheckedIn = false,   // one document per student, and the rules block resubmitting after Verified
            deviceUsedByAnotherStudent = all.any {
                it.studentId != checkIn.studentId && it.deviceId == checkIn.deviceId &&
                    it.deviceId.isNotBlank() && it.status == AttendanceStatus.VERIFIED
            }
        )

        val now = clock()
        when (val r = CheckInVerifier.verify(attempt, context)) {
            is CheckInVerifier.Result.Verified -> repository.decide(
                checkIn.id, AttendanceStatus.VERIFIED, null, null, r.counter, organiserId, now
            )
            is CheckInVerifier.Result.Rejected -> repository.decide(
                checkIn.id, AttendanceStatus.REJECTED, r.reason.message, r.reason.nextStep, null, organiserId, now
            )
            is CheckInVerifier.Result.NeedsReview -> repository.decide(
                checkIn.id, AttendanceStatus.NEEDS_REVIEW, r.reason.message, r.reason.nextStep, r.counter, organiserId, now
            )
        }
    }

    companion object {
        /** Every counter shown in the session log, or null if nothing has been logged yet. */
        fun sessionCounters(displays: List<CodeDisplay>): Set<Long>? =
            if (displays.isEmpty()) null
            else displays.flatMap { (it.firstCounter..it.lastCounter).toList() }.toSet()

        /** R3-S5: how many check-ins used each display's codes. */
        fun scansPerDisplay(displays: List<CodeDisplay>, checkIns: List<Attendance>): Map<CodeDisplay, Int> =
            displays.associateWith { d ->
                checkIns.count { c -> c.counter != null && c.counter in d.firstCounter..d.lastCounter }
            }
    }
}
