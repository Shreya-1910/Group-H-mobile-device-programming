package com.mdpgroup08.scpd.ui.club

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.data.attendance.CheckInCodes
import com.mdpgroup08.scpd.data.qr.CheckInQrGenerator
import com.mdpgroup08.scpd.ui.components.CenteredContent
import com.mdpgroup08.scpd.ui.components.ErrorWithRetry
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.ShowCodeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Club device shows the rotating check-in code (R3-S1, R4-S23). */
@Composable
fun ShowCodeScreen(
    onBack: () -> Unit,
    onViewList: () -> Unit,
    viewModel: ShowCodeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val time = remember { SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()) }

    // R3-S1: keep the screen awake while the code is shown.
    val view = LocalView.current
    DisposableEffect(state.isShowing) {
        view.keepScreenOn = state.isShowing
        onDispose { view.keepScreenOn = false }
    }

    when {
        state.isLoading -> CenteredContent { CircularProgressIndicator() }
        state.errorMessage != null -> Column(Modifier.fillMaxSize()) {
            TextButton(onClick = onBack) { Text("Back") }
            ErrorWithRetry(state.errorMessage!!, onRetry = viewModel::load)
        }
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("Back") }
            Text(state.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                if (state.isWindowOpen) "Check-in open until ${time.format(Date(state.closesAt))}"
                else "Check-in opens ${time.format(Date(state.opensAt))} and closes ${time.format(Date(state.closesAt))}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // R3-S5: live count, with a link to the full list.
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (state.bookedCount > 0) "${state.verifiedCount} / ${state.bookedCount} verified"
                        else "${state.verifiedCount} verified",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("verifiedCount")
                    )
                    if (state.pendingCount > 0) {
                        Text("${state.pendingCount} being checked", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onViewList) { Text("View list") }
                }
            }

            if (state.isShowing && state.qrPayload != null) {
                val bitmap = remember(state.qrPayload) { CheckInQrGenerator.toBitmap(state.qrPayload!!) }
                Card(Modifier.fillMaxWidth().widthIn(max = 360.dp).aspectRatio(1f)) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Check-in QR code",
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    )
                }
                Text(
                    state.sixDigits.orEmpty().chunked(3).joinToString(" "),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("sixDigits")
                )
                Text("Can't scan? Type this code in the app", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(
                    progress = { state.secondsUntilNext / (CheckInCodes.ROTATION_MS / 1000f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("New code in ${state.secondsUntilNext} s", color = MaterialTheme.colorScheme.primary)
                OutlinedButton(
                    onClick = viewModel::hideCode,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Hide code") }
            } else {
                Button(
                    onClick = viewModel::showCode,
                    enabled = state.isWindowOpen,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("showCode")
                ) { Text("Show code") }
                if (!state.isWindowOpen) {
                    Text(
                        "Show code is available during the check-in window only.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Each time you show the code it is recorded in the session log.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
