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


    fun getTodayEvents():
            List<CypherCalendarEvent> {

        if (!hasReadPermission()) {
            return emptyList()
        }


        val startOfDay =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }


        val endOfDay =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    23
                )

                set(
                    Calendar.MINUTE,
                    59
                )

                set(
                    Calendar.SECOND,
                    59
                )

                set(
                    Calendar.MILLISECOND,
                    999
                )
            }


        return getEventsBetween(
            startMillis =
                startOfDay.timeInMillis,

            endMillis =
                endOfDay.timeInMillis,
        )
    }


    fun getTomorrowEvents():
            List<CypherCalendarEvent> {

        if (!hasReadPermission()) {
            return emptyList()
        }


        val startOfTomorrow =
            Calendar.getInstance().apply {

                add(
                    Calendar.DAY_OF_YEAR,
                    1
                )

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }


        val endOfTomorrow =
            Calendar.getInstance().apply {

                add(
                    Calendar.DAY_OF_YEAR,
                    1
                )

                set(
                    Calendar.HOUR_OF_DAY,
                    23
                )

                set(
                    Calendar.MINUTE,
                    59
                )

                set(
                    Calendar.SECOND,
                    59
                )

                set(
                    Calendar.MILLISECOND,
                    999
                )
            }


        return getEventsBetween(
            startMillis =
                startOfTomorrow.timeInMillis,

            endMillis =
                endOfTomorrow.timeInMillis,
        )
    }


    fun getEventsBetween(
        startMillis: Long,
        endMillis: Long,
    ): List<CypherCalendarEvent> {

        if (!hasReadPermission()) {
            return emptyList()
        }


        val events =
            mutableListOf<
                    CypherCalendarEvent
                    >()


        val builder =
            CalendarContract
                .Instances
                .CONTENT_URI
                .buildUpon()


        ContentUris.appendId(
            builder,
            startMillis,
        )


        ContentUris.appendId(
            builder,
            endMillis,
        )


        val projection =
            arrayOf(

                CalendarContract
                    .Instances
                    .EVENT_ID,

                CalendarContract
                    .Instances
                    .TITLE,

                CalendarContract
                    .Instances
                    .BEGIN,

                CalendarContract
                    .Instances
                    .END,

                CalendarContract
                    .Instances
                    .ALL_DAY,

                CalendarContract
                    .Instances
                    .CALENDAR_DISPLAY_NAME,
            )


        appContext
            .contentResolver
            .query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )
            ?.use { cursor ->


                val eventIdIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract
                            .Instances
                            .EVENT_ID
                    )


                val titleIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract
                            .Instances
                            .TITLE
                    )


                val beginIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract
                            .Instances
                            .BEGIN
                    )


                val endIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract
                            .Instances
                            .END
                    )


                val allDayIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract
                            .Instances
                            .ALL_DAY
                    )


                val calendarNameIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract
                            .Instances
                            .CALENDAR_DISPLAY_NAME
                    )


                while (
                    cursor.moveToNext()
                ) {

                    events.add(
                        CypherCalendarEvent(

                            id =
                                cursor.getLong(
                                    eventIdIndex
                                ),

                            title =
                                cursor.getString(
                                    titleIndex
                                )
                                    ?: "Untitled event",

                            startTimeMillis =
                                cursor.getLong(
                                    beginIndex
                                ),

                            endTimeMillis =
                                cursor.getLong(
                                    endIndex
                                ),

                            allDay =
                                cursor.getInt(
                                    allDayIndex
                                ) == 1,

                            calendarName =
                                cursor.getString(
                                    calendarNameIndex
                                )
                                    ?: "Calendar",
                        )
                    )
                }
            }


        return events
    }


    fun createEvent(
        title: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
    ): Long? {

        if (!hasWritePermission()) {
            return null
        }


        val calendarId =
            getWritableCalendarId()
                ?: return null


        val values =
            ContentValues().apply {

                put(
                    CalendarContract
                        .Events
                        .CALENDAR_ID,
                    calendarId,
                )

                put(
                    CalendarContract
                        .Events
                        .TITLE,
                    title,
                )

                put(
                    CalendarContract
                        .Events
                        .DTSTART,
                    startTimeMillis,
                )

                put(
                    CalendarContract
                        .Events
                        .DTEND,
                    endTimeMillis,
                )

                put(
                    CalendarContract
                        .Events
                        .EVENT_TIMEZONE,
                    TimeZone
                        .getDefault()
                        .id,
                )
            }


        val uri =
            appContext
                .contentResolver
                .insert(
                    CalendarContract
                        .Events
                        .CONTENT_URI,
                    values,
                )
                ?: return null


        return uri
            .lastPathSegment
            ?.toLongOrNull()
    }


    private fun getWritableCalendarId():
            Long? {

        if (!hasReadPermission()) {
            return null
        }


        val projection =
            arrayOf(

                CalendarContract
                    .Calendars
                    ._ID,

                CalendarContract
                    .Calendars
                    .CALENDAR_DISPLAY_NAME,

                CalendarContract
                    .Calendars
                    .VISIBLE,

                CalendarContract
                    .Calendars
                    .CALENDAR_ACCESS_LEVEL,
            )


        appContext
            .contentResolver
            .query(
                CalendarContract
                    .Calendars
                    .CONTENT_URI,

                projection,

                null,
                null,
                null,
            )
            ?.use { cursor ->


                val idIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract
                            .Calendars
                            ._ID
                    )


                val visibleIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract
                            .Calendars
                            .VISIBLE
                    )


                val accessIndex =
                    cursor.getColumnIndexOrThrow(
                        CalendarContract
                            .Calendars
                            .CALENDAR_ACCESS_LEVEL
                    )


                while (
                    cursor.moveToNext()
                ) {

                    val visible =
                        cursor.getInt(
                            visibleIndex
                        ) == 1


                    val accessLevel =
                        cursor.getInt(
                            accessIndex
                        )


                    val writable =
                        accessLevel >=
                                CalendarContract
                                    .Calendars
                                    .CAL_ACCESS_CONTRIBUTOR


                    if (
                        visible &&
                        writable
                    ) {

                        return cursor.getLong(
                            idIndex
                        )
                    }
                }
            }


        return null
    }
}