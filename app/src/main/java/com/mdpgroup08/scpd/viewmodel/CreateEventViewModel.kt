package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.model.UserRole
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Which action the user last asked for, so Retry can repeat it. */
enum class SaveAction { DRAFT, SUBMIT }

/** Where a copied event came from, shown as a banner so the club sees what to fix (R1-S11). */
data class CopiedFrom(val eventId: String, val title: String, val reason: String?)
data class CreateEventUiState(
    val form: EventForm = EventForm(),
    val errors: EventFormErrors = EventFormErrors(),
    /** ID of the draft being edited, or "" for a new event. */
    val editingId: String = "",
    /** Reserved ID for a new event, so a retry uses the same document. */
    val savingId: String = "",
    /** Set when the form was filled from a rejected, cancelled or expired event (R1-S11). */
    val copiedFrom: CopiedFrom? = null,
    val editingCreatedAt: Long = 0L,
    val isSaving: Boolean = false,
    val isOffline: Boolean = false,
    val showSubmitConfirm: Boolean = false,
    val saveError: String? = null,
    val lastAction: SaveAction? = null,
    /** Set after a successful save; the screen shows it and then calls messageShown(). */
    val successMessage: String? = null
)

/** Create event (R1-S1, R1-S2). All validation and saving lives here, not in the screen. */
class CreateEventViewModel(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val saveTimeoutMs: Long = SAVE_TIMEOUT_MS,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }
    }

    fun onFormChange(form: EventForm) =
        _uiState.update { it.copy(form = form, saveError = null) }

    /** Opens an existing DRAFT for editing (R1-S6: only drafts can be edited). */
    fun loadDraft(eventId: String) {
        if (eventId.isBlank() || _uiState.value.editingId == eventId) return
        viewModelScope.launch {
            val event = eventRepository.getEvent(eventId).first() ?: return@launch
            if (event.status != EventStatus.DRAFT) return@launch
            val fmtDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val fmtTime = DateTimeFormatter.ofPattern("HH:mm")
            val start = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault())
            val end = Instant.ofEpochMilli(event.endTime).atZone(ZoneId.systemDefault())
            _uiState.update {
                it.copy(
                    editingId = event.id,
                    editingCreatedAt = event.createdAt,
                    form = EventForm(
                        title = event.title,
                        description = event.description,
                        category = event.category,
                        location = event.location,
                        date = if (event.startTime > 0) start.format(fmtDate) else "",
                        time = if (event.startTime > 0) start.format(fmtTime) else "",
                        endTime = if (event.endTime > 0) end.format(fmtTime) else "",
                        capacity = event.capacity.toString(),
                        points = event.requestedPoints.toString()
                    )
                )
            }
        }
    }

    /**
     * R1-S11: start a NEW draft filled from a rejected, cancelled or expired event.
     * The original is left unchanged. A date that has already passed is cleared, so a new one must be chosen.
     */
    fun loadCopy(eventId: String) {
        if (eventId.isBlank() || _uiState.value.copiedFrom?.eventId == eventId) return
        viewModelScope.launch {
            val event = eventRepository.getEvent(eventId).first() ?: return@launch
            if (event.status !in COPYABLE) return@launch

            val fmtDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val fmtTime = DateTimeFormatter.ofPattern("HH:mm")
            val start = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault())
            val end = Instant.ofEpochMilli(event.endTime).atZone(ZoneId.systemDefault())
            val keepDate = event.startTime > clock()

            _uiState.update {
                it.copy(
                    editingId = "",          // a new event, not an edit
                    editingCreatedAt = 0L,
                    savingId = "",
                    copiedFrom = CopiedFrom(event.id, event.title, event.rejectionReason ?: event.cancellationReason),
                    form = EventForm(
                        title = event.title,
                        description = event.description,
                        category = event.category,
                        location = event.location,
                        date = if (keepDate) start.format(fmtDate) else "",
                        time = if (keepDate) start.format(fmtTime) else "",
                        endTime = if (keepDate) end.format(fmtTime) else "",
                        capacity = event.capacity.toString(),
                        points = event.requestedPoints.toString()
                    )
                )
            }
        }
    }

    fun saveDraft() = save(SaveAction.DRAFT)

    /** Validates first; only shows the confirmation dialog if the form is valid. */
    fun requestSubmit() {
        if (validate()) _uiState.update { it.copy(showSubmitConfirm = true) }
    }

    fun dismissSubmit() = _uiState.update { it.copy(showSubmitConfirm = false) }

    fun confirmSubmit() {
        _uiState.update { it.copy(showSubmitConfirm = false) }
        save(SaveAction.SUBMIT)
    }

    fun retry() { _uiState.value.lastAction?.let { save(it) } }

    fun messageShown() = _uiState.update { it.copy(successMessage = null) }

    /** Returns true when there are no errors. */
    fun validate(): Boolean {
        val errors = EventValidation.validate(_uiState.value.form, clock())
        _uiState.update { it.copy(errors = errors) }
        return !errors.hasErrors
    }

    private fun save(action: SaveAction) {
        if (!validate()) return

        // R1-S2 AC2: saving and submitting need a connection. The form is kept.
        if (_uiState.value.isOffline) {
            _uiState.update { it.copy(saveError = OFFLINE_MESSAGE, lastAction = action) }
            return
        }

        val state = _uiState.value
        // Reserve the ID once, so a retry after a lost reply overwrites the same event
        val id = state.editingId.ifBlank { state.savingId.ifBlank { eventRepository.newId() } }
        _uiState.update { it.copy(isSaving = true, saveError = null, lastAction = action, savingId = id) }

        viewModelScope.launch {
            try {
                val user = authRepository.currentUser()
                // Use equals with ignoreCase to support both "CLUB" and "club"
                if (user?.role?.equals(UserRole.CLUB.name, ignoreCase = true) != true) {
                    throw IllegalStateException("Only club accounts can create events. You are logged in as an ${user?.role ?: "User"}.")
                }
                val clubId = user.clubId?.ifBlank { null }
                    ?: throw IllegalStateException("No club linked to this account. Please log in with a club account.")
                val clubName = user.clubName?.ifBlank { null } ?: user.name.ifBlank { "Club" }

                val event = buildEvent(state, clubId, clubName).copy(id = id)

                val savedId = withTimeoutOrNull(saveTimeoutMs) {
                    if (action == SaveAction.DRAFT) eventRepository.saveDraft(event)
                    else eventRepository.submit(event)
                }
                if (savedId == null) {
                    _uiState.update { it.copy(isSaving = false, saveError = TIMEOUT_MESSAGE) }
                    return@launch
                }

                _uiState.value = CreateEventUiState(
                    isOffline = !networkMonitor.isOnline.value,
                    successMessage = if (action == SaveAction.DRAFT) "Draft saved" else "Submitted for approval"
                )
            } catch (e: IllegalStateException) {
                _uiState.update {
                    it.copy(isSaving = false, saveError = e.message ?: "No club linked to this account")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, saveError = e.message ?: "Couldn't save the event. Try again.")
                }
            }
        }
    }

    private fun buildEvent(state: CreateEventUiState, clubId: String, clubName: String): Event {
        val f = state.form
        return Event(
            id = state.editingId,
            title = f.title.trim(),
            description = f.description.trim(),
            category = f.category,
            clubId = clubId,
            clubName = clubName,
            location = f.location.trim(),
            startTime = EventValidation.parseDateTime(f.date, f.time) ?: 0L,
            endTime = EventValidation.parseDateTime(f.date, f.endTime) ?: 0L,
            requestedPoints = f.points.trim().toInt(),
            capacity = f.capacity.trim().toInt(),
            createdAt = if (state.editingCreatedAt > 0) state.editingCreatedAt else clock()
        )
    }

    companion object {
        const val SAVE_TIMEOUT_MS = 15_000L
        const val OFFLINE_MESSAGE = "You're offline. Connect to the internet to save or submit. Your form is kept."
        const val TIMEOUT_MESSAGE = "The server is taking too long to respond. Tap Retry; your event won't be duplicated."
        /** Events that can be copied into a new draft (R1-S11). */
        val COPYABLE = setOf(EventStatus.REJECTED, EventStatus.CANCELLED, EventStatus.EXPIRED)
    }
}
