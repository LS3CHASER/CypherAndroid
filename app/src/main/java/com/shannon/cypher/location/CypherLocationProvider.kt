package com.shannon.cypher.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull


data class CypherLocation(
    val latitude: Double,
    val longitude: Double,
)


class CypherLocationProvider(
    context: Context,
) {

    companion object {

        /*
         * Weather does not require metre-perfect GPS accuracy.
         *
         * If Android already has a location from the last
         * 10 minutes, use it immediately instead of waiting
         * several seconds for another GPS fix.
         */
        private const val MAX_CACHED_LOCATION_AGE_MS =
            10 * 60 * 1000L

        /*
         * If a fresh fix is genuinely required, do not let
         * Weather sit waiting indefinitely.
         */
        private const val FRESH_LOCATION_TIMEOUT_MS =
            2_500L

        /*
         * A cached suburb/city name can be reused while the
         * phone remains reasonably close to where it was
         * originally resolved.
         */
        private const val PLACE_NAME_CACHE_DISTANCE_METRES =
            10_000f

        private const val PREFS_NAME =
            "cypher_location"

        private const val PREF_LOCATION_NAME =
            "location_name"

        private const val PREF_LOCATION_LAT =
            "location_lat"

        private const val PREF_LOCATION_LON =
            "location_lon"
    }


    private val appContext =
        context.applicationContext


    private val locationManager =
        appContext.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager


    private val preferences =
        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )


    fun hasLocationPermission():
            Boolean {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) ==
                    PackageManager.PERMISSION_GRANTED


        val coarseGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) ==
                    PackageManager.PERMISSION_GRANTED


        return (
                fineGranted ||
                        coarseGranted
                )
    }


    @Suppress(
        "MissingPermission"
    )
    suspend fun getCurrentLocation():
            CypherLocation? {

        if (
            !hasLocationPermission()
        ) {

            return null
        }


        /*
         * FAST PATH
         *
         * Android normally already knows where the phone is.
         * For weather this is accurate enough and avoids waiting
         * for another satellite fix every time Cypher is asked.
         */
        val cachedLocation =
            getBestLastKnownLocation()


        if (
            cachedLocation != null &&
            isLocationRecent(
                cachedLocation
            )
        ) {

            return cachedLocation
                .toCypherLocation()
        }


        /*
         * Only ask Android for a fresh fix when the cached
         * position is missing or genuinely old.
         */
        val freshLocation =
            withTimeoutOrNull(
                FRESH_LOCATION_TIMEOUT_MS
            ) {

                getFreshLocation()
            }


        if (
            freshLocation != null
        ) {

            return freshLocation
                .toCypherLocation()
        }


        /*
         * If Android couldn't obtain a fresh fix quickly,
         * an older known location is still preferable to
         * making Weather fail completely.
         */
        return cachedLocation
            ?.toCypherLocation()
    }


    suspend fun getDisplayName(
        location: CypherLocation,
    ): String? {

        /*
         * Reuse the previously resolved suburb/city if the
         * phone is still close to that position.
         */
        getCachedDisplayName(
            location
        )
            ?.let {

                return it
            }


        val resolvedName =
            reverseGeocode(
                location
            )


        if (
            !resolvedName.isNullOrBlank()
        ) {

            saveDisplayName(
                location =
                    location,

                displayName =
                    resolvedName,
            )
        }


        return resolvedName
    }


    @Suppress(
        "MissingPermission"
    )
    private suspend fun getFreshLocation():
            Location? {

        return suspendCancellableCoroutine {
                continuation ->


            /*
             * Network location is normally much quicker than
             * waiting for GPS satellites and is more than
             * accurate enough for a weather forecast.
             */
            val provider =
                when {

                    locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                    ) -> {

                        LocationManager.NETWORK_PROVIDER
                    }


                    locationManager.isProviderEnabled(
                        LocationManager.GPS_PROVIDER
                    ) -> {

                        LocationManager.GPS_PROVIDER
                    }


                    else -> {

                        null
                    }
                }


            if (
                provider == null
            ) {

                continuation.resume(
                    null
                )

                return@suspendCancellableCoroutine
            }


            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {

                locationManager.getCurrentLocation(
                    provider,
                    null,
                    appContext.mainExecutor,
                ) {
                        result ->


                    if (
                        continuation.isActive
                    ) {

                        continuation.resume(
                            result
                        )
                    }
                }

            } else {

                continuation.resume(
                    getBestLastKnownLocation()
                )
            }
        }
    }


    @Suppress(
        "MissingPermission"
    )
    private fun getBestLastKnownLocation():
            Location? {

        if (
            !hasLocationPermission()
        ) {

            return null
        }


        val locations =
            mutableListOf<Location>()


        try {

            locationManager
                .getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
                )
                ?.let {

                    locations.add(
                        it
                    )
                }

        } catch (
            _: Exception
        ) {
        }


        try {

            locationManager
                .getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )
                ?.let {

                    locations.add(
                        it
                    )
                }

        } catch (
            _: Exception
        ) {
        }


        return locations
            .maxByOrNull {

                it.time
            }
    }


    private fun isLocationRecent(
        location: Location,
    ): Boolean {

        val age =
            System.currentTimeMillis() -
                    location.time


        return (
                age >= 0L &&
                        age <=
                        MAX_CACHED_LOCATION_AGE_MS
                )
    }


    private fun Location.toCypherLocation():
            CypherLocation {

        return CypherLocation(
            latitude =
                latitude,

            longitude =
                longitude,
        )
    }


    private suspend fun reverseGeocode(
        location: CypherLocation,
    ): String? {

        if (
            !Geocoder.isPresent()
        ) {

            return null
        }


        return withContext(
            Dispatchers.IO
        ) {

            try {

                val geocoder =
                    Geocoder(
                        appContext,
                        Locale.getDefault(),
                    )


                @Suppress(
                    "DEPRECATION"
                )
                val addresses =
                    geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1,
                    )


                val address =
                    addresses
                        ?.firstOrNull()


                /*
                 * Android normally returns locality = "Taree".
                 *
                 * Some areas don't have a locality value, so
                 * fall back through the next useful geographic
                 * names rather than saying "your location".
                 */
                address
                    ?.locality
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: address
                        ?.subAdminArea
                        ?.takeIf {
                            it.isNotBlank()
                        }
                    ?: address
                        ?.adminArea
                        ?.takeIf {
                            it.isNotBlank()
                        }

            } catch (
                _: Exception
            ) {

                null
            }
        }
    }


    private fun getCachedDisplayName(
        location: CypherLocation,
    ): String? {

        val name =
            preferences.getString(
                PREF_LOCATION_NAME,
                null,
            )
                ?: return null


        if (
            !preferences.contains(
                PREF_LOCATION_LAT
            ) ||
            !preferences.contains(
                PREF_LOCATION_LON
            )
        ) {

            return null
        }


        val cachedLatitude =
            Double.fromBits(
                preferences.getLong(
                    PREF_LOCATION_LAT,
                    0L,
                )
            )


        val cachedLongitude =
            Double.fromBits(
                preferences.getLong(
                    PREF_LOCATION_LON,
                    0L,
                )
            )


        val distance =
            FloatArray(
                1
            )


        Location.distanceBetween(
            location.latitude,
            location.longitude,
            cachedLatitude,
            cachedLongitude,
            distance,
        )


        return if (
            distance[0] <=
            PLACE_NAME_CACHE_DISTANCE_METRES
        ) {

            name

        } else {

            null
        }
    }


    private fun saveDisplayName(
        location: CypherLocation,
        displayName: String,
    ) {

        preferences
            .edit()
            .putString(
                PREF_LOCATION_NAME,
                displayName,
            )
            .putLong(
                PREF_LOCATION_LAT,
                location.latitude.toBits(),
            )
            .putLong(
                PREF_LOCATION_LON,
                location.longitude.toBits(),
            )
            .apply()
    }
}