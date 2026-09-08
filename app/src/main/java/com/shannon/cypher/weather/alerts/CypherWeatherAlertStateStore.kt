package com.shannon.cypher.weather.alerts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject


data class CypherActiveWeatherWarning(
    val key: String,
    val notificationId: Int,
    val title: String,
    val description: String,
    val url: String,
)


class CypherWeatherAlertStateStore(
    context: Context,
) {

    companion object {

        private const val PREFERENCES_NAME =
            "cypher_weather_alert_state"

        private const val KEY_ACTIVE_WARNINGS_JSON =
            "active_warnings_json"

        private const val KEY_LAST_SUCCESSFUL_CHECK =
            "last_successful_check"
    }


    private val preferences =
        context.applicationContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )


    fun getActiveWarnings():
            List<CypherActiveWeatherWarning> {

        val raw =
            preferences.getString(
                KEY_ACTIVE_WARNINGS_JSON,
                null,
            )
                ?: return emptyList()


        return try {

            val array =
                JSONArray(
                    raw
                )


            buildList {

                for (
                index in
                0 until array.length()
                ) {

                    val item =
                        array.getJSONObject(
                            index
                        )


                    add(
                        CypherActiveWeatherWarning(
                            key =
                                item.optString(
                                    "key"
                                ),

                            notificationId =
                                item.optInt(
                                    "notificationId"
                                ),

                            title =
                                item.optString(
                                    "title"
                                ),

                            description =
                                item.optString(
                                    "description"
                                ),

                            url =
                                item.optString(
                                    "url"
                                ),
                        )
                    )
                }
            }

        } catch (
            _: Exception
        ) {

            emptyList()
        }
    }


    fun replaceActiveWarnings(
        warnings:
        List<CypherActiveWeatherWarning>,
    ) {

        val array =
            JSONArray()


        warnings.forEach {
                warning ->

            array.put(
                JSONObject()
                    .put(
                        "key",
                        warning.key,
                    )
                    .put(
                        "notificationId",
                        warning.notificationId,
                    )
                    .put(
                        "title",
                        warning.title,
                    )
                    .put(
                        "description",
                        warning.description,
                    )
                    .put(
                        "url",
                        warning.url,
                    )
            )
        }


        preferences
            .edit()
            .putString(
                KEY_ACTIVE_WARNINGS_JSON,
                array.toString(),
            )
            .putLong(
                KEY_LAST_SUCCESSFUL_CHECK,
                System.currentTimeMillis(),
            )
            .apply()
    }


    fun getLastSuccessfulCheckMillis():
            Long {

        return preferences
            .getLong(
                KEY_LAST_SUCCESSFUL_CHECK,
                0L,
            )
    }
}