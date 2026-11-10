package com.mdpgroup08.scpd.ui.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.repository.FakeEventRepository

@Composable
fun MarketplaceScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Events", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(FakeEventRepository.getEvents()) { event -> EventCard(event) }
        }
    }
}

@Composable
fun EventCard(event: Event) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text("${event.club} • ${event.date}")
            Spacer(Modifier.height(8.dp))
            Text("${event.points} s-CPD points")
            Text(
                if (event.seatsLeft == 0) "Event full" else "${event.seatsLeft} places left",
                color = if (event.seatsLeft == 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { }, enabled = event.seatsLeft > 0) {
                Text(if (event.seatsLeft == 0) "Full" else "Book")
            }
        }
    }
}
