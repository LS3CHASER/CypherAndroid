package com.shannon.cypher.alarm

data class CypherAlarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val repeatDays: Set<Int> = emptySet(),
    val enabled: Boolean = true,

    /*
     * Used only for a one-off alarm when the user explicitly says
     * "tomorrow". Zero keeps backward compatibility with alarms
     * created before this field existed.
     */
    val oneOffDateMillis: Long = 0L,
)
