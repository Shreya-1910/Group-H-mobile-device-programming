package com.mdpgroup08.scpd.viewmodel

import java.time.Instant
import java.time.ZoneId

/** Date ranges a student can filter by (R2-S2). */
enum class DateFilter(val label: String) {
    ANY("Any date"),
    TODAY("Today"),
    NEXT_7_DAYS("Next 7 days"),
    NEXT_30_DAYS("Next 30 days")
}

/** Everything the student has typed or chosen to narrow the Marketplace (R2-S2 AC1). */
data class MarketplaceFilters(
    val query: String = "",
    val category: String? = null,
    val date: DateFilter = DateFilter.ANY,
    val minPoints: Int? = null,
    val club: String? = null,
    val availableOnly: Boolean = false
) {
    /** How many filters are on, shown on the Clear button. The search text counts as one. */
    val activeCount: Int
        get() = listOf(
            query.isNotBlank(), category != null, date != DateFilter.ANY,
            minPoints != null, club != null, availableOnly
        ).count { it }

    val isActive: Boolean get() = activeCount > 0

    companion object {
        /** Options in the Points filter: "3+ pts", "5+ pts", "8+ pts". */
        val POINTS_OPTIONS = listOf(3, 5, 8)
    }
}

/** Pure matching rules for search and filters, kept out of the ViewModel so they're easy to unit test. */
object MarketplaceSearch {
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun apply(
        items: List<MarketplaceItem>, filters: MarketplaceFilters,
        now: Long, zone: ZoneId = ZoneId.systemDefault()
    ): List<MarketplaceItem> = items.filter { matches(it, filters, now, zone) }

    fun matches(
        item: MarketplaceItem, filters: MarketplaceFilters,
        now: Long, zone: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        val event = item.event
        val category = event.category.ifBlank { "Other" }

        // Every word typed must appear somewhere in the title, description, club, venue or category
        val words = filters.query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val searchable = listOf(event.title, event.description, event.clubName, event.location, category)
            .joinToString(" ").lowercase()
        if (words.any { it !in searchable }) return false

        if (filters.category != null && !category.equals(filters.category, ignoreCase = true)) return false
        if (filters.club != null && event.clubName != filters.club) return false
        if (filters.minPoints != null && item.points < filters.minPoints) return false
        if (filters.availableOnly && item.availability != Availability.OPEN) return false

        return when (filters.date) {
            DateFilter.ANY -> true
            DateFilter.TODAY -> sameDay(event.startTime, now, zone)
            DateFilter.NEXT_7_DAYS -> event.startTime <= now + 7 * DAY_MS
            DateFilter.NEXT_30_DAYS -> event.startTime <= now + 30 * DAY_MS
        }
    }

    private fun sameDay(a: Long, b: Long, zone: ZoneId): Boolean =
        Instant.ofEpochMilli(a).atZone(zone).toLocalDate() == Instant.ofEpochMilli(b).atZone(zone).toLocalDate()
}