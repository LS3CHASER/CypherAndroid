package com.shannon.cypher.alarm

data class CypherAlarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val repeatDays: Set<Int> = emptySet(),
    val enabled: Boolean = true,
)