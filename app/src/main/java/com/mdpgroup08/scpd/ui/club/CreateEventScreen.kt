package com.mdpgroup08.scpd.ui.club

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.ui.components.OfflineBanner
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.CopiedFrom
import com.mdpgroup08.scpd.viewmodel.CreateEventViewModel
import com.mdpgroup08.scpd.viewmodel.EventValidation

/**
 * Create event (R1-S1, R1-S2). Pass a draft's id to edit it (R1-S6),
 * or a rejected, cancelled or expired event's id to start a new draft from a copy (R1-S11).
 */
@Composable
fun CreateEventScreen(
    draftId: String = "",
    copyFromId: String = "",
    onSaved: () -> Unit = {},
    viewModel: CreateEventViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val f = state.form
    val e = state.errors

    LaunchedEffect(draftId) { viewModel.loadDraft(draftId) }
    LaunchedEffect(copyFromId) { viewModel.loadCopy(copyFromId) }
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            viewModel.messageShown()
            onSaved()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            when {
                state.copiedFrom != null -> "New event from a copy"
                state.editingId.isBlank() -> "Create event"
                else -> "Edit draft"
            },
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Saved as a Draft until you submit it for approval.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.isOffline) {
            OfflineBanner("You're offline. You can keep filling in the form, but saving and submitting need a connection.")
        }
        state.copiedFrom?.let { CopiedBanner(it) }

        Field("Title", f.title, e.title, "title") { viewModel.onFormChange(f.copy(title = it)) }
        Field("Description", f.description, null, "description", singleLine = false) {
            viewModel.onFormChange(f.copy(description = it))
        }
        CategoryField(f.category, e.category) { viewModel.onFormChange(f.copy(category = it)) }
        Field("Location", f.location, e.location, "location") { viewModel.onFormChange(f.copy(location = it)) }
        Field("Date (dd/MM/yyyy)", f.date, e.date, "date") {
            viewModel.onFormChange(f.copy(date = it))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Start time (HH:mm)", f.time, e.time, "time", Modifier.weight(1f)) {
                viewModel.onFormChange(f.copy(time = it))
            }
            Field("End time (HH:mm)", f.endTime, e.endTime, "endTime", Modifier.weight(1f)) {
                viewModel.onFormChange(f.copy(endTime = it))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Capacity", f.capacity, e.capacity, "capacity", Modifier.weight(1f), number = true) {
                viewModel.onFormChange(f.copy(capacity = it))
            }
            Field("Points (1–10)", f.points, e.points, "points", Modifier.weight(1f), number = true) {
                viewModel.onFormChange(f.copy(points = it))
            }
        }

        state.saveError?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::retry) { Text("Retry") }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = viewModel::saveDraft,
                enabled = !state.isSaving && !state.isOffline,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Save draft") }
            Button(
                onClick = viewModel::requestSubmit,
                enabled = !state.isSaving && !state.isOffline,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Submit") }
        }

        if (state.isSaving) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(32.dp))
            }
        }
    }

    if (state.showSubmitConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSubmit,
            title = { Text("Submit for approval?") },
            text = { Text("After submitting, the event can't be changed. Cancel and resubmit if plans change.") },
            confirmButton = { TextButton(onClick = viewModel::confirmSubmit) { Text("Submit") } },
            dismissButton = { TextButton(onClick = viewModel::dismissSubmit) { Text("Keep editing") } }
        )
    }
}

/** R1-S11: shows which event this was copied from and why it was rejected or cancelled. */
@Composable
private fun CopiedBanner(copied: CopiedFrom) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("copied_banner")
    ) {
        Text(
            buildString {
                append("Copied from \"${copied.title}\". This creates a new event; the original stays as it is.")
                copied.reason?.let { append("\nReason given: $it") }
            },
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    error: String?,
    tag: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    number: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = modifier.testTag(tag)
    )
}

/** Read-only field that opens a list of categories when tapped. */
@Composable
private fun CategoryField(value: String, error: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { Text("▾") },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth().testTag("category")
        )
        // Transparent layer so tapping anywhere on the field opens the menu
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EventValidation.CATEGORIES.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = { onSelect(category); expanded = false }
                )
            }
        }
    }
}