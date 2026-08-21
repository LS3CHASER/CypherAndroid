package com.shannon.cypher.calendar


data class CypherCalendarEvent(
    val id: Long,
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val allDay: Boolean,
    val calendarName: String,
)