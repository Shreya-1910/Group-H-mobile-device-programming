package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.attendance.CheckInCodes
import com.mdpgroup08.scpd.data.attendance.CheckInVerifier
import com.mdpgroup08.scpd.data.attendance.CheckInWindow
import com.mdpgroup08.scpd.data.model.Attendance
import com.mdpgroup08.scpd.data.model.AttendanceMethod
import com.mdpgroup08.scpd.data.model.AttendanceStatus
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.AttendanceRepository
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.BookingRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import com.mdpgroup08.scpd.data.validation.CheckInCodeLockoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A booked event the student can check in to. */
data class CheckInEventItem(val event: Event, val isOpen: Boolean)

data class StudentCheckInUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val events: List<CheckInEventItem> = emptyList(),
    val selected: Event? = null,
    val code: String = "",
    val formatError: String? = null,
    val checkIn: Attendance? = null,
    val isOnline: Boolean = true,
    val attemptsRemaining: Int = CheckInCodeLockoutManager.MAX_ATTEMPTS,
    val lockoutSeconds: Int = 0,
    /** True if the check-in has waited a while without an answer. */
    val slowToConfirm: Boolean = false,
    /** Something went wrong sending or loading the check-in. */
    val error: String? = null,
    /** True from tapping Check in until the answer for that code comes back. */
    val checking: Boolean = false,
    /** R3-S2: the camera is open, looking for the organiser's QR code. */
    val scanning: Boolean = false
)

/**
 * Student check-in with the 6-digit code (R3-S2, R3-S3, R3-S4, R4-S6).
 * The phone only sends the code; whether it is valid is decided by the club side with the
 * event secret, never on the student's phone (R3-S9, R4-S16).
 */
class StudentCheckInViewModel(
    private val bookingRepository: BookingRepository,
    private val eventRepository: EventRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val deviceId: () -> String,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val lockout: CheckInCodeLockoutManager = CheckInCodeLockoutManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentCheckInUiState())
    val uiState: StateFlow<StudentCheckInUiState> = _uiState.asStateFlow()

    private var watchJob: Job? = null
    private var lockJob: Job? = null
    private var slowJob: Job? = null
    /** Rejections already counted towards the 5-attempt lockout. */
    private val countedRejections = mutableSetOf<Long>()
    /** Only rejections from this visit count, not old ones from an earlier day. */
    private val openedAt = clock()
    /** When the code now being checked was sent, to match its answer. */
    private var lastSubmittedAt = 0L

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } }
        }
        loadEvents()
    }

    /** Booked, approved events that haven't finished, soonest first. */
    fun loadEvents() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val now = clock()
                val bookings = bookingRepository.getMyBookings().first()
                    .filter { it.status == BookingStatus.CONFIRMED }
                val events = bookings.mapNotNull { eventRepository.getEvent(it.eventId).first() }
                    .filter { it.status == EventStatus.APPROVED && now <= CheckInWindow.closesAt(it.endTime) }
                    .sortedBy { it.startTime }
                    .map { CheckInEventItem(it, CheckInWindow.isOpen(it.startTime, it.endTime, now)) }
                _uiState.update { it.copy(isLoading = false, events = events) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, loadError = "Couldn't load your bookings. Check your connection and try again.")
                }
            }
        }
    }

    fun select(event: Event) {
        _uiState.update {
            it.copy(selected = event, code = "", formatError = null, checkIn = null, slowToConfirm = false, scanning = false, error = null)
        }
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            val me = authRepository.currentUserId() ?: return@launch
            attendanceRepository.observeMyCheckIn(event.id, me).catch { e ->
                android.util.Log.w("SCPD-CheckIn", "Can't read my check-in", e)
                _uiState.update { it.copy(checking = false, error = "Couldn't load your check-in status. Go back and open the event again.") }
            }.collect { checkIn ->
                _uiState.update { it.copy(checkIn = checkIn) }
                if (checkIn != null && checkIn.submittedAt == lastSubmittedAt &&
                    checkIn.status != AttendanceStatus.PENDING
                ) {
                    _uiState.update { it.copy(checking = false) }
                }
                if (checkIn != null && checkIn.status != AttendanceStatus.PENDING) {
                    slowJob?.cancel()
                    _uiState.update { it.copy(slowToConfirm = false) }
                } else if (checkIn != null && clock() - checkIn.submittedAt > SLOW_AFTER_MS) {
                    // An older Pending check-in: let the student try again.
                    _uiState.update { it.copy(slowToConfirm = true) }
                }
                countIncorrect(checkIn)
            }
        }
    }

    fun back() {
        _uiState.update { it.copy(checking = false, scanning = false, error = null) }
        watchJob?.cancel()
        slowJob?.cancel()
        _uiState.update { it.copy(selected = null, checkIn = null, code = "", formatError = null) }
    }

    fun onCodeChange(text: String) {
        val digits = text.filter(Char::isDigit).take(6)
        _uiState.update { it.copy(code = digits, formatError = null) }
    }

    /** R3-S3: the typed code is sent and checked exactly like a scan. */
    fun submit() {
        val state = _uiState.value
        val event = state.selected ?: return
        if (state.lockoutSeconds > 0) return
        if (!CheckInCodes.isSixDigitFormat(state.code)) {
            // R4-S6 EX2: a format mistake doesn't use up an attempt.
            _uiState.update { it.copy(formatError = "A check-in code has 6 digits") }
            return
        }
        if (state.checkIn?.status == AttendanceStatus.VERIFIED) return
        send(event, AttendanceMethod.CODE, sixDigits = state.code)
    }

    /** R3-S2: open the camera. The screen asks for the camera permission first. */
    fun startScan() {
        val state = _uiState.value
        if (state.selected == null || state.checking) return
        if (state.checkIn?.status == AttendanceStatus.VERIFIED) return
        _uiState.update { it.copy(scanning = true, error = null, formatError = null) }
    }

    fun stopScan() {
        _uiState.update { it.copy(scanning = false) }
    }

    fun cameraUnavailable(message: String) {
        _uiState.update { it.copy(scanning = false, error = message) }
    }

    /**
     * R3-S2: a QR code was read. It is sent and checked by the club like a typed code;
     * the phone only checks that it looks like an s-CPD code for this event (R4-S6 EX1).
     */
    fun onQrScanned(text: String) {
        val state = _uiState.value
        val event = state.selected ?: return
        if (!state.scanning) return
        _uiState.update { it.copy(scanning = false) }
        val qr = CheckInCodes.parseQr(text)
        when {
            qr == null -> _uiState.update {
                it.copy(error = "This isn't an s-CPD check-in code. Scan the QR code on the organiser's screen.")
            }
            qr.eventId != event.id -> _uiState.update {
                it.copy(error = "This QR code is for a different event. Scan the code for ${event.title}.")
            }
            else -> send(event, AttendanceMethod.QR, qrCounter = qr.counter, qrSignature = qr.signature)
        }
    }

    private fun send(
        event: Event,
        method: AttendanceMethod,
        sixDigits: String? = null,
        qrCounter: Long? = null,
        qrSignature: String? = null
    ) {
        lastSubmittedAt = clock()
        _uiState.update { it.copy(checking = true, slowToConfirm = false, error = null) }
        viewModelScope.launch {
            val uid = authRepository.currentUserId()
            if (uid == null) {
                _uiState.update { it.copy(checking = false, error = "You're signed out. Log in again, then check in.") }
                return@launch
            }
            val name = try { authRepository.currentUser()?.name } catch (e: Exception) { null }
            attendanceRepository.submitCheckIn(
                Attendance(
                    eventId = event.id,
                    clubId = event.clubId,
                    studentId = uid,
                    studentName = name?.ifBlank { null } ?: "Student",
                    deviceId = deviceId(),
                    method = method,
                    sixDigits = sixDigits,
                    qrCounter = qrCounter,
                    qrSignature = qrSignature,
                    online = networkMonitor.isOnline.value,
                    submittedAt = lastSubmittedAt
                ),
                onRejected = {
                    _uiState.update {
                        it.copy(checking = false, error = "Couldn't send your check-in. Please try again.")
                    }
                }
            )
            _uiState.update { it.copy(code = "") }
            slowJob?.cancel()
            slowJob = launch {
                delay(SLOW_AFTER_MS)
                _uiState.update { it.copy(slowToConfirm = true) }
            }
        }
    }

    /** R3-S3 AC2: 5 incorrect codes lock entry for 60 s. Only "Incorrect code" counts. */
    private fun countIncorrect(checkIn: Attendance?) {
        if (checkIn == null || checkIn.status != AttendanceStatus.REJECTED) return
        if (checkIn.reason != CheckInVerifier.Reason.INCORRECT_CODE.message) return
        if (checkIn.submittedAt < openedAt) return
        if (!countedRejections.add(checkIn.submittedAt)) return

        if (lockout.registerFailedAttempt()) {
            startLockCountdown()
        } else {
            _uiState.update { it.copy(attemptsRemaining = lockout.attemptsRemaining) }
        }
    }

    private fun startLockCountdown() {
        lockJob?.cancel()
        lockJob = viewModelScope.launch {
            while (true) {
                val left = lockout.getRemainingLockoutSeconds()
                _uiState.update {
                    it.copy(lockoutSeconds = left, attemptsRemaining = if (left > 0) 0 else lockout.attemptsRemaining)
                }
                if (left <= 0) break
                delay(1_000)
            }
        }
    }

    companion object {
        const val SLOW_AFTER_MS = 10_000L
    }
}
