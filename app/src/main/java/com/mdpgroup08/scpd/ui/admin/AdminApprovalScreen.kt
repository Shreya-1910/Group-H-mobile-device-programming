package com.mdpgroup08.scpd.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.ui.components.CenteredContent
import com.mdpgroup08.scpd.ui.components.ErrorWithRetry
import com.mdpgroup08.scpd.ui.components.OfflineBanner
import com.mdpgroup08.scpd.viewmodel.AdminApprovalViewModel
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.ApprovalQueue
import com.mdpgroup08.scpd.viewmodel.DecisionDialog
import com.mdpgroup08.scpd.viewmodel.PendingEventItem
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Admin approval queue (R1-S4, R1-S5). Pending events, overdue first, with Approve and Reject.
 * All rules live in AdminApprovalViewModel; this screen only shows state and forwards taps.
 * Dialog state is in the ViewModel, so an open dialog survives screen rotation.
 */
@Composable
fun AdminApprovalScreen(
    viewModel: AdminApprovalViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Show "Approved …", "Rejected …" or "Another admin already decided" once
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Approval queue", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Review within 5 working days. Overdue events are listed first.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (state.isOffline) {
                OfflineBanner("You're offline. Approving and rejecting need a connection.")
                Spacer(Modifier.height(12.dp))
            }

            when (val queue = state.queue) {
                ApprovalQueue.Loading -> CenteredContent { CircularProgressIndicator() }
                is ApprovalQueue.Error -> ErrorWithRetry(queue.message, viewModel::load)
                is ApprovalQueue.Loaded ->
                    if (queue.items.isEmpty()) {
                        CenteredContent { Text("No events waiting for review.") }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(queue.items, key = { it.event.id }) { item ->
                                PendingEventCard(
                                    item = item,
                                    enabled = !state.isOffline,
                                    onApprove = { viewModel.openApprove(item.event) },
                                    onReject = { viewModel.openReject(item.event) }
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
            }
        }
    }

    when (val dialog = state.dialog) {
        is DecisionDialog.Approve -> ApproveDialog(
            dialog = dialog,
            saving = state.isSaving,
            onPointsChange = viewModel::onPointsChange,
            onNoteChange = viewModel::onNoteChange,
            onConfirm = viewModel::confirm,
            onDismiss = viewModel::dismissDialog
        )
        is DecisionDialog.Reject -> RejectDialog(
            dialog = dialog,
            saving = state.isSaving,
            onReasonChange = viewModel::onReasonChange,
            onConfirm = viewModel::confirm,
            onDismiss = viewModel::dismissDialog
        )
        null -> {}
    }
}

@Composable
private fun PendingEventCard(
    item: PendingEventItem,
    enabled: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val event = item.event
    val dateFormat = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val due = item.dueDate.format(DateTimeFormatter.ofPattern("EEE d MMM"))

    Card(Modifier.fillMaxWidth().testTag("pending_${event.id}")) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                DueChip(due, item.isOverdue)
            }
            Text(event.clubName.ifBlank { event.clubId }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("${dateFormat.format(Date(event.startTime))} – ${timeFormat.format(Date(event.endTime))}")
            Text(
                "${event.location} • ${event.category.ifBlank { "No category" }} • Capacity ${event.capacity}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${event.requestedPoints} points requested",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (event.description.isNotBlank()) {
                Text(
                    event.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("reject_${event.id}")
                ) { Text("Reject") }
                Button(
                    onClick = onApprove,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("approve_${event.id}")
                ) { Text("Approve") }
            }
        }
    }
}

/** Colour + symbol + word, so the deadline never relies on colour alone. */
@Composable
private fun DueChip(due: String, overdue: Boolean) {
    val (label, color) =
        if (overdue) "⚠ Overdue · $due" to Color(0xFFC62828)
        else "◷ Due $due" to Color(0xFF6B6B6B)
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(
            label, color = Color.White, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ApproveDialog(
    dialog: DecisionDialog.Approve,
    saving: Boolean,
    onPointsChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approve \"${dialog.event.title}\"?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("It will be published in the Marketplace. The points can't be changed once students attend.")
                OutlinedTextField(
                    value = dialog.points,
                    onValueChange = onPointsChange,
                    label = { Text("Points to award (1–${dialog.event.requestedPoints})") },
                    singleLine = true,
                    enabled = !saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("approve_points")
                )
                OutlinedTextField(
                    value = dialog.note,
                    onValueChange = onNoteChange,
                    label = { Text("Note for the audit log (optional)") },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("approve_note")
                )
                dialog.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("dialog_error"))
                }
            }
        },
        confirmButton = { ConfirmButton("Approve", saving, onConfirm) },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}

@Composable
private fun RejectDialog(
    dialog: DecisionDialog.Reject,
    saving: Boolean,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject \"${dialog.event.title}\"?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The club will see your reason and can create a corrected event.")
                OutlinedTextField(
                    value = dialog.reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason (shown to the club)") },
                    minLines = 3,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("reject_reason")
                )
                dialog.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("dialog_error"))
                }
            }
        },
        confirmButton = { ConfirmButton("Reject", saving, onConfirm) },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } }
    )
}

/** Confirm button that shows a small spinner while saving, so it can't be tapped twice. */
@Composable
private fun ConfirmButton(label: String, saving: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = !saving, modifier = Modifier.testTag("dialog_confirm")) {
        if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Text(label)
    }
}