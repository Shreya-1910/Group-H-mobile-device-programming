package com.mdpgroup08.scpd.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.ui.components.CenteredContent
import com.mdpgroup08.scpd.ui.components.ErrorWithRetry
import com.mdpgroup08.scpd.ui.components.OfflineBanner
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.DetailsAction
import com.mdpgroup08.scpd.viewmodel.DetailsContent
import com.mdpgroup08.scpd.viewmodel.EventDetailsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Event details (R2-S3). The main button shows the student's state; all rules are in the ViewModel. */
@Composable
fun EventDetailsScreen(
    onBack: () -> Unit,
    viewModel: EventDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Marketplace")
                }
                Text("Event details", style = MaterialTheme.typography.titleLarge)
            }

            if (state.isOffline) {
                OfflineBanner(
                    "Offline: showing saved data. Booking needs a connection.",
                    Modifier.padding(horizontal = 16.dp)
                )
            }

            when (val content = state.content) {
                DetailsContent.Loading -> CenteredContent { CircularProgressIndicator() }
                DetailsContent.NotFound -> CenteredContent { Text("This event is no longer available.") }
                is DetailsContent.Error -> ErrorWithRetry(content.message, viewModel::load)
                is DetailsContent.Loaded -> DetailsBody(
                    content = content,
                    isOffline = state.isOffline,
                    isWorking = state.isWorking,
                    onBook = viewModel::book,
                    onSwap = viewModel::requestSwap
                )
            }
        }
    }

    // R2-S7: keep the existing booking, or swap it for this event in one step
    val loaded = state.content as? DetailsContent.Loaded
    val clash = loaded?.action as? DetailsAction.Clashes
    if (state.showSwapConfirm && loaded != null && clash != null) {
        AlertDialog(
            onDismissRequest = viewModel::keepExisting,
            title = { Text("Swap your booking?") },
            text = {
                Text(
                    "You'll give up your place at \"${clash.other.title}\" and book \"${loaded.event.title}\" " +
                            "instead. You can't hold both because they overlap."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSwap, modifier = Modifier.testTag("confirm_swap")) {
                    Text("Swap")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::keepExisting, modifier = Modifier.testTag("keep_existing")) {
                    Text("Keep my booking")
                }
            }
        )
    }
}

@Composable
private fun DetailsBody(
    content: DetailsContent.Loaded,
    isOffline: Boolean,
    isWorking: Boolean,
    onBook: () -> Unit,
    onSwap: () -> Unit
) {
    val event = content.event
    val dateFormat = SimpleDateFormat("EEEE d MMMM, h:mm a", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Column(Modifier.fillMaxSize()) {
        // Scrollable details, so long descriptions work in landscape too
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).testTag("details_title")
                )
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                    Text(
                        "★ ${content.points} pts",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(event.clubName.ifBlank { "Club" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            DetailRow("When", "${dateFormat.format(Date(event.startTime))} – ${timeFormat.format(Date(event.endTime))}")
            DetailRow("Where", event.location)
            DetailRow("Category", event.category.ifBlank { "Other" })
            DetailRow("Places", "${content.placesLeft} of ${event.capacity} left")
            if (event.description.isNotBlank()) {
                HorizontalDivider()
                Text(event.description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider()
        MainButton(content.action, isOffline, isWorking, onBook, onSwap)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.3f))
        Text(value, modifier = Modifier.weight(0.7f))
    }
}

/** R2-S3 AC1: Book, Booked ✓, Join waitlist, Clashes with [event] or Booking closed, plus a line explaining why. */
@Composable
private fun MainButton(
    action: DetailsAction,
    isOffline: Boolean,
    isWorking: Boolean,
    onBook: () -> Unit,
    onSwap: () -> Unit
) {
    // Which tap does something right now, if any
    val onClick: (() -> Unit)? = when {
        action == DetailsAction.Book -> onBook
        action is DetailsAction.Clashes && action.canSwap -> onSwap
        else -> null
    }

    val (label, note) = when (action) {
        DetailsAction.Book ->
            "Book a place" to if (isOffline) "Booking needs a connection." else "Booking closes 2 hours before the start."
        DetailsAction.Booked -> "Booked ✓" to "You have a place at this event."
        DetailsAction.JoinWaitlist -> "Join waitlist" to "This event is full. The waitlist is coming in the next update."
        is DetailsAction.Clashes ->
            if (action.canSwap) {
                "Swap for this event" to "Clashes with \"${action.other.title}\". Keep that booking or swap it for this one."
            } else {
                "Clashes with ${action.other.title}" to
                        "You're booked on \"${action.other.title}\" at the same time, and it can't be swapped now."
            }
        DetailsAction.BookingClosed -> "Booking closed" to "Booking closes 2 hours before the event starts."
        DetailsAction.Unavailable -> "Not available" to "This event can't be booked."
    }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        if (onClick != null) {
            Button(
                onClick = onClick,
                enabled = !isOffline && !isWorking,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("main_action")
            ) {
                if (isWorking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(label)
            }
        } else {
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("main_action")
            ) { Text(label) }
        }
        Text(
            note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp).testTag("main_action_note")
        )
    }
}