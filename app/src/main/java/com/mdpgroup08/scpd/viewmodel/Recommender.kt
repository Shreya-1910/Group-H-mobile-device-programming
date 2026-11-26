package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Booking
import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus

/** One "For you" pick, with the reason shown on the card. */
data class Recommendation(val item: MarketplaceItem, val reason: String)

/**
 * R2-S11: picks events for a student from their own booking history, worked out on the phone.
 * Only events they can actually book are suggested. Pure Kotlin, so every rule is unit tested.
 */
object Recommender {
    const val MAX = 5

    /** A club the student keeps going back to says more than a broad category. */
    private const val CLUB_WEIGHT = 3
    private const val CATEGORY_WEIGHT = 2

    /**
     * @param upcoming approved upcoming events (the Marketplace)
     * @param myBookings the student's confirmed bookings
     * @param history the events those bookings are for, past and upcoming
     */
    fun recommend(
        upcoming: List<Event>,
        myBookings: List<Booking>,
        history: List<Event>,
        myId: String?,
        now: Long
    ): List<Recommendation> {
        val bookedIds = myBookings.filter { it.status == BookingStatus.CONFIRMED }.map { it.eventId }.toSet()
        val myUpcoming = upcoming.filter { it.id in bookedIds }

        val candidates = upcoming.filter { event ->
            event.status == EventStatus.APPROVED &&
                    event.startTime > now &&
                    !BookingRules.isBookingClosed(event, now) &&
                    !event.isFull &&
                    event.id !in bookedIds &&
                    (myId == null || myId !in event.waitlist) &&
                    myUpcoming.none { BookingRules.clashes(event, it) }
        }
        if (candidates.isEmpty()) return emptyList()

        val pastEvents = history.filter { it.id in bookedIds }
        val categoryCounts = pastEvents.groupingBy { categoryOf(it) }.eachCount()
        val clubCounts = pastEvents.filter { it.clubName.isNotBlank() }.groupingBy { it.clubName }.eachCount()

        val personal = candidates
            .map { event ->
                val clubs = clubCounts[event.clubName] ?: 0
                val categories = categoryCounts[categoryOf(event)] ?: 0
                Triple(event, clubs * CLUB_WEIGHT + categories * CATEGORY_WEIGHT, reasonFor(event, clubs, categories))
            }
            .filter { (_, score, _) -> score > 0 }
            .sortedWith(compareByDescending<Triple<Event, Int, String>> { it.second }.thenBy { it.first.startTime })
            .take(MAX)
            .map { (event, _, reason) -> Recommendation(toItem(event), reason) }

        // New students, or nothing similar coming up: suggest what's popular instead
        return personal.ifEmpty { popular(candidates) }
    }

    private fun popular(candidates: List<Event>): List<Recommendation> = candidates
        .sortedWith(compareByDescending<Event> { fillRatio(it) }.thenBy { it.startTime })
        .take(MAX)
        .map { event ->
            val percent = (fillRatio(event) * 100).toInt()
            val reason = if (percent > 0) "Popular: $percent% of places taken" else "Coming up soon"
            Recommendation(toItem(event), reason)
        }

    private fun reasonFor(event: Event, clubBookings: Int, categoryBookings: Int): String = when {
        clubBookings > 0 -> "From ${event.clubName}, which you've booked before"
        else -> "Because you booked $categoryBookings ${categoryOf(event)} event${if (categoryBookings == 1) "" else "s"}"
    }

    private fun categoryOf(event: Event) = event.category.ifBlank { "Other" }

    private fun fillRatio(event: Event): Double =
        if (event.capacity <= 0) 0.0 else event.bookedCount.toDouble() / event.capacity

    private fun toItem(event: Event) = MarketplaceItem(
        event = event,
        points = event.approvedPoints ?: event.requestedPoints,
        placesLeft = event.seatsLeft,
        availability = Availability.OPEN
    )
}