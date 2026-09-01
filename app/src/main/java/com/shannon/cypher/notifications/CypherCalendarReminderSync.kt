package com.shannon.cypher.notifications

import android.content.Context
import com.shannon.cypher.calendar.CypherCalendarManager
import java.util.Calendar


class CypherCalendarReminderSync(
    context: Context,
) {

    companion object {

        /*
         * Scan far enough ahead to cover normal upcoming events
         * without querying an unnecessarily large Calendar range.
         */
        private const val SYNC_DAYS_AHEAD =
            30


        private const val PREFERENCES_NAME =
            "cypher_calendar_reminder_sync"


        private const val KEY_SCHEDULED_OCCURRENCES =
            "scheduled_occurrences"
    }


    private val appContext =
        context.applicationContext


    private val calendarManager =
        CypherCalendarManager(
            appContext
        )


    private val reminderScheduler =
        CypherCalendarReminderScheduler(
            appContext
        )


    private val preferences =
        appContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )


    /*
     * Scan upcoming Android / Google Calendar events and mirror
     * their existing alert reminders into Cypher's alarm system.
     *
     * The scheduler uses event ID + occurrence start time, so
     * recurring event occurrences remain separate and calling this
     * sync repeatedly does not create duplicate PendingIntents.
     */
    fun syncUpcomingCalendarReminders() {

        if (
            !calendarManager.hasReadPermission()
        ) {

            return
        }


        val now =
            System.currentTimeMillis()


        val endCalendar =
            Calendar.getInstance().apply {

                timeInMillis =
                    now

                add(
                    Calendar.DAY_OF_YEAR,
                    SYNC_DAYS_AHEAD,
                )
            }


        val events =
            calendarManager.getEventsBetween(
                startMillis =
                    now,

                endMillis =
                    endCalendar.timeInMillis,
            )


        val activeOccurrenceKeys =
            mutableSetOf<String>()


        for (
        event in events
        ) {

            /*
             * All Calendar occurrences can be read here, including
             * recurring events returned through CalendarContract.Instances.
             *
             * Reminder settings belong to the base event ID, so the
             * same reminder minutes are applied to each occurrence.
             */
            val reminderMinutes =
                calendarManager.getReminderMinutes(
                    event.id
                )
                    ?: continue


            val reminderAtMillis =
                event.startTimeMillis -
                        (
                                reminderMinutes.toLong() *
                                        60_000L
                                )


            /*
             * Do not track or schedule reminders that are already past.
             */
            if (
                reminderAtMillis <=
                now
            ) {

                continue
            }


            reminderScheduler
                .scheduleCalendarReminder(
                    eventId =
                        event.id,

                    eventTitle =
                        event.title,

                    eventStartMillis =
                        event.startTimeMillis,

                    reminderMinutesBefore =
                        reminderMinutes,
                )


            activeOccurrenceKeys.add(
                occurrenceKey(
                    eventId =
                        event.id,

                    eventStartMillis =
                        event.startTimeMillis,
                )
            )
        }


        /*
         * Cancel alarms that Cypher scheduled during an earlier sync
         * but which no longer appear in the upcoming Calendar window.
         *
         * This covers deleted events and moved/rescheduled occurrences.
         */
        val previouslyScheduled =
            preferences
                .getStringSet(
                    KEY_SCHEDULED_OCCURRENCES,
                    emptySet(),
                )
                ?.toSet()
                ?: emptySet()


        val staleOccurrences =
            previouslyScheduled -
                    activeOccurrenceKeys


        for (
        occurrence in staleOccurrences
        ) {

            val parsed =
                parseOccurrenceKey(
                    occurrence
                )
                    ?: continue


            reminderScheduler
                .cancelCalendarReminder(
                    eventId =
                        parsed.first,

                    eventStartMillis =
                        parsed.second,
                )
        }


        preferences
            .edit()
            .putStringSet(
                KEY_SCHEDULED_OCCURRENCES,
                activeOccurrenceKeys,
            )
            .apply()
    }


    private fun occurrenceKey(
        eventId: Long,
        eventStartMillis: Long,
    ): String {

        return "$eventId:$eventStartMillis"
    }


    private fun parseOccurrenceKey(
        value: String,
    ): Pair<Long, Long>? {

        val separatorIndex =
            value.indexOf(
                ':'
            )


        if (
            separatorIndex <= 0 ||
            separatorIndex >=
            value.lastIndex
        ) {

            return null
        }


        val eventId =
            value
                .substring(
                    0,
                    separatorIndex,
                )
                .toLongOrNull()
                ?: return null


        val eventStartMillis =
            value
                .substring(
                    separatorIndex + 1
                )
                .toLongOrNull()
                ?: return null


        if (
            eventId <= 0L ||
            eventStartMillis <= 0L
        ) {

            return null
        }


        return Pair(
            eventId,
            eventStartMillis,
        )
    }
}
