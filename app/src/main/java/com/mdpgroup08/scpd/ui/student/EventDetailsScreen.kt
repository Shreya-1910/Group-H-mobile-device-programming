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

/** Event details (R2-S3) with book, waitlist, swap and cancel. All rules are in the ViewModel. */
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
                    actions = ButtonActions(
                        book = viewModel::book,
                        swap = viewModel::requestSwap,
                        cancel = viewModel::requestCancel,
                        joinWaitlist = viewModel::joinWaitlist,
                        leaveWaitlist = viewModel::leaveWaitlist
                    )
                )
            }
        }
    }

    val loaded = state.content as? DetailsContent.Loaded

    // R2-S7: keep the existing booking, or swap it for this event in one step
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

    // R2-S8: cancel with confirmation, so a place isn't given away by accident
    if (state.showCancelConfirm && loaded != null) {
        AlertDialog(
            onDismissRequest = viewModel::keepBooking,
            title = { Text("Cancel your booking?") },
            text = {
                Text(
                    "Your place at \"${loaded.event.title}\" will go to the next student on the waitlist. " +
                            "You can book again later if places are still free."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmCancel, modifier = Modifier.testTag("confirm_cancel")) {
                    Text("Cancel booking")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::keepBooking, modifier = Modifier.testTag("keep_booking")) {
                    Text("Keep booking")
                }
            }
        )
    }
}

/** The taps the main button can make. */
private data class ButtonActions(
    val book: () -> Unit,
    val swap: () -> Unit,
    val cancel: () -> Unit,
    val joinWaitlist: () -> Unit,
    val leaveWaitlist: () -> Unit
)

@Composable
private fun DetailsBody(
    content: DetailsContent.Loaded,
    isOffline: Boolean,
    isWorking: Boolean,
    actions: ButtonActions
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
            if (event.waitlist.isNotEmpty()) DetailRow("Waitlist", "${event.waitlist.size} waiting")
            if (event.description.isNotBlank()) {
                HorizontalDivider()
                Text(event.description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider()
        MainButton(content, isOffline, isWorking, actions)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.3f))
        Text(value, modifier = Modifier.weight(0.7f))
    }
}

/** What the button area shows for each state. */
private data class ButtonSpec(
    val status: String? = null,
    val label: String,
    val note: String,
    val onClick: (() -> Unit)? = null,
    val primary: Boolean = true
)

/** R2-S3 AC1: one main button for the student's state, a status line, and a note explaining why. */
@Composable
private fun MainButton(
    content: DetailsContent.Loaded,
    isOffline: Boolean,
    isWorking: Boolean,
    actions: ButtonActions
) {
    val spec = when (val action = content.action) {
        DetailsAction.Book -> ButtonSpec(
            label = "Book a place",
            note = when {
                content.leavesWaitlists.isNotEmpty() ->
                    "Booking this removes you from the waitlist for \"${content.leavesWaitlists.first().title}\", which overlaps it."
                isOffline -> "Booking needs a connection."
                else -> "Booking closes 2 hours before the start."
            },
            onClick = actions.book
        )
        DetailsAction.Booked -> ButtonSpec(
            status = "✓ You're booked",
            label = "Cancel booking",
            note = if (content.canCancel) "You can cancel until 2 hours before the start."
            else "Cancellation closed 2 hours before the event.",
            onClick = if (content.canCancel) actions.cancel else null,
            primary = false
        )
        DetailsAction.JoinWaitlist -> ButtonSpec(
            label = "Join waitlist",
            note = "This event is full. If a place frees up, the first student in line is booked automatically.",
            onClick = actions.joinWaitlist
        )
        is DetailsAction.OnWaitlist -> ButtonSpec(
            status = "⏳ On the waitlist: #${action.position}",
            label = "Leave waitlist",
            note = if (action.isClosed) "The waitlist closed 2 hours before the start without a place becoming free."
            else "You'll be booked automatically if a place frees up while you're first in line.",
            onClick = if (action.isClosed) null else actions.leaveWaitlist,
            primary = false
        )
        is DetailsAction.Clashes ->
            if (action.canSwap) ButtonSpec(
                label = "Swap for this event",
                note = "Clashes with \"${action.other.title}\". Keep that booking or swap it for this one.",
                onClick = actions.swap
            ) else ButtonSpec(
                label = "Clashes with ${action.other.title}",
                note = "You're booked on \"${action.other.title}\" at the same time, and it can't be swapped now."
            )
        DetailsAction.BookingClosed -> ButtonSpec(
            label = "Booking closed",
            note = "Booking and the waitlist close 2 hours before the event starts."
        )
        DetailsAction.Unavailable -> ButtonSpec(label = "Not available", note = "This event can't be booked.")
    }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        spec.status?.let {
            Text(
                it,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp).testTag("booking_status")
            )
        }
        val enabled = spec.onClick != null && !isOffline && !isWorking
        val modifier = Modifier.fillMaxWidth().height(52.dp).testTag("main_action")
        if (spec.primary && spec.onClick != null) {
            Button(onClick = spec.onClick, enabled = enabled, modifier = modifier) {
                if (isWorking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(spec.label)
            }
        } else {
            OutlinedButton(onClick = { spec.onClick?.invoke() }, enabled = enabled, modifier = modifier) {
                if (isWorking && spec.onClick != null) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(spec.label)
            }
        }
        Text(
            spec.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp).testTag("main_action_note")
        )
    }
}