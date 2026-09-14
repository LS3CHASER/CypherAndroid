package com.shannon.cypher.alarm

import java.util.Calendar
import java.util.Locale

sealed interface CypherAlarmCommand {
    data class CreateAlarm(
        val hour: Int,
        val minute: Int,
        val repeatDays: Set<Int>,
        val label: String,
    ) : CypherAlarmCommand

    data class CreateTimer(
        val durationMillis: Long,
        val label: String,
    ) : CypherAlarmCommand

    data object ListAlarms : CypherAlarmCommand
    data object ListTimers : CypherAlarmCommand
    data object RemainingTimer : CypherAlarmCommand
    data object CancelLatestTimer : CypherAlarmCommand
}

object CypherAlarmCommandParser {

    fun parse(message: String): CypherAlarmCommand? {
        val lower = normalise(message)

        parseTimerCreate(lower)?.let { return it }
        parseAlarmCreate(lower)?.let { return it }

        if (
            listOf(
                "what alarms",
                "which alarms",
                "list my alarms",
                "show my alarms",
                "alarms do i have",
                "alarms have i got",
            ).any { it in lower }
        ) {
            return CypherAlarmCommand.ListAlarms
        }

        if (
            listOf(
                "what timers",
                "which timers",
                "list my timers",
                "show my timers",
                "timers do i have",
                "timers have i got",
            ).any { it in lower }
        ) {
            return CypherAlarmCommand.ListTimers
        }

        if (
            "timer" in lower &&
            (
                    "how long" in lower ||
                            "time left" in lower ||
                            "remaining" in lower
                    )
        ) {
            return CypherAlarmCommand.RemainingTimer
        }

        if (
            "timer" in lower &&
            listOf("cancel", "stop", "delete", "remove").any {
                Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower)
            }
        ) {
            return CypherAlarmCommand.CancelLatestTimer
        }

        return null
    }

    private fun parseTimerCreate(
        message: String,
    ): CypherAlarmCommand.CreateTimer? {
        if ("timer" !in message) return null

        val creationWord =
            listOf("set", "start", "create", "make").any {
                Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(message)
            }

        if (!creationWord) return null

        var totalMillis = 0L

        Regex("\\b(\\d+)\\s*hours?\\b")
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.let { totalMillis += it * 60L * 60L * 1000L }

        Regex("\\b(\\d+)\\s*minutes?\\b")
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.let { totalMillis += it * 60L * 1000L }

        Regex("\\b(\\d+)\\s*seconds?\\b")
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.let { totalMillis += it * 1000L }

        if (totalMillis <= 0L) {
            parseSpokenDurationMillis(message)?.let {
                totalMillis = it
            }
        }

        if (totalMillis <= 0L) return null

        return CypherAlarmCommand.CreateTimer(
            durationMillis = totalMillis,
            label = extractLabel(message, "Timer"),
        )
    }

    private fun parseAlarmCreate(
        message: String,
    ): CypherAlarmCommand.CreateAlarm? {
        if ("alarm" !in message && "wake me" !in message) return null

        val creationWord =
            listOf("set", "create", "make", "wake me").any { it in message }

        if (!creationWord) return null

        val time = parseClockTime(message) ?: return null

        return CypherAlarmCommand.CreateAlarm(
            hour = time.first,
            minute = time.second,
            repeatDays = parseRepeatDays(message),
            label = extractLabel(message, "Alarm"),
        )
    }

    private fun parseClockTime(
        message: String,
    ): Pair<Int, Int>? {
        val match =
            Regex(
                "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b",
                RegexOption.IGNORE_CASE,
            ).find(message)
                ?: return null

        val rawHour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: return null

        if (rawHour !in 1..12 || minute !in 0..59) return null

        val period = match.groupValues[3].lowercase(Locale.getDefault())

        val hour =
            when {
                period == "am" && rawHour == 12 -> 0
                period == "pm" && rawHour != 12 -> rawHour + 12
                else -> rawHour
            }

        return Pair(hour, minute)
    }

    private fun parseRepeatDays(
        message: String,
    ): Set<Int> {
        if ("every weekday" in message || "weekdays" in message) {
            return setOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
            )
        }

        if ("every day" in message || "daily" in message) {
            return setOf(
                Calendar.SUNDAY,
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
            )
        }

        val days = linkedSetOf<Int>()

        mapOf(
            "sunday" to Calendar.SUNDAY,
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
        ).forEach { (name, day) ->
            if (
                Regex("\\b(?:every\\s+)?${Regex.escape(name)}\\b")
                    .containsMatchIn(message)
            ) {
                days.add(day)
            }
        }

        return days
    }

    private fun extractLabel(
        message: String,
        fallback: String,
    ): String {
        val label =
            Regex("\\b(?:called|named|labelled|labeled)\\s+(.+)$")
                .find(message)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()

        return label
            ?.takeIf { it.isNotBlank() }
            ?.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(Locale.getDefault())
                } else {
                    it.toString()
                }
            }
            ?: fallback
    }

    private fun parseSpokenDurationMillis(
        message: String,
    ): Long? {
        val numberWords =
            mapOf(
                "one" to 1L,
                "two" to 2L,
                "three" to 3L,
                "four" to 4L,
                "five" to 5L,
                "six" to 6L,
                "seven" to 7L,
                "eight" to 8L,
                "nine" to 9L,
                "ten" to 10L,
                "eleven" to 11L,
                "twelve" to 12L,
                "thirteen" to 13L,
                "fourteen" to 14L,
                "fifteen" to 15L,
                "sixteen" to 16L,
                "seventeen" to 17L,
                "eighteen" to 18L,
                "nineteen" to 19L,
                "twenty" to 20L,
                "thirty" to 30L,
                "forty" to 40L,
                "fifty" to 50L,
                "sixty" to 60L,
            )

        for ((word, value) in numberWords) {
            when {
                Regex("\\b$word\\s+hours?\\b").containsMatchIn(message) ->
                    return value * 60L * 60L * 1000L

                Regex("\\b$word\\s+minutes?\\b").containsMatchIn(message) ->
                    return value * 60L * 1000L

                Regex("\\b$word\\s+seconds?\\b").containsMatchIn(message) ->
                    return value * 1000L
            }
        }

        return null
    }

    private fun normalise(
        message: String,
    ): String {
        return message
            .lowercase(Locale.getDefault())
            .replace("a.m.", "am")
            .replace("p.m.", "pm")
            .replace("a.m", "am")
            .replace("p.m", "pm")
            .replace("a m", "am")
            .replace("p m", "pm")
            .trim()
    }
}
