package com.mdpgroup08.scpd.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.data.model.AttendanceStatus
import com.mdpgroup08.scpd.ui.components.CenteredContent
import com.mdpgroup08.scpd.ui.components.ErrorWithRetry
import com.mdpgroup08.scpd.ui.components.OfflineBanner
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.StudentCheckInUiState
import com.mdpgroup08.scpd.viewmodel.StudentCheckInViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Student Check-in tab (R3-S2, R3-S3, R3-S4). */
@Composable
fun StudentCheckInScreen(
    viewModel: StudentCheckInViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val time = remember { SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (!state.isOnline) {
            OfflineBanner("You're offline. Check-ins are saved on your phone and checked when you reconnect.")
            Spacer(Modifier.height(8.dp))
        }
        if (state.selected == null) EventPicker(state, time, viewModel)
        else CodeEntry(state, time, viewModel)
    }
}

@Composable
private fun EventPicker(state: StudentCheckInUiState, time: SimpleDateFormat, viewModel: StudentCheckInViewModel) {
    Text("Check in", style = MaterialTheme.typography.headlineSmall)
    Text("Choose the event you're at.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
    when {
        state.isLoading -> CenteredContent { CircularProgressIndicator() }
        state.loadError != null -> ErrorWithRetry(state.loadError!!, onRetry = viewModel::loadEvents)
        state.events.isEmpty() -> CenteredContent {
            Text("No booked events to check in to. Book an event in the Marketplace first.")
        }
        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.events, key = { it.event.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(item.event.title, style = MaterialTheme.typography.titleMedium)
                        Text("${time.format(Date(item.event.startTime))} • ${item.event.location}")
                        Text(
                            if (item.isOpen) "✓ Check-in open" else "◷ Check-in opens when the event starts",
                            color = if (item.isOpen) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.select(item.event) },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text("Check in") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeEntry(state: StudentCheckInUiState, time: SimpleDateFormat, viewModel: StudentCheckInViewModel) {
    val event = state.selected ?: return
    val checkIn = state.checkIn
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = viewModel::back) { Text("Back") }
        Text(event.title, style = MaterialTheme.typography.headlineSmall)
        Text("${time.format(Date(event.startTime))} • ${event.location}", color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Straight after tapping Check in: the code is being checked
        if (state.checking) {
            Card(Modifier.fillMaxWidth().testTag("checking")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text("Checking your code…", style = MaterialTheme.typography.titleMedium)
                    if (!state.isOnline) {
                        Text("You're offline. It's saved and will be checked when you reconnect.")
                    } else if (state.slowToConfirm) {
                        Text("Still waiting. The organiser's Show code screen must be open to check codes.")
                    }
                }
            }
        }

        // Result of the last check-in (R3-S2 AC1, R3-S4)
        if (!state.checking) checkIn?.let { c ->
            val (title, color) = when (c.status) {
                AttendanceStatus.VERIFIED -> "✓ Checked in" to Color(0xFF2E7D32)
                AttendanceStatus.PENDING -> "◷ Pending: being checked" to Color(0xFFB45309)
                AttendanceStatus.NEEDS_REVIEW -> "! Needs review" to Color(0xFF1565C0)
                AttendanceStatus.REJECTED -> "✕ ${c.reason ?: "Not checked in"}" to Color(0xFFC62828)
            }
            Card(
                Modifier.fillMaxWidth().testTag("checkInResult"),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = color, style = MaterialTheme.typography.titleMedium)
                    if (c.status != AttendanceStatus.REJECTED) c.reason?.let { Text(it) }
                    c.nextStep?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (c.status == AttendanceStatus.PENDING && !c.online) {
                        Text("Saved on your phone. It will be checked when you're back online.")
                    }
                    if (c.status == AttendanceStatus.PENDING && state.slowToConfirm) {
                        Text("Still waiting for the organiser's phone to confirm. Keep this screen open.")
                    }
                }
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("checkInError"))
        }

        if (checkIn?.status != AttendanceStatus.VERIFIED) {
            Text("Type the 6-digit code shown on the organiser's screen.")
            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("6-digit code") },
                singleLine = true,
                isError = state.formatError != null,
                supportingText = state.formatError?.let { { Text(it) } },
                enabled = state.lockoutSeconds == 0,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().testTag("codeField")
            )
            if (state.lockoutSeconds > 0) {
                Text(
                    "Too many incorrect codes. Try again in ${state.lockoutSeconds} s.",
                    color = MaterialTheme.colorScheme.error
                )
            } else if (state.attemptsRemaining < 5) {
                Text("${state.attemptsRemaining} attempts left", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = viewModel::submit,
                enabled = state.lockoutSeconds == 0 &&
                    ((!state.checking && checkIn?.status != AttendanceStatus.PENDING) || state.slowToConfirm),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("checkInButton")
            ) { Text("Check in") }
        }
    }
}
