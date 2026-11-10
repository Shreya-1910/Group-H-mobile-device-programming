package com.mdpgroup08.scpd.ui.club

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.CreateEventViewModel

/** Create event (R1-S1, R1-S2). Pass a draft's id to edit it. */
@Composable
fun CreateEventScreen(
    draftId: String = "",
    onSaved: () -> Unit = {},
    viewModel: CreateEventViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val f = state.form
    val e = state.errors

    LaunchedEffect(draftId) { viewModel.loadDraft(draftId) }
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
            if (state.editingId.isBlank()) "Create event" else "Edit draft",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Saved as a Draft until you submit it for approval.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Field("Title", f.title, e.title, "title") { viewModel.onFormChange(f.copy(title = it)) }
        Field("Description", f.description, null, "description", singleLine = false) {
            viewModel.onFormChange(f.copy(description = it))
        }
        Field("Location", f.location, e.location, "location") { viewModel.onFormChange(f.copy(location = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Date (dd/MM/yyyy)", f.date, e.dateTime, "date", Modifier.weight(1f)) {
                viewModel.onFormChange(f.copy(date = it))
            }
            Field("Time (HH:mm)", f.time, null, "time", Modifier.weight(1f)) {
                viewModel.onFormChange(f.copy(time = it))
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
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Save draft") }
            Button(
                onClick = viewModel::requestSubmit,
                enabled = !state.isSaving,
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
