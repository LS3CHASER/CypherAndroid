package com.shannon.cypher.weather

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt


object CypherWeatherFormatter {

    fun format(
        request: CypherWeatherRequest,
        result: CypherWeatherResult,
    ): String {

        return when (
            request.timeTarget
        ) {

            CypherWeatherTimeTarget.CURRENT -> {

                formatCurrent(
                    request,
                    result,
                )
            }


            CypherWeatherTimeTarget.TODAY -> {

                formatDay(
                    request =
                        request,

                    result =
                        result,

                    day =
                        result.daily.getOrNull(
                            0
                        ),

                    spokenDay =
                        "today",
                )
            }


            CypherWeatherTimeTarget.TOMORROW -> {

                formatDay(
                    request =
                        request,

                    result =
                        result,

                    day =
                        result.daily.getOrNull(
                            1
                        ),

                    spokenDay =
                        "tomorrow",
                )
            }


            CypherWeatherTimeTarget.WEEKDAY -> {

                val weekday =
                    request.weekday
                        ?: return "I couldn't work out which day you wanted."


                val day =
                    findForecastDayByWeekday(
                        result,
                        weekday,
                    )


                formatDay(
                    request =
                        request,

                    result =
                        result,

                    day =
                        day,

                    spokenDay =
                        weekday.replaceFirstChar { character ->
                            character.uppercase()
                        },
                )
            }


            CypherWeatherTimeTarget.WEEKEND -> {

                formatWeekend(
                    request,
                    result,
                )
            }
        }
    }


    private fun formatCurrent(
        request: CypherWeatherRequest,
        result: CypherWeatherResult,
    ): String {

        return when (
            request.intent
        ) {

            CypherWeatherIntent.TEMPERATURE -> {

                formatCurrentTemperature(
                    result
                )
            }


            CypherWeatherIntent.RAIN -> {

                formatCurrentRain(
                    result
                )
            }


            CypherWeatherIntent.WEATHER -> {

                formatCurrentWeather(
                    result
                )
            }
        }
    }


    private fun formatCurrentTemperature(
        result: CypherWeatherResult,
    ): String {

        val temperature =
            result.currentTemperatureC
                ?.roundToInt()


        val feelsLike =
            result.apparentTemperatureC
                ?.roundToInt()


        if (
            temperature == null
        ) {

            return (
                    "I couldn't get the current temperature " +
                            "for ${result.locationName}."
                    )
        }


        return if (
            feelsLike != null &&
            feelsLike != temperature
        ) {

            "It's currently $temperature degrees in ${result.locationName}, " +
                    "and it feels like $feelsLike."

        } else {

            "It's currently $temperature degrees in ${result.locationName}."
        }
    }


    private fun formatCurrentRain(
        result: CypherWeatherResult,
    ): String {

        val probability =
            result.currentPrecipitationProbability
                ?: result.daily
                    .firstOrNull()
                    ?.precipitationProbability


        if (
            probability == null
        ) {

            return (
                    "I couldn't get a reliable rain probability " +
                            "for ${result.locationName}."
                    )
        }


        return rainSentence(
            probability =
                probability,

            spokenDay =
                "right now",

            location =
                result.locationName,
        )
    }


    private fun formatCurrentWeather(
        result: CypherWeatherResult,
    ): String {

        val parts =
            mutableListOf<String>()


        val temperature =
            result.currentTemperatureC
                ?.roundToInt()


        val feelsLike =
            result.apparentTemperatureC
                ?.roundToInt()


        val condition =
            describeWeatherCode(
                result.currentWeatherCode
            )


        if (
            temperature != null
        ) {

            parts.add(
                "It's currently $temperature degrees in ${result.locationName}"
            )

        } else {

            parts.add(
                "Here's the current weather for ${result.locationName}"
            )
        }


        if (
            condition.isNotBlank()
        ) {

            parts.add(
                condition
            )
        }


        if (
            feelsLike != null &&
            temperature != null &&
            feelsLike != temperature
        ) {

            parts.add(
                "It feels like $feelsLike degrees"
            )
        }


        return finish(
            parts.joinToString(
                ". "
            )
        )
    }


    private fun formatDay(
        request: CypherWeatherRequest,
        result: CypherWeatherResult,
        day: CypherWeatherDay?,
        spokenDay: String,
    ): String {

        if (
            day == null
        ) {

            return (
                    "I couldn't find a forecast for $spokenDay " +
                            "in ${result.locationName}."
                    )
        }


        if (
            request.intent ==
            CypherWeatherIntent.RAIN
        ) {

            val rain =
                day.precipitationProbability


            if (
                rain == null
            ) {

                return (
                        "I couldn't get a reliable rain probability " +
                                "for $spokenDay in ${result.locationName}."
                        )
            }


            return rainSentence(
                probability =
                    rain,

                spokenDay =
                    spokenDay,

                location =
                    result.locationName,
            )
        }


        val max =
            day.maxTemperatureC
                ?.roundToInt()


        val min =
            day.minTemperatureC
                ?.roundToInt()


        if (
            request.intent ==
            CypherWeatherIntent.TEMPERATURE
        ) {

            return when {

                max != null &&
                        min != null -> {

                    "For $spokenDay in ${result.locationName}, " +
                            "the high will be $max degrees " +
                            "and the low will be $min."
                }


                max != null -> {

                    "For $spokenDay in ${result.locationName}, " +
                            "the high will be $max degrees."
                }


                else -> {

                    "I couldn't get the temperature forecast " +
                            "for $spokenDay in ${result.locationName}."
                }
            }
        }


        val condition =
            describeWeatherCode(
                day.weatherCode
            )


        val rain =
            day.precipitationProbability


        val parts =
            mutableListOf<String>()


        parts.add(
            "For $spokenDay in ${result.locationName}"
        )


        if (
            condition.isNotBlank()
        ) {

            parts.add(
                "expect $condition"
            )
        }


        if (
            max != null &&
            min != null
        ) {

            parts.add(
                "a high of $max degrees and a low of $min"
            )
        }


        if (
            rain != null
        ) {

            parts.add(
                "$rain percent chance of rain"
            )
        }


        return finish(
            parts.joinToString(
                ". "
            )
        )
    }


    private fun formatWeekend(
        request: CypherWeatherRequest,
        result: CypherWeatherResult,
    ): String {

        val saturday =
            findForecastDayByWeekday(
                result,
                "saturday",
            )


        val sunday =
            findForecastDayByWeekday(
                result,
                "sunday",
            )


        if (
            saturday == null &&
            sunday == null
        ) {

            return (
                    "This weekend isn't available in the current forecast " +
                            "for ${result.locationName}."
                    )
        }


        if (
            request.intent ==
            CypherWeatherIntent.RAIN
        ) {

            val saturdayRain =
                saturday
                    ?.precipitationProbability


            val sundayRain =
                sunday
                    ?.precipitationProbability


            val parts =
                mutableListOf<String>()


            if (
                saturdayRain != null
            ) {

                parts.add(
                    "Saturday has a $saturdayRain percent chance of rain"
                )
            }


            if (
                sundayRain != null
            ) {

                parts.add(
                    "Sunday has a $sundayRain percent chance"
                )
            }


            if (
                parts.isEmpty()
            ) {

                return (
                        "I couldn't get a reliable rain forecast " +
                                "for this weekend in ${result.locationName}."
                        )
            }


            return finish(
                "For this weekend in ${result.locationName}, " +
                        parts.joinToString(
                            ". "
                        )
            )
        }


        val summaries =
            mutableListOf<String>()


        if (
            saturday != null
        ) {

            summaries.add(
                shortDaySummary(
                    "Saturday",
                    saturday,
                )
            )
        }


        if (
            sunday != null
        ) {

            summaries.add(
                shortDaySummary(
                    "Sunday",
                    sunday,
                )
            )
        }


        return finish(
            "For this weekend in ${result.locationName}, " +
                    summaries.joinToString(
                        ". "
                    )
        )
    }


    private fun shortDaySummary(
        label: String,
        day: CypherWeatherDay,
    ): String {

        val parts =
            mutableListOf<String>()


        parts.add(
            label
        )


        val condition =
            describeWeatherCode(
                day.weatherCode
            )


        if (
            condition.isNotBlank()
        ) {

            parts.add(
                condition
            )
        }


        day.maxTemperatureC
            ?.roundToInt()
            ?.let { high ->

                parts.add(
                    "a high of $high degrees"
                )
            }


        day.precipitationProbability
            ?.let { rain ->

                parts.add(
                    "$rain percent chance of rain"
                )
            }


        return parts.joinToString(
            ", "
        )
    }


    private fun findForecastDayByWeekday(
        result: CypherWeatherResult,
        weekday: String,
    ): CypherWeatherDay? {

        val parser =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US,
            )


        val formatter =
            SimpleDateFormat(
                "EEEE",
                Locale.US,
            )


        return result.daily
            .firstOrNull { day ->

                try {

                    val date =
                        parser.parse(
                            day.date
                        )


                    date != null &&
                            formatter
                                .format(
                                    date
                                )
                                .equals(
                                    weekday,
                                    ignoreCase = true,
                                )

                } catch (
                    _: Exception
                ) {

                    false
                }
            }
    }


    private fun rainSentence(
        probability: Int,
        spokenDay: String,
        location: String,
    ): String {

        return when {

            probability <= 10 -> {

                "Rain looks unlikely $spokenDay in $location. " +
                        "The chance is around $probability percent."
            }


            probability <= 30 -> {

                "There's a slight chance of rain $spokenDay in $location, " +
                        "at around $probability percent."
            }


            probability <= 60 -> {

                "There's about a $probability percent chance of rain " +
                        "$spokenDay in $location."
            }


            probability <= 80 -> {

                "Rain is likely $spokenDay in $location, " +
                        "with around a $probability percent chance."
            }


            else -> {

                "Rain is very likely $spokenDay in $location, " +
                        "with around a $probability percent chance."
            }
        }
    }


    private fun describeWeatherCode(
        code: Int?,
    ): String {

        return when (
            code
        ) {

            0 ->
                "clear skies"

            1 ->
                "mostly clear conditions"

            2 ->
                "partly cloudy conditions"

            3 ->
                "overcast conditions"

            45,
            48 ->
                "foggy conditions"

            51,
            53,
            55 ->
                "drizzle"

            56,
            57 ->
                "freezing drizzle"

            61,
            63,
            65 ->
                "rain"

            66,
            67 ->
                "freezing rain"

            71,
            73,
            75,
            77 ->
                "snow"

            80,
            81,
            82 ->
                "rain showers"

            85,
            86 ->
                "snow showers"

            95 ->
                "thunderstorms"

            96,
            99 ->
                "thunderstorms with hail"

            else ->
                ""
        }
    }


    private fun finish(
        text: String,
    ): String {

        val cleaned =
            text.trim()


        if (
            cleaned.isBlank()
        ) {

            return cleaned
        }


        return if (
            cleaned.endsWith(".")
        ) {

            cleaned

        } else {

            "$cleaned."
        }
    }
}