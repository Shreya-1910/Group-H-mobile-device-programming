package com.mdpgroup08.scpd.data.repository

import com.mdpgroup08.scpd.data.model.Event

object FakeEventRepository {
    fun getEvents(): List<Event> = listOf(
        Event("1", "Hackathon 2026", "Computing Society", "12 Nov, 9:00 AM", 10, 25),
        Event("2", "Beach Clean-up", "Green Club", "15 Nov, 8:00 AM", 5, 0),
        Event("3", "Career Talk: AI", "Careers Office", "18 Nov, 2:00 PM", 3, 60),
        Event("4", "Charity Run", "Sports Club", "22 Nov, 7:00 AM", 8, 12)
    )
}