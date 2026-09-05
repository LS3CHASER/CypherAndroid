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


    private data class RecurringEventBase(
        val calendarId: Long,
        val title: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val allDay: Boolean,
        val eventTimezone: String,
    )

    data class EventDetails(
        val location: String,
        val description: String,
    )


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


    fun getEventDetails(
        eventId: Long,
    ): EventDetails {

        if (
            !hasReadPermission() ||
            eventId <= 0L
        ) {
            return EventDetails(
                location = "",
                description = "",
            )
        }

        val uri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )

        val projection =
            arrayOf(
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DESCRIPTION,
            )

        appContext.contentResolver
            .query(
                uri,
                projection,
                null,
                null,
                null,
            )
            ?.use { cursor ->

                if (cursor.moveToFirst()) {
                    return EventDetails(
                        location =
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    CalendarContract.Events.EVENT_LOCATION
                                )
                            ).orEmpty(),

                        description =
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    CalendarContract.Events.DESCRIPTION
                                )
                            ).orEmpty(),
                    )
                }
            }

        return EventDetails(
            location = "",
            description = "",
        )
    }


    fun getRecurrenceRule(
        eventId: Long,
    ): String? {

        if (
            !hasReadPermission() ||
            eventId <= 0L
        ) {
            return null
        }

        val uri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )

        val projection =
            arrayOf(
                CalendarContract.Events.RRULE,
            )

        appContext.contentResolver
            .query(
                uri,
                projection,
                null,
                null,
                null,
            )
            ?.use { cursor ->

                if (!cursor.moveToFirst()) {
                    return null
                }

                return cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        CalendarContract.Events.RRULE
                    )
                )
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }

        return null
    }


    /*
     * Returns true when the base Android Calendar event belongs to
     * a recurring series.
     *
     * The Calendar screen uses this to avoid accidentally changing
     * or deleting an entire recurring series when the user only
     * intended to touch one visible occurrence.
     */
    fun isRecurringEvent(
        eventId: Long,
    ): Boolean {

        if (
            !hasReadPermission() ||
            eventId <= 0L
        ) {

            return false
        }


        val uri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )


        val projection =
            arrayOf(
                CalendarContract.Events.RRULE,
                CalendarContract.Events.RDATE,
            )


        appContext
            .contentResolver
            .query(
                uri,
                projection,
                null,
                null,
                null,
            )
            ?.use {
                    cursor ->

                if (
                    !cursor.moveToFirst()
                ) {

                    return false
                }


                val rruleIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract.Events.RRULE
                    )


                val rdateIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract.Events.RDATE
                    )


                val rrule =
                    cursor.getString(
                        rruleIndex
                    )
                        ?.trim()
                        .orEmpty()


                val rdate =
                    cursor.getString(
                        rdateIndex
                    )
                        ?.trim()
                        .orEmpty()


                return (
                        rrule.isNotBlank() ||
                                rdate.isNotBlank()
                        )
            }


        return false
    }


    /*
     * Create an exception row for one occurrence of a recurring event.
     *
     * Android Calendar keeps the base series intact and replaces only
     * the selected occurrence with this exception.
     */
    fun updateRecurringOccurrence(
        eventId: Long,
        originalOccurrenceStartMillis: Long,
        originalAllDay: Boolean,
        title: String,
        newStartTimeMillis: Long,
        newEndTimeMillis: Long,
        reminderMinutes: Int?,
        location: String = "",
        description: String = "",
    ): Long? {

        if (
            !hasWritePermission() ||
            eventId <= 0L ||
            originalOccurrenceStartMillis <= 0L ||
            title.isBlank() ||
            newStartTimeMillis <= 0L ||
            newEndTimeMillis <= newStartTimeMillis
        ) {

            return null
        }


        val base =
            getRecurringEventBase(
                eventId
            )
                ?: return null


        val values =
            ContentValues().apply {

                put(
                    CalendarContract.Events.CALENDAR_ID,
                    base.calendarId,
                )

                put(
                    CalendarContract.Events.TITLE,
                    title.trim(),
                )

                put(
                    CalendarContract.Events.EVENT_LOCATION,
                    location.trim(),
                )

                put(
                    CalendarContract.Events.DESCRIPTION,
                    description.trim(),
                )

                put(
                    CalendarContract.Events.DTSTART,
                    newStartTimeMillis,
                )

                put(
                    CalendarContract.Events.DTEND,
                    newEndTimeMillis,
                )

                put(
                    CalendarContract.Events.EVENT_TIMEZONE,
                    base.eventTimezone,
                )

                put(
                    CalendarContract.Events.ALL_DAY,
                    if (
                        base.allDay
                    ) {
                        1
                    } else {
                        0
                    },
                )

                put(
                    CalendarContract.Events.ORIGINAL_ID,
                    eventId,
                )

                put(
                    CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
                    originalOccurrenceStartMillis,
                )

                put(
                    CalendarContract.Events.ORIGINAL_ALL_DAY,
                    if (
                        originalAllDay
                    ) {
                        1
                    } else {
                        0
                    },
                )

                put(
                    CalendarContract.Events.STATUS,
                    CalendarContract.Events.STATUS_CONFIRMED,
                )

                put(
                    CalendarContract.Events.HAS_ALARM,
                    if (
                        reminderMinutes != null
                    ) {
                        1
                    } else {
                        0
                    },
                )
            }


        val uri =
            appContext
                .contentResolver
                .insert(
                    CalendarContract.Events.CONTENT_URI,
                    values,
                )
                ?: return null


        val exceptionEventId =
            uri.lastPathSegment
                ?.toLongOrNull()
                ?: return null


        if (
            reminderMinutes != null
        ) {

            addReminder(
                eventId =
                    exceptionEventId,

                minutesBefore =
                    reminderMinutes,
            )
        }


        return exceptionEventId
    }


    /*
     * Cancel one occurrence of a recurring series without deleting
     * the complete series.
     */
    fun deleteRecurringOccurrence(
        eventId: Long,
        occurrenceStartMillis: Long,
        occurrenceEndMillis: Long,
        originalAllDay: Boolean,
    ): Boolean {

        if (
            !hasWritePermission() ||
            eventId <= 0L ||
            occurrenceStartMillis <= 0L ||
            occurrenceEndMillis <= occurrenceStartMillis
        ) {

            return false
        }


        val base =
            getRecurringEventBase(
                eventId
            )
                ?: return false


        val values =
            ContentValues().apply {

                put(
                    CalendarContract.Events.CALENDAR_ID,
                    base.calendarId,
                )

                put(
                    CalendarContract.Events.TITLE,
                    base.title,
                )

                put(
                    CalendarContract.Events.DTSTART,
                    occurrenceStartMillis,
                )

                put(
                    CalendarContract.Events.DTEND,
                    occurrenceEndMillis,
                )

                put(
                    CalendarContract.Events.EVENT_TIMEZONE,
                    base.eventTimezone,
                )

                put(
                    CalendarContract.Events.ALL_DAY,
                    if (
                        base.allDay
                    ) {
                        1
                    } else {
                        0
                    },
                )

                put(
                    CalendarContract.Events.ORIGINAL_ID,
                    eventId,
                )

                put(
                    CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
                    occurrenceStartMillis,
                )

                put(
                    CalendarContract.Events.ORIGINAL_ALL_DAY,
                    if (
                        originalAllDay
                    ) {
                        1
                    } else {
                        0
                    },
                )

                put(
                    CalendarContract.Events.STATUS,
                    CalendarContract.Events.STATUS_CANCELED,
                )
            }


        return appContext
            .contentResolver
            .insert(
                CalendarContract.Events.CONTENT_URI,
                values,
            ) != null
    }


    /*
     * Apply an edit to the entire recurring series.
     *
     * The selected occurrence provides the offset between the existing
     * series time and the new desired time. That same offset is applied
     * to the base event while the existing RRULE / RDATE stays intact.
     */
    fun updateRecurringSeries(
        eventId: Long,
        selectedOccurrenceStartMillis: Long,
        title: String,
        newOccurrenceStartMillis: Long,
        newOccurrenceEndMillis: Long,
        recurrenceRule: String? = null,
        location: String = "",
        description: String = "",
        allDay: Boolean = false,
    ): Boolean {

        if (
            !hasWritePermission() ||
            eventId <= 0L ||
            selectedOccurrenceStartMillis <= 0L ||
            title.isBlank() ||
            newOccurrenceStartMillis <= 0L ||
            newOccurrenceEndMillis <= newOccurrenceStartMillis
        ) {
            return false
        }

        val base =
            getRecurringEventBase(
                eventId
            )
                ?: return false

        val startOffsetMillis =
            newOccurrenceStartMillis -
                    selectedOccurrenceStartMillis

        val newBaseStartMillis =
            base.startTimeMillis +
                    startOffsetMillis

        val newBaseEndMillis =
            newBaseStartMillis +
                    (
                            newOccurrenceEndMillis -
                                    newOccurrenceStartMillis
                            )

        val ruleToUse =
            recurrenceRule
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: getRecurrenceRule(
                    eventId
                )

        return updateEventRecurrence(
            eventId =
                eventId,

            title =
                title,

            startTimeMillis =
                newBaseStartMillis,

            endTimeMillis =
                newBaseEndMillis,

            recurrenceRule =
                ruleToUse,

            location =
                location,

            description =
                description,

            allDay =
                allDay,
        )
    }


    private fun getRecurringEventBase(
        eventId: Long,
    ): RecurringEventBase? {

        if (
            !hasReadPermission() ||
            eventId <= 0L
        ) {

            return null
        }


        val uri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )


        val projection =
            arrayOf(
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_TIMEZONE,
            )


        appContext
            .contentResolver
            .query(
                uri,
                projection,
                null,
                null,
                null,
            )
            ?.use {
                    cursor ->

                if (
                    !cursor.moveToFirst()
                ) {

                    return null
                }


                val calendarId =
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            CalendarContract.Events.CALENDAR_ID
                        )
                    )


                val title =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            CalendarContract.Events.TITLE
                        )
                    )
                        ?: "Untitled event"


                val start =
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            CalendarContract.Events.DTSTART
                        )
                    )


                val end =
                    cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            CalendarContract.Events.DTEND
                        )
                    )


                val allDay =
                    cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                            CalendarContract.Events.ALL_DAY
                        )
                    ) == 1


                val timezone =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            CalendarContract.Events.EVENT_TIMEZONE
                        )
                    )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: TimeZone.getDefault().id


                return RecurringEventBase(
                    calendarId =
                        calendarId,

                    title =
                        title,

                    startTimeMillis =
                        start,

                    endTimeMillis =
                        end,

                    allDay =
                        allDay,

                    eventTimezone =
                        timezone,
                )
            }


        return null
    }


    fun createEvent(
        title: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        reminderMinutes: Int? = null,
        recurrenceRule: String? = null,
        location: String = "",
        description: String = "",
        allDay: Boolean = false,
    ): Long? {

        if (
            !hasWritePermission() ||
            title.isBlank() ||
            startTimeMillis <= 0L ||
            endTimeMillis <= startTimeMillis
        ) {
            return null
        }

        val calendarId =
            getWritableCalendarId()
                ?: return null

        val cleanRule =
            recurrenceRule
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val durationSeconds =
            (endTimeMillis - startTimeMillis) / 1000L

        val values =
            ContentValues().apply {

                put(
                    CalendarContract.Events.CALENDAR_ID,
                    calendarId,
                )

                put(
                    CalendarContract.Events.TITLE,
                    title.trim(),
                )

                put(
                    CalendarContract.Events.EVENT_LOCATION,
                    location.trim(),
                )

                put(
                    CalendarContract.Events.DESCRIPTION,
                    description.trim(),
                )

                put(
                    CalendarContract.Events.ALL_DAY,
                    if (allDay) 1 else 0,
                )


                put(
                    CalendarContract.Events.DTSTART,
                    startTimeMillis,
                )

                put(
                    CalendarContract.Events.EVENT_TIMEZONE,
                    if (allDay) "UTC" else TimeZone.getDefault().id,
                )

                if (cleanRule == null) {

                    put(
                        CalendarContract.Events.DTEND,
                        endTimeMillis,
                    )

                } else {

                    put(
                        CalendarContract.Events.RRULE,
                        cleanRule,
                    )

                    put(
                        CalendarContract.Events.DURATION,
                        "P${durationSeconds}S",
                    )
                }

                if (reminderMinutes != null) {
                    put(
                        CalendarContract.Events.HAS_ALARM,
                        1,
                    )
                }
            }

        val uri =
            appContext.contentResolver
                .insert(
                    CalendarContract.Events.CONTENT_URI,
                    values,
                )
                ?: return null

        val eventId =
            uri.lastPathSegment
                ?.toLongOrNull()
                ?: return null

        reminderMinutes
            ?.let { minutes ->
                addReminder(
                    eventId =
                        eventId,

                    minutesBefore =
                        minutes,
                )
            }

        return eventId
    }


    /*
     * Update the normal editable fields used by the Calendar screen.
     *
     * Recurring-series safety is handled by the UI before this
     * method is called.
     */
    fun updateEvent(
        eventId: Long,
        title: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        location: String = "",
        description: String = "",
        allDay: Boolean = false,
    ): Boolean {

        if (
            !hasWritePermission() ||
            eventId <= 0L ||
            title.isBlank() ||
            startTimeMillis <= 0L ||
            endTimeMillis <= startTimeMillis
        ) {

            return false
        }


        val values =
            ContentValues().apply {

                put(
                    CalendarContract.Events.TITLE,
                    title.trim(),
                )

                put(
                    CalendarContract.Events.EVENT_LOCATION,
                    location.trim(),
                )

                put(
                    CalendarContract.Events.DESCRIPTION,
                    description.trim(),
                )

                put(
                    CalendarContract.Events.ALL_DAY,
                    if (allDay) 1 else 0,
                )


                put(
                    CalendarContract.Events.DTSTART,
                    startTimeMillis,
                )

                put(
                    CalendarContract.Events.DTEND,
                    endTimeMillis,
                )

                put(
                    CalendarContract.Events.EVENT_TIMEZONE,
                    if (allDay) "UTC" else TimeZone.getDefault().id,
                )
            }


        val uri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )


        val rowsUpdated =
            appContext
                .contentResolver
                .update(
                    uri,
                    values,
                    null,
                    null,
                )


        return rowsUpdated > 0
    }


    fun updateEventRecurrence(
        eventId: Long,
        title: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        recurrenceRule: String?,
        location: String = "",
        description: String = "",
        allDay: Boolean = false,
    ): Boolean {

        if (
            !hasWritePermission() ||
            eventId <= 0L ||
            title.isBlank() ||
            startTimeMillis <= 0L ||
            endTimeMillis <= startTimeMillis
        ) {
            return false
        }

        val cleanRule =
            recurrenceRule
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val durationSeconds =
            (endTimeMillis - startTimeMillis) / 1000L

        val values =
            ContentValues().apply {

                put(
                    CalendarContract.Events.TITLE,
                    title.trim(),
                )

                put(
                    CalendarContract.Events.EVENT_LOCATION,
                    location.trim(),
                )

                put(
                    CalendarContract.Events.DESCRIPTION,
                    description.trim(),
                )

                put(
                    CalendarContract.Events.ALL_DAY,
                    if (allDay) 1 else 0,
                )


                put(
                    CalendarContract.Events.DTSTART,
                    startTimeMillis,
                )

                put(
                    CalendarContract.Events.EVENT_TIMEZONE,
                    if (allDay) "UTC" else TimeZone.getDefault().id,
                )

                if (cleanRule == null) {

                    putNull(
                        CalendarContract.Events.RRULE
                    )

                    putNull(
                        CalendarContract.Events.RDATE
                    )

                    putNull(
                        CalendarContract.Events.DURATION
                    )

                    put(
                        CalendarContract.Events.DTEND,
                        endTimeMillis,
                    )

                } else {

                    put(
                        CalendarContract.Events.RRULE,
                        cleanRule,
                    )

                    put(
                        CalendarContract.Events.DURATION,
                        "P${durationSeconds}S",
                    )

                    putNull(
                        CalendarContract.Events.DTEND
                    )
                }
            }

        val uri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )

        return appContext.contentResolver
            .update(
                uri,
                values,
                null,
                null,
            ) > 0
    }


    fun updateEventTime(
        eventId: Long,
        startTimeMillis: Long,
        endTimeMillis: Long,
        location: String = "",
        description: String = "",
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


    fun clearReminder(
        eventId: Long,
    ): Boolean {

        if (
            !hasWritePermission() ||
            eventId <= 0L
        ) {

            return false
        }


        appContext
            .contentResolver
            .delete(
                CalendarContract.Reminders.CONTENT_URI,
                "${CalendarContract.Reminders.EVENT_ID} = ?",
                arrayOf(
                    eventId.toString()
                ),
            )


        val eventUri =
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId,
            )


        val rowsUpdated =
            appContext
                .contentResolver
                .update(
                    eventUri,
                    ContentValues().apply {

                        put(
                            CalendarContract.Events.HAS_ALARM,
                            0,
                        )
                    },
                    null,
                    null,
                )


        return rowsUpdated >= 0
    }


    /*
     * Set one Cypher/Android alert reminder, or remove the reminder
     * entirely when minutesBefore is null.
     */
    fun setReminder(
        eventId: Long,
        minutesBefore: Int?,
    ): Boolean {

        if (
            minutesBefore == null
        ) {

            return clearReminder(
                eventId
            )
        }


        if (
            minutesBefore < 0
        ) {

            return false
        }


        return replaceReminder(
            eventId =
                eventId,

            minutesBefore =
                minutesBefore,
        )
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
