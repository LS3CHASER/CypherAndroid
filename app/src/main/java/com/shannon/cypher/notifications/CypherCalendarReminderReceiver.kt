package com.shannon.cypher.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class CypherCalendarReminderReceiver :
    BroadcastReceiver() {

    companion object {

        const val EXTRA_EVENT_ID =
            "cypher_calendar_event_id"

        const val EXTRA_EVENT_TITLE =
            "cypher_calendar_event_title"

        const val EXTRA_EVENT_START_MILLIS =
            "cypher_calendar_event_start_millis"

        const val EXTRA_REMINDER_AT_MILLIS =
            "cypher_calendar_reminder_at_millis"
    }


    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {

        val eventId =
            intent.getLongExtra(
                EXTRA_EVENT_ID,
                -1L,
            )


        val eventTitle =
            intent.getStringExtra(
                EXTRA_EVENT_TITLE
            )
                ?.trim()
                .orEmpty()


        val eventStartMillis =
            intent.getLongExtra(
                EXTRA_EVENT_START_MILLIS,
                -1L,
            )


        if (
            eventId <= 0L ||
            eventTitle.isBlank() ||
            eventStartMillis <= 0L
        ) {

            return
        }


        val notificationManager =
            CypherNotificationManager(
                context.applicationContext
            )


        /*
         * The reminder can fire while Cypher is completely closed,
         * so make sure the Calendar notification channel exists.
         */
        notificationManager
            .createNotificationChannels()


        val now =
            System.currentTimeMillis()


        val minutesUntilEvent =
            ((eventStartMillis - now) / 60_000L)
                .coerceAtLeast(
                    0L
                )


        val message =
            when {

                minutesUntilEvent <= 0L -> {

                    "$eventTitle is starting now."
                }


                minutesUntilEvent == 1L -> {

                    "$eventTitle starts in 1 minute."
                }


                minutesUntilEvent < 60L -> {

                    "$eventTitle starts in $minutesUntilEvent minutes."
                }


                minutesUntilEvent < 120L -> {

                    "$eventTitle starts in about 1 hour."
                }


                else -> {

                    val hours =
                        minutesUntilEvent / 60L

                    "$eventTitle starts in about $hours hours."
                }
            }


        notificationManager
            .showCalendarNotification(
                notificationId =
                    notificationIdFor(
                        eventId =
                            eventId,

                        eventStartMillis =
                            eventStartMillis,
                    ),

                title =
                    "Calendar reminder",

                message =
                    message,

                calendarEventId =
                    eventId,
            )
    }


    private fun notificationIdFor(
        eventId: Long,
        eventStartMillis: Long,
    ): Int {

        /*
         * Include the occurrence start time as well as the event ID.
         * This keeps separate occurrences of a recurring event from
         * replacing each other's notifications.
         */
        return (
                31L * eventId +
                        eventStartMillis
                )
            .hashCode()
    }
}