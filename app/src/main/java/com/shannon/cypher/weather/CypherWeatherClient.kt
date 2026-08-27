package com.shannon.cypher.weather

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL


class CypherWeatherClient {

    companion object {

        private const val GEOCODING_URL =
            "https://geocoding-api.open-meteo.com/v1/search"

        private const val FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast"

        private const val CONNECT_TIMEOUT_MS =
            8_000

        private const val READ_TIMEOUT_MS =
            10_000
    }


    fun getWeatherForCoordinates(
        latitude: Double,
        longitude: Double,
        displayName: String = "your current location",
    ): CypherWeatherResult {

        val location =
            CypherWeatherLocation(
                latitude = latitude,
                longitude = longitude,
                displayName = displayName,
            )


        return getWeather(
            location
        )
    }


    fun getWeatherForNamedLocation(
        location: String,
    ): CypherWeatherResult {

        val resolvedLocation =
            geocodeLocation(
                location
            )


        return getWeather(
            resolvedLocation
        )
    }


    private fun getWeather(
        location: CypherWeatherLocation,
    ): CypherWeatherResult {

        val requestUrl =
            "$FORECAST_URL" +
                    "?latitude=${location.latitude}" +
                    "&longitude=${location.longitude}" +
                    "&current=temperature_2m," +
                    "apparent_temperature," +
                    "weather_code," +
                    "precipitation_probability" +
                    "&daily=weather_code," +
                    "temperature_2m_max," +
                    "temperature_2m_min," +
                    "precipitation_probability_max" +
                    "&timezone=auto" +
                    "&forecast_days=7"


        val connection =
            URL(
                requestUrl
            ).openConnection() as HttpURLConnection


        try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                CONNECT_TIMEOUT_MS

            connection.readTimeout =
                READ_TIMEOUT_MS

            connection.useCaches =
                false


            val responseCode =
                connection.responseCode


            if (
                responseCode !in 200..299
            ) {

                throw RuntimeException(
                    "Weather service returned HTTP $responseCode"
                )
            }


            val responseText =
                connection.inputStream
                    .bufferedReader()
                    .use { reader ->
                        reader.readText()
                    }


            val root =
                JSONObject(
                    responseText
                )


            val current =
                root.optJSONObject(
                    "current"
                )


            val daily =
                root.optJSONObject(
                    "daily"
                )


            return CypherWeatherResult(
                locationName =
                    location.displayName,

                currentTemperatureC =
                    optionalDouble(
                        current,
                        "temperature_2m",
                    ),

                apparentTemperatureC =
                    optionalDouble(
                        current,
                        "apparent_temperature",
                    ),

                currentWeatherCode =
                    optionalInt(
                        current,
                        "weather_code",
                    ),

                currentPrecipitationProbability =
                    optionalInt(
                        current,
                        "precipitation_probability",
                    ),

                daily =
                    parseDailyWeather(
                        daily
                    ),
            )

        } finally {

            connection.disconnect()
        }
    }


    private fun parseDailyWeather(
        daily: JSONObject?,
    ): List<CypherWeatherDay> {

        if (
            daily == null
        ) {

            return emptyList()
        }


        val dates =
            daily.optJSONArray(
                "time"
            )
                ?: return emptyList()


        val weatherCodes =
            daily.optJSONArray(
                "weather_code"
            )


        val maxTemperatures =
            daily.optJSONArray(
                "temperature_2m_max"
            )


        val minTemperatures =
            daily.optJSONArray(
                "temperature_2m_min"
            )


        val rainProbabilities =
            daily.optJSONArray(
                "precipitation_probability_max"
            )


        val days =
            mutableListOf<CypherWeatherDay>()


        for (
        index in 0 until dates.length()
        ) {

            val date =
                dates.optString(
                    index,
                    "",
                )


            if (
                date.isBlank()
            ) {

                continue
            }


            days.add(
                CypherWeatherDay(
                    date =
                        date,

                    weatherCode =
                        optionalArrayInt(
                            weatherCodes,
                            index,
                        ),

                    maxTemperatureC =
                        optionalArrayDouble(
                            maxTemperatures,
                            index,
                        ),

                    minTemperatureC =
                        optionalArrayDouble(
                            minTemperatures,
                            index,
                        ),

                    precipitationProbability =
                        optionalArrayInt(
                            rainProbabilities,
                            index,
                        ),
                )
            )
        }


        return days
    }


    private fun geocodeLocation(
        location: String,
    ): CypherWeatherLocation {

        val encodedLocation =
            URLEncoder.encode(
                location,
                Charsets.UTF_8.name(),
            )


        val requestUrl =
            "$GEOCODING_URL" +
                    "?name=$encodedLocation" +
                    "&count=1" +
                    "&language=en" +
                    "&format=json"


        val connection =
            URL(
                requestUrl
            ).openConnection() as HttpURLConnection


        try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                CONNECT_TIMEOUT_MS

            connection.readTimeout =
                READ_TIMEOUT_MS

            connection.useCaches =
                false


            val responseCode =
                connection.responseCode


            if (
                responseCode !in 200..299
            ) {

                throw RuntimeException(
                    "Weather geocoding returned HTTP $responseCode"
                )
            }


            val responseText =
                connection.inputStream
                    .bufferedReader()
                    .use { reader ->
                        reader.readText()
                    }


            val root =
                JSONObject(
                    responseText
                )


            val results =
                root.optJSONArray(
                    "results"
                )


            if (
                results == null ||
                results.length() == 0
            ) {

                throw RuntimeException(
                    "Weather location could not be found: $location"
                )
            }


            val first =
                results.getJSONObject(
                    0
                )


            val name =
                first.optString(
                    "name",
                    location,
                )


            val state =
                first.optString(
                    "admin1",
                    "",
                )


            val country =
                first.optString(
                    "country",
                    "",
                )


            val displayName =
                listOf(
                    name,
                    state,
                    country,
                )
                    .filter { value ->
                        value.isNotBlank()
                    }
                    .distinct()
                    .joinToString(
                        ", "
                    )


            return CypherWeatherLocation(
                latitude =
                    first.getDouble(
                        "latitude"
                    ),

                longitude =
                    first.getDouble(
                        "longitude"
                    ),

                displayName =
                    displayName,
            )

        } finally {

            connection.disconnect()
        }
    }


    private fun optionalDouble(
        json: JSONObject?,
        key: String,
    ): Double? {

        if (
            json == null ||
            !json.has(key) ||
            json.isNull(key)
        ) {

            return null
        }


        return try {

            json.getDouble(
                key
            )

        } catch (
            _: Exception
        ) {

            null
        }
    }


    private fun optionalInt(
        json: JSONObject?,
        key: String,
    ): Int? {

        if (
            json == null ||
            !json.has(key) ||
            json.isNull(key)
        ) {

            return null
        }


        return try {

            json.getInt(
                key
            )

        } catch (
            _: Exception
        ) {

            null
        }
    }


    private fun optionalArrayDouble(
        array: JSONArray?,
        index: Int,
    ): Double? {

        if (
            array == null ||
            index >= array.length() ||
            array.isNull(index)
        ) {

            return null
        }


        return try {

            array.getDouble(
                index
            )

        } catch (
            _: Exception
        ) {

            null
        }
    }


    private fun optionalArrayInt(
        array: JSONArray?,
        index: Int,
    ): Int? {

        if (
            array == null ||
            index >= array.length() ||
            array.isNull(index)
        ) {

            return null
        }


        return try {

            array.getInt(
                index
            )

        } catch (
            _: Exception
        ) {

            null
        }
    }
}