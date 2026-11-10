package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.data.repository.AuthRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Which action the user last asked for, so Retry can repeat it. */
enum class SaveAction { DRAFT, SUBMIT }

data class CreateEventUiState(
    val form: EventForm = EventForm(),
    val errors: EventFormErrors = EventFormErrors(),
    /** Id of the draft being edited, or "" for a new event. */
    val editingId: String = "",
    val isSaving: Boolean = false,
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
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

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
            val start = Instant.ofEpochMilli(event.date).atZone(ZoneId.systemDefault())
            _uiState.update {
                it.copy(
                    editingId = event.id,
                    form = EventForm(
                        title = event.title,
                        description = event.description,
                        location = event.location,
                        date = if (event.date > 0) start.format(fmtDate) else "",
                        time = if (event.date > 0) start.format(fmtTime) else "",
                        capacity = event.capacity.toString(),
                        points = event.points.toString()
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
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, saveError = null, lastAction = action) }

        viewModelScope.launch {
            try {
                val clubId = authRepository.currentClubId()
                    ?: throw IllegalStateException("No club linked to this account")
                val event = buildEvent(state, clubId)
                if (action == SaveAction.DRAFT) eventRepository.saveDraft(event)
                else eventRepository.submit(event)

                _uiState.value = CreateEventUiState(
                    successMessage = if (action == SaveAction.DRAFT) "Draft saved"
                    else "Submitted for approval"
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, saveError = "Couldn't save the event. Check your connection and try again.")
                }
            }
        }
    }

    private fun buildEvent(state: CreateEventUiState, clubId: String): Event {
        val f = state.form
        return Event(
            id = state.editingId,
            title = f.title.trim(),
            description = f.description.trim(),
            clubId = clubId,
            location = f.location.trim(),
            date = EventValidation.parseDateTime(f.date, f.time) ?: 0L,
            points = f.points.trim().toInt(),
            capacity = f.capacity.trim().toInt()
        )
    }
}
