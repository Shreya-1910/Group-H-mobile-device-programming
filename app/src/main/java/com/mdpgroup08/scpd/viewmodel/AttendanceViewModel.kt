package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.attendance.ClubCheckInProcessor
import com.mdpgroup08.scpd.data.attendance.ReviewRules
import com.mdpgroup08.scpd.data.model.Attendance
import com.mdpgroup08.scpd.data.model.AttendanceStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.repository.AttendanceRepository
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.CheckInAlreadyDecidedException
import com.mdpgroup08.scpd.data.repository.CheckInNotAllowedException
import com.mdpgroup08.scpd.data.repository.CheckInNotLoadedException
import com.mdpgroup08.scpd.data.repository.CodeDisplay
import com.mdpgroup08.scpd.data.repository.EventRepository
import com.mdpgroup08.scpd.data.repository.ReviewNeedsConnectionException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row in the club's attendance list. */
data class AttendanceRow(
    val studentId: String,
    val name: String,
    /** null = booked but not checked in yet. */
    val checkIn: Attendance?
)

data class AttendanceUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val title: String = "",
    val rows: List<AttendanceRow> = emptyList(),
    val verifiedCount: Int = 0,
    val bookedCount: Int = 0,
    /** R3-S5: how many check-ins came in during each time the code was shown. */
    val scansPerDisplay: List<Pair<CodeDisplay, Int>> = emptyList(),
    /** Student currently being marked present, for the dialog. */
    val marking: AttendanceRow? = null,
    val markReason: String = "",
    val markError: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    /** R4-S18: Needs review check-in being approved or rejected, for the dialog. */
    val reviewing: AttendanceRow? = null,
    val reviewApprove: Boolean = true,
    val reviewReason: String = "",
    val reviewError: String? = null
)

/**
 * Club attendance list (R3-S5, R3-S6): who is verified, who is still being checked,
 * who hasn't checked in, and Mark present with a reason for a student whose phone fails.
 */
class AttendanceViewModel(
    savedState: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val eventId: String = savedState["eventId"] ?: ""

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private var event: Event? = null
    private var organiserId = ""
    private var jobs = emptyList<Job>()
    private val names = mutableMapOf<String, String>()

    init { load() }

    fun load() {
        jobs.forEach { it.cancel() }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val e = eventRepository.getEvent(eventId).first()
                    ?: throw IllegalStateException("Event not found.")
                val myClub = authRepository.currentClubId()
                    ?: throw IllegalStateException("Couldn't find your club.")
                if (e.clubId != myClub) throw IllegalStateException("You can only see attendance for your own club's events.")
                if (e.status != EventStatus.APPROVED && e.status != EventStatus.CANCELLED) {
                    throw IllegalStateException("Attendance is only for approved events.")
                }
                event = e
                organiserId = authRepository.currentUserId().orEmpty()
                val secret = attendanceRepository.getOrCreateSecret(e.id, myClub)
                val booked = attendanceRepository.bookedStudentIds(e.id)
                _uiState.update { it.copy(title = e.title) }

                val processor = ClubCheckInProcessor(attendanceRepository, clock)
                jobs = listOf(
                    launch { try { processor.run(e, secret, organiserId) } catch (ex: Exception) { } },
                    launch {
                        combine(
                            attendanceRepository.observeEventCheckIns(e.id, e.clubId),
                            attendanceRepository.observeDisplays(e.id, e.clubId)
                        ) { c, d -> c to d }
                            .catch {
                                _uiState.update {
                                    it.copy(isLoading = false, errorMessage = "Couldn't load attendance. Check your connection and try again.")
                                }
                            }
                            .collect { (checkIns, displays) ->
                            show(checkIns, displays, booked, e)
                        }
                    }
                )
            } catch (e: CheckInNotAllowedException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (e: CheckInNotLoadedException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load attendance. Try again.")
                }
            }
        }
    }

    private suspend fun show(checkIns: List<Attendance>, displays: List<CodeDisplay>, booked: List<String>, e: Event) {
        val byStudent = checkIns.associateBy { it.studentId }
        val notYet = booked.filter { it !in byStudent }
        for (id in notYet) if (id !in names) names[id] = attendanceRepository.studentName(id)

        val rows = checkIns.map { AttendanceRow(it.studentId, it.studentName.ifBlank { "Student" }, it) } +
            notYet.map { AttendanceRow(it, names[it] ?: "Student", null) }

        _uiState.update {
            it.copy(
                isLoading = false,
                rows = rows.sortedWith(compareBy({ order(it.checkIn?.status) }, { it.name.lowercase() })),
                verifiedCount = checkIns.count { c -> c.status == AttendanceStatus.VERIFIED },
                bookedCount = booked.size.takeIf { n -> n > 0 } ?: e.bookedCount,
                scansPerDisplay = ClubCheckInProcessor.scansPerDisplay(displays, checkIns)
                    .toList().sortedBy { (d, _) -> d.startedAt }
            )
        }
    }

    /** Problems first so the organiser sees them: review, rejected, not yet, pending, verified. */
    private fun order(s: AttendanceStatus?) = when (s) {
        AttendanceStatus.NEEDS_REVIEW -> 0
        AttendanceStatus.REJECTED -> 1
        null -> 2
        AttendanceStatus.PENDING -> 3
        AttendanceStatus.VERIFIED -> 4
    }

    fun startMarking(row: AttendanceRow) {
        _uiState.update { it.copy(marking = row, markReason = "", markError = null) }
    }

    fun onReasonChange(text: String) {
        _uiState.update { it.copy(markReason = text.take(200), markError = null) }
    }

    fun cancelMarking() {
        _uiState.update { it.copy(marking = null, markReason = "", markError = null) }
    }

    /** R3-S6: a reason is required, and the change is written to the audit log. */
    fun confirmMarkPresent() {
        val state = _uiState.value
        val row = state.marking ?: return
        val e = event ?: return
        val reason = state.markReason.trim()
        if (reason.length < 3) {
            _uiState.update { it.copy(markError = "Give a reason, for example \"Phone battery died\"") }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                attendanceRepository.markPresent(e, row.studentId, row.name, reason, organiserId, clock())
                _uiState.update {
                    it.copy(isSaving = false, marking = null, markReason = "", message = "${row.name} marked present")
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isSaving = false, markError = "Couldn't save. Check your connection and try again.") }
            }
        }
    }

    // ---- R4-S18: organiser review of Needs review check-ins ----

    fun startReview(row: AttendanceRow, approve: Boolean) {
        val c = row.checkIn ?: return
        if (c.status != AttendanceStatus.NEEDS_REVIEW) return
        if (ReviewRules.whoDecides(c.reason) != ReviewRules.Who.CLUB) return
        _uiState.update { it.copy(reviewing = row, reviewApprove = approve, reviewReason = "", reviewError = null) }
    }

    fun onReviewReasonChange(text: String) {
        _uiState.update { it.copy(reviewReason = text.take(200), reviewError = null) }
    }

    fun cancelReview() {
        _uiState.update { it.copy(reviewing = null, reviewReason = "", reviewError = null) }
    }

    fun confirmReview() {
        val state = _uiState.value
        val row = state.reviewing ?: return
        val checkIn = row.checkIn ?: return
        val approve = state.reviewApprove
        val reason = state.reviewReason.trim()
        // EX3: a rejection needs a reason the student can read
        if (!approve && !ReviewRules.isValidRejectReason(reason)) {
            _uiState.update { it.copy(reviewError = "Give a reason the student can read, for example \"Not at the event\"") }
            return
        }
        // EX2: decisions are never saved only on the organiser's phone
        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(reviewError = "You need a connection to approve or reject. Try again when you're online.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, reviewError = null) }
        viewModelScope.launch {
            try {
                attendanceRepository.review(
                    checkInId = checkIn.id,
                    approve = approve,
                    reason = if (approve) "Approved by organiser after review (was: ${checkIn.reason ?: "Needs review"})" else reason,
                    organiserId = organiserId,
                    now = clock()
                )
                _uiState.update {
                    it.copy(
                        isSaving = false, reviewing = null, reviewReason = "",
                        message = if (approve) "${row.name} approved" else "${row.name} rejected"
                    )
                }
            } catch (e: CheckInAlreadyDecidedException) {
                // EX1: someone else decided first; their decision stays
                val who = e.decidedBy?.let { id -> attendanceRepository.studentName(id) }
                    ?.takeIf { it.isNotBlank() && it != "Student" } ?: "another organiser"
                _uiState.update { it.copy(isSaving = false, reviewError = "Already decided by $who") }
            } catch (e: ReviewNeedsConnectionException) {
                _uiState.update { it.copy(isSaving = false, reviewError = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, reviewError = "Couldn't save. Check your connection and try again.") }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
