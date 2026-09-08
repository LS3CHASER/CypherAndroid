package com.shannon.cypher.weather.alerts

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.shannon.cypher.notifications.CypherNotificationManager


class CypherWeatherAlertService(
    context: Context,
) {

    companion object {

        private const val TAG =
            "CypherWeatherAlerts"

        private const val PREFERENCES_NAME =
            "cypher_weather_alerts"

        private const val KEY_SEEN_WARNING_FINGERPRINTS =
            "seen_warning_fingerprints"


        private val IMPORTANT_WARNING_TERMS =
            listOf(
                "severe thunderstorm",
                "severe weather",
                "flood",
                "heatwave",
                "fire weather",
                "tsunami",
                "tropical cyclone",
                "damaging wind",
                "destructive wind",
                "dangerous surf",
                "hazardous surf",
                "coastal hazard",
                "abnormally high tide",
            )


        private val CANCELLATION_TERMS =
            listOf(
                "warning cancelled",
                "warning canceled",
                "has been cancelled",
                "has been canceled",
                "is cancelled",
                "is canceled",
                "cancelled warning",
                "canceled warning",
            )
    }


    private val appContext =
        context.applicationContext


    private val alertProfile =
        CypherWeatherAlertProfile(
            appContext
        )


    private val warningClient =
        CypherBomWarningClient()


    private val notificationManager =
        CypherNotificationManager(
            appContext
        )


    private val systemNotificationManager =
        NotificationManagerCompat
            .from(
                appContext
            )


    private val alertStateStore =
        CypherWeatherAlertStateStore(
            appContext
        )


    private val preferences =
        appContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )


    fun checkForAlerts() {

        Log.i(
            TAG,
            "Service: weather alert check started."
        )

        val alertLocation =
            alertProfile
                .getLocation()

        if (
            alertLocation == null
        ) {
            Log.w(
                TAG,
                "Service: no saved weather alert location profile. " +
                        "Open the Weather screen once to save the current location."
            )
            return
        }

        Log.i(
            TAG,
            "Service: saved alert profile found. " +
                    "location=${alertLocation.displayName}, " +
                    "feed=${alertLocation.warningFeedUrl}, " +
                    "matchTerms=${alertLocation.matchTerms.joinToString()}"
        )

        Log.i(
            TAG,
            "Service: requesting BOM warning feed."
        )

        /*
         * IMPORTANT:
         * If the BOM request throws, this method exits without replacing the
         * stored active-warning state. That means a temporary network/BOM
         * failure cannot falsely make an active warning disappear.
         */
        val warnings =
            warningClient
                .getWarnings(
                    alertLocation
                        .warningFeedUrl
                )

        Log.i(
            TAG,
            "Service: BOM feed returned ${warnings.size} warning item(s)."
        )


        val previousActiveWarnings =
            alertStateStore
                .getActiveWarnings()


        val relevantActiveWarnings =
            warnings
                .filter {
                        warning ->

                    isImportantWarning(
                        warning
                    ) &&
                            isRelevantToLocation(
                                warning =
                                    warning,
                                alertLocation =
                                    alertLocation,
                            ) &&
                            !isCancellationNotice(
                                warning
                            )
                }


        val activeWarningRecords =
            relevantActiveWarnings
                .map {
                        warning ->

                    val key =
                        stableWarningKey(
                            warning
                        )

                    CypherActiveWeatherWarning(
                        key =
                            key,
                        notificationId =
                            key.hashCode(),
                        title =
                            warning.title,
                        description =
                            warning.description,
                        url =
                            warning.link,
                    )
                }


        /*
         * A successful BOM refresh is the source of truth.
         *
         * If a previously active warning is now gone from the feed, or BOM has
         * replaced it with a cancellation notice, it is removed here. Cypher's
         * Weather-screen warning triangle will therefore disappear on the next
         * UI state refresh.
         */
        alertStateStore
            .replaceActiveWarnings(
                activeWarningRecords
            )


        val activeKeys =
            activeWarningRecords
                .map {
                    it.key
                }
                .toSet()


        previousActiveWarnings
            .filter {
                it.key !in
                        activeKeys
            }
            .forEach {
                    oldWarning ->

                systemNotificationManager
                    .cancel(
                        oldWarning.notificationId
                    )

                Log.i(
                    TAG,
                    "Service: cleared inactive/cancelled warning: ${oldWarning.title}"
                )
            }


        notificationManager
            .createNotificationChannels()


        val alreadySeen =
            preferences
                .getStringSet(
                    KEY_SEEN_WARNING_FINGERPRINTS,
                    emptySet(),
                )
                ?.toMutableSet()
                ?: mutableSetOf()


        var changed =
            false

        var importantCount =
            0

        var relevantCount =
            0

        var cancelledCount =
            0

        var newAlertCount =
            0


        for (
        warning in
        warnings
        ) {

            if (
                !isImportantWarning(
                    warning
                )
            ) {
                continue
            }

            importantCount++


            if (
                !isRelevantToLocation(
                    warning =
                        warning,
                    alertLocation =
                        alertLocation,
                )
            ) {
                continue
            }

            relevantCount++


            if (
                isCancellationNotice(
                    warning
                )
            ) {

                cancelledCount++

                Log.i(
                    TAG,
                    "Service: BOM cancellation notice detected: ${warning.title}"
                )

                continue
            }


            val fingerprint =
                fingerprint(
                    warning
                )


            if (
                fingerprint in
                alreadySeen
            ) {

                Log.i(
                    TAG,
                    "Service: relevant warning already seen: ${warning.title}"
                )

                continue
            }


            val message =
                warning
                    .description
                    .ifBlank {
                        "Bureau of Meteorology warning for ${alertLocation.displayName}."
                    }
                    .take(
                        240
                    )


            val stableKey =
                stableWarningKey(
                    warning
                )


            notificationManager
                .showWeatherNotification(
                    notificationId =
                        stableKey
                            .hashCode(),
                    title =
                        warning.title,
                    message =
                        message,
                    bomWarningUrl =
                        warning.link,
                )


            Log.i(
                TAG,
                "Service: posted new weather alert: ${warning.title}"
            )

            newAlertCount++

            alreadySeen.add(
                fingerprint
            )

            changed =
                true
        }


        if (
            changed
        ) {

            val saved =
                if (
                    alreadySeen.size <=
                    200
                ) {

                    alreadySeen

                } else {

                    alreadySeen
                        .toList()
                        .takeLast(
                            100
                        )
                        .toSet()
                }


            preferences
                .edit()
                .putStringSet(
                    KEY_SEEN_WARNING_FINGERPRINTS,
                    saved,
                )
                .apply()
        }


        Log.i(
            TAG,
            "Service: check complete. " +
                    "feedItems=${warnings.size}, " +
                    "important=$importantCount, " +
                    "locationMatches=$relevantCount, " +
                    "cancelled=$cancelledCount, " +
                    "active=${activeWarningRecords.size}, " +
                    "newAlerts=$newAlertCount."
        )
    }


    private fun isImportantWarning(
        warning: CypherBomWarning,
    ): Boolean {

        val searchable =
            (
                    warning.title +
                            " " +
                            warning.description
                    )
                .lowercase()


        return IMPORTANT_WARNING_TERMS
            .any {
                    term ->

                term in
                        searchable
            }
    }


    private fun isRelevantToLocation(
        warning: CypherBomWarning,
        alertLocation: CypherWeatherAlertLocation,
    ): Boolean {

        val searchable =
            (
                    warning.title +
                            " " +
                            warning.description
                    )
                .lowercase()


        return alertLocation
            .matchTerms
            .any {
                    term ->

                term in
                        searchable
            }
    }


    private fun isCancellationNotice(
        warning: CypherBomWarning,
    ): Boolean {

        val searchable =
            (
                    warning.title +
                            " " +
                            warning.description
                    )
                .lowercase()


        return CANCELLATION_TERMS
            .any {
                    term ->

                term in
                        searchable
            }
    }


    private fun stableWarningKey(
        warning: CypherBomWarning,
    ): String {

        return warning
            .identifier
            .takeIf {
                it.isNotBlank()
            }
            ?: warning
                .link
                .takeIf {
                    it.isNotBlank()
                }
            ?: warning.title
    }


    private fun fingerprint(
        warning: CypherBomWarning,
    ): String {

        /*
         * Include title and description so a materially updated BOM warning
         * can produce a fresh notification while using the stable warning key
         * as the Android notification ID. Updated warnings therefore replace
         * the old notification instead of stacking duplicates.
         */
        return (
                stableWarningKey(
                    warning
                ) +
                        "|" +
                        warning.title +
                        "|" +
                        warning.description
                )
            .hashCode()
            .toString()
    }
}
