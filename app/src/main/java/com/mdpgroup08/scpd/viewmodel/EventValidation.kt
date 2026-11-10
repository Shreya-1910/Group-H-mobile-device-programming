package com.mdpgroup08.scpd.viewmodel

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** The raw text the club has typed into the Create event form. */
data class EventForm(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",      // dd/MM/yyyy
    val time: String = "",      // HH:mm
    val capacity: String = "",
    val points: String = ""
)

/** One error message per field, or null when the field is fine (R1-S1). */
data class EventFormErrors(
    val title: String? = null,
    val location: String? = null,
    val dateTime: String? = null,
    val capacity: String? = null,
    val points: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(title, location, dateTime, capacity, points).any { it != null }
}

/**
 * Business rules for a new event. Kept out of the screen so they can be unit tested.
 * The shared Event model only stores a start time (date), so there is no end-time check.
 */
object EventValidation {
    const val MIN_POINTS = 1
    const val MAX_POINTS = 10
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    /** Turns the date and time text into epoch millis, or null if it isn't a valid date. */
    fun parseDateTime(date: String, time: String, zone: ZoneId = ZoneId.systemDefault()): Long? = try {
        LocalDateTime.parse("${date.trim()} ${time.trim()}", formatter)
            .atZone(zone).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }

    fun validate(form: EventForm, now: Long, zone: ZoneId = ZoneId.systemDefault()): EventFormErrors {
        val start = parseDateTime(form.date, form.time, zone)
        val capacity = form.capacity.trim().toIntOrNull()
        val points = form.points.trim().toIntOrNull()

        return EventFormErrors(
            title = if (form.title.isBlank()) "Title is required" else null,
            location = if (form.location.isBlank()) "Location is required" else null,
            dateTime = when {
                form.date.isBlank() || form.time.isBlank() -> "Date and time are required"
                start == null -> "Use the format dd/MM/yyyy and HH:mm"
                start <= now -> "The start time must be in the future"
                else -> null
            },
            capacity = if (capacity == null || capacity < 1) "Capacity must be at least 1" else null,
            points = if (points == null || points !in MIN_POINTS..MAX_POINTS)
                "Points must be between $MIN_POINTS and $MAX_POINTS" else null
        )
    }
}
