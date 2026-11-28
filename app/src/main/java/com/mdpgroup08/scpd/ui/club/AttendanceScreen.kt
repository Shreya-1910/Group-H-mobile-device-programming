package com.mdpgroup08.scpd.ui.club

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.data.attendance.ReviewRules
import com.mdpgroup08.scpd.data.model.AttendanceStatus
import com.mdpgroup08.scpd.ui.components.CenteredContent
import com.mdpgroup08.scpd.ui.components.ErrorWithRetry
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.AttendanceRow
import com.mdpgroup08.scpd.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Club attendance list for one event (R3-S5, R3-S6). */
@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    viewModel: AttendanceViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            kotlinx.coroutines.delay(3_000)
            viewModel.messageShown()
        }
    }

    when {
        state.isLoading -> CenteredContent { CircularProgressIndicator() }
        state.errorMessage != null -> Column(Modifier.fillMaxSize()) {
            TextButton(onClick = onBack) { Text("Back") }
            ErrorWithRetry(state.errorMessage!!, onRetry = viewModel::load)
        }
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TextButton(onClick = onBack) { Text("Back") }
                Text(state.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${state.verifiedCount} / ${state.bookedCount} verified",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("attendanceCount")
                )
                state.message?.let { Text("✓ $it", color = Color(0xFF2E7D32)) }
            }
            if (state.rows.isEmpty()) {
                item { Text("No bookings yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.rows, key = { it.studentId }) { row ->
                AttendanceRowCard(
                    row,
                    onMarkPresent = { viewModel.startMarking(row) },
                    onApprove = { viewModel.startReview(row, approve = true) },
                    onReject = { viewModel.startReview(row, approve = false) }
                )
            }
            if (state.scansPerDisplay.isNotEmpty()) {
                item {
                    Text("Session log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
                items(state.scansPerDisplay, key = { it.first.id }) { (d, scans) ->
                    val end = d.endedAt?.let { time.format(Date(it)) } ?: "now"
                    Text("Shown ${time.format(Date(d.startedAt))}–$end: $scans check-ins")
                }
            }
        }
    }

    state.marking?.let { row ->
        AlertDialog(
            onDismissRequest = viewModel::cancelMarking,
            title = { Text("Mark ${row.name} present?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This is saved in the audit log with your name.")
                    OutlinedTextField(
                        value = state.markReason,
                        onValueChange = viewModel::onReasonChange,
                        label = { Text("Reason") },
                        isError = state.markError != null,
                        supportingText = state.markError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth().testTag("markReason")
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmMarkPresent, enabled = !state.isSaving) { Text("Mark present") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelMarking) { Text("Cancel") } }
        )
    }

    // R4-S18: approve or reject a Needs review check-in
    state.reviewing?.let { row ->
        val approve = state.reviewApprove
        AlertDialog(
            onDismissRequest = viewModel::cancelReview,
            title = { Text(if (approve) "Approve ${row.name}?" else "Reject ${row.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Needs review: ${row.checkIn?.reason ?: ""}")
                    Text(
                        if (approve) "They will be marked Verified. Saved with your name in the audit log."
                        else "They will see your reason and can appeal. Saved with your name in the audit log."
                    )
                    if (!approve) {
                        OutlinedTextField(
                            value = state.reviewReason,
                            onValueChange = viewModel::onReviewReasonChange,
                            label = { Text("Reason for the student") },
                            isError = state.reviewError != null,
                            modifier = Modifier.fillMaxWidth().testTag("rejectReason")
                        )
                    }
                    state.reviewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReview, enabled = !state.isSaving) {
                    Text(if (approve) "Approve" else "Reject")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelReview) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AttendanceRowCard(
    row: AttendanceRow,
    onMarkPresent: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val status = row.checkIn?.status
    val (label, color) = when (status) {
        AttendanceStatus.VERIFIED -> "Verified" to Color(0xFF2E7D32)
        AttendanceStatus.PENDING -> "Pending" to Color(0xFFB45309)
        AttendanceStatus.NEEDS_REVIEW -> "Needs review" to Color(0xFF1565C0)
        AttendanceStatus.REJECTED -> "Rejected" to Color(0xFFC62828)
        null -> "Not yet" to Color(0xFF616161)
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.titleSmall)
                row.checkIn?.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                row.checkIn?.manualReason?.let { Text("Manual: $it", style = MaterialTheme.typography.bodySmall) }
            }
            Text(label, color = color, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp))
            if (status != AttendanceStatus.VERIFIED && status != AttendanceStatus.NEEDS_REVIEW) {
                TextButton(onClick = onMarkPresent) { Text("Mark present") }
            }
        }
        // R4-S18 AC1 / EX5: who decides a Needs review check-in
        if (status == AttendanceStatus.NEEDS_REVIEW) {
            when (ReviewRules.whoDecides(row.checkIn?.reason)) {
                ReviewRules.Who.CLUB -> Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onReject, modifier = Modifier.testTag("rejectButton")) {
                        Text("Reject", color = Color(0xFFC62828))
                    }
                    TextButton(onClick = onApprove, modifier = Modifier.testTag("approveButton")) { Text("Approve") }
                }
                ReviewRules.Who.WAITING -> Text(
                    "Waiting for the organiser's phone to sync",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )
                ReviewRules.Who.ADMIN -> Text(
                    "With s-CPD admin",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )
            }
        }
    }
}
