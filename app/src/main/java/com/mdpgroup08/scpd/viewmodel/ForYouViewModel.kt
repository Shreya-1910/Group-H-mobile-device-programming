package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.repository.BookingRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** The "For you" row on the Marketplace (R2-S11). Updates live as bookings and events change. */
class ForYouViewModel(
    private val eventRepository: EventRepository,
    private val bookingRepository: BookingRepository,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()

    init {
        viewModelScope.launch {
            combine(bookingRepository.getMyBookings(), eventRepository.getApprovedUpcomingEvents()) { bookings, upcoming ->
                val history = try {
                    eventRepository.getEventsByIds(bookings.map { it.eventId })
                } catch (e: Exception) {
                    emptyList()  // offline with nothing saved: fall back to popular events
                }
                Recommender.recommend(upcoming, bookings, history, bookingRepository.myId(), clock())
            }
                // Recommendations are a bonus: if they fail, the row is simply hidden
                .catch { emit(emptyList()) }
                .collect { _recommendations.value = it }
        }
    }
}