package com.shannon.cypher.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shannon.cypher.R
import com.shannon.cypher.calendar.CypherCalendarEvent
import com.shannon.cypher.calendar.CypherCalendarManager
import com.shannon.cypher.notifications.CypherCalendarReminderSync
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private enum class RecurringEditScope {

    THIS_OCCURRENCE,

    ENTIRE_SERIES,
}


private enum class CalendarRepeatPattern {
    NONE,
    WEEKLY,
    FORTNIGHTLY,
    MONTHLY,
    CUSTOM_DAYS,
}


private enum class CalendarRepeatEnd {
    NEVER,
    AFTER_COUNT,
    ON_DATE,
}


@Composable
fun CypherCalendarScreen(
    calendarManager: CypherCalendarManager,
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    onMenuClick: () -> Unit,
    onMicClick: () -> Unit,
) {

    val context =
        LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()


    val background =
        Color(
            0xFF070509
        )

    val panel =
        Color(
            0xFF110D16
        )

    val accent =
        Color(
            0xFF8A2BE2
        )

    val secondaryAccent =
        Color(
            0xFF76FF03
        )

    val primaryText =
        Color.White

    val secondaryText =
        Color(
            0xFFA99AAF
        )

    val danger =
        Color(
            0xFFFF6B6B
        )


    val compactRingColor =
        if (
            isListening
        ) {
            secondaryAccent
        } else {
            accent
        }


    val infiniteTransition =
        rememberInfiniteTransition(
            label =
                "CalendarScreenCypherAnimation"
        )


    /*
     * Same compact Cypher animation as the To-Do screen.
     */
    val ringRotation by
    infiniteTransition
        .animateFloat(

            initialValue =
                0f,

            targetValue =
                360f,

            animationSpec =
                infiniteRepeatable(

                    animation =
                        tween(

                            durationMillis =
                                when {

                                    isThinking ->
                                        1600

                                    isListening ->
                                        2400

                                    isSpeaking ->
                                        3500

                                    else ->
                                        7000
                                },

                            easing =
                                FastOutSlowInEasing,
                        ),

                    repeatMode =
                        RepeatMode.Restart,
                ),

            label =
                "CalendarScreenRingRotation",
        )


    var displayedMonth by
    remember {
        mutableStateOf(
            startOfMonth(
                Calendar.getInstance()
            )
        )
    }


    var selectedDay by
    remember {
        mutableStateOf(
            startOfDay(
                Calendar.getInstance()
            )
        )
    }


    var monthEvents by
    remember {
        mutableStateOf(
            emptyList<CypherCalendarEvent>()
        )
    }


    /*
     * Increment this after Add/Edit/Delete so the visible month
     * refreshes immediately.
     */
    var refreshToken by
    remember {
        mutableIntStateOf(
            0
        )
    }


    var showEditor by
    remember {
        mutableStateOf(
            false
        )
    }


    var editingEvent by
    remember {
        mutableStateOf<CypherCalendarEvent?>(
            null
        )
    }


    var editingRecurringEvent by
    remember {
        mutableStateOf(
            false
        )
    }


    var recurringEditScope by
    remember {
        mutableStateOf<RecurringEditScope?>(
            null
        )
    }


    var editorTitle by
    remember {
        mutableStateOf(
            ""
        )
    }


    var editorStartMillis by
    remember {
        mutableStateOf(
            0L
        )
    }


    var editorEndMillis by
    remember {
        mutableStateOf(
            0L
        )
    }


    var editorReminderMinutes by
    remember {
        mutableStateOf<Int?>(
            null
        )
    }


    var editorRepeatPattern by
    remember {
        mutableStateOf(
            CalendarRepeatPattern.NONE
        )
    }


    var editorCustomRepeatDays by
    remember {
        mutableStateOf(
            "3"
        )
    }


    var editorRepeatEnd by
    remember {
        mutableStateOf(
            CalendarRepeatEnd.NEVER
        )
    }


    var editorRepeatCount by
    remember {
        mutableStateOf(
            "10"
        )
    }


    var editorRepeatUntilMillis by
    remember {
        mutableStateOf(
            0L
        )
    }


    var editorError by
    remember {
        mutableStateOf<String?>(
            null
        )
    }


    var editorBusy by
    remember {
        mutableStateOf(
            false
        )
    }


    fun applyRecurrenceRuleToEditor(
        recurrenceRule: String?,
    ) {

        val rule =
            recurrenceRule
                ?.uppercase()
                .orEmpty()

        editorRepeatPattern =
            when {

                "FREQ=WEEKLY" in rule &&
                        "INTERVAL=2" in rule ->
                    CalendarRepeatPattern.FORTNIGHTLY

                "FREQ=WEEKLY" in rule ->
                    CalendarRepeatPattern.WEEKLY

                "FREQ=MONTHLY" in rule ->
                    CalendarRepeatPattern.MONTHLY

                "FREQ=DAILY" in rule -> {

                    editorCustomRepeatDays =
                        Regex(
                            "INTERVAL=(\\d+)"
                        )
                            .find(rule)
                            ?.groupValues
                            ?.getOrNull(1)
                            ?: "1"

                    CalendarRepeatPattern.CUSTOM_DAYS
                }

                else ->
                    CalendarRepeatPattern.NONE
            }

        val count =
            Regex(
                "COUNT=(\\d+)"
            )
                .find(rule)
                ?.groupValues
                ?.getOrNull(1)

        val until =
            Regex(
                "UNTIL=([0-9TZ]+)"
            )
                .find(rule)
                ?.groupValues
                ?.getOrNull(1)

        when {

            count != null -> {

                editorRepeatEnd =
                    CalendarRepeatEnd.AFTER_COUNT

                editorRepeatCount =
                    count
            }

            until != null -> {

                editorRepeatEnd =
                    CalendarRepeatEnd.ON_DATE

                editorRepeatUntilMillis =
                    parseRRuleUntilMillis(
                        until
                    )
                        ?: defaultRepeatUntilMillis(
                            editorStartMillis
                        )
            }

            else -> {

                editorRepeatEnd =
                    CalendarRepeatEnd.NEVER
            }
        }
    }


    fun buildEditorRecurrenceRule(): String? {

        val base =
            when (editorRepeatPattern) {

                CalendarRepeatPattern.NONE ->
                    return null

                CalendarRepeatPattern.WEEKLY ->
                    "FREQ=WEEKLY;INTERVAL=1"

                CalendarRepeatPattern.FORTNIGHTLY ->
                    "FREQ=WEEKLY;INTERVAL=2"

                CalendarRepeatPattern.MONTHLY ->
                    "FREQ=MONTHLY;INTERVAL=1"

                CalendarRepeatPattern.CUSTOM_DAYS -> {

                    val days =
                        editorCustomRepeatDays
                            .toIntOrNull()
                            ?.coerceIn(
                                1,
                                365,
                            )
                            ?: return null

                    "FREQ=DAILY;INTERVAL=$days"
                }
            }

        return when (editorRepeatEnd) {

            CalendarRepeatEnd.NEVER ->
                base

            CalendarRepeatEnd.AFTER_COUNT -> {

                val count =
                    editorRepeatCount
                        .toIntOrNull()
                        ?.coerceIn(
                            2,
                            999,
                        )
                        ?: return null

                "$base;COUNT=$count"
            }

            CalendarRepeatEnd.ON_DATE -> {

                if (
                    editorRepeatUntilMillis <=
                    editorStartMillis
                ) {
                    return null
                }

                "$base;UNTIL=" +
                        formatRRuleUntil(
                            editorRepeatUntilMillis
                        )
            }
        }
    }


    fun openRepeatUntilDatePicker() {

        val base =
            Calendar.getInstance()
                .apply {
                    timeInMillis =
                        if (
                            editorRepeatUntilMillis > 0L
                        ) {
                            editorRepeatUntilMillis
                        } else {
                            defaultRepeatUntilMillis(
                                editorStartMillis
                            )
                        }
                }

        DatePickerDialog(
            context,
            {
                    _,
                    year,
                    month,
                    dayOfMonth ->

                editorRepeatUntilMillis =
                    Calendar.getInstance()
                        .apply {
                            set(
                                Calendar.YEAR,
                                year,
                            )
                            set(
                                Calendar.MONTH,
                                month,
                            )
                            set(
                                Calendar.DAY_OF_MONTH,
                                dayOfMonth,
                            )
                            set(
                                Calendar.HOUR_OF_DAY,
                                23,
                            )
                            set(
                                Calendar.MINUTE,
                                59,
                            )
                            set(
                                Calendar.SECOND,
                                59,
                            )
                            set(
                                Calendar.MILLISECOND,
                                0,
                            )
                        }
                        .timeInMillis
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH),
        )
            .show()
    }


    fun closeEditor() {

        showEditor =
            false

        editingEvent =
            null

        editingRecurringEvent =
            false

        recurringEditScope =
            null

        editorError =
            null

        editorBusy =
            false
    }


    fun openAddEditor() {

        val start =
            startOfDay(
                selectedDay
            )
                .apply {

                    set(
                        Calendar.HOUR_OF_DAY,
                        9,
                    )
                }


        val end =
            (start.clone() as Calendar)
                .apply {

                    add(
                        Calendar.HOUR_OF_DAY,
                        1,
                    )
                }


        editingEvent =
            null

        editingRecurringEvent =
            false

        recurringEditScope =
            null

        editorTitle =
            ""

        editorStartMillis =
            start.timeInMillis

        editorEndMillis =
            end.timeInMillis

        editorReminderMinutes =
            30

        editorRepeatPattern =
            CalendarRepeatPattern.NONE

        editorCustomRepeatDays =
            "3"

        editorRepeatEnd =
            CalendarRepeatEnd.NEVER

        editorRepeatCount =
            "10"

        editorRepeatUntilMillis =
            defaultRepeatUntilMillis(
                start.timeInMillis
            )

        editorError =
            null

        showEditor =
            true
    }


    fun openEditEditor(
        event: CypherCalendarEvent,
    ) {

        coroutineScope.launch {

            val details =
                withContext(
                    Dispatchers.IO
                ) {

                    Triple(
                        calendarManager
                            .getReminderMinutes(
                                event.id
                            ),

                        calendarManager
                            .isRecurringEvent(
                                event.id
                            ),

                        calendarManager
                            .getRecurrenceRule(
                                event.id
                            ),
                    )
                }


            editingEvent =
                event

            editingRecurringEvent =
                details.second

            recurringEditScope =
                null

            editorTitle =
                event.title

            editorStartMillis =
                event.startTimeMillis

            editorEndMillis =
                event.endTimeMillis

            editorReminderMinutes =
                details.first

            editorRepeatUntilMillis =
                defaultRepeatUntilMillis(
                    event.startTimeMillis
                )

            applyRecurrenceRuleToEditor(
                details.third
            )

            editorError =
                null

            showEditor =
                true
        }
    }


    fun openDatePicker() {

        val current =
            Calendar
                .getInstance()
                .apply {

                    timeInMillis =
                        editorStartMillis
                }


        DatePickerDialog(
            context,
            {
                    _,
                    year,
                    month,
                    dayOfMonth ->

                val oldStart =
                    Calendar
                        .getInstance()
                        .apply {

                            timeInMillis =
                                editorStartMillis
                        }


                val oldEnd =
                    Calendar
                        .getInstance()
                        .apply {

                            timeInMillis =
                                editorEndMillis
                        }


                val duration =
                    (
                            editorEndMillis -
                                    editorStartMillis
                            )
                        .coerceAtLeast(
                            60_000L
                        )


                oldStart.set(
                    Calendar.YEAR,
                    year,
                )

                oldStart.set(
                    Calendar.MONTH,
                    month,
                )

                oldStart.set(
                    Calendar.DAY_OF_MONTH,
                    dayOfMonth,
                )


                editorStartMillis =
                    oldStart.timeInMillis

                editorEndMillis =
                    editorStartMillis +
                            duration
            },
            current.get(
                Calendar.YEAR
            ),
            current.get(
                Calendar.MONTH
            ),
            current.get(
                Calendar.DAY_OF_MONTH
            ),
        )
            .show()
    }


    fun openStartTimePicker() {

        val start =
            Calendar
                .getInstance()
                .apply {

                    timeInMillis =
                        editorStartMillis
                }


        TimePickerDialog(
            context,
            {
                    _,
                    hourOfDay,
                    minute ->

                start.set(
                    Calendar.HOUR_OF_DAY,
                    hourOfDay,
                )

                start.set(
                    Calendar.MINUTE,
                    minute,
                )

                start.set(
                    Calendar.SECOND,
                    0,
                )

                start.set(
                    Calendar.MILLISECOND,
                    0,
                )


                editorStartMillis =
                    start.timeInMillis


                if (
                    editorEndMillis <=
                    editorStartMillis
                ) {

                    editorEndMillis =
                        editorStartMillis +
                                60L *
                                60_000L
                }
            },
            start.get(
                Calendar.HOUR_OF_DAY
            ),
            start.get(
                Calendar.MINUTE
            ),
            false,
        )
            .show()
    }


    fun openEndTimePicker() {

        val end =
            Calendar
                .getInstance()
                .apply {

                    timeInMillis =
                        editorEndMillis
                }


        TimePickerDialog(
            context,
            {
                    _,
                    hourOfDay,
                    minute ->

                end.set(
                    Calendar.HOUR_OF_DAY,
                    hourOfDay,
                )

                end.set(
                    Calendar.MINUTE,
                    minute,
                )

                end.set(
                    Calendar.SECOND,
                    0,
                )

                end.set(
                    Calendar.MILLISECOND,
                    0,
                )


                if (
                    end.timeInMillis <=
                    editorStartMillis
                ) {

                    end.add(
                        Calendar.DAY_OF_YEAR,
                        1,
                    )
                }


                editorEndMillis =
                    end.timeInMillis
            },
            end.get(
                Calendar.HOUR_OF_DAY
            ),
            end.get(
                Calendar.MINUTE
            ),
            false,
        )
            .show()
    }


    fun saveEditor() {

        val cleanTitle =
            editorTitle
                .trim()
                .replace(
                    Regex("\\s+"),
                    " ",
                )


        if (
            cleanTitle.isBlank()
        ) {

            editorError =
                "Please enter an event title."

            return
        }


        if (
            editorEndMillis <=
            editorStartMillis
        ) {

            editorError =
                "The finish time must be after the start time."

            return
        }


        if (
            editingRecurringEvent &&
            recurringEditScope == null
        ) {

            editorError =
                "Choose whether to change this event only or the entire series."

            return
        }


        val recurrenceRule =
            if (
                editingRecurringEvent &&
                recurringEditScope ==
                RecurringEditScope.THIS_OCCURRENCE
            ) {
                null
            } else {
                buildEditorRecurrenceRule()
            }


        if (
            editorRepeatPattern !=
            CalendarRepeatPattern.NONE &&
            recurrenceRule == null &&
            !(
                    editingRecurringEvent &&
                            recurringEditScope ==
                            RecurringEditScope.THIS_OCCURRENCE
                    )
        ) {

            editorError =
                when (editorRepeatEnd) {

                    CalendarRepeatEnd.AFTER_COUNT ->
                        "Enter a repeat count of at least 2."

                    CalendarRepeatEnd.ON_DATE ->
                        "Choose an end date after the event starts."

                    else ->
                        "Enter a valid custom repeat interval."
                }

            return
        }


        editorBusy =
            true

        editorError =
            null


        coroutineScope.launch {

            val success =
                withContext(
                    Dispatchers.IO
                ) {

                    val existing =
                        editingEvent


                    if (
                        existing == null
                    ) {

                        calendarManager
                            .createEvent(
                                title =
                                    cleanTitle,

                                startTimeMillis =
                                    editorStartMillis,

                                endTimeMillis =
                                    editorEndMillis,

                                reminderMinutes =
                                    editorReminderMinutes,

                                recurrenceRule =
                                    recurrenceRule,
                            ) !=
                                null

                    } else {

                        if (
                            editingRecurringEvent
                        ) {

                            when (
                                recurringEditScope
                            ) {

                                RecurringEditScope.THIS_OCCURRENCE -> {

                                    calendarManager
                                        .updateRecurringOccurrence(
                                            eventId =
                                                existing.id,

                                            originalOccurrenceStartMillis =
                                                existing.startTimeMillis,

                                            originalAllDay =
                                                existing.allDay,

                                            title =
                                                cleanTitle,

                                            newStartTimeMillis =
                                                editorStartMillis,

                                            newEndTimeMillis =
                                                editorEndMillis,

                                            reminderMinutes =
                                                editorReminderMinutes,
                                        ) != null
                                }


                                RecurringEditScope.ENTIRE_SERIES -> {

                                    val updated =
                                        calendarManager
                                            .updateRecurringSeries(
                                                eventId =
                                                    existing.id,

                                                selectedOccurrenceStartMillis =
                                                    existing.startTimeMillis,

                                                title =
                                                    cleanTitle,

                                                newOccurrenceStartMillis =
                                                    editorStartMillis,

                                                newOccurrenceEndMillis =
                                                    editorEndMillis,

                                                recurrenceRule =
                                                    recurrenceRule,
                                            )


                                    val reminderUpdated =
                                        if (
                                            updated
                                        ) {

                                            calendarManager
                                                .setReminder(
                                                    eventId =
                                                        existing.id,

                                                    minutesBefore =
                                                        editorReminderMinutes,
                                                )

                                        } else {

                                            false
                                        }


                                    updated &&
                                            reminderUpdated
                                }


                                null ->
                                    false
                            }

                        } else {

                            val updated =
                                calendarManager
                                    .updateEventRecurrence(
                                        eventId =
                                            existing.id,

                                        title =
                                            cleanTitle,

                                        startTimeMillis =
                                            editorStartMillis,

                                        endTimeMillis =
                                            editorEndMillis,

                                        recurrenceRule =
                                            recurrenceRule,
                                    )


                            val reminderUpdated =
                                if (
                                    updated
                                ) {

                                    calendarManager
                                        .setReminder(
                                            eventId =
                                                existing.id,

                                            minutesBefore =
                                                editorReminderMinutes,
                                        )

                                } else {

                                    false
                                }


                            updated &&
                                    reminderUpdated
                        }
                    }
                }


            if (
                success
            ) {

                withContext(
                    Dispatchers.IO
                ) {

                    CypherCalendarReminderSync(
                        context.applicationContext
                    )
                        .syncUpcomingCalendarReminders()
                }


                refreshToken +=
                    1

                closeEditor()

            } else {

                editorBusy =
                    false

                editorError =
                    "Cypher couldn't save that Calendar event."
            }
        }
    }


    fun deleteEditorEvent() {

        val event =
            editingEvent
                ?: return


        if (
            editingRecurringEvent &&
            recurringEditScope == null
        ) {

            editorError =
                "Choose whether to delete this event only or the entire series."

            return
        }


        editorBusy =
            true

        editorError =
            null


        coroutineScope.launch {

            val deleted =
                withContext(
                    Dispatchers.IO
                ) {

                    if (
                        editingRecurringEvent
                    ) {

                        when (
                            recurringEditScope
                        ) {

                            RecurringEditScope.THIS_OCCURRENCE -> {

                                calendarManager
                                    .deleteRecurringOccurrence(
                                        eventId =
                                            event.id,

                                        occurrenceStartMillis =
                                            event.startTimeMillis,

                                        occurrenceEndMillis =
                                            event.endTimeMillis,

                                        originalAllDay =
                                            event.allDay,
                                    )
                            }


                            RecurringEditScope.ENTIRE_SERIES -> {

                                calendarManager
                                    .deleteEvent(
                                        event.id
                                    )
                            }


                            null ->
                                false
                        }

                    } else {

                        calendarManager
                            .deleteEvent(
                                event.id
                            )
                    }
                }


            if (
                deleted
            ) {

                withContext(
                    Dispatchers.IO
                ) {

                    CypherCalendarReminderSync(
                        context.applicationContext
                    )
                        .syncUpcomingCalendarReminders()
                }


                refreshToken +=
                    1

                closeEditor()

            } else {

                editorBusy =
                    false

                editorError =
                    "Cypher couldn't delete that Calendar event."
            }
        }
    }


    LaunchedEffect(
        displayedMonth.timeInMillis,
        refreshToken,
    ) {

        val monthStart =
            startOfMonth(
                displayedMonth
            )


        val monthEnd =
            monthAfter(
                monthStart
            )


        monthEvents =
            withContext(
                Dispatchers.IO
            ) {

                calendarManager
                    .getEventsBetween(
                        startMillis =
                            monthStart.timeInMillis,

                        endMillis =
                            monthEnd.timeInMillis,
                    )
            }
    }


    val selectedDayEvents =
        monthEvents
            .filter { event ->

                eventOccursOnDay(
                    event =
                        event,

                    day =
                        selectedDay,
                )
            }
            .sortedBy {
                it.startTimeMillis
            }


    val nextEvent =
        monthEvents
            .firstOrNull { event ->

                event.startTimeMillis >
                        System.currentTimeMillis()
            }


    Surface(
        modifier =
            Modifier.fillMaxSize(),

        color =
            background,
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        horizontal =
                            20.dp,

                        vertical =
                            12.dp,
                    ),
        ) {


            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement
                        .SpaceBetween,

                verticalAlignment =
                    Alignment
                        .CenterVertically,
            ) {


                Box(
                    modifier =
                        Modifier
                            .size(
                                52.dp
                            )
                            .clickable {
                                onMenuClick()
                            },

                    contentAlignment =
                        Alignment.Center,
                ) {

                    Text(
                        text =
                            "☰",

                        color =
                            accent,

                        fontSize =
                            30.sp,

                        fontWeight =
                            FontWeight.Light,
                    )
                }


                Box(
                    modifier =
                        Modifier
                            .size(
                                76.dp
                            )
                            .clickable {

                                if (
                                    !isThinking
                                ) {

                                    onMicClick()
                                }
                            },

                    contentAlignment =
                        Alignment.Center,
                ) {


                    Canvas(
                        modifier =
                            Modifier
                                .size(
                                    70.dp
                                )
                                .rotate(
                                    ringRotation
                                ),
                    ) {


                        drawArc(
                            color =
                                compactRingColor,

                            startAngle =
                                -90f,

                            sweepAngle =
                                85f,

                            useCenter =
                                false,

                            style =
                                Stroke(
                                    width =
                                        2.4.dp.toPx(),

                                    cap =
                                        StrokeCap.Round,
                                ),
                        )


                        drawArc(
                            color =
                                compactRingColor,

                            startAngle =
                                45f,

                            sweepAngle =
                                55f,

                            useCenter =
                                false,

                            style =
                                Stroke(
                                    width =
                                        2.4.dp.toPx(),

                                    cap =
                                        StrokeCap.Round,
                                ),
                        )


                        drawArc(
                            color =
                                compactRingColor,

                            startAngle =
                                150f,

                            sweepAngle =
                                120f,

                            useCenter =
                                false,

                            style =
                                Stroke(
                                    width =
                                        2.4.dp.toPx(),

                                    cap =
                                        StrokeCap.Round,
                                ),
                        )
                    }


                    Image(
                        painter =
                            painterResource(
                                id =
                                    R.drawable
                                        .cypher_head
                            ),

                        contentDescription =
                            if (
                                isListening
                            ) {
                                "Cypher is listening"
                            } else {
                                "Cypher voice control"
                            },

                        modifier =
                            Modifier
                                .size(
                                    42.dp
                                ),
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            /*
             * Same sub-screen title row style as To-Do:
             * title on the left, primary action on the right.
             */
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement
                        .SpaceBetween,

                verticalAlignment =
                    Alignment
                        .CenterVertically,
            ) {

                Text(
                    text =
                        "CALENDAR",

                    color =
                        Color.White,

                    fontSize =
                        26.sp,

                    fontWeight =
                        FontWeight.Medium,

                    letterSpacing =
                        3.sp,
                )


                Row(
                    modifier =
                        Modifier
                            .clickable {
                                openAddEditor()
                            }
                            .background(
                                color =
                                    accent.copy(
                                        alpha =
                                            0.16f
                                    ),

                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),
                            )
                            .border(
                                width =
                                    1.dp,

                                color =
                                    accent.copy(
                                        alpha =
                                            0.35f
                                    ),

                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),
                            )
                            .padding(
                                horizontal =
                                    12.dp,

                                vertical =
                                    8.dp,
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {

                    Text(
                        text =
                            "+ ADD EVENT",

                        color =
                            secondaryAccent,

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.Bold,

                        letterSpacing =
                            1.sp,
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically,
            ) {


                MonthButton(
                    text =
                        "‹",

                    accent =
                        accent,

                    onClick = {

                        val newMonth =
                            previousMonth(
                                displayedMonth
                            )

                        displayedMonth =
                            newMonth

                        selectedDay =
                            startOfMonth(
                                newMonth
                            )
                    },
                )


                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                ) {

                    Text(
                        text =
                            SimpleDateFormat(
                                "MMMM",
                                Locale.getDefault(),
                            )
                                .format(
                                    displayedMonth.time
                                )
                                .uppercase(),

                        color =
                            primaryText,

                        fontSize =
                            20.sp,

                        fontWeight =
                            FontWeight.Bold,

                        letterSpacing =
                            2.sp,
                    )


                    Text(
                        text =
                            SimpleDateFormat(
                                "yyyy",
                                Locale.getDefault(),
                            )
                                .format(
                                    displayedMonth.time
                                ),

                        color =
                            secondaryText,

                        fontSize =
                            12.sp,
                    )
                }


                MonthButton(
                    text =
                        "›",

                    accent =
                        accent,

                    onClick = {

                        val newMonth =
                            nextMonth(
                                displayedMonth
                            )

                        displayedMonth =
                            newMonth

                        selectedDay =
                            startOfMonth(
                                newMonth
                            )
                    },
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            CalendarMonthGrid(
                displayedMonth =
                    displayedMonth,

                selectedDay =
                    selectedDay,

                events =
                    monthEvents,

                accent =
                    accent,

                secondaryAccent =
                    secondaryAccent,

                primaryText =
                    primaryText,

                secondaryText =
                    secondaryText,

                onDaySelected = { day ->

                    selectedDay =
                        day
                },
            )


            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )


            Text(
                text =
                    if (
                        isToday(
                            selectedDay
                        )
                    ) {

                        "TODAY // " +
                                formatSelectedDate(
                                    selectedDay
                                )

                    } else {

                        formatSelectedDate(
                            selectedDay
                        )
                    },

                color =
                    accent,

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.Bold,

                letterSpacing =
                    1.5.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    ),
            ) {


                if (
                    selectedDayEvents.isEmpty()
                ) {

                    item {

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color =
                                            panel,

                                        shape =
                                            RoundedCornerShape(
                                                16.dp
                                            ),
                                    )
                                    .border(
                                        width =
                                            1.dp,

                                        color =
                                            accent.copy(
                                                alpha =
                                                    0.20f
                                            ),

                                        shape =
                                            RoundedCornerShape(
                                                16.dp
                                            ),
                                    )
                                    .padding(
                                        20.dp
                                    ),
                        ) {

                            Text(
                                text =
                                    "No events scheduled.",

                                color =
                                    secondaryText,

                                fontSize =
                                    14.sp,
                            )
                        }
                    }

                } else {

                    items(
                        items =
                            selectedDayEvents,

                        key = { event ->

                            "${event.id}-${event.startTimeMillis}"
                        },
                    ) { event ->

                        CalendarEventCard(
                            event =
                                event,

                            panel =
                                panel,

                            accent =
                                accent,

                            secondaryAccent =
                                secondaryAccent,

                            primaryText =
                                primaryText,

                            secondaryText =
                                secondaryText,

                            onClick = {

                                openEditEditor(
                                    event
                                )
                            },
                        )
                    }
                }


                if (
                    nextEvent != null
                ) {

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )


                        Text(
                            text =
                                "NEXT UP",

                            color =
                                accent,

                            fontSize =
                                11.sp,

                            fontWeight =
                                FontWeight.Bold,

                            letterSpacing =
                                2.sp,
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )


                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color =
                                            secondaryAccent.copy(
                                                alpha =
                                                    0.08f
                                            ),

                                        shape =
                                            RoundedCornerShape(
                                                16.dp
                                            ),
                                    )
                                    .border(
                                        width =
                                            1.dp,

                                        color =
                                            secondaryAccent.copy(
                                                alpha =
                                                    0.45f
                                            ),

                                        shape =
                                            RoundedCornerShape(
                                                16.dp
                                            ),
                                    )
                                    .padding(
                                        16.dp
                                    ),
                        ) {

                            Column {

                                Text(
                                    text =
                                        nextEvent.title,

                                    color =
                                        primaryText,

                                    fontSize =
                                        15.sp,

                                    fontWeight =
                                        FontWeight.Bold,
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            5.dp
                                        )
                                )


                                Text(
                                    text =
                                        formatNextEvent(
                                            nextEvent
                                        ),

                                    color =
                                        secondaryAccent,

                                    fontSize =
                                        12.sp,
                                )
                            }
                        }
                    }
                }


                item {

                    Spacer(
                        modifier =
                            Modifier.height(
                                24.dp
                            )
                    )
                }
            }
        }
    }


    if (
        showEditor
    ) {

        AlertDialog(
            onDismissRequest = {

                if (
                    !editorBusy
                ) {

                    closeEditor()
                }
            },

            title = {

                Text(
                    text =
                        if (
                            editingEvent == null
                        ) {
                            "ADD EVENT"
                        } else {
                            "EDIT EVENT"
                        },

                    color =
                        Color.White,

                    letterSpacing =
                        2.sp,
                )
            },

            text = {

                Column {

                    if (
                        editingRecurringEvent
                    ) {

                        Text(
                            text =
                                "RECURRING EVENT",

                            color =
                                secondaryAccent,

                            fontSize =
                                10.sp,

                            fontWeight =
                                FontWeight.Bold,

                            letterSpacing =
                                1.5.sp,
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )


                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                ),
                        ) {

                            RecurringScopeChoice(
                                text =
                                    "THIS EVENT",

                                selected =
                                    recurringEditScope ==
                                            RecurringEditScope.THIS_OCCURRENCE,

                                accent =
                                    accent,

                                secondaryAccent =
                                    secondaryAccent,

                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                onClick = {

                                    recurringEditScope =
                                        RecurringEditScope.THIS_OCCURRENCE

                                    editorError =
                                        null
                                },
                            )


                            RecurringScopeChoice(
                                text =
                                    "ENTIRE SERIES",

                                selected =
                                    recurringEditScope ==
                                            RecurringEditScope.ENTIRE_SERIES,

                                accent =
                                    accent,

                                secondaryAccent =
                                    secondaryAccent,

                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                onClick = {

                                    recurringEditScope =
                                        RecurringEditScope.ENTIRE_SERIES

                                    editorError =
                                        null
                                },
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(
                                    14.dp
                                )
                        )
                    }


                    OutlinedTextField(
                        value =
                            editorTitle,

                        onValueChange = {

                            editorTitle =
                                it

                            editorError =
                                null
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        label = {
                            Text(
                                "Event title"
                            )
                        },

                        singleLine =
                            true,

                        enabled =
                            !editorBusy &&
                                    (
                                            !editingRecurringEvent ||
                                                    recurringEditScope != null
                                            ),
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )


                    EditorValueRow(
                        label =
                            "DATE",

                        value =
                            formatEditorDate(
                                editorStartMillis
                            ),

                        accent =
                            accent,

                        enabled =
                            !editorBusy &&
                                    (
                                            !editingRecurringEvent ||
                                                    recurringEditScope != null
                                            ),

                        onClick = {
                            openDatePicker()
                        },
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    EditorValueRow(
                        label =
                            "START",

                        value =
                            formatEditorTime(
                                editorStartMillis
                            ),

                        accent =
                            accent,

                        enabled =
                            !editorBusy &&
                                    (
                                            !editingRecurringEvent ||
                                                    recurringEditScope != null
                                            ),

                        onClick = {
                            openStartTimePicker()
                        },
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    EditorValueRow(
                        label =
                            "END",

                        value =
                            formatEditorTime(
                                editorEndMillis
                            ),

                        accent =
                            accent,

                        enabled =
                            !editorBusy &&
                                    (
                                            !editingRecurringEvent ||
                                                    recurringEditScope != null
                                            ),

                        onClick = {
                            openEndTimePicker()
                        },
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )


                    Text(
                        text =
                            "REMINDER",

                        color =
                            secondaryText,

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.Bold,

                        letterSpacing =
                            1.5.sp,
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                7.dp
                            ),
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    7.dp
                                ),
                        ) {

                            ReminderChoice(
                                text =
                                    "NONE",

                                value =
                                    null,

                                selected =
                                    editorReminderMinutes == null,

                                accent =
                                    accent,

                                secondaryAccent =
                                    secondaryAccent,

                                enabled =
                                    !editorBusy &&
                                            (
                                                    !editingRecurringEvent ||
                                                            recurringEditScope != null
                                                    ),

                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                onSelected = {

                                    editorReminderMinutes =
                                        null
                                },
                            )


                            ReminderChoice(
                                text =
                                    "10 MIN",

                                value =
                                    10,

                                selected =
                                    editorReminderMinutes == 10,

                                accent =
                                    accent,

                                secondaryAccent =
                                    secondaryAccent,

                                enabled =
                                    !editorBusy &&
                                            (
                                                    !editingRecurringEvent ||
                                                            recurringEditScope != null
                                                    ),

                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                onSelected = {

                                    editorReminderMinutes =
                                        10
                                },
                            )


                            ReminderChoice(
                                text =
                                    "30 MIN",

                                value =
                                    30,

                                selected =
                                    editorReminderMinutes == 30,

                                accent =
                                    accent,

                                secondaryAccent =
                                    secondaryAccent,

                                enabled =
                                    !editorBusy &&
                                            (
                                                    !editingRecurringEvent ||
                                                            recurringEditScope != null
                                                    ),

                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                onSelected = {

                                    editorReminderMinutes =
                                        30
                                },
                            )
                        }


                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    7.dp
                                ),
                        ) {

                            ReminderChoice(
                                text =
                                    "1 HOUR",

                                value =
                                    60,

                                selected =
                                    editorReminderMinutes == 60,

                                accent =
                                    accent,

                                secondaryAccent =
                                    secondaryAccent,

                                enabled =
                                    !editorBusy &&
                                            (
                                                    !editingRecurringEvent ||
                                                            recurringEditScope != null
                                                    ),

                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                onSelected = {

                                    editorReminderMinutes =
                                        60
                                },
                            )


                            ReminderChoice(
                                text =
                                    "1 DAY",

                                value =
                                    1440,

                                selected =
                                    editorReminderMinutes == 1440,

                                accent =
                                    accent,

                                secondaryAccent =
                                    secondaryAccent,

                                enabled =
                                    !editorBusy &&
                                            (
                                                    !editingRecurringEvent ||
                                                            recurringEditScope != null
                                                    ),

                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                onSelected = {

                                    editorReminderMinutes =
                                        1440
                                },
                            )
                        }
                    }


                    if (
                        !editingRecurringEvent ||
                        recurringEditScope !=
                        RecurringEditScope.THIS_OCCURRENCE
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    16.dp
                                )
                        )


                        Text(
                            text =
                                "REPEAT",

                            color =
                                secondaryText,

                            fontSize =
                                10.sp,

                            fontWeight =
                                FontWeight.Bold,

                            letterSpacing =
                                1.5.sp,
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )


                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    7.dp
                                ),
                        ) {

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        7.dp
                                    ),
                            ) {

                                RepeatChoice(
                                    text = "NONE",
                                    selected =
                                        editorRepeatPattern ==
                                                CalendarRepeatPattern.NONE,
                                    accent = accent,
                                    secondaryAccent = secondaryAccent,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        editorRepeatPattern =
                                            CalendarRepeatPattern.NONE
                                    },
                                )

                                RepeatChoice(
                                    text = "WEEKLY",
                                    selected =
                                        editorRepeatPattern ==
                                                CalendarRepeatPattern.WEEKLY,
                                    accent = accent,
                                    secondaryAccent = secondaryAccent,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        editorRepeatPattern =
                                            CalendarRepeatPattern.WEEKLY
                                    },
                                )

                                RepeatChoice(
                                    text = "FORTNIGHTLY",
                                    selected =
                                        editorRepeatPattern ==
                                                CalendarRepeatPattern.FORTNIGHTLY,
                                    accent = accent,
                                    secondaryAccent = secondaryAccent,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        editorRepeatPattern =
                                            CalendarRepeatPattern.FORTNIGHTLY
                                    },
                                )
                            }


                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        7.dp
                                    ),
                            ) {

                                RepeatChoice(
                                    text = "MONTHLY",
                                    selected =
                                        editorRepeatPattern ==
                                                CalendarRepeatPattern.MONTHLY,
                                    accent = accent,
                                    secondaryAccent = secondaryAccent,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        editorRepeatPattern =
                                            CalendarRepeatPattern.MONTHLY
                                    },
                                )

                                RepeatChoice(
                                    text = "CUSTOM",
                                    selected =
                                        editorRepeatPattern ==
                                                CalendarRepeatPattern.CUSTOM_DAYS,
                                    accent = accent,
                                    secondaryAccent = secondaryAccent,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        editorRepeatPattern =
                                            CalendarRepeatPattern.CUSTOM_DAYS
                                    },
                                )
                            }
                        }


                        if (
                            editorRepeatPattern ==
                            CalendarRepeatPattern.CUSTOM_DAYS
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )


                            OutlinedTextField(
                                value =
                                    editorCustomRepeatDays,

                                onValueChange = {
                                    editorCustomRepeatDays =
                                        it.filter { char ->
                                            char.isDigit()
                                        }
                                },

                                modifier =
                                    Modifier.fillMaxWidth(),

                                label = {
                                    Text(
                                        "Repeat every how many days?"
                                    )
                                },

                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType =
                                            KeyboardType.Number,
                                    ),

                                singleLine =
                                    true,
                            )
                        }


                        if (
                            editorRepeatPattern !=
                            CalendarRepeatPattern.NONE
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        14.dp
                                    )
                            )


                            Text(
                                text =
                                    "ENDS",

                                color =
                                    secondaryText,

                                fontSize =
                                    10.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                letterSpacing =
                                    1.5.sp,
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )


                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        7.dp
                                    ),
                            ) {

                                RepeatChoice(
                                    text = "NEVER",
                                    selected =
                                        editorRepeatEnd ==
                                                CalendarRepeatEnd.NEVER,
                                    accent = accent,
                                    secondaryAccent = secondaryAccent,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        editorRepeatEnd =
                                            CalendarRepeatEnd.NEVER
                                    },
                                )

                                RepeatChoice(
                                    text = "AFTER # DAYS",
                                    selected =
                                        editorRepeatEnd ==
                                                CalendarRepeatEnd.AFTER_COUNT,
                                    accent = accent,
                                    secondaryAccent = secondaryAccent,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        editorRepeatEnd =
                                            CalendarRepeatEnd.AFTER_COUNT
                                    },
                                )

                                RepeatChoice(
                                    text = "ON DATE",
                                    selected =
                                        editorRepeatEnd ==
                                                CalendarRepeatEnd.ON_DATE,
                                    accent = accent,
                                    secondaryAccent = secondaryAccent,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        editorRepeatEnd =
                                            CalendarRepeatEnd.ON_DATE
                                    },
                                )
                            }


                            if (
                                editorRepeatEnd ==
                                CalendarRepeatEnd.AFTER_COUNT
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            10.dp
                                        )
                                )


                                OutlinedTextField(
                                    value =
                                        editorRepeatCount,

                                    onValueChange = {
                                        editorRepeatCount =
                                            it.filter { char ->
                                                char.isDigit()
                                            }
                                    },

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    label = {
                                        Text(
                                            "Number of occurrences"
                                        )
                                    },

                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType =
                                                KeyboardType.Number,
                                        ),

                                    singleLine =
                                        true,
                                )
                            }


                            if (
                                editorRepeatEnd ==
                                CalendarRepeatEnd.ON_DATE
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            10.dp
                                        )
                                )


                                EditorValueRow(
                                    label =
                                        "END DATE",

                                    value =
                                        formatEditorDate(
                                            editorRepeatUntilMillis
                                        ),

                                    accent =
                                        accent,

                                    enabled =
                                        !editorBusy,

                                    onClick = {
                                        openRepeatUntilDatePicker()
                                    },
                                )
                            }
                        }
                    }


                    if (
                        editingRecurringEvent
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    14.dp
                                )
                        )


                        Text(
                            text =
                                when (
                                    recurringEditScope
                                ) {

                                    RecurringEditScope.THIS_OCCURRENCE ->
                                        "Only this occurrence will change. The rest of the recurring series stays untouched."

                                    RecurringEditScope.ENTIRE_SERIES ->
                                        "This will change the full recurring series."

                                    null ->
                                        "Choose THIS EVENT or ENTIRE SERIES before saving or deleting."
                                },

                            color =
                                secondaryAccent,

                            fontSize =
                                12.sp,

                            lineHeight =
                                17.sp,
                        )
                    }


                    editorError
                        ?.let {
                                error ->

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp
                                    )
                            )


                            Text(
                                text =
                                    error,

                                color =
                                    danger,

                                fontSize =
                                    12.sp,
                            )
                        }
                }
            },

            confirmButton = {

                if (
                    !editingRecurringEvent ||
                    recurringEditScope != null
                ) {

                    TextButton(
                        enabled =
                            !editorBusy,

                        onClick = {
                            saveEditor()
                        },
                    ) {

                        Text(
                            text =
                                if (
                                    editorBusy
                                ) {
                                    "SAVING..."
                                } else {
                                    "SAVE"
                                },

                            color =
                                secondaryAccent,
                        )
                    }
                }
            },

            dismissButton = {

                Row {

                    if (
                        editingEvent != null &&
                        (
                                !editingRecurringEvent ||
                                        recurringEditScope != null
                                )
                    ) {

                        TextButton(
                            enabled =
                                !editorBusy,

                            onClick = {
                                deleteEditorEvent()
                            },
                        ) {

                            Text(
                                text =
                                    "DELETE",

                                color =
                                    danger,
                            )
                        }
                    }


                    TextButton(
                        enabled =
                            !editorBusy,

                        onClick = {
                            closeEditor()
                        },
                    ) {

                        Text(
                            text =
                                "CLOSE",

                            color =
                                secondaryText,
                        )
                    }
                }
            },

            containerColor =
                panel,
        )
    }
}


@Composable
private fun RecurringScopeChoice(
    text: String,
    selected: Boolean,
    accent: Color,
    secondaryAccent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {

    Box(
        modifier =
            modifier
                .background(
                    color =
                        if (
                            selected
                        ) {

                            secondaryAccent.copy(
                                alpha =
                                    0.14f
                            )

                        } else {

                            accent.copy(
                                alpha =
                                    0.08f
                            )
                        },

                    shape =
                        RoundedCornerShape(
                            10.dp
                        ),
                )
                .border(
                    width =
                        1.dp,

                    color =
                        if (
                            selected
                        ) {
                            secondaryAccent
                        } else {
                            accent.copy(
                                alpha =
                                    0.30f
                            )
                        },

                    shape =
                        RoundedCornerShape(
                            10.dp
                        ),
                )
                .clickable {
                    onClick()
                }
                .padding(
                    vertical =
                        10.dp,
                ),

        contentAlignment =
            Alignment.Center,
    ) {

        Text(
            text =
                text,

            color =
                if (
                    selected
                ) {
                    secondaryAccent
                } else {
                    Color.White
                },

            fontSize =
                10.sp,

            fontWeight =
                FontWeight.Bold,

            letterSpacing =
                0.8.sp,
        )
    }
}


@Composable
private fun EditorValueRow(
    label: String,
    value: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        accent.copy(
                            alpha =
                                0.10f
                        ),

                    shape =
                        RoundedCornerShape(
                            10.dp
                        ),
                )
                .clickable(
                    enabled =
                        enabled
                ) {

                    onClick()
                }
                .padding(
                    horizontal =
                        12.dp,

                    vertical =
                        11.dp,
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically,
    ) {

        Text(
            text =
                label,

            color =
                accent,

            fontSize =
                10.sp,

            fontWeight =
                FontWeight.Bold,

            letterSpacing =
                1.4.sp,
        )


        Text(
            text =
                value,

            color =
                if (
                    enabled
                ) {
                    Color.White
                } else {
                    Color.Gray
                },

            fontSize =
                13.sp,
        )
    }
}


@Composable
private fun RepeatChoice(
    text: String,
    selected: Boolean,
    accent: Color,
    secondaryAccent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {

    Box(
        modifier =
            modifier
                .background(
                    color =
                        if (selected) {
                            secondaryAccent.copy(alpha = 0.14f)
                        } else {
                            accent.copy(alpha = 0.08f)
                        },

                    shape =
                        RoundedCornerShape(
                            9.dp
                        ),
                )
                .border(
                    width =
                        1.dp,

                    color =
                        if (selected) {
                            secondaryAccent
                        } else {
                            accent.copy(alpha = 0.25f)
                        },

                    shape =
                        RoundedCornerShape(
                            9.dp
                        ),
                )
                .clickable {
                    onClick()
                }
                .padding(
                    vertical =
                        9.dp,
                ),

        contentAlignment =
            Alignment.Center,
    ) {

        Text(
            text =
                text,

            color =
                if (selected) {
                    secondaryAccent
                } else {
                    Color.White
                },

            fontSize =
                9.sp,

            fontWeight =
                FontWeight.Bold,
        )
    }
}


@Composable
private fun ReminderChoice(
    text: String,
    value: Int?,
    selected: Boolean,
    accent: Color,
    secondaryAccent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (Int?) -> Unit,
) {

    Box(
        modifier =
            modifier
                .background(
                    color =
                        if (
                            selected
                        ) {

                            secondaryAccent.copy(
                                alpha =
                                    0.14f
                            )

                        } else {

                            accent.copy(
                                alpha =
                                    0.08f
                            )
                        },

                    shape =
                        RoundedCornerShape(
                            9.dp
                        ),
                )
                .border(
                    width =
                        1.dp,

                    color =
                        if (
                            selected
                        ) {
                            secondaryAccent
                        } else {
                            accent.copy(
                                alpha =
                                    0.25f
                            )
                        },

                    shape =
                        RoundedCornerShape(
                            9.dp
                        ),
                )
                .clickable(
                    enabled =
                        enabled
                ) {

                    onSelected(
                        value
                    )
                }
                .padding(
                    vertical =
                        9.dp,
                ),

        contentAlignment =
            Alignment.Center,
    ) {

        Text(
            text =
                text,

            color =
                if (
                    selected
                ) {
                    secondaryAccent
                } else {
                    Color.White
                },

            fontSize =
                9.sp,

            fontWeight =
                FontWeight.Bold,
        )
    }
}


@Composable
private fun CalendarMonthGrid(
    displayedMonth: Calendar,
    selectedDay: Calendar,
    events: List<CypherCalendarEvent>,
    accent: Color,
    secondaryAccent: Color,
    primaryText: Color,
    secondaryText: Color,
    onDaySelected: (Calendar) -> Unit,
) {

    val weekdayLabels =
        listOf(
            "MON",
            "TUE",
            "WED",
            "THU",
            "FRI",
            "SAT",
            "SUN",
        )


    Row(
        modifier =
            Modifier.fillMaxWidth(),
    ) {

        weekdayLabels
            .forEach {
                    label ->

                Text(
                    text =
                        label,

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    color =
                        secondaryText,

                    fontSize =
                        9.sp,

                    textAlign =
                        TextAlign.Center,

                    letterSpacing =
                        1.sp,
                )
            }
    }


    Spacer(
        modifier =
            Modifier.height(
                8.dp
            )
    )


    val days =
        buildMonthCells(
            displayedMonth
        )


    days
        .chunked(
            7
        )
        .forEach {
                week ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
            ) {

                week.forEach {
                        day ->

                    if (
                        day == null
                    ) {

                        Spacer(
                            modifier =
                                Modifier
                                    .weight(
                                        1f
                                    )
                                    .height(
                                        44.dp
                                    )
                        )

                    } else {

                        val selected =
                            sameDay(
                                day,
                                selectedDay,
                            )


                        val today =
                            isToday(
                                day
                            )


                        val hasEvent =
                            events.any {
                                    event ->

                                eventOccursOnDay(
                                    event =
                                        event,

                                    day =
                                        day,
                                )
                            }


                        Box(
                            modifier =
                                Modifier
                                    .weight(
                                        1f
                                    )
                                    .height(
                                        44.dp
                                    )
                                    .padding(
                                        2.dp
                                    )
                                    .background(
                                        color =
                                            if (
                                                selected
                                            ) {

                                                accent.copy(
                                                    alpha =
                                                        0.22f
                                                )

                                            } else {

                                                Color.Transparent
                                            },

                                        shape =
                                            RoundedCornerShape(
                                                10.dp
                                            ),
                                    )
                                    .border(
                                        width =
                                            if (
                                                today
                                            ) {
                                                1.dp
                                            } else {
                                                0.dp
                                            },

                                        color =
                                            if (
                                                today
                                            ) {
                                                secondaryAccent
                                            } else {
                                                Color.Transparent
                                            },

                                        shape =
                                            RoundedCornerShape(
                                                10.dp
                                            ),
                                    )
                                    .clickable {

                                        onDaySelected(
                                            day
                                        )
                                    },

                            contentAlignment =
                                Alignment.Center,
                        ) {

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                            ) {

                                Text(
                                    text =
                                        day.get(
                                            Calendar.DAY_OF_MONTH
                                        )
                                            .toString(),

                                    color =
                                        if (
                                            selected ||
                                            today
                                        ) {
                                            primaryText
                                        } else {
                                            secondaryText
                                        },

                                    fontSize =
                                        13.sp,

                                    fontWeight =
                                        if (
                                            selected ||
                                            today
                                        ) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                )


                                Text(
                                    text =
                                        if (
                                            hasEvent
                                        ) {
                                            "●"
                                        } else {
                                            ""
                                        },

                                    color =
                                        if (
                                            today
                                        ) {
                                            secondaryAccent
                                        } else {
                                            accent
                                        },

                                    fontSize =
                                        7.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
}


@Composable
private fun CalendarEventCard(
    event: CypherCalendarEvent,
    panel: Color,
    accent: Color,
    secondaryAccent: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        panel,

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                )
                .border(
                    width =
                        1.dp,

                    color =
                        accent.copy(
                            alpha =
                                0.25f
                        ),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                )
                .clickable {
                    onClick()
                }
                .padding(
                    16.dp
                ),
    ) {


        Box(
            modifier =
                Modifier
                    .size(
                        width =
                            3.dp,

                        height =
                            54.dp,
                    )
                    .background(
                        color =
                            secondaryAccent,

                        shape =
                            RoundedCornerShape(
                                2.dp
                            ),
                    )
        )


        Spacer(
            modifier =
                Modifier.size(
                    12.dp
                )
        )


        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    event.title,

                color =
                    primaryText,

                fontSize =
                    15.sp,

                fontWeight =
                    FontWeight.Bold,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )


            Text(
                text =
                    formatEventTime(
                        event
                    ),

                color =
                    secondaryAccent,

                fontSize =
                    12.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(
                text =
                    event.calendarName,

                color =
                    secondaryText,

                fontSize =
                    10.sp,

                letterSpacing =
                    0.5.sp,
            )
        }
    }
}


@Composable
private fun MonthButton(
    text: String,
    accent: Color,
    onClick: () -> Unit,
) {

    Box(
        modifier =
            Modifier
                .size(
                    44.dp
                )
                .background(
                    color =
                        accent.copy(
                            alpha =
                                0.12f
                        ),

                    shape =
                        RoundedCornerShape(
                            12.dp
                        ),
                )
                .clickable {
                    onClick()
                },

        contentAlignment =
            Alignment.Center,
    ) {

        Text(
            text =
                text,

            color =
                accent,

            fontSize =
                28.sp,
        )
    }
}


private fun buildMonthCells(
    month: Calendar,
): List<Calendar?> {

    val firstDay =
        startOfMonth(
            month
        )


    val leadingEmptyCells =
        (
                firstDay.get(
                    Calendar.DAY_OF_WEEK
                ) +
                        5
                ) %
                7


    val daysInMonth =
        firstDay.getActualMaximum(
            Calendar.DAY_OF_MONTH
        )


    val cells =
        mutableListOf<Calendar?>()


    repeat(
        leadingEmptyCells
    ) {

        cells.add(
            null
        )
    }


    for (
    dayNumber in
    1..daysInMonth
    ) {

        cells.add(
            (firstDay.clone() as Calendar)
                .apply {

                    set(
                        Calendar.DAY_OF_MONTH,
                        dayNumber,
                    )
                }
        )
    }


    while (
        cells.size %
        7 !=
        0
    ) {

        cells.add(
            null
        )
    }


    return cells
}


private fun eventOccursOnDay(
    event: CypherCalendarEvent,
    day: Calendar,
): Boolean {

    val dayStart =
        startOfDay(
            day
        )
            .timeInMillis


    val dayEnd =
        (startOfDay(day).clone() as Calendar)
            .apply {

                add(
                    Calendar.DAY_OF_YEAR,
                    1,
                )
            }
            .timeInMillis


    return event.startTimeMillis <
            dayEnd &&
            event.endTimeMillis >
            dayStart
}


private fun formatEventTime(
    event: CypherCalendarEvent,
): String {

    if (
        event.allDay
    ) {

        return "ALL DAY"
    }


    return formatEditorTime(
        event.startTimeMillis
    ) +
            " — " +
            formatEditorTime(
                event.endTimeMillis
            )
}


private fun formatNextEvent(
    event: CypherCalendarEvent,
): String {

    val dateFormatter =
        SimpleDateFormat(
            "EEE d MMM",
            Locale.getDefault(),
        )


    if (
        event.allDay
    ) {

        return dateFormatter
            .format(
                event.startTimeMillis
            ) +
                " · All day"
    }


    return dateFormatter
        .format(
            event.startTimeMillis
        ) +
            " · " +
            formatEditorTime(
                event.startTimeMillis
            )
}


private fun formatSelectedDate(
    day: Calendar,
): String {

    return SimpleDateFormat(
        "EEEE d MMMM",
        Locale.getDefault(),
    )
        .format(
            day.time
        )
        .uppercase()
}


private fun formatEditorDate(
    millis: Long,
): String {

    return SimpleDateFormat(
        "EEEE d MMMM yyyy",
        Locale.getDefault(),
    )
        .format(
            millis
        )
}


private fun formatEditorTime(
    millis: Long,
): String {

    val calendar =
        Calendar
            .getInstance()
            .apply {

                timeInMillis =
                    millis
            }


    val pattern =
        if (
            calendar.get(
                Calendar.MINUTE
            ) ==
            0
        ) {

            "h a"

        } else {

            "h:mm a"
        }


    return SimpleDateFormat(
        pattern,
        Locale.getDefault(),
    )
        .format(
            millis
        )
        .lowercase()
}


private fun defaultRepeatUntilMillis(
    startMillis: Long,
): Long {

    return Calendar.getInstance()
        .apply {
            timeInMillis =
                startMillis

            add(
                Calendar.YEAR,
                1,
            )

            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 0)
        }
        .timeInMillis
}


private fun formatRRuleUntil(
    millis: Long,
): String {

    val formatter =
        SimpleDateFormat(
            "yyyyMMdd'T'HHmmss'Z'",
            Locale.US,
        )

    formatter.timeZone =
        java.util.TimeZone.getTimeZone(
            "UTC"
        )

    return formatter.format(
        millis
    )
}


private fun parseRRuleUntilMillis(
    value: String,
): Long? {

    return try {

        val formatter =
            SimpleDateFormat(
                "yyyyMMdd'T'HHmmss'Z'",
                Locale.US,
            )

        formatter.timeZone =
            java.util.TimeZone.getTimeZone(
                "UTC"
            )

        formatter.parse(
            value
        )
            ?.time

    } catch (_: Exception) {
        null
    }
}


private fun startOfDay(
    source: Calendar,
): Calendar {

    return (source.clone() as Calendar)
        .apply {

            set(
                Calendar.HOUR_OF_DAY,
                0,
            )

            set(
                Calendar.MINUTE,
                0,
            )

            set(
                Calendar.SECOND,
                0,
            )

            set(
                Calendar.MILLISECOND,
                0,
            )
        }
}


private fun startOfMonth(
    source: Calendar,
): Calendar {

    return startOfDay(
        source
    )
        .apply {

            set(
                Calendar.DAY_OF_MONTH,
                1,
            )
        }
}


private fun monthAfter(
    source: Calendar,
): Calendar {

    return startOfMonth(
        source
    )
        .apply {

            add(
                Calendar.MONTH,
                1,
            )
        }
}


private fun previousMonth(
    source: Calendar,
): Calendar {

    return startOfMonth(
        source
    )
        .apply {

            add(
                Calendar.MONTH,
                -1,
            )
        }
}


private fun nextMonth(
    source: Calendar,
): Calendar {

    return startOfMonth(
        source
    )
        .apply {

            add(
                Calendar.MONTH,
                1,
            )
        }
}


private fun sameDay(
    first: Calendar,
    second: Calendar,
): Boolean {

    return first.get(
        Calendar.YEAR
    ) ==
            second.get(
                Calendar.YEAR
            ) &&
            first.get(
                Calendar.DAY_OF_YEAR
            ) ==
            second.get(
                Calendar.DAY_OF_YEAR
            )
}


private fun isToday(
    day: Calendar,
): Boolean {

    return sameDay(
        day,
        Calendar.getInstance(),
    )
}
