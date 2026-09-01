package com.shannon.cypher.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone


class CypherCalendarManager(
    context: Context,
) {

    private val appContext =
        context.applicationContext


    fun hasReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun hasWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.WRITE_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun getTodayEvents(): List<CypherCalendarEvent> {
        val today = Calendar.getInstance()
        val range = getDayRange(today)
        return getEventsBetween(range.first, range.second)
    }


    fun getTomorrowEvents(): List<CypherCalendarEvent> {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val range = getDayRange(tomorrow)
        return getEventsBetween(range.first, range.second)
    }


    fun getEventsBetween(
        startMillis: Long,
        endMillis: Long,
    ): List<CypherCalendarEvent> {

        if (!hasReadPermission()) {
            return emptyList()
        }

        val events = mutableListOf<CypherCalendarEvent>()

        val builder =
            CalendarContract.Instances.CONTENT_URI.buildUpon()

        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection =
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            )

        appContext.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->

            val eventIdIndex =
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.EVENT_ID
                )

            val titleIndex =
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.TITLE
                )

            val beginIndex =
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.BEGIN
                )

            val endIndex =
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.END
                )

            val allDayIndex =
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.ALL_DAY
                )

            val calendarNameIndex =
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.CALENDAR_DISPLAY_NAME
                )

            while (cursor.moveToNext()) {
                events.add(
                    CypherCalendarEvent(
                        id = cursor.getLong(eventIdIndex),
                        title = cursor.getString(titleIndex) ?: "Untitled event",
                        startTimeMillis = cursor.getLong(beginIndex),
                        endTimeMillis = cursor.getLong(endIndex),
                        allDay = cursor.getInt(allDayIndex) == 1,
                        calendarName = cursor.getString(calendarNameIndex) ?: "Calendar",
                    )
                )
            }
        }

        return events
            .distinctBy { event ->
                Triple(
                    event.id,
                    event.startTimeMillis,
                    event.title,
                )
            }
            .sortedBy { it.startTimeMillis }
    }


    /*
     * Read the first alert reminder attached to an existing
     * Android / Google Calendar event.
     *
     * Returns the number of minutes before the event, or null
     * when the event has no alert reminder that Cypher should
     * mirror into its own notification system.
     */
    fun getReminderMinutes(
        eventId: Long,
    ): Int? {

        if (
            !hasReadPermission() ||
            eventId <= 0L
        ) {

            return null
        }


        val projection =
            arrayOf(
                CalendarContract.Reminders.MINUTES,
                CalendarContract.Reminders.METHOD,
            )


        appContext
            .contentResolver
            .query(
                CalendarContract.Reminders.CONTENT_URI,
                projection,
                "${CalendarContract.Reminders.EVENT_ID} = ?",
                arrayOf(
                    eventId.toString()
                ),
                "${CalendarContract.Reminders.MINUTES} ASC",
            )
            ?.use {
                    cursor ->

                val minutesIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract.Reminders.MINUTES
                    )


                val methodIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract.Reminders.METHOD
                    )


                while (
                    cursor.moveToNext()
                ) {

                    val method =
                        cursor.getInt(
                            methodIndex
                        )


                    if (
                        method ==
                        CalendarContract.Reminders.METHOD_ALERT
                    ) {

                        val minutes =
                            cursor.getInt(
                                minutesIndex
                            )


                        if (
                            minutes >= 0
                        ) {

                            return minutes
                        }
                    }
                }
            }


        return null
    }


    fun createEvent(
        title: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        reminderMinutes: Int? = null,
    ): Long? {

        if (!hasWritePermission()) {
            return null
        }

        val calendarId = getWritableCalendarId() ?: return null

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startTimeMillis)
            put(CalendarContract.Events.DTEND, endTimeMillis)
            put(
                CalendarContract.Events.EVENT_TIMEZONE,
                TimeZone.getDefault().id,
            )

            if (reminderMinutes != null) {
                put(CalendarContract.Events.HAS_ALARM, 1)
            }
        }

        val uri =
            appContext.contentResolver.insert(
                CalendarContract.Events.CONTENT_URI,
                values,
            ) ?: return null

        val eventId =
            uri.lastPathSegment?.toLongOrNull()
                ?: return null

        if (reminderMinutes != null) {
            addReminder(
                eventId = eventId,
                minutesBefore = reminderMinutes,
            )
        }

        return eventId
    }


    fun updateEventTime(
        eventId: Long,
        startTimeMillis: Long,
        endTimeMillis: Long,
    ): Boolean {

        if (!hasWritePermission()) {
            return false
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTimeMillis)
            put(CalendarContract.Events.DTEND, endTimeMillis)
            put(
                CalendarContract.Events.EVENT_TIMEZONE,
                TimeZone.getDefault().id,
            )
        }

        val uri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )

        val rowsUpdated =
            appContext.contentResolver.update(
                uri,
                values,
                null,
                null,
            )

        return rowsUpdated > 0
    }


    fun deleteEvent(
        eventId: Long,
    ): Boolean {

        if (!hasWritePermission()) {
            return false
        }

        val uri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )

        val rowsDeleted =
            appContext.contentResolver.delete(
                uri,
                null,
                null,
            )

        return rowsDeleted > 0
    }


    fun addReminder(
        eventId: Long,
        minutesBefore: Int,
    ): Boolean {

        if (!hasWritePermission()) {
            return false
        }

        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, minutesBefore)
            put(
                CalendarContract.Reminders.METHOD,
                CalendarContract.Reminders.METHOD_ALERT,
            )
        }

        return appContext.contentResolver.insert(
            CalendarContract.Reminders.CONTENT_URI,
            values,
        ) != null
    }


    fun replaceReminder(
        eventId: Long,
        minutesBefore: Int,
    ): Boolean {

        if (!hasWritePermission()) {
            return false
        }

        appContext.contentResolver.delete(
            CalendarContract.Reminders.CONTENT_URI,
            "${CalendarContract.Reminders.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
        )

        val eventUri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )

        appContext.contentResolver.update(
            eventUri,
            ContentValues().apply {
                put(CalendarContract.Events.HAS_ALARM, 1)
            },
            null,
            null,
        )

        return addReminder(
            eventId = eventId,
            minutesBefore = minutesBefore,
        )
    }


    fun findEvents(
        titleWords: String,
        startMillis: Long,
        endMillis: Long,
    ): List<CypherCalendarEvent> {

        val words =
            titleWords
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.length > 1 }

        if (words.isEmpty()) {
            return emptyList()
        }

        return getEventsBetween(
            startMillis,
            endMillis,
        ).filter { event ->

            val title = event.title.lowercase()

            words.all { word ->
                title.contains(word)
            }
        }
    }


    private fun getWritableCalendarId(): Long? {

        if (!hasReadPermission()) {
            return null
        }

        val projection =
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.VISIBLE,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            )

        appContext.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Calendars._ID
                )

            val visibleIndex =
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Calendars.VISIBLE
                )

            val accessIndex =
                cursor.getColumnIndexOrThrow(
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
                )

            while (cursor.moveToNext()) {

                val visible =
                    cursor.getInt(visibleIndex) == 1

                val accessLevel =
                    cursor.getInt(accessIndex)

                val writable =
                    accessLevel >=
                            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR

                if (visible && writable) {
                    return cursor.getLong(idIndex)
                }
            }
        }

        return null
    }


    private fun getDayRange(
        date: Calendar,
    ): Pair<Long, Long> {

        val start = date.clone() as Calendar
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = date.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return Pair(
            start.timeInMillis,
            end.timeInMillis,
        )
    }
}
