package com.mdpgroup08.scpd.data.model

data class Event(
    val id: String,
    val title: String,
    val club: String,
    val date: String,
    val points: Int,
    val seatsLeft: Int
)
