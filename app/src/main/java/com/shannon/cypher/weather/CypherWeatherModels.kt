package com.shannon.cypher.weather


enum class CypherWeatherIntent {
    WEATHER,
    TEMPERATURE,
    RAIN,
}


enum class CypherWeatherTimeTarget {
    CURRENT,
    TODAY,
    TOMORROW,
    WEEKDAY,
    WEEKEND,
}


data class CypherWeatherRequest(
    val intent: CypherWeatherIntent,
    val timeTarget: CypherWeatherTimeTarget,
    val weekday: String? = null,
    val namedLocation: String? = null,
)


data class CypherWeatherLocation(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
)


data class CypherWeatherDay(
    val date: String,
    val weatherCode: Int?,
    val maxTemperatureC: Double?,
    val minTemperatureC: Double?,
    val precipitationProbability: Int?,
)


data class CypherWeatherResult(
    val locationName: String,
    val currentTemperatureC: Double?,
    val apparentTemperatureC: Double?,
    val currentWeatherCode: Int?,
    val currentPrecipitationProbability: Int?,
    val daily: List<CypherWeatherDay>,
)