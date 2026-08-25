package com.shannon.cypher.tasks

import com.shannon.cypher.calendar.CypherCalendarDateParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


object CypherTaskDateParser {

    private val weekdays =
        mapOf(
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY,
        )


    private val monthPattern =
        "january|february|march|april|may|june|july|august|" +
                "september|october|november|december"


    private val spokenDayPattern =
        "first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|" +
                "tenth|eleventh|twelfth|thirteenth|fourteenth|fifteenth|" +
                "sixteenth|seventeenth|eighteenth|nineteenth|twentieth|" +
                "twenty\\s+first|twenty\\s+second|twenty\\s+third|" +
                "twenty\\s+fourth|twenty\\s+fifth|twenty\\s+sixth|" +
                "twenty\\s+seventh|twenty\\s+eighth|twenty\\s+ninth|" +
                "thirtieth|thirty\\s+first"


    fun hasDueDateLanguage(
        message: String,
    ): Boolean {

        val lower =
            message
                .lowercase(
                    Locale.getDefault()
                )


        if (
            CypherCalendarDateParser
                .containsSpecificDate(
                    message
                )
        ) {

            return true
        }


        if (
            listOf(
                "today",
                "tomorrow",
                "tonight",
            ).any {
                Regex(
                    "\\b${Regex.escape(it)}\\b"
                ).containsMatchIn(
                    lower
                )
            }
        ) {

            return true
        }


        return weekdays.keys.any {
                weekday ->

            Regex(
                "\\b(?:this\\s+|next\\s+)?${Regex.escape(weekday)}\\b"
            ).containsMatchIn(
                lower
            )
        }
    }


    fun hasExplicitTimeLanguage(
        message: String,
    ): Boolean {

        val lower =
            normaliseTimeText(
                message
            )


        if (
            Regex(
                "\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)\\b"
            ).containsMatchIn(
                lower
            )
        ) {

            return true
        }


        if (
            Regex(
                "\\bat\\s+\\d{1,2}(?::\\d{2})?\\b"
            ).containsMatchIn(
                lower
            )
        ) {

            return true
        }


        return listOf(
            "morning",
            "afternoon",
            "evening",
            "tonight",
            "noon",
            "midday",
            "midnight",
        ).any {
            Regex(
                "\\b${Regex.escape(it)}\\b"
            ).containsMatchIn(
                lower
            )
        }
    }


    fun parseDueAtMillis(
        message: String,
    ): Long? {

        val hasDate =
            hasDueDateLanguage(
                message
            )


        val hasTime =
            hasExplicitTimeLanguage(
                message
            )


        if (
            !hasDate &&
            !hasTime
        ) {

            return null
        }


        val now =
            Calendar
                .getInstance()


        val date =
            parseDate(
                message
            )
                ?: (now.clone() as Calendar)


        val parsedTime =
            parseTime(
                message
            )


        if (
            parsedTime != null
        ) {

            date.set(
                Calendar.HOUR_OF_DAY,
                parsedTime.first,
            )

            date.set(
                Calendar.MINUTE,
                parsedTime.second,
            )

        } else {

            /*
             * A date-only task defaults to 9:00 AM.
             *
             * Example:
             * "Add buy flowers for Wedding Anniversary on 2nd of November"
             */
            date.set(
                Calendar.HOUR_OF_DAY,
                9,
            )

            date.set(
                Calendar.MINUTE,
                0,
            )
        }


        date.set(
            Calendar.SECOND,
            0,
        )

        date.set(
            Calendar.MILLISECOND,
            0,
        )


        /*
         * Time-only command:
         * use today if the time is still ahead,
         * otherwise use tomorrow.
         */
        if (
            !hasDate &&
            date.timeInMillis <=
            now.timeInMillis
        ) {

            date.add(
                Calendar.DAY_OF_YEAR,
                1,
            )
        }


        return date
            .timeInMillis
    }


    fun stripDueDateTimeLanguage(
        message: String,
    ): String {

        var text =
            message


        /*
         * Specific numeric date:
         * 2 November
         * 2nd of November
         * the 2nd of November 2026
         */
        text =
            text.replace(
                Regex(
                    "(?i)\\b(?:on\\s+|for\\s+|due\\s+)?" +
                            "(?:the\\s+)?\\d{1,2}(?:st|nd|rd|th)?" +
                            "(?:\\s+of)?\\s+(?:$monthPattern)" +
                            "(?:\\s+\\d{4})?\\b"
                ),
                " "
            )


        /*
         * Month-first date:
         * November 2
         * November the 2nd
         */
        text =
            text.replace(
                Regex(
                    "(?i)\\b(?:on\\s+|for\\s+|due\\s+)?" +
                            "(?:$monthPattern)\\s+" +
                            "(?:the\\s+)?\\d{1,2}(?:st|nd|rd|th)?" +
                            "(?:\\s+\\d{4})?\\b"
                ),
                " "
            )


        /*
         * Spoken date:
         * second of November
         * twenty seventh of August
         */
        text =
            text.replace(
                Regex(
                    "(?i)\\b(?:on\\s+|for\\s+|due\\s+)?" +
                            "(?:the\\s+)?(?:$spokenDayPattern)" +
                            "(?:\\s+of)?\\s+(?:$monthPattern)" +
                            "(?:\\s+\\d{4})?\\b"
                ),
                " "
            )


        /*
         * Relative dates and weekdays.
         */
        text =
            text.replace(
                Regex(
                    "(?i)\\b(?:on\\s+|for\\s+|due\\s+)?" +
                            "(?:today|tomorrow|tonight|" +
                            "(?:this\\s+|next\\s+)?" +
                            "(?:monday|tuesday|wednesday|thursday|" +
                            "friday|saturday|sunday))\\b"
                ),
                " "
            )


        /*
         * Explicit AM/PM time.
         */
        text =
            text.replace(
                Regex(
                    "(?i)\\b(?:at\\s+|by\\s+)?" +
                            "\\d{1,2}(?::\\d{2})?\\s*" +
                            "(?:a\\.?m\\.?|p\\.?m\\.?)\\b"
                ),
                " "
            )


        /*
         * Time without AM/PM:
         * "at 4"
         * "at 10:30"
         */
        text =
            text.replace(
                Regex(
                    "(?i)\\bat\\s+\\d{1,2}(?::\\d{2})?\\b"
                ),
                " "
            )


        /*
         * Natural dayparts.
         */
        text =
            text.replace(
                Regex(
                    "(?i)\\b(?:in\\s+the\\s+|this\\s+)?" +
                            "(?:morning|afternoon|evening)\\b|" +
                            "\\b(?:at\\s+)?(?:noon|midday|midnight)\\b"
                ),
                " "
            )


        return text
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim(
                ' ',
                ',',
                '.',
                '-',
            )
    }


    fun formatForSpeech(
        millis: Long,
    ): String {

        return SimpleDateFormat(
            "EEEE d MMMM 'at' h:mm a",
            Locale.getDefault(),
        ).format(
            millis
        )
    }


    private fun parseDate(
        message: String,
    ): Calendar? {

        val specific =
            CypherCalendarDateParser
                .parseSpecificDate(
                    message
                )


        if (
            specific != null
        ) {

            return specific
        }


        val lower =
            message
                .lowercase(
                    Locale.getDefault()
                )


        val result =
            Calendar
                .getInstance()


        when {

            Regex(
                "\\btomorrow\\b"
            ).containsMatchIn(
                lower
            ) -> {

                result.add(
                    Calendar.DAY_OF_YEAR,
                    1,
                )

                return result
            }


            Regex(
                "\\b(today|tonight)\\b"
            ).containsMatchIn(
                lower
            ) -> {

                return result
            }
        }


        for (
        entry in weekdays
        ) {

            val weekday =
                entry.key

            val targetDay =
                entry.value


            val match =
                Regex(
                    "\\b(next\\s+|this\\s+)?${Regex.escape(weekday)}\\b"
                ).find(
                    lower
                )
                    ?: continue


            val currentDay =
                result.get(
                    Calendar.DAY_OF_WEEK
                )


            var daysAhead =
                (
                        targetDay -
                                currentDay +
                                7
                        ) % 7


            val prefix =
                match.groupValues[1]
                    .trim()


            if (
                prefix == "next"
            ) {

                if (
                    daysAhead == 0
                ) {
                    daysAhead = 7
                }

            } else if (
                daysAhead == 0
            ) {

                /*
                 * A plain weekday spoken on that same weekday
                 * means the next occurrence, avoiding an
                 * accidental past due time.
                 */
                daysAhead = 7
            }


            result.add(
                Calendar.DAY_OF_YEAR,
                daysAhead,
            )


            return result
        }


        return null
    }


    private fun parseTime(
        message: String,
    ): Pair<Int, Int>? {

        val lower =
            normaliseTimeText(
                message
            )


        val amPm =
            Regex(
                "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b"
            ).find(
                lower
            )


        if (
            amPm != null
        ) {

            val rawHour =
                amPm.groupValues[1]
                    .toIntOrNull()
                    ?: return null


            val minute =
                amPm.groupValues[2]
                    .ifBlank {
                        "0"
                    }
                    .toIntOrNull()
                    ?: return null


            if (
                rawHour !in 1..12 ||
                minute !in 0..59
            ) {

                return null
            }


            val period =
                amPm.groupValues[3]


            val hour =
                when {

                    period == "am" &&
                            rawHour == 12 ->
                        0

                    period == "pm" &&
                            rawHour != 12 ->
                        rawHour + 12

                    else ->
                        rawHour
                }


            return Pair(
                hour,
                minute,
            )
        }


        when {

            Regex(
                "\\b(midday|noon)\\b"
            ).containsMatchIn(
                lower
            ) -> {

                return Pair(
                    12,
                    0,
                )
            }


            Regex(
                "\\bmidnight\\b"
            ).containsMatchIn(
                lower
            ) -> {

                return Pair(
                    0,
                    0,
                )
            }


            Regex(
                "\\bmorning\\b"
            ).containsMatchIn(
                lower
            ) -> {

                return Pair(
                    9,
                    0,
                )
            }


            Regex(
                "\\bafternoon\\b"
            ).containsMatchIn(
                lower
            ) -> {

                return Pair(
                    15,
                    0,
                )
            }


            Regex(
                "\\b(evening|tonight)\\b"
            ).containsMatchIn(
                lower
            ) -> {

                return Pair(
                    19,
                    0,
                )
            }
        }


        /*
         * "at 4" / "at 10:30"
         *
         * Voice-friendly heuristic:
         * 1-7 = PM
         * 8-11 = AM
         * 12 = midday
         */
        val withoutPeriod =
            Regex(
                "\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\b"
            ).find(
                lower
            )


        if (
            withoutPeriod != null
        ) {

            val rawHour =
                withoutPeriod.groupValues[1]
                    .toIntOrNull()
                    ?: return null


            val minute =
                withoutPeriod.groupValues[2]
                    .ifBlank {
                        "0"
                    }
                    .toIntOrNull()
                    ?: return null


            if (
                rawHour !in 1..12 ||
                minute !in 0..59
            ) {

                return null
            }


            val hour =
                when (
                    rawHour
                ) {

                    in 1..7 ->
                        rawHour + 12

                    in 8..11 ->
                        rawHour

                    else ->
                        12
                }


            return Pair(
                hour,
                minute,
            )
        }


        return null
    }


    private fun normaliseTimeText(
        message: String,
    ): String {

        return message
            .lowercase(
                Locale.getDefault()
            )
            .replace(
                "a.m.",
                "am"
            )
            .replace(
                "p.m.",
                "pm"
            )
            .replace(
                "a.m",
                "am"
            )
            .replace(
                "p.m",
                "pm"
            )
            .replace(
                "a m",
                "am"
            )
            .replace(
                "p m",
                "pm"
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }
}
