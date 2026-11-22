package com.mdpgroup08.scpd.viewmodel

import com.mdpgroup08.scpd.data.model.Event
import com.mdpgroup08.scpd.data.model.EventStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** Status messages on My events (R1-S3, R1-S6, R1-S8) and the review deadline (R1-S4). */
class ClubEventStatusTest {

    private val utc = ZoneOffset.UTC
    private fun millis(date: LocalDate) = date.atStartOfDay(utc).toInstant().toEpochMilli()

    @Test
    fun `submitted on Monday is due the next Monday`() {
        val monday = LocalDate.of(2026, 11, 23)
        assertEquals(LocalDate.of(2026, 11, 30), ReviewDeadline.dueDate(millis(monday), utc))
    }

    @Test
    fun `submitted on Friday skips the weekend`() {
        val friday = LocalDate.of(2026, 11, 27)
        assertEquals(LocalDate.of(2026, 12, 4), ReviewDeadline.dueDate(millis(friday), utc))
    }

    @Test
    fun `submitted on Saturday is due the next Friday`() {
        val saturday = LocalDate.of(2026, 11, 28)
        assertEquals(LocalDate.of(2026, 12, 4), ReviewDeadline.dueDate(millis(saturday), utc))
    }

    @Test
    fun `draft has no note`() {
        assertNull(ClubEventStatus.toItem(Event(status = EventStatus.DRAFT), utc).note)
    }

    @Test
    fun `pending shows due date and the edit lock`() {
        val submitted = millis(LocalDate.of(2026, 11, 23))
        val note = ClubEventStatus.toItem(Event(status = EventStatus.PENDING, submittedAt = submitted), utc).note!!
        assertTrue(note.contains("Mon 30 Nov"))
        assertTrue(note.contains("can't be edited"))
    }

    @Test
    fun `rejected shows the admin's reason as a warning`() {
        val item = ClubEventStatus.toItem(
            Event(status = EventStatus.REJECTED, rejectionReason = "Venue not booked"), utc
        )
        assertTrue(item.note!!.contains("Venue not booked"))
        assertTrue(item.noteIsWarning)
    }

    @Test
    fun `approved shows the approved points, not the requested`() {
        val item = ClubEventStatus.toItem(
            Event(status = EventStatus.APPROVED, requestedPoints = 8, approvedPoints = 5), utc
        )
        assertTrue(item.note!!.contains("Approved for 5 points"))
        assertFalse(item.noteIsWarning)
    }

    @Test
    fun `expired tells the club not to advertise`() {
        val item = ClubEventStatus.toItem(Event(status = EventStatus.EXPIRED), utc)
        assertTrue(item.note!!.contains("Don't advertise"))
        assertTrue(item.noteIsWarning)
    }
}