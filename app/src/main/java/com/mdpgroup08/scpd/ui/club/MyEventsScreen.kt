package com.mdpgroup08.scpd.ui.club

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.ui.components.CenteredContent
import com.mdpgroup08.scpd.ui.components.ErrorWithRetry
import com.mdpgroup08.scpd.ui.components.OfflineBanner
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.CancelEventDialog
import com.mdpgroup08.scpd.viewmodel.ClubEventItem
import com.mdpgroup08.scpd.viewmodel.MyEventsUiState
import com.mdpgroup08.scpd.viewmodel.MyEventsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Club "My events" screen (R1-S3, R1-S6, R1-S8, R1-S9, R2-S13). */
@Composable
fun MyEventsScreen(
    onCreateEvent: () -> Unit = {},
    onEditDraft: (String) -> Unit = {},
    onGenerateQr: (String) -> Unit = {},
    viewModel: MyEventsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.cancelDialog.collectAsStateWithLifecycle()
    val isCancelling by viewModel.isCancelling.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("My events", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            if (isOffline) {
                OfflineBanner("Offline: showing saved data. Changes need a connection.")
                Spacer(Modifier.height(12.dp))
            }

            when (val s = state) {
                MyEventsUiState.Loading -> CenteredContent { CircularProgressIndicator() }
                is MyEventsUiState.Error -> ErrorWithRetry(s.message, viewModel::load)
                is MyEventsUiState.Success ->
                    if (s.events.isEmpty()) {
                        CenteredContent {
                            Text("No events yet. Tap Create event.")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onCreateEvent) { Text("Create event") }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(s.events, key = { it.event.id }) { item ->
                                ClubEventCard(
                                    item = item,
                                    onEdit = { onEditDraft(item.event.id) },
                                    onGenerateQr = { onGenerateQr(item.event.id) },
                                    onCancel = { viewModel.openCancel(item.event) }
                                )
                            }
                        }
                    }
            }
        }
    }

    dialog?.let {
        CancelDialog(
            dialog = it,
            isCancelling = isCancelling,
            onReasonChange = viewModel::onCancelReasonChange,
            onConfirm = viewModel::confirmCancel,
            onDismiss = viewModel::dismissCancel
        )
    }
}

@Composable
private fun ClubEventCard(
    item: ClubEventItem,
    onEdit: () -> Unit,
    onGenerateQr: () -> Unit,
    onCancel: () -> Unit
) {
    val event = item.event
    val dateText = if (event.startTime > 0) {
        val fmt = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
        val endFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        "${fmt.format(Date(event.startTime))} – ${endFmt.format(Date(event.endTime))}"
    } else "Date not set"

    Card(Modifier.fillMaxWidth().testTag("club_event_${event.id}")) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(event.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(dateText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${event.location} • Capacity ${event.capacity} • ${event.requestedPoints} points requested",
                style = MaterialTheme.typography.bodySmall
            )

            // R2-S13: how full the event is
            item.bookingSummary?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp).testTag("booking_summary_${event.id}")
                )
            }

            item.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.noteIsWarning) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row {
                // R1-S6: only drafts can be edited
                if (event.status == EventStatus.DRAFT) {
                    TextButton(onClick = onEdit) { Text("Edit draft") }
                }
                // Only approved events can generate check-in QR codes (Member 2)
                if (event.status == EventStatus.APPROVED) {
                    TextButton(onClick = onGenerateQr) { Text("Generate Check-in QR") }
                }
                // R1-S9: cancel an approved event that hasn't started
                if (item.canCancel) {
                    TextButton(onClick = onCancel, modifier = Modifier.testTag("cancel_event_${event.id}")) {
                        Text("Cancel event", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelDialog(
    dialog: CancelEventDialog,
    isCancelling: Boolean,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel \"${dialog.event.title}\"?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "It will be removed from the Marketplace and can't be undone. " +
                            "${dialog.event.bookedCount} booked students will see your reason."
                )
                OutlinedTextField(
                    value = dialog.reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason (shown to students)") },
                    minLines = 2,
                    enabled = !isCancelling,
                    modifier = Modifier.fillMaxWidth().testTag("cancel_reason")
                )
                dialog.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("cancel_error"))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isCancelling, modifier = Modifier.testTag("confirm_cancel_event")) {
                if (isCancelling) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Cancel event", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCancelling) { Text("Keep event") }
        }
    )
}

/** Colour + symbol + word, so status never relies on colour alone. */
@Composable
private fun StatusChip(status: EventStatus) {
    val (label, color) = when (status) {
        EventStatus.DRAFT -> "✎ Draft" to Color(0xFF6B6B6B)
        EventStatus.PENDING -> "◷ Pending" to Color(0xFFB45309)
        EventStatus.APPROVED -> "✓ Approved" to Color(0xFF2E7D32)
        EventStatus.REJECTED -> "✕ Rejected" to Color(0xFFC62828)
        EventStatus.CANCELLED -> "⊘ Cancelled" to Color(0xFF424242)
        EventStatus.EXPIRED -> "⌛ Expired" to Color(0xFF5D4037)
    }
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}