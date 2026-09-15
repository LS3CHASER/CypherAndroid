package com.shannon.cypher.alarm

import java.util.Calendar
import java.util.Locale

sealed interface CypherAlarmCommand {

    data class CreateAlarm(
        val hour: Int,
        val minute: Int,
        val repeatDays: Set<Int>,
        val label: String,
        val tomorrow: Boolean,
    ) : CypherAlarmCommand

    data class CreateTimer(
        val durationMillis: Long,
        val label: String,
    ) : CypherAlarmCommand

    data class DeleteAlarm(
        val hour: Int?,
        val minute: Int?,
    ) : CypherAlarmCommand

    data class DisableAlarm(
        val hour: Int?,
        val minute: Int?,
    ) : CypherAlarmCommand

    data class EnableAlarm(
        val hour: Int?,
        val minute: Int?,
    ) : CypherAlarmCommand

    data object ListAlarms :
        CypherAlarmCommand

    data object ListTimers :
        CypherAlarmCommand

    data object RemainingTimer :
        CypherAlarmCommand

    data object CancelLatestTimer :
        CypherAlarmCommand
}

object CypherAlarmCommandParser {

    fun parse(
        message: String,
    ): CypherAlarmCommand? {
        val lower =
            normalise(
                message
            )

        parseTimerCreate(lower)
            ?.let {
                return it
            }

        parseAlarmManagement(lower)
            ?.let {
                return it
            }

        parseAlarmCreate(lower)
            ?.let {
                return it
            }

        if (
            listOf(
                "what alarms",
                "which alarms",
                "list my alarms",
                "show my alarms",
                "alarms do i have",
                "alarms have i got",
            ).any {
                it in lower
            }
        ) {
            return CypherAlarmCommand
                .ListAlarms
        }

        if (
            listOf(
                "what timers",
                "which timers",
                "list my timers",
                "show my timers",
                "timers do i have",
                "timers have i got",
            ).any {
                it in lower
            }
        ) {
            return CypherAlarmCommand
                .ListTimers
        }

        if (
            "timer" in lower &&
            (
                    "how long" in lower ||
                            "time left" in lower ||
                            "remaining" in lower
                    )
        ) {
            return CypherAlarmCommand
                .RemainingTimer
        }

        if (
            "timer" in lower &&
            listOf(
                "cancel",
                "stop",
                "delete",
                "remove",
            ).any {
                Regex(
                    "\\b${Regex.escape(it)}\\b"
                ).containsMatchIn(
                    lower
                )
            }
        ) {
            return CypherAlarmCommand
                .CancelLatestTimer
        }

        return null
    }

    private fun parseTimerCreate(
        message: String,
    ): CypherAlarmCommand.CreateTimer? {
        if (
            "timer" !in
            message
        ) {
            return null
        }

        val creationWord =
            listOf(
                "set",
                "start",
                "create",
                "make",
            ).any {
                Regex(
                    "\\b${Regex.escape(it)}\\b"
                ).containsMatchIn(
                    message
                )
            }

        if (!creationWord) {
            return null
        }

        var totalMillis = 0L

        Regex(
            "\\b(\\d+)\\s*hours?\\b"
        )
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.let {
                totalMillis +=
                    it *
                            60L *
                            60L *
                            1000L
            }

        Regex(
            "\\b(\\d+)\\s*minutes?\\b"
        )
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.let {
                totalMillis +=
                    it *
                            60L *
                            1000L
            }

        Regex(
            "\\b(\\d+)\\s*seconds?\\b"
        )
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.let {
                totalMillis +=
                    it *
                            1000L
            }

        if (
            totalMillis <=
            0L
        ) {
            parseSpokenDurationMillis(
                message
            )
                ?.let {
                    totalMillis =
                        it
                }
        }

        if (
            totalMillis <=
            0L
        ) {
            return null
        }

        return CypherAlarmCommand
            .CreateTimer(
                durationMillis =
                    totalMillis,
                label =
                    extractLabel(
                        message,
                        "Timer",
                    ),
            )
    }

    private fun parseAlarmManagement(
        message: String,
    ): CypherAlarmCommand? {
        if (
            "alarm" !in message
        ) {
            return null
        }

        val time =
            parseClockTime(
                message
            )

        if (
            listOf(
                "delete",
                "remove",
            ).any {
                Regex(
                    "\\b$it\\b"
                ).containsMatchIn(
                    message
                )
            }
        ) {
            return CypherAlarmCommand
                .DeleteAlarm(
                    hour =
                        time?.first,
                    minute =
                        time?.second,
                )
        }

        if (
            listOf(
                "turn off",
                "disable",
                "switch off",
            ).any {
                it in message
            }
        ) {
            return CypherAlarmCommand
                .DisableAlarm(
                    hour =
                        time?.first,
                    minute =
                        time?.second,
                )
        }

        if (
            listOf(
                "turn on",
                "enable",
                "switch on",
            ).any {
                it in message
            }
        ) {
            return CypherAlarmCommand
                .EnableAlarm(
                    hour =
                        time?.first,
                    minute =
                        time?.second,
                )
        }

        return null
    }

    private fun parseAlarmCreate(
        message: String,
    ): CypherAlarmCommand.CreateAlarm? {
        if (
            "alarm" !in message &&
            "wake me" !in message
        ) {
            return null
        }

        val creationWord =
            listOf(
                "set",
                "create",
                "make",
                "wake me",
            ).any {
                it in message
            }

        if (!creationWord) {
            return null
        }

        val time =
            parseClockTime(
                message
            ) ?: return null

        return CypherAlarmCommand
            .CreateAlarm(
                hour = time.first,
                minute = time.second,
                repeatDays =
                    parseRepeatDays(
                        message
                    ),
                label =
                    extractLabel(
                        message,
                        "Alarm",
                    ),
                tomorrow =
                    Regex(
                        "\\btomorrow\\b"
                    ).containsMatchIn(
                        message
                    ),
            )
    }

    private fun parseClockTime(
        message: String,
    ): Pair<Int, Int>? {

        /*
         * Android speech recognition can return clock times in several
         * equivalent forms:
         *
         * 9:30 pm
         * 9.30 pm
         * 9:30 p.m.
         * 9 30 pm
         * 930 pm
         * 9 pm
         *
         * Accept all of them before allowing the command to fall through
         * to Tasks / Calendar / CypherOS.
         */
        val speechFriendly =
            message
                .replace(
                    Regex(
                        "\\b([ap])\\s*\\.\\s*m\\s*\\.?\\b",
                        RegexOption.IGNORE_CASE,
                    )
                ) {
                    "${it.groupValues[1]}m"
                }
                .replace(
                    Regex(
                        "(?<=\\d)[.](?=\\d)"
                    ),
                    ":",
                )

        /*
         * Standard separated time:
         * 9:30 pm, 9 30 pm, 9 pm.
         */
        val explicitSeparated =
            Regex(
                "\\b(\\d{1,2})(?:(?::|\\s)(\\d{2}))?\\s*(am|pm)\\b",
                RegexOption.IGNORE_CASE,
            ).find(
                speechFriendly
            )

        if (
            explicitSeparated !=
            null
        ) {
            return convertClockTime(
                rawHour =
                    explicitSeparated
                        .groupValues[1]
                        .toIntOrNull()
                        ?: return null,
                minute =
                    explicitSeparated
                        .groupValues[2]
                        .ifBlank {
                            "0"
                        }
                        .toIntOrNull()
                        ?: return null,
                period =
                    explicitSeparated
                        .groupValues[3],
            )
        }

        /*
         * Compact speech-recognition form:
         * 930 pm, 0630 am, 1230 pm.
         */
        val explicitCompact =
            Regex(
                "\\b(\\d{3,4})\\s*(am|pm)\\b",
                RegexOption.IGNORE_CASE,
            ).find(
                speechFriendly
            )

        if (
            explicitCompact !=
            null
        ) {
            val digits =
                explicitCompact
                    .groupValues[1]

            val rawHour =
                digits
                    .dropLast(2)
                    .toIntOrNull()
                    ?: return null

            val minute =
                digits
                    .takeLast(2)
                    .toIntOrNull()
                    ?: return null

            return convertClockTime(
                rawHour =
                    rawHour,
                minute =
                    minute,
                period =
                    explicitCompact
                        .groupValues[2],
            )
        }

        /*
         * Natural phrases such as:
         * "6:30 tomorrow morning"
         * "6.30 tomorrow morning"
         * "6 30 tomorrow morning"
         * "7 tomorrow evening"
         */
        val dayPartMatch =
            Regex(
                "\\b(\\d{1,2})(?:(?::|\\s)(\\d{2}))?\\s+(?:tomorrow\\s+)?(morning|afternoon|evening|tonight)\\b",
                RegexOption.IGNORE_CASE,
            ).find(
                speechFriendly
            )

        if (
            dayPartMatch !=
            null
        ) {
            val rawHour =
                dayPartMatch
                    .groupValues[1]
                    .toIntOrNull()
                    ?: return null

            val minute =
                dayPartMatch
                    .groupValues[2]
                    .ifBlank {
                        "0"
                    }
                    .toIntOrNull()
                    ?: return null

            val part =
                dayPartMatch
                    .groupValues[3]
                    .lowercase(
                        Locale.getDefault()
                    )

            val period =
                when (part) {
                    "morning" -> "am"
                    else -> "pm"
                }

            return convertClockTime(
                rawHour =
                    rawHour,
                minute =
                    minute,
                period =
                    period,
            )
        }

        /*
         * Also accept a 24-hour clock when speech recognition supplies
         * one without AM/PM, for example "set an alarm for 21:30".
         * Requiring a colon keeps this conservative so ordinary numbers
         * elsewhere in the sentence are not mistaken for alarm times.
         */
        val twentyFourHour =
            Regex(
                "\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b"
            ).find(
                speechFriendly
            )

        if (
            twentyFourHour !=
            null
        ) {
            val hour =
                twentyFourHour
                    .groupValues[1]
                    .toIntOrNull()
                    ?: return null

            val minute =
                twentyFourHour
                    .groupValues[2]
                    .toIntOrNull()
                    ?: return null

            return Pair(
                hour,
                minute,
            )
        }

        return null
    }

    private fun convertClockTime(
        rawHour: Int,
        minute: Int,
        period: String,
    ): Pair<Int, Int>? {
        if (
            rawHour !in
            1..12 ||
            minute !in
            0..59
        ) {
            return null
        }

        val lowerPeriod =
            period.lowercase(
                Locale.getDefault()
            )

        val hour =
            when {
                lowerPeriod ==
                        "am" &&
                        rawHour ==
                        12 ->
                    0

                lowerPeriod ==
                        "pm" &&
                        rawHour !=
                        12 ->
                    rawHour +
                            12

                else ->
                    rawHour
            }

        return Pair(
            hour,
            minute,
        )
    }

    private fun parseRepeatDays(
        message: String,
    ): Set<Int> {
        if (
            "every weekday" in message ||
            "weekdays" in message
        ) {
            return setOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
            )
        }

        if (
            "every day" in message ||
            "daily" in message
        ) {
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

        val days =
            linkedSetOf<Int>()

        mapOf(
            "sunday" to Calendar.SUNDAY,
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
        ).forEach {
                (name, day) ->

            if (
                Regex(
                    "\\b(?:every\\s+)?${Regex.escape(name)}\\b"
                ).containsMatchIn(
                    message
                )
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
            Regex(
                "\\b(?:called|named|labelled|labeled)\\s+(.+)$"
            )
                .find(message)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()

        return label
            ?.takeIf {
                it.isNotBlank()
            }
            ?.replaceFirstChar {
                if (
                    it.isLowerCase()
                ) {
                    it.titlecase(
                        Locale.getDefault()
                    )
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

        val unitMatch =
            Regex(
                "\\b([a-z]+)\\s+(hours?|minutes?|seconds?)\\b"
            ).find(message)
                ?: return null

        val amount =
            numberWords[
                unitMatch.groupValues[1]
            ] ?: return null

        return when {
            unitMatch
                .groupValues[2]
                .startsWith(
                    "hour"
                ) ->
                amount *
                        60L *
                        60L *
                        1000L

            unitMatch
                .groupValues[2]
                .startsWith(
                    "minute"
                ) ->
                amount *
                        60L *
                        1000L

            else ->
                amount *
                        1000L
        }
    }

    private fun normalise(
        message: String,
    ): String {
        return message
            .lowercase(
                Locale.getDefault()
            )
            .replace(
                Regex("\\s+"),
                " ",
            )
            .trim()
    }
}
