package com.shannon.cypher.alarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CypherAlarmRepository(
    context: Context,
) {
    companion object {
        private const val PREFERENCES_NAME = "cypher_alarm_timer_store"
        private const val KEY_ALARMS = "alarms_json"
        private const val KEY_TIMERS = "timers_json"
    }

    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun getAlarms(): List<CypherAlarm> {
        val raw =
            preferences.getString(
                KEY_ALARMS,
                null,
            ) ?: return emptyList()

        return try {
            val array = JSONArray(raw)

            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val repeatDaysJson =
                        item.optJSONArray("repeatDays")
                            ?: JSONArray()

                    val repeatDays =
                        buildSet {
                            for (
                            dayIndex in
                            0 until repeatDaysJson.length()
                            ) {
                                add(
                                    repeatDaysJson.getInt(
                                        dayIndex
                                    )
                                )
                            }
                        }

                    add(
                        CypherAlarm(
                            id = item.getLong("id"),
                            hour = item.getInt("hour"),
                            minute = item.getInt("minute"),
                            label =
                                item.optString(
                                    "label",
                                    "Alarm",
                                ),
                            repeatDays = repeatDays,
                            enabled =
                                item.optBoolean(
                                    "enabled",
                                    true,
                                ),
                            oneOffDateMillis =
                                item.optLong(
                                    "oneOffDateMillis",
                                    0L,
                                ),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getTimers(): List<CypherTimer> {
        val raw =
            preferences.getString(
                KEY_TIMERS,
                null,
            ) ?: return emptyList()

        return try {
            val array = JSONArray(raw)

            buildList {
                for (index in 0 until array.length()) {
                    val item =
                        array.getJSONObject(index)

                    add(
                        CypherTimer(
                            id = item.getLong("id"),
                            label =
                                item.optString(
                                    "label",
                                    "Timer",
                                ),
                            durationMillis =
                                item.getLong(
                                    "durationMillis"
                                ),
                            startedAtMillis =
                                item.getLong(
                                    "startedAtMillis"
                                ),
                            endsAtMillis =
                                item.getLong(
                                    "endsAtMillis"
                                ),
                            active =
                                item.optBoolean(
                                    "active",
                                    true,
                                ),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAlarm(alarm: CypherAlarm) {
        val alarms =
            getAlarms().toMutableList()

        val existingIndex =
            alarms.indexOfFirst {
                it.id == alarm.id
            }

        if (existingIndex >= 0) {
            alarms[existingIndex] = alarm
        } else {
            alarms.add(alarm)
        }

        saveAlarms(alarms)
    }

    fun saveTimer(timer: CypherTimer) {
        val timers =
            getTimers().toMutableList()

        val existingIndex =
            timers.indexOfFirst {
                it.id == timer.id
            }

        if (existingIndex >= 0) {
            timers[existingIndex] = timer
        } else {
            timers.add(timer)
        }

        saveTimers(timers)
    }

    fun getAlarm(alarmId: Long): CypherAlarm? =
        getAlarms()
            .firstOrNull {
                it.id == alarmId
            }

    fun getTimer(timerId: Long): CypherTimer? =
        getTimers()
            .firstOrNull {
                it.id == timerId
            }

    fun setAlarmEnabled(
        alarmId: Long,
        enabled: Boolean,
    ): CypherAlarm? {
        val alarm =
            getAlarm(alarmId)
                ?: return null

        val updated =
            alarm.copy(
                enabled = enabled
            )

        saveAlarm(updated)
        return updated
    }

    fun markTimerInactive(
        timerId: Long,
    ): CypherTimer? {
        val timer =
            getTimer(timerId)
                ?: return null

        val updated =
            timer.copy(
                active = false
            )

        saveTimer(updated)
        return updated
    }

    fun deleteAlarm(
        alarmId: Long,
    ): Boolean {
        val alarms = getAlarms()
        val updated =
            alarms.filterNot {
                it.id == alarmId
            }

        if (updated.size == alarms.size) {
            return false
        }

        saveAlarms(updated)
        return true
    }

    fun deleteTimer(
        timerId: Long,
    ): Boolean {
        val timers = getTimers()
        val updated =
            timers.filterNot {
                it.id == timerId
            }

        if (updated.size == timers.size) {
            return false
        }

        saveTimers(updated)
        return true
    }

    fun clearFinishedTimers() {
        saveTimers(
            getTimers()
                .filter {
                    it.active
                }
        )
    }

    private fun saveAlarms(
        alarms: List<CypherAlarm>,
    ) {
        val array = JSONArray()

        alarms.forEach { alarm ->
            val repeatDays = JSONArray()

            alarm.repeatDays
                .sorted()
                .forEach {
                    repeatDays.put(it)
                }

            array.put(
                JSONObject()
                    .put("id", alarm.id)
                    .put("hour", alarm.hour)
                    .put("minute", alarm.minute)
                    .put("label", alarm.label)
                    .put("repeatDays", repeatDays)
                    .put("enabled", alarm.enabled)
                    .put(
                        "oneOffDateMillis",
                        alarm.oneOffDateMillis,
                    )
            )
        }

        preferences.edit()
            .putString(
                KEY_ALARMS,
                array.toString(),
            )
            .apply()
    }

    private fun saveTimers(
        timers: List<CypherTimer>,
    ) {
        val array = JSONArray()

        timers.forEach { timer ->
            array.put(
                JSONObject()
                    .put("id", timer.id)
                    .put("label", timer.label)
                    .put(
                        "durationMillis",
                        timer.durationMillis,
                    )
                    .put(
                        "startedAtMillis",
                        timer.startedAtMillis,
                    )
                    .put(
                        "endsAtMillis",
                        timer.endsAtMillis,
                    )
                    .put(
                        "active",
                        timer.active,
                    )
            )
        }

        preferences.edit()
            .putString(
                KEY_TIMERS,
                array.toString(),
            )
            .apply()
    }
}
