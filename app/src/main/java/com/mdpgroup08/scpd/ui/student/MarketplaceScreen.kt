package com.mdpgroup08.scpd.ui.student

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.ui.components.CenteredContent
import com.mdpgroup08.scpd.ui.components.ErrorWithRetry
import com.mdpgroup08.scpd.ui.components.OfflineBanner
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.Availability
import com.mdpgroup08.scpd.viewmodel.DateFilter
import com.mdpgroup08.scpd.viewmodel.EventValidation
import com.mdpgroup08.scpd.viewmodel.ForYouViewModel
import com.mdpgroup08.scpd.viewmodel.MarketplaceFilters
import com.mdpgroup08.scpd.viewmodel.MarketplaceItem
import com.mdpgroup08.scpd.viewmodel.MarketplaceList
import com.mdpgroup08.scpd.viewmodel.MarketplaceViewModel
import com.mdpgroup08.scpd.viewmodel.Recommendation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Student Marketplace (R2-S1, R2-S2, R2-S9, R2-S11). Shows state only; all rules are in the ViewModels.
 * Tapping a card opens the event details (R2-S3).
 */
@Composable
fun MarketplaceScreen(
    onOpenEvent: (String) -> Unit = {},
    viewModel: MarketplaceViewModel = viewModel(factory = AppViewModelProvider.Factory),
    forYouViewModel: ForYouViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recommendations by forYouViewModel.recommendations.collectAsStateWithLifecycle()
    val filters = state.filters

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Marketplace", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        SearchField(query = filters.query, onQueryChange = viewModel::onQueryChange)
        Spacer(Modifier.height(8.dp))
        FilterRow(
            filters = filters,
            clubs = state.clubs,
            onCategory = viewModel::setCategory,
            onDate = viewModel::setDate,
            onPoints = viewModel::setMinPoints,
            onClub = viewModel::setClub,
            onAvailableOnly = viewModel::setAvailableOnly,
            onClear = viewModel::clearFilters
        )
        Spacer(Modifier.height(8.dp))

        if (state.isOffline) {
            OfflineBanner("Offline: showing saved data. Booking needs a connection.")
            Spacer(Modifier.height(8.dp))
        }

        when (val list = state.list) {
            MarketplaceList.Loading -> CenteredContent { CircularProgressIndicator() }
            is MarketplaceList.Error -> ErrorWithRetry(list.message, viewModel::load)
            is MarketplaceList.Loaded -> when {
                list.total == 0 -> CenteredContent {
                    Text(
                        if (state.isOffline) "No saved events on this phone. Connect to the internet to load events."
                        else "No upcoming events right now. Check back soon.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("marketplace_empty")
                    )
                }
                // R2-S2 AC2: nothing matches, so suggest clearing the filters
                list.items.isEmpty() -> CenteredContent {
                    Text(
                        "No events match your search.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("marketplace_no_match")
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::clearFilters, modifier = Modifier.testTag("clear_filters")) {
                        Text("Clear search and filters")
                    }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // R2-S11: shown only when the student isn't searching, so it doesn't crowd the results
                    if (!filters.isActive && recommendations.isNotEmpty()) {
                        item(key = "for_you") { ForYouRow(recommendations, onOpenEvent) }
                        item(key = "all_title") {
                            Text("All events", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    if (filters.isActive) {
                        item(key = "count") {
                            Text(
                                "${list.items.size} of ${list.total} events",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(list.items, key = { it.event.id }) { item ->
                        MarketplaceCard(item, onClick = { onOpenEvent(item.event.id) })
                    }
                    item(key = "bottom_space") { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/** R2-S11: up to 5 picks, each saying why it was chosen. */
@Composable
private fun ForYouRow(recommendations: List<Recommendation>, onOpenEvent: (String) -> Unit) {
    Column(Modifier.testTag("for_you")) {
        Text("For you", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recommendations, key = { "rec_${it.item.event.id}" }) { rec ->
                RecommendationCard(rec, onClick = { onOpenEvent(rec.item.event.id) })
            }
        }
    }
}

@Composable
private fun RecommendationCard(rec: Recommendation, onClick: () -> Unit) {
    val event = rec.item.event
    val dateFormat = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.width(240.dp).testTag("rec_${event.id}")
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                event.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(dateFormat.format(Date(event.startTime)), style = MaterialTheme.typography.bodySmall)
            Text("★ ${rec.item.points} pts • ${rec.item.placesLeft} places left", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                rec.reason,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search events, clubs or venues") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = Modifier.fillMaxWidth().testTag("search")
    )
}

/** Category, date, points, club and availability filters in one scrollable row (R2-S2 AC1). */
@Composable
private fun FilterRow(
    filters: MarketplaceFilters,
    clubs: List<String>,
    onCategory: (String?) -> Unit,
    onDate: (DateFilter) -> Unit,
    onPoints: (Int?) -> Unit,
    onClub: (String?) -> Unit,
    onAvailableOnly: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropdownChip(
            label = "Category",
            selectedLabel = filters.category,
            options = listOf<Pair<String, String?>>("Any category" to null) +
                    EventValidation.CATEGORIES.map { it to it },
            onSelect = onCategory,
            tag = "filter_category"
        )
        DropdownChip(
            label = "Date",
            selectedLabel = filters.date.takeIf { it != DateFilter.ANY }?.label,
            options = DateFilter.entries.map { it.label to it },
            onSelect = onDate,
            tag = "filter_date"
        )
        DropdownChip(
            label = "Points",
            selectedLabel = filters.minPoints?.let { "$it+ pts" },
            options = listOf<Pair<String, Int?>>("Any points" to null) +
                    MarketplaceFilters.POINTS_OPTIONS.map { "$it+ pts" to it },
            onSelect = onPoints,
            tag = "filter_points"
        )
        if (clubs.isNotEmpty()) {
            DropdownChip(
                label = "Club",
                selectedLabel = filters.club,
                options = listOf<Pair<String, String?>>("Any club" to null) + clubs.map { it to it },
                onSelect = onClub,
                tag = "filter_club"
            )
        }
        FilterChip(
            selected = filters.availableOnly,
            onClick = { onAvailableOnly(!filters.availableOnly) },
            label = { Text(if (filters.availableOnly) "✓ Available" else "Available") },
            modifier = Modifier.testTag("filter_available")
        )
        if (filters.isActive) {
            TextButton(onClick = onClear, modifier = Modifier.testTag("filter_clear")) {
                Text("Clear (${filters.activeCount})")
            }
        }
    }
}

/** A filter chip that opens a menu of options. */
@Composable
private fun <T> DropdownChip(
    label: String,
    selectedLabel: String?,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
    tag: String
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selectedLabel != null,
            onClick = { expanded = true },
            label = { Text(selectedLabel ?: label) },
            trailingIcon = { Text("▾") },
            modifier = Modifier.testTag(tag)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (text, value) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onSelect(value); expanded = false }
                )
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