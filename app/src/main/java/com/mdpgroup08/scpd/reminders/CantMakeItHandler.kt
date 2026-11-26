package com.mdpgroup08.scpd.reminders

import com.mdpgroup08.scpd.data.model.BookingStatus
import com.mdpgroup08.scpd.data.network.NetworkMonitor
import com.mdpgroup08.scpd.data.repository.BookingException
import com.mdpgroup08.scpd.data.repository.BookingRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import com.mdpgroup08.scpd.viewmodel.BookingRules
import kotlinx.coroutines.flow.first

/** The outcome of tapping "Can't make it", shown back in the notification. */
sealed interface CantMakeItResult {
    data class Cancelled(val title: String) : CantMakeItResult
    data object Offline : CantMakeItResult
    data object TooLate : CantMakeItResult
    /** Already cancelled, e.g. the button was tapped twice (R4-S21 EX3). */
    data object NotBooked : CantMakeItResult
    data object Failed : CantMakeItResult
}

/** R4-S21: cancel from the reminder, only when online and before the cut-off. */
class CantMakeItHandler(
    private val bookingRepository: BookingRepository,
    private val eventRepository: EventRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun handle(eventId: String): CantMakeItResult {
        val event = eventRepository.getEvent(eventId).first() ?: return CantMakeItResult.NotBooked
        val booking = bookingRepository.getMyBooking(eventId).first()
        if (booking?.status != BookingStatus.CONFIRMED) return CantMakeItResult.NotBooked
        if (BookingRules.isBookingClosed(event, clock())) return CantMakeItResult.TooLate
        // AC2: nothing is saved or queued offline, because the place can't be released offline
        if (!networkMonitor.isOnline.value) return CantMakeItResult.Offline

        return try {
            bookingRepository.cancel(eventId)   // also hands the place to the waitlist (EX4)
            CantMakeItResult.Cancelled(event.title)
        } catch (e: BookingException.NotBooked) {
            CantMakeItResult.NotBooked
        } catch (e: Exception) {
            // EX2: the server refuses once the cut-off has passed, even if the phone thought it hadn't
            if (BookingRules.isBookingClosed(event, clock())) CantMakeItResult.TooLate else CantMakeItResult.Failed
        }
    }
}