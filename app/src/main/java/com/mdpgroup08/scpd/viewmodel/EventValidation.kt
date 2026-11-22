package com.mdpgroup08.scpd.viewmodel

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** The raw text the club has typed into the Create event form. */
data class EventForm(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val date: String = "",      // dd/MM/yyyy
    val time: String = "",      // start time, HH:mm
    val endTime: String = "",   // end time, HH:mm, same day as the start
    val capacity: String = "",
    val points: String = ""
)

/** One error message per field, or null when the field is fine (R1-S1). */
data class EventFormErrors(
    val title: String? = null,
    val category: String? = null,
    val location: String? = null,
    val date: String? = null,
    val dateTime: String? = null,
    val time: String? = null,
    val endTime: String? = null,
    val capacity: String? = null,
    val points: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(title, category, location, date, dateTime, time, endTime, capacity, points).any { it != null }
}

/**
 * Business rules for a new event. Kept out of the screen so they can be unit tested.
 * Assumption: an event starts and ends on the same day.
 */
object EventValidation {
    const val MIN_POINTS = 1
    const val MAX_POINTS = 10

    /** Categories used by Marketplace filters (R2-S2) and points by category (R5-S10). */
    val CATEGORIES = listOf("Technology", "Career", "Community", "Sports", "Arts & Culture", "Leadership", "Other")

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Turns the date and time text into epoch millis, or null if it isn't a valid date. */
    fun parseDateTime(date: String, time: String, zone: ZoneId = ZoneId.systemDefault()): Long? = try {
        LocalDateTime.parse("${date.trim()} ${time.trim()}", dateTimeFormatter)
            .atZone(zone).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }

    private fun isValidDate(date: String): Boolean = try {
        LocalDate.parse(date.trim(), dateFormatter); true
    } catch (e: DateTimeParseException) {
        false
    }

    private fun isValidTime(time: String): Boolean = try {
        LocalTime.parse(time.trim(), timeFormatter); true
    } catch (e: DateTimeParseException) {
        false
    }

    fun validate(form: EventForm, now: Long, zone: ZoneId = ZoneId.systemDefault()): EventFormErrors {
        val dateBlank = form.date.isBlank()
        val dateValid = isValidDate(form.date)
        val timeBlank = form.time.isBlank()
        val timeValid = isValidTime(form.time)

        val dateError = when {
            dateBlank -> "Date is required"
            !dateValid -> "Use the format dd/MM/yyyy"
            else -> null
        }

        val start = if (dateValid && timeValid) parseDateTime(form.date, form.time, zone) else null
        val end = if (dateValid && isValidTime(form.endTime)) parseDateTime(form.date, form.endTime, zone) else null

        val timeError = when {
            timeBlank -> "Start time is required"
            !timeValid -> "Use the format HH:mm"
            start != null && start <= now -> "The start time must be in the future"
            else -> null
        }

        val dateTimeError = when {
            dateBlank && timeBlank -> "Date and start time are required"
            !dateValid -> "Use the format dd/MM/yyyy and HH:mm"
            start != null && start <= now -> "The start time must be in the future"
            else -> timeError
        }

        val capacity = form.capacity.trim().toIntOrNull()
        val points = form.points.trim().toIntOrNull()

        return EventFormErrors(
            title = if (form.title.isBlank()) "Title is required" else null,
            category = if (form.category !in CATEGORIES) "Choose a category" else null,
            location = if (form.location.isBlank()) "Location is required" else null,
            date = dateError,
            dateTime = dateTimeError,
            time = timeError,
            endTime = when {
                form.endTime.isBlank() -> "End time is required"
                !isValidTime(form.endTime) -> "Use the format HH:mm"
                start == null || end == null -> null  // date/start time error is already shown
                end <= start -> "End time must be after the start time"
                else -> null
            },
            capacity = if (capacity == null || capacity < 1) "Capacity must be at least 1" else null,
            points = if (points == null || points !in MIN_POINTS..MAX_POINTS)
                "Points must be between $MIN_POINTS and $MAX_POINTS" else null
        )
    }
}
