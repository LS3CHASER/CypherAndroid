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

                    /*
                     * Explicit locations do not need GPS.
                     */
                    withContext(
                        Dispatchers.IO
                    ) {

                        weatherClient
                            .getWeatherForNamedLocation(
                                request.namedLocation
                            )
                    }

                } else {

                    if (
                        !locationProvider
                            .hasLocationPermission()
                    ) {

                        return (
                                "I need location permission to check " +
                                        "the weather where you are."
                                )
                    }


                    /*
                     * This should normally return almost instantly
                     * from Android's recent location cache.
                     */
                    val location =
                        locationProvider
                            .getCurrentLocation()


                    if (
                        location == null
                    ) {

                        return (
                                "I couldn't get your current location. " +
                                        "Please make sure location services are turned on."
                                )
                    }


                    /*
                     * IMPORTANT:
                     *
                     * The weather request and suburb/city lookup run
                     * in parallel rather than one after the other.
                     *
                     * This avoids adding reverse-geocoding time onto
                     * the weather API response time.
                     */
                    coroutineScope {

                        val weatherDeferred =
                            async(
                                Dispatchers.IO
                            ) {

                                weatherClient
                                    .getWeatherForCoordinates(
                                        latitude =
                                            location.latitude,

                                        longitude =
                                            location.longitude,

                                        displayName =
                                            "your current location",
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
                            weatherDeferred
                                .await()


                        val displayName =
                            displayNameDeferred
                                .await()
                                ?.takeIf {

                                    it.isNotBlank()
                                }
                                ?: weatherResult.locationName


                        /*
                         * CypherWeatherResult is a data class, so we
                         * can preserve the forecast and simply replace
                         * the temporary GPS label with "Taree",
                         * "Newcastle", etc.
                         */
                        weatherResult.copy(
                            locationName =
                                displayName
                        )
                    }
                }


            CypherWeatherFormatter.format(
                request =
                    request,

                result =
                    weather,
            )

        } catch (
            _: Exception
        ) {

            "I couldn't retrieve the weather right now."
        }
    }
}