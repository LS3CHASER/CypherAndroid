package com.shannon.cypher.calendar

import java.util.Calendar
import java.util.Locale


object CypherCalendarDateParser {

    private val months =
        mapOf(
            "january" to Calendar.JANUARY,
            "february" to Calendar.FEBRUARY,
            "march" to Calendar.MARCH,
            "april" to Calendar.APRIL,
            "may" to Calendar.MAY,
            "june" to Calendar.JUNE,
            "july" to Calendar.JULY,
            "august" to Calendar.AUGUST,
            "september" to Calendar.SEPTEMBER,
            "october" to Calendar.OCTOBER,
            "november" to Calendar.NOVEMBER,
            "december" to Calendar.DECEMBER,
        )

    private val spokenDays =
        mapOf(
            "first" to 1,
            "second" to 2,
            "third" to 3,
            "fourth" to 4,
            "fifth" to 5,
            "sixth" to 6,
            "seventh" to 7,
            "eighth" to 8,
            "ninth" to 9,
            "tenth" to 10,
            "eleventh" to 11,
            "twelfth" to 12,
            "thirteenth" to 13,
            "fourteenth" to 14,
            "fifteenth" to 15,
            "sixteenth" to 16,
            "seventeenth" to 17,
            "eighteenth" to 18,
            "nineteenth" to 19,
            "twentieth" to 20,
            "twenty first" to 21,
            "twenty second" to 22,
            "twenty third" to 23,
            "twenty fourth" to 24,
            "twenty fifth" to 25,
            "twenty sixth" to 26,
            "twenty seventh" to 27,
            "twenty eighth" to 28,
            "twenty ninth" to 29,
            "thirtieth" to 30,
            "thirty first" to 31,
        )


    fun containsSpecificDate(
        message: String,
    ): Boolean {

        return parseSpecificDate(
            message
        ) != null
    }


    fun parseSpecificDate(
        message: String,
    ): Calendar? {

        val text =
            message
                .lowercase(
                    Locale.getDefault()
                )
                .replace(
                    ",",
                    ""
                )
                .replace(
                    "-",
                    " "
                )
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()


        val monthPattern =
            "january|february|march|april|may|june|" +
                    "july|august|september|october|november|december"


        val dayMonthRegex =
            Regex(
                "\\b(?:the\\s+)?(\\d{1,2})(?:st|nd|rd|th)?" +
                        "(?:\\s+of)?\\s+" +
                        "($monthPattern)" +
                        "(?:\\s+(\\d{4}))?\\b",
                RegexOption.IGNORE_CASE,
            )


        val monthDayRegex =
            Regex(
                "\\b($monthPattern)\\s+" +
                        "(?:the\\s+)?(\\d{1,2})(?:st|nd|rd|th)?" +
                        "(?:\\s+(\\d{4}))?\\b",
                RegexOption.IGNORE_CASE,
            )


        val spokenDayPattern =
            spokenDays
                .keys
                .sortedByDescending {
                    it.length
                }
                .joinToString(
                    "|"
                ) {
                    Regex.escape(
                        it
                    )
                }


        val spokenDayMonthRegex =
            Regex(
                "\\b(?:the\\s+)?($spokenDayPattern)" +
                        "(?:\\s+of)?\\s+" +
                        "($monthPattern)" +
                        "(?:\\s+(\\d{4}))?\\b",
                RegexOption.IGNORE_CASE,
            )


        val spokenMonthDayRegex =
            Regex(
                "\\b($monthPattern)\\s+" +
                        "(?:the\\s+)?($spokenDayPattern)" +
                        "(?:\\s+(\\d{4}))?\\b",
                RegexOption.IGNORE_CASE,
            )


        val dayMonthMatch =
            dayMonthRegex.find(
                text
            )

        val monthDayMatch =
            monthDayRegex.find(
                text
            )

        val spokenDayMonthMatch =
            spokenDayMonthRegex.find(
                text
            )

        val spokenMonthDayMatch =
            spokenMonthDayRegex.find(
                text
            )


        val day: Int
        val monthName: String
        val explicitYear: Int?


        when {

            dayMonthMatch != null -> {

                day =
                    dayMonthMatch
                        .groupValues[1]
                        .toIntOrNull()
                        ?: return null

                monthName =
                    dayMonthMatch
                        .groupValues[2]
                        .lowercase(
                            Locale.getDefault()
                        )

                explicitYear =
                    dayMonthMatch
                        .groupValues[3]
                        .toIntOrNull()
            }


            monthDayMatch != null -> {

                monthName =
                    monthDayMatch
                        .groupValues[1]
                        .lowercase(
                            Locale.getDefault()
                        )

                day =
                    monthDayMatch
                        .groupValues[2]
                        .toIntOrNull()
                        ?: return null

                explicitYear =
                    monthDayMatch
                        .groupValues[3]
                        .toIntOrNull()
            }


            spokenDayMonthMatch != null -> {

                val spokenDay =
                    spokenDayMonthMatch
                        .groupValues[1]
                        .lowercase(
                            Locale.getDefault()
                        )
                        .replace(
                            Regex("\\s+"),
                            " "
                        )

                day =
                    spokenDays[
                        spokenDay
                    ]
                        ?: return null

                monthName =
                    spokenDayMonthMatch
                        .groupValues[2]
                        .lowercase(
                            Locale.getDefault()
                        )

                explicitYear =
                    spokenDayMonthMatch
                        .groupValues[3]
                        .toIntOrNull()
            }


            spokenMonthDayMatch != null -> {

                monthName =
                    spokenMonthDayMatch
                        .groupValues[1]
                        .lowercase(
                            Locale.getDefault()
                        )

                val spokenDay =
                    spokenMonthDayMatch
                        .groupValues[2]
                        .lowercase(
                            Locale.getDefault()
                        )
                        .replace(
                            Regex("\\s+"),
                            " "
                        )

                day =
                    spokenDays[
                        spokenDay
                    ]
                        ?: return null

                explicitYear =
                    spokenMonthDayMatch
                        .groupValues[3]
                        .toIntOrNull()
            }


            else ->
                return null
        }


        val month =
            months[
                monthName
            ]
                ?: return null


        val now =
            Calendar.getInstance()


        var year =
            explicitYear
                ?: now.get(
                    Calendar.YEAR
                )


        val result =
            Calendar
                .getInstance()
                .apply {

                    isLenient =
                        false

                    clear()

                    set(
                        Calendar.YEAR,
                        year,
                    )

                    set(
                        Calendar.MONTH,
                        month,
                    )

                    set(
                        Calendar.DAY_OF_MONTH,
                        day,
                    )
                }


        try {

            result.timeInMillis

        } catch (
            _: IllegalArgumentException
        ) {

            return null
        }


        if (
            explicitYear == null &&
            result.before(
                startOfToday()
            )
        ) {

            year +=
                1

            result.set(
                Calendar.YEAR,
                year,
            )
        }


        return result
    }


    fun dayRange(
        date: Calendar,
    ): Pair<Long, Long> {

        val start =
            date.clone()
                    as Calendar

        start.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        start.set(
            Calendar.MINUTE,
            0
        )

        start.set(
            Calendar.SECOND,
            0
        )

        start.set(
            Calendar.MILLISECOND,
            0
        )


        val end =
            date.clone()
                    as Calendar

        end.set(
            Calendar.HOUR_OF_DAY,
            23
        )

        end.set(
            Calendar.MINUTE,
            59
        )

        end.set(
            Calendar.SECOND,
            59
        )

        end.set(
            Calendar.MILLISECOND,
            999
        )


        return Pair(
            start.timeInMillis,
            end.timeInMillis,
        )
    }


    private fun startOfToday(): Calendar {

        return Calendar
            .getInstance()
            .apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }
    }
}