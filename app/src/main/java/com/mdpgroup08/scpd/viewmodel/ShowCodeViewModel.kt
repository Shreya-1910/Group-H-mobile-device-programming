package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.attendance.CheckInCodes
import com.mdpgroup08.scpd.data.attendance.ClubCheckInProcessor
import com.mdpgroup08.scpd.data.attendance.CheckInWindow
import com.mdpgroup08.scpd.data.model.AttendanceStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.repository.AttendanceRepository
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.CheckInNotAllowedException
import com.mdpgroup08.scpd.data.repository.CheckInNotLoadedException
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ShowCodeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val title: String = "",
    val opensAt: Long = 0L,
    val closesAt: Long = 0L,
    val isWindowOpen: Boolean = false,
    val isShowing: Boolean = false,
    val qrPayload: String? = null,
    val sixDigits: String? = null,
    val secondsUntilNext: Int = 0,
    /** R3-S5: live count for the organiser. */
    val verifiedCount: Int = 0,
    val pendingCount: Int = 0,
    val bookedCount: Int = 0
)

/**
 * Club "Show code" (R3-S1, R4-S23).
 * A new signed code every 15 s, Show / Hide logged in the session log,
 * and Show code only inside the check-in window. All logic is here, not in the screen.
 */
class ShowCodeViewModel(
    savedState: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val attendanceRepository: AttendanceRepository,
    private val authRepository: AuthRepository,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val eventId: String = savedState["eventId"] ?: ""

    private val _uiState = MutableStateFlow(ShowCodeUiState())
    val uiState: StateFlow<ShowCodeUiState> = _uiState.asStateFlow()

    private var event: Event? = null
    private var secret: String? = null
    private var clubId: String = ""
    private var organiserId: String = ""
    private var displayId: String? = null
    private var shownCounter: Long = -1L
    private var checking = false
    private val processor = ClubCheckInProcessor(attendanceRepository, clock)

    init {
        load()
        viewModelScope.launch {
            while (isActive) {
                tick()
                delay(1_000)
            }
        }
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val e = eventRepository.getEvent(eventId).first()
                    ?: throw IllegalStateException("Event not found.")
                val myClub = authRepository.currentClubId()
                    ?: throw IllegalStateException("Couldn't find your club.")
                if (e.clubId != myClub) throw IllegalStateException("You can only run check-in for your own club's events.")
                if (e.status != EventStatus.APPROVED) throw IllegalStateException("Check-in is only for approved events.")

                val sec = attendanceRepository.getOrCreateSecret(e.id, myClub)
                secret = sec
                event = e
                clubId = myClub
                organiserId = authRepository.currentUserId().orEmpty()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = e.title,
                        opensAt = CheckInWindow.opensAt(e.startTime),
                        closesAt = CheckInWindow.closesAt(e.endTime)
                    )
                }
                tick()
                startChecking(e, sec, organiserId)
            } catch (e: CheckInNotAllowedException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (e: CheckInNotLoadedException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load check-in. Try again.")
                }
            }
        }
    }

    /** Checks students' check-ins on this phone and keeps the live count up to date. */
    private fun startChecking(e: Event, secret: String, organiserId: String) {
        if (checking) return
        checking = true
        viewModelScope.launch {
            // A lost connection must not crash the code screen; the code keeps rotating.
            try { processor.run(e, secret, organiserId) } catch (ex: Exception) {
                android.util.Log.w("SCPD-CheckIn", "Club checking stopped", ex)
            }
        }
        viewModelScope.launch {
            attendanceRepository.observeEventCheckIns(e.id, e.clubId).catch { ex ->
                android.util.Log.w("SCPD-CheckIn", "Can't read check-ins", ex)
            }.collect { list ->
                _uiState.update { s ->
                    s.copy(
                        verifiedCount = list.count { it.status == AttendanceStatus.VERIFIED },
                        pendingCount = list.count { it.status == AttendanceStatus.PENDING }
                    )
                }
            }
        }
        viewModelScope.launch {
            val booked = attendanceRepository.bookedStudentIds(e.id).size.takeIf { it > 0 } ?: e.bookedCount
            _uiState.update { it.copy(bookedCount = booked) }
        }
    }

    /** R3-S1 AC1: only inside the window. Starts a new entry in the session log. */
    fun showCode() {
        val e = event ?: return
        val now = clock()
        if (!CheckInWindow.isOpen(e.startTime, e.endTime, now)) return
        shownCounter = CheckInCodes.counterAt(now)
        displayId = attendanceRepository.startDisplay(e.id, clubId, organiserId, shownCounter, now)
        _uiState.update { it.copy(isShowing = true) }
        tick()
    }

    /** R4-S23 AC2: no new codes after Hide. Ends the session log entry. */
    fun hideCode() {
        displayId?.let { attendanceRepository.endDisplay(it, shownCounter, clock()) }
        displayId = null
        _uiState.update { it.copy(isShowing = false, qrPayload = null, sixDigits = null) }
    }

    /** Called every second: rotates the code, updates the countdown, closes at the end of the window. */
    fun tick() {
        val e = event ?: return
        val s = secret ?: return
        val now = clock()
        val open = CheckInWindow.isOpen(e.startTime, e.endTime, now)

        if (_uiState.value.isShowing && !open) {
            hideCode()
        }

        if (_uiState.value.isShowing) {
            val counter = CheckInCodes.counterAt(now)
            if (counter != shownCounter) {
                shownCounter = counter
                displayId?.let { attendanceRepository.updateDisplay(it, counter) }
            }
            _uiState.update {
                it.copy(
                    isWindowOpen = true,
                    qrPayload = CheckInCodes.qrPayload(s, e.id, counter),
                    sixDigits = CheckInCodes.sixDigitCode(s, e.id, counter),
                    secondsUntilNext = CheckInCodes.secondsUntilNext(now)
                )
            }
        } else {
            _uiState.update { it.copy(isWindowOpen = open) }
        }
    }

    override fun onCleared() {
        // Leaving the screen ends the display, so the session log is never left open.
        if (_uiState.value.isShowing) hideCode()
        super.onCleared()
    }
}
