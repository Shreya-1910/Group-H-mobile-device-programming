package com.mdpgroup08.scpd.ui.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.repository.EventRepository
import com.mdpgroup08.scpd.data.repository.FakeEventRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MarketplaceScreen(

    repository: EventRepository = FakeEventRepository()
) {
    val events by repository.getApprovedUpcomingEvents().collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Events", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(events, key = { it.id }) { event -> EventCard(event) }
        }
    }
}

@Composable
fun EventCard(event: Event) {
    val dateText = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(event.date))
    val full = event.seatsLeft <= 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text("${event.clubId} • $dateText")
            Spacer(Modifier.height(8.dp))
            Text("${event.points} s-CPD points")
            Text(
                if (full) "Event full" else "${event.seatsLeft} places left",
                color = if (full) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {  }, enabled = !full) {
                Text(if (full) "Full" else "Book")
            }
        }
    }
}