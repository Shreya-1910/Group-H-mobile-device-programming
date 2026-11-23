package com.mdpgroup08.scpd.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.ApprovalQueueViewModel
import com.mdpgroup08.scpd.viewmodel.ReviewDeadline
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun ApprovalQueueScreen(
    viewModel: ApprovalQueueViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        // Offline Airplane mode banner (Item 4 in image)
        if (state.isOffline) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("offline_banner")
            ) {
                Text(
                    "You're offline. You can review the queue, but approving and rejecting need a connection.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.pendingEvents.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Approval queue is empty", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.pendingEvents, key = { it.id }) { event ->
                    PendingEventCard(
                        event = event,
                        isOffline = state.isOffline,
                        isSaving = state.isSaving,
                        onApprove = { viewModel.openApproveDialog(event) },
                        onReject = { viewModel.openRejectDialog(event) }
                    )
                }
            }
        }
    }

    // Approve Dialog (Rotates screen & keeps dialog open with ViewModel state)
    state.selectedEventForApprove?.let { event ->
        AlertDialog(
            onDismissRequest = viewModel::dismissApproveDialog,
            title = { Text("Approve event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Requested points: ${event.requestedPoints}")
                    OutlinedTextField(
                        value = state.approvedPointsInput,
                        onValueChange = viewModel::onApprovedPointsChange,
                        label = { Text("Approved points") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.approveReasonInput,
                        onValueChange = viewModel::onApproveReasonChange,
                        label = { Text("Note / Reason (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.approveError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmApprove,
                    enabled = !state.isSaving && !state.isOffline
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissApproveDialog) { Text("Cancel") }
            }
        )
    }

    // Reject Dialog
    state.selectedEventForReject?.let { event ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRejectDialog,
            title = { Text("Reject event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Event: ${event.title}")
                    OutlinedTextField(
                        value = state.rejectReasonInput,
                        onValueChange = viewModel::onRejectReasonChange,
                        label = { Text("Reason for rejection (Required)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.rejectError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmReject,
                    enabled = !state.isSaving && !state.isOffline,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRejectDialog) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PendingEventCard(
    event: Event,
    isOffline: Boolean,
    isSaving: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault()) }
    val dateText = if (event.startTime > 0) dateFmt.format(Date(event.startTime)) else "Date not set"
    val dueFormat = remember { DateTimeFormatter.ofPattern("EEE d MMM") }
    val dueText = event.submittedAt?.let {
        "Due ${ReviewDeadline.dueDate(it).format(dueFormat)}"
    } ?: "Due soon"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        dueText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(event.clubName.ifBlank { "Club" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${event.location} • Capacity ${event.capacity} • ${event.requestedPoints} points requested", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    enabled = !isOffline && !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Approve")
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isOffline && !isSaving,
                    colors = ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}
