package com.shannon.cypher.weather


object CypherWeatherParser {

    private val weekdays =
        listOf(
            "monday",
            "tuesday",
            "wednesday",
            "thursday",
            "friday",
            "saturday",
            "sunday",
        )


    fun parse(
        message: String,
    ): CypherWeatherRequest? {

        val text =
            normalise(
                message
            )


        if (
            text.isBlank() ||
            !hasWeatherIntent(
                text
            )
        ) {

            return null
        }


        val intent =
            when {

                isRainRequest(
                    text
                ) -> {

                    CypherWeatherIntent.RAIN
                }


                isTemperatureRequest(
                    text
                ) -> {

                    CypherWeatherIntent.TEMPERATURE
                }


                else -> {

                    CypherWeatherIntent.WEATHER
                }
            }


        val weekday =
            weekdays.firstOrNull { day ->
                Regex(
                    "\\b$day\\b"
                ).containsMatchIn(
                    text
                )
            }


        val timeTarget =
            when {

                "weekend" in text -> {

                    CypherWeatherTimeTarget.WEEKEND
                }


                "tomorrow" in text -> {

                    CypherWeatherTimeTarget.TOMORROW
                }


                weekday != null -> {

                    CypherWeatherTimeTarget.WEEKDAY
                }


                "today" in text -> {

                    CypherWeatherTimeTarget.TODAY
                }


                else -> {

                    CypherWeatherTimeTarget.CURRENT
                }
            }


        return CypherWeatherRequest(
            intent =
                intent,

            timeTarget =
                timeTarget,

            weekday =
                weekday,

            namedLocation =
                extractNamedLocation(
                    text
                ),
        )
    }


    private fun hasWeatherIntent(
        text: String,
    ): Boolean {

        val phrases =
            listOf(
                "weather",
                "forecast",
                "temperature",
                "temp",
                "degrees",
                "rain",
                "raining",
                "rainy",
                "shower",
                "showers",
                "storm",
                "storms",
                "thunderstorm",
                "thunderstorms",
                "what's it like outside",
                "whats it like outside",
                "what is it like outside",
                "what's it like out",
                "whats it like out",
                "what is it like out",
                "how hot is it",
                "how cold is it",
                "how warm is it",
                "how cool is it",
                "umbrella",
            )


        return phrases.any { phrase ->
            phrase in text
        }
    }


    private fun isTemperatureRequest(
        text: String,
    ): Boolean {

        val phrases =
            listOf(
                "current temperature",
                "current temp",
                "what's the temperature",
                "whats the temperature",
                "what is the temperature",
                "what's the temp",
                "whats the temp",
                "what is the temp",
                "how hot is it",
                "how cold is it",
                "how warm is it",
                "how cool is it",
                "how many degrees",
                "temperature outside",
                "temp outside",
            )


        return phrases.any { phrase ->
            phrase in text
        }
    }


    private fun isRainRequest(
        text: String,
    ): Boolean {

        val phrases =
            listOf(
                "will it rain",
                "is it going to rain",
                "is it gonna rain",
                "is rain expected",
                "chance of rain",
                "chance of showers",
                "any rain",
                "any showers",
                "is it raining",
                "will i need an umbrella",
                "do i need an umbrella",
                "should i take an umbrella",
            )


        return phrases.any { phrase ->
            phrase in text
        }
    }


    private fun extractNamedLocation(
        text: String,
    ): String? {

        val patterns =
            listOf(
                Regex(
                    "(?i)\\b(?:weather|forecast|temperature|temp)" +
                            "\\s+(?:in|for|at)\\s+(.+)$"
                ),

                Regex(
                    "(?i)\\b(?:will it rain|is it going to rain|" +
                            "is it gonna rain|chance of rain|" +
                            "chance of showers|is it raining)" +
                            "\\s+(?:in|at|for)\\s+(.+)$"
                ),

                Regex(
                    "(?i)\\bwhat(?:'s| is)?\\s+the\\s+" +
                            "(?:weather|temperature|temp)" +
                            "\\s+(?:like\\s+)?(?:in|at|for)\\s+(.+)$"
                ),
            )


        for (
        pattern in patterns
        ) {

            val match =
                pattern.find(
                    text
                )
                    ?: continue


            var location =
                cleanLocation(
                    match.groupValues[1]
                )


            location =
                removeForecastTimeWords(
                    location
                )


            if (
                location.isNotBlank()
            ) {

                return location
            }
        }


        return null
    }


    private fun removeForecastTimeWords(
        location: String,
    ): String {

        var cleaned =
            location


        val words =
            mutableListOf(
                "today",
                "tomorrow",
                "this weekend",
                "weekend",
            )


        words.addAll(
            weekdays
        )


        for (
        word in words
        ) {

            cleaned =
                cleaned.replace(
                    Regex(
                        "(?i)\\b${Regex.escape(word)}\\b"
                    ),
                    " ",
                )
        }


        return cleaned
            .replace(
                Regex(
                    "\\s+"
                ),
                " ",
            )
            .trim()
    }


    private fun cleanLocation(
        location: String,
    ): String {

        return location
            .trim()
            .trimEnd(
                '?',
                '.',
                ',',
                '!',
            )
            .replace(
                Regex(
                    "\\s+"
                ),
                " ",
            )
            .trim()
    }


    private fun normalise(
        message: String,
    ): String {

        return message
            .lowercase()
            .replace(
                "’",
                "'",
            )
            .replace(
                Regex(
                    "\\s+"
                ),
                " ",
            )
            .trim()
            .trimEnd(
                '?',
                '.',
                '!',
            )
    }
}