package com.mdpgroup08.scpd.ui.student

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.ui.components.CenteredContent
import com.mdpgroup08.scpd.ui.components.ErrorWithRetry
import com.mdpgroup08.scpd.ui.components.OfflineBanner
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.StudentEventItem
import com.mdpgroup08.scpd.viewmodel.StudentEventStatus
import com.mdpgroup08.scpd.viewmodel.StudentEventsList
import com.mdpgroup08.scpd.viewmodel.StudentEventsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Student "My events" (R4-S10). Tap an event to see details, cancel or leave the waitlist. */
@Composable
fun StudentEventsScreen(
    onOpenEvent: (String) -> Unit,
    onBrowse: () -> Unit,
    viewModel: StudentEventsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("My events", style = MaterialTheme.typography.headlineSmall)
        Text("Your upcoming bookings and waitlists.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        if (state.isOffline) {
            OfflineBanner("Offline: showing saved data. Changes need a connection.")
            Spacer(Modifier.height(12.dp))
        }

        when (val list = state.list) {
            StudentEventsList.Loading -> CenteredContent { CircularProgressIndicator() }
            is StudentEventsList.Error -> ErrorWithRetry(list.message, viewModel::load)
            is StudentEventsList.Loaded ->
                if (list.isEmpty) {
                    CenteredContent {
                        if (state.isOffline) {
                            // R4-S10 EX1: nothing saved on this phone yet
                            Text(
                                "Connect to the internet to load your events.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("my_events_empty")
                            )
                        } else {
                            Text(
                                "You haven't booked any upcoming events yet.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("my_events_empty")
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onBrowse) { Text("Browse the Marketplace") }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (list.booked.isNotEmpty()) {
                            item { SectionTitle("Booked (${list.booked.size})") }
                            items(list.booked, key = { "b_${it.event.id}" }) { StudentEventCard(it) { onOpenEvent(it.event.id) } }
                        }
                        if (list.waitlisted.isNotEmpty()) {
                            item { SectionTitle("On a waitlist (${list.waitlisted.size})") }
                            items(list.waitlisted, key = { "w_${it.event.id}" }) { StudentEventCard(it) { onOpenEvent(it.event.id) } }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun StudentEventCard(item: StudentEventItem, onClick: () -> Unit) {
    val event = item.event
    val dateFormat = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("my_event_${event.id}")) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(item.status)
            }
            Text(event.clubName.ifBlank { "Club" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("${dateFormat.format(Date(event.startTime))} – ${timeFormat.format(Date(event.endTime))}")
            Text("${event.location} • ★ ${item.points} pts", style = MaterialTheme.typography.bodySmall)
            val status = item.status
            if (status is StudentEventStatus.Booked && status.fromWaitlist) {
                Text(
                    "You got this place from the waitlist.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Colour + symbol + word, so status never relies on colour alone. */
@Composable
private fun StatusChip(status: StudentEventStatus) {
    val (label, color) = when (status) {
        is StudentEventStatus.Booked -> "✓ Booked" to Color(0xFF2E7D32)
        is StudentEventStatus.Waitlisted -> "⏳ Waitlist #${status.position}" to Color(0xFFB45309)
    }
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(
            label, color = Color.White, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}