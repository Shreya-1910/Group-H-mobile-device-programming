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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mdpgroup08.scpd.viewmodel.Availability
import com.mdpgroup08.scpd.viewmodel.MarketplaceItem
import com.mdpgroup08.scpd.viewmodel.MarketplaceList
import com.mdpgroup08.scpd.viewmodel.MarketplaceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Student Marketplace (R2-S1, R2-S9). Shows state only; all rules are in MarketplaceViewModel.
 * Tapping a card opens the event details (R2-S3).
 */
@Composable
fun MarketplaceScreen(
    onOpenEvent: (String) -> Unit = {},
    viewModel: MarketplaceViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Marketplace", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Approved events you can book, soonest first.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (state.isOffline) {
            OfflineBanner("Offline: showing saved data. Booking needs a connection.")
            Spacer(Modifier.height(12.dp))
        }

        when (val list = state.list) {
            MarketplaceList.Loading -> CenteredContent { CircularProgressIndicator() }
            is MarketplaceList.Error -> ErrorWithRetry(list.message, viewModel::load)
            is MarketplaceList.Loaded ->
                if (list.items.isEmpty()) {
                    CenteredContent {
                        Text(
                            if (state.isOffline) "No saved events on this phone. Connect to the internet to load events."
                            else "No upcoming events right now. Check back soon.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("marketplace_empty")
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(list.items, key = { it.event.id }) { item ->
                            MarketplaceCard(item, onClick = { onOpenEvent(item.event.id) })
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
        }
    }
}

@Composable
private fun MarketplaceCard(item: MarketplaceItem, onClick: () -> Unit) {
    val event = item.event
    val dateFormat = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("event_${event.id}")) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                PointsBadge(item.points)
            }
            Text(event.clubName.ifBlank { "Club" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("${dateFormat.format(Date(event.startTime))} – ${timeFormat.format(Date(event.endTime))}")
            Text(
                "${event.location} • ${event.category.ifBlank { "Other" }}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            AvailabilityText(item)
        }
    }
}

@Composable
private fun PointsBadge(points: Int) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
        Text(
            "★ $points pts",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Symbol + words, so availability never relies on colour alone. */
@Composable
private fun AvailabilityText(item: MarketplaceItem) {
    val (text, color) = when (item.availability) {
        Availability.OPEN -> "● ${item.placesLeft} places left" to MaterialTheme.colorScheme.primary
        Availability.FULL -> "✕ Full · join the waitlist" to MaterialTheme.colorScheme.error
        Availability.CLOSED -> "⊘ Booking closed" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
}