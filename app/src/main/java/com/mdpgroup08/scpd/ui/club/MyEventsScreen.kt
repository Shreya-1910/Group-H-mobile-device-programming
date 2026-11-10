package com.mdpgroup08.scpd.ui.club

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.MyEventsUiState
import com.mdpgroup08.scpd.viewmodel.MyEventsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Club "My events" (R1-S3, R1-S6). */
@Composable
fun MyEventsScreen(
    onCreateEvent: () -> Unit = {},
    onEditDraft: (String) -> Unit = {},
    viewModel: MyEventsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("My events", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        when (val s = state) {
            MyEventsUiState.Loading -> Centered { CircularProgressIndicator() }
            is MyEventsUiState.Error -> Centered {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = viewModel::load) { Text("Retry") }
            }
            is MyEventsUiState.Success ->
                if (s.events.isEmpty()) Centered {
                    Text("No events yet. Tap Create event.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onCreateEvent) { Text("Create event") }
                } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.events, key = { it.id }) { event ->
                        ClubEventCard(event, onEdit = { onEditDraft(event.id) })
                    }
                }
        }
    }
}

@Composable
private fun ClubEventCard(event: Event, onEdit: () -> Unit) {
    val dateText = if (event.date > 0)
        SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault()).format(Date(event.date))
    else "Date not set"

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(event.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(dateText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${event.location} • Capacity ${event.capacity} • ${event.points} points",
                style = MaterialTheme.typography.bodySmall)

            when (event.status) {
                EventStatus.DRAFT -> TextButton(onClick = onEdit) { Text("Edit draft") }
                EventStatus.PENDING, EventStatus.APPROVED -> Text(
                    "Submitted events can't be changed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                else -> {}
            }
        }
    }
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
    }
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}
