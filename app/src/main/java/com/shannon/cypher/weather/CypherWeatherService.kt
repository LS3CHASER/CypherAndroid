package com.shannon.cypher.weather

import android.content.Context
import com.shannon.cypher.location.CypherLocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext


class CypherWeatherService(
    context: Context,
) {

    private val locationProvider =
        CypherLocationProvider(
            context.applicationContext
        )


    private val weatherClient =
        CypherWeatherClient()


    suspend fun getCurrentWeatherResult(): CypherWeatherResult {

        if (
            !locationProvider
                .hasLocationPermission()
        ) {

            throw SecurityException(
                "Location permission is required."
            )
        }


        val location =
            locationProvider
                .getCurrentLocation()
                ?: throw IllegalStateException(
                    "Current location is unavailable."
                )


        return coroutineScope {

            val weatherDeferred =
                async(
                    Dispatchers.IO
                ) {

                    weatherClient
                        .getWeatherForCoordinates(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            displayName = "your current location",
                        )
                }


            val displayNameDeferred =
                async(
                    Dispatchers.IO
                ) {

                    locationProvider
                        .getDisplayName(
                            location
                        )
                }


            val weatherResult =
                weatherDeferred.await()


            val displayName =
                displayNameDeferred
                    .await()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: weatherResult.locationName


            weatherResult.copy(
                locationName = displayName
            )
        }
    }


    suspend fun handle(
        message: String,
    ): String? {

        val request =
            CypherWeatherParser.parse(
                message
            )
                ?: return null


        return try {

            val weather =
                if (
                    request.namedLocation != null
                ) {

                    withContext(
                        Dispatchers.IO
                    ) {

                        weatherClient
                            .getWeatherForNamedLocation(
                                request.namedLocation
                            )
                    }

                } else {

                    getCurrentWeatherResult()
                }


            CypherWeatherFormatter.format(
                request = request,
                result = weather,
            )

        } catch (
            _: Exception
        ) {

            "I couldn't retrieve the weather right now."
        }
    }
}
