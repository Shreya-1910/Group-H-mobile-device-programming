package com.mdpgroup08.scpd.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdpgroup08.scpd.data.repository.BookingRepository
import com.mdpgroup08.scpd.data.repository.EventRepository
import com.mdpgroup08.scpd.reminders.ReminderRules
import com.mdpgroup08.scpd.reminders.ReminderScheduler
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps one 3-hour reminder per confirmed booking (R4-S21): booking schedules one, cancelling removes it.
 * Started from the student home screen.
 */
class StudentRemindersViewModel(
    private val bookingRepository: BookingRepository,
    private val eventRepository: EventRepository,
    private val scheduler: ReminderScheduler,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    /** eventId -> remindAt for the reminders scheduled in this session. */
    private var scheduled: Map<String, Long> = emptyMap()

    init {
        viewModelScope.launch {
            combine(bookingRepository.getMyBookings(), eventRepository.getApprovedUpcomingEvents()) { bookings, events ->
                ReminderRules.plan(bookings, events, clock())
            }
                .catch { /* reminders are best effort; the worker double-checks before showing anything */ }
                .collect { plan ->
                    val wanted = plan.associate { it.eventId to it.remindAt }
                    (scheduled.keys - wanted.keys).forEach { scheduler.cancel(it) }
                    wanted.filter { (id, at) -> scheduled[id] != at }.forEach { (id, at) -> scheduler.schedule(id, at) }
                    scheduled = wanted
                }
        }
    }
}