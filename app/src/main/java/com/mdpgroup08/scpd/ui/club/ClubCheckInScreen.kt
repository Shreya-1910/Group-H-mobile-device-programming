package com.mdpgroup08.scpd.ui.club

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.data.attendance.CheckInWindow
import com.mdpgroup08.scpd.ui.components.CenteredContent
import com.mdpgroup08.scpd.ui.components.ErrorWithRetry
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.ClubCheckInUiState
import com.mdpgroup08.scpd.viewmodel.ClubCheckInViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Club Check-in tab: pick an approved event to run check-in for. */
@Composable
fun ClubCheckInScreen(
    onOpenEvent: (String) -> Unit,
    title: String = "Check-in",
    subtitle: String = "Open your event while online before it starts, so check-in also works without a connection.",
    buttonLabel: String = "Open check-in",
    viewModel: ClubCheckInViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val time = remember { SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        when (val s = state) {
            ClubCheckInUiState.Loading -> CenteredContent { CircularProgressIndicator() }
            is ClubCheckInUiState.Error -> ErrorWithRetry(s.message, onRetry = viewModel::load)
            is ClubCheckInUiState.Success ->
                if (s.events.isEmpty()) CenteredContent {
                    Text("No approved events yet.")
                } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.events, key = { it.id }) { event ->
                        val now = System.currentTimeMillis()
                        val open = CheckInWindow.isOpen(event.startTime, event.endTime, now)
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(event.title, style = MaterialTheme.typography.titleMedium)
                                Text("${time.format(Date(event.startTime))} • ${event.location}")
                                Text(
                                    when {
                                        open -> "✓ Check-in open"
                                        now < event.startTime -> "◷ Opens ${time.format(Date(event.startTime))}"
                                        else -> "Check-in closed"
                                    },
                                    color = if (open) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { onOpenEvent(event.id) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) { Text(buttonLabel) }
                            }
                        }
                    }
                }
        }
    }
}
