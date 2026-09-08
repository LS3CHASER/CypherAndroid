package com.shannon.cypher.weather.alerts

import android.content.Context


data class CypherWeatherAlertLocation(
    val stateCode: String,
    val displayName: String,
    val warningFeedUrl: String,
    val matchTerms: Set<String>,
)


class CypherWeatherAlertProfile(
    context: Context,
) {

    companion object {

        private const val PREFERENCES_NAME =
            "cypher_weather_alert_profile"

        private const val KEY_STATE =
            "state"

        private const val KEY_DISPLAY_NAME =
            "display_name"

        private const val KEY_FEED_URL =
            "feed_url"

        private const val KEY_MATCH_TERMS =
            "match_terms"


        private const val NSW_LAND_WARNINGS =
            "https://www.bom.gov.au/fwo/IDZ00061.warnings_land_nsw.xml"

        private const val VIC_LAND_WARNINGS =
            "https://www.bom.gov.au/fwo/IDZ00066.warnings_land_vic.xml"

        private const val QLD_LAND_WARNINGS =
            "https://www.bom.gov.au/fwo/IDZ00063.warnings_land_qld.xml"

        private const val WA_LAND_WARNINGS =
            "https://www.bom.gov.au/fwo/IDZ00067.warnings_land_wa.xml"

        private const val SA_LAND_WARNINGS =
            "https://www.bom.gov.au/fwo/IDZ00064.warnings_land_sa.xml"

        private const val TAS_LAND_WARNINGS =
            "https://www.bom.gov.au/fwo/IDZ00065.warnings_land_tas.xml"

        private const val NT_LAND_WARNINGS =
            "https://www.bom.gov.au/fwo/IDZ00062.warnings_land_nt.xml"
    }


    private val preferences =
        context.applicationContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )


    fun updateFromLocation(
        displayName: String,
        latitude: Double,
        longitude: Double,
    ) {

        val cleanDisplayName =
            displayName
                .trim()
                .ifBlank {
                    "Current location"
                }


        val state =
            stateForCoordinates(
                latitude = latitude,
                longitude = longitude,
            )


        val feedUrl =
            feedForState(
                state
            )
                ?: return


        val terms =
            buildMatchTerms(
                displayName = cleanDisplayName,
                latitude = latitude,
                longitude = longitude,
                stateCode = state,
            )


        preferences
            .edit()
            .putString(
                KEY_STATE,
                state,
            )
            .putString(
                KEY_DISPLAY_NAME,
                cleanDisplayName,
            )
            .putString(
                KEY_FEED_URL,
                feedUrl,
            )
            .putStringSet(
                KEY_MATCH_TERMS,
                terms,
            )
            .apply()
    }


    fun getLocation():
            CypherWeatherAlertLocation? {

        val state =
            preferences
                .getString(
                    KEY_STATE,
                    null,
                )
                ?.trim()
                .orEmpty()


        val displayName =
            preferences
                .getString(
                    KEY_DISPLAY_NAME,
                    null,
                )
                ?.trim()
                .orEmpty()


        val feedUrl =
            preferences
                .getString(
                    KEY_FEED_URL,
                    null,
                )
                ?.trim()
                .orEmpty()


        val terms =
            preferences
                .getStringSet(
                    KEY_MATCH_TERMS,
                    emptySet(),
                )
                ?.map {
                    it.trim().lowercase()
                }
                ?.filter {
                    it.isNotBlank()
                }
                ?.toSet()
                ?: emptySet()


        if (
            state.isBlank() ||
            feedUrl.isBlank() ||
            terms.isEmpty()
        ) {

            return null
        }


        return CypherWeatherAlertLocation(
            stateCode =
                state,

            displayName =
                displayName.ifBlank {
                    "your area"
                },

            warningFeedUrl =
                feedUrl,

            matchTerms =
                terms,
        )
    }


    private fun stateForCoordinates(
        latitude: Double,
        longitude: Double,
    ): String {

        /*
         * This only selects the BOM state warning feed.
         * Location relevance is then narrowed using the saved
         * locality / forecast-district terms below.
         *
         * The ranges are deliberately conservative and are more than
         * adequate for Cypher's normal Australian use. Border areas
         * can still be refined later with reverse-geocoded admin areas.
         */
        return when {

            latitude <= -39.0 ->
                "TAS"

            latitude <= -33.8 &&
                    longitude in 140.7..150.2 ->
                "VIC"

            longitude < 129.0 ->
                "WA"

            longitude < 141.0 &&
                    latitude < -26.0 ->
                "SA"

            latitude > -26.0 &&
                    longitude < 138.0 ->
                "NT"

            latitude > -29.2 &&
                    longitude >= 138.0 ->
                "QLD"

            else ->
                "NSW"
        }
    }


    private fun feedForState(
        stateCode: String,
    ): String? {

        return when (
            stateCode
        ) {

            "NSW" ->
                NSW_LAND_WARNINGS

            "VIC" ->
                VIC_LAND_WARNINGS

            "QLD" ->
                QLD_LAND_WARNINGS

            "WA" ->
                WA_LAND_WARNINGS

            "SA" ->
                SA_LAND_WARNINGS

            "TAS" ->
                TAS_LAND_WARNINGS

            "NT" ->
                NT_LAND_WARNINGS

            else ->
                null
        }
    }


    private fun buildMatchTerms(
        displayName: String,
        latitude: Double,
        longitude: Double,
        stateCode: String,
    ): Set<String> {

        val terms =
            linkedSetOf<String>()


        val locality =
            displayName
                .lowercase()
                .trim()


        if (
            locality.isNotBlank() &&
            locality !=
            "your current location"
        ) {

            terms.add(
                locality
            )
        }


        /*
         * Forecast-district aliases make BOM warnings useful even when
         * the warning names the district rather than the suburb/town.
         *
         * Taree, for example, is normally covered by Mid North Coast
         * wording rather than every warning explicitly saying "Taree".
         */
        if (
            stateCode ==
            "NSW"
        ) {

            when {

                latitude > -30.2 &&
                        longitude > 151.0 -> {

                    terms.add(
                        "northern rivers"
                    )

                    terms.add(
                        "northern tablelands"
                    )
                }


                latitude in -33.0..-30.2 &&
                        longitude > 151.0 -> {

                    terms.add(
                        "mid north coast"
                    )

                    terms.add(
                        "manning"
                    )

                    terms.add(
                        "midcoast"
                    )

                    terms.add(
                        "gloucester"
                    )
                }


                latitude in -33.6..-31.5 &&
                        longitude > 150.4 -> {

                    terms.add(
                        "hunter"
                    )

                    terms.add(
                        "newcastle"
                    )
                }


                latitude in -34.2..-33.4 &&
                        longitude > 150.4 -> {

                    terms.add(
                        "sydney"
                    )

                    terms.add(
                        "metropolitan"
                    )
                }


                latitude < -34.2 &&
                        longitude > 149.4 -> {

                    terms.add(
                        "south coast"
                    )

                    terms.add(
                        "illawarra"
                    )
                }
            }
        }


        return terms
    }
}
