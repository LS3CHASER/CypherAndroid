package com.shannon.cypher.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build


class CypherCalendarReminderScheduler(
    context: Context,
) {

    private val appContext =
        context.applicationContext


    private val alarmManager =
        appContext.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager


    fun scheduleCalendarReminder(
        eventId: Long,
        eventTitle: String,
        eventStartMillis: Long,
        reminderMinutesBefore: Int,
    ) {

        if (
            eventId <= 0L ||
            eventTitle.isBlank() ||
            eventStartMillis <= 0L ||
            reminderMinutesBefore < 0
        ) {

            return
        }


        val reminderAtMillis =
            eventStartMillis -
                    (
                            reminderMinutesBefore.toLong() *
                                    60_000L
                            )


        /*
         * Do not schedule reminders that have already passed.
         */
        if (
            reminderAtMillis <=
            System.currentTimeMillis()
        ) {

            return
        }


        val pendingIntent =
            createPendingIntent(
                eventId =
                    eventId,

                eventTitle =
                    eventTitle,

                eventStartMillis =
                    eventStartMillis,

                reminderAtMillis =
                    reminderAtMillis,
            )


        val canScheduleExactAlarm =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                alarmManager
                    .canScheduleExactAlarms()

            } else {

                true
            }


        if (
            canScheduleExactAlarm
        ) {

            alarmManager
                .setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderAtMillis,
                    pendingIntent,
                )

        } else {

            /*
             * Android can still deliver the reminder without exact
             * alarm access, but the delivery time may be less precise.
             */
            alarmManager
                .setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderAtMillis,
                    pendingIntent,
                )
        }
    }


    fun cancelCalendarReminder(
        eventId: Long,
        eventStartMillis: Long,
    ) {

        if (
            eventId <= 0L ||
            eventStartMillis <= 0L
        ) {

            return
        }


        val intent =
            Intent(
                appContext,
                CypherCalendarReminderReceiver::class.java,
            )


        val pendingIntent =
            PendingIntent.getBroadcast(
                appContext,
                requestCodeForOccurrence(
                    eventId =
                        eventId,

                    eventStartMillis =
                        eventStartMillis,
                ),
                intent,
                PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE,
            )


        if (
            pendingIntent != null
        ) {

            alarmManager
                .cancel(
                    pendingIntent
                )


            pendingIntent
                .cancel()
        }
    }


    fun rescheduleCalendarReminder(
        eventId: Long,
        eventTitle: String,
        oldEventStartMillis: Long,
        newEventStartMillis: Long,
        reminderMinutesBefore: Int?,
    ) {

        /*
         * Remove the alarm belonging to the old occurrence/time first.
         */
        cancelCalendarReminder(
            eventId =
                eventId,

            eventStartMillis =
                oldEventStartMillis,
        )


        /*
         * A null reminder means the event no longer needs a
         * Cypher Calendar notification.
         */
        if (
            reminderMinutesBefore == null
        ) {

            return
        }


        scheduleCalendarReminder(
            eventId =
                eventId,

            eventTitle =
                eventTitle,

            eventStartMillis =
                newEventStartMillis,

            reminderMinutesBefore =
                reminderMinutesBefore,
        )
    }


    private fun createPendingIntent(
        eventId: Long,
        eventTitle: String,
        eventStartMillis: Long,
        reminderAtMillis: Long,
    ): PendingIntent {

        val intent =
            Intent(
                appContext,
                CypherCalendarReminderReceiver::class.java,
            ).apply {

                putExtra(
                    CypherCalendarReminderReceiver.EXTRA_EVENT_ID,
                    eventId,
                )


                putExtra(
                    CypherCalendarReminderReceiver.EXTRA_EVENT_TITLE,
                    eventTitle,
                )


                putExtra(
                    CypherCalendarReminderReceiver.EXTRA_EVENT_START_MILLIS,
                    eventStartMillis,
                )


                putExtra(
                    CypherCalendarReminderReceiver.EXTRA_REMINDER_AT_MILLIS,
                    reminderAtMillis,
                )
            }


        return PendingIntent.getBroadcast(
            appContext,
            requestCodeForOccurrence(
                eventId =
                    eventId,

                eventStartMillis =
                    eventStartMillis,
            ),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
        )
    }


    private fun requestCodeForOccurrence(
        eventId: Long,
        eventStartMillis: Long,
    ): Int {

        /*
         * Event ID alone is not enough for recurring Calendar events.
         * Combining the event ID with the occurrence start time gives
         * each occurrence its own PendingIntent.
         */
        return (
                31L * eventId +
                        eventStartMillis
                )
            .hashCode()
    }
}