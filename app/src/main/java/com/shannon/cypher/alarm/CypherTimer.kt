package com.shannon.cypher.alarm

data class CypherTimer(
    val id: Long,
    val label: String = "Timer",
    val durationMillis: Long,
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    val active: Boolean = true,
)
