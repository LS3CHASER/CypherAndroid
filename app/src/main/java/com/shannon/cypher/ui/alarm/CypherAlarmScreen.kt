package com.shannon.cypher.ui.alarm

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shannon.cypher.R
import com.shannon.cypher.alarm.CypherAlarm
import com.shannon.cypher.alarm.CypherAlarmRepository
import com.shannon.cypher.alarm.CypherAlarmScheduler
import com.shannon.cypher.alarm.CypherTimer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay


@Composable
fun CypherAlarmScreen(
    alarmRepository: CypherAlarmRepository,
    alarmScheduler: CypherAlarmScheduler,
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    onMenuClick: () -> Unit,
    onMicClick: () -> Unit,
) {

    val background = Color(0xFF070509)
    val panel = Color(0xFF110D16)
    val accent = Color(0xFF8A2BE2)
    val secondaryAccent = Color(0xFF76FF03)
    val primaryText = Color.White
    val secondaryText = Color(0xFFA99AAF)
    val danger = Color(0xFFFF6B6B)

    val compactRingColor =
        if (isListening) {
            secondaryAccent
        } else {
            accent
        }

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "AlarmScreenCypherAnimation"
        )

    val ringRotation by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis =
                            when {
                                isThinking -> 1600
                                isListening -> 2400
                                isSpeaking -> 3500
                                else -> 7000
                            },
                        easing = FastOutSlowInEasing,
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        label = "AlarmScreenRingRotation",
    )

    var alarms by
    remember {
        mutableStateOf(
            alarmRepository.getAlarms()
        )
    }

    var timers by
    remember {
        mutableStateOf(
            alarmRepository.getTimers()
        )
    }

    var nowMillis by
    remember {
        mutableLongStateOf(
            System.currentTimeMillis()
        )
    }

    var pendingDeleteAlarm by
    remember {
        mutableStateOf<CypherAlarm?>(null)
    }

    fun refresh() {
        alarms =
            alarmRepository
                .getAlarms()
                .sortedWith(
                    compareBy<CypherAlarm> { it.hour }
                        .thenBy { it.minute }
                )

        timers =
            alarmRepository
                .getTimers()
                .sortedBy { it.endsAtMillis }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis =
                System.currentTimeMillis()

            /*
             * Refresh persisted state as well so this screen notices
             * timers that finish while it is open.
             */
            refresh()

            delay(1000L)
        }
    }

    val activeTimers =
        timers.filter {
            it.active &&
                    it.endsAtMillis >
                    nowMillis
        }

    val finishedTimers =
        timers.filter {
            !it.active ||
                    it.endsAtMillis <=
                    nowMillis
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
                        horizontal = 20.dp,
                        vertical = 12.dp,
                    ),
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clickable {
                                onMenuClick()
                            },
                    contentAlignment =
                        Alignment.Center,
                ) {

                    Text(
                        text = "☰",
                        color = accent,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light,
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .size(76.dp)
                            .clickable {
                                if (!isThinking) {
                                    onMicClick()
                                }
                            },
                    contentAlignment =
                        Alignment.Center,
                ) {

                    Canvas(
                        modifier =
                            Modifier
                                .size(70.dp)
                                .rotate(ringRotation),
                    ) {

                        drawArc(
                            color = compactRingColor,
                            startAngle = -90f,
                            sweepAngle = 85f,
                            useCenter = false,
                            style =
                                Stroke(
                                    width = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                ),
                        )

                        drawArc(
                            color = compactRingColor,
                            startAngle = 45f,
                            sweepAngle = 55f,
                            useCenter = false,
                            style =
                                Stroke(
                                    width = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                ),
                        )

                        drawArc(
                            color = compactRingColor,
                            startAngle = 150f,
                            sweepAngle = 120f,
                            useCenter = false,
                            style =
                                Stroke(
                                    width = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                ),
                        )
                    }

                    Image(
                        painter =
                            painterResource(
                                id =
                                    R.drawable.cypher_head
                            ),
                        contentDescription =
                            if (isListening) {
                                "Cypher is listening"
                            } else {
                                "Cypher voice control"
                            },
                        modifier =
                            Modifier.size(42.dp),
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {

                Text(
                    text = "ALARMS & TIMERS",
                    color = primaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.4.sp,
                )

                Text(
                    text =
                        when {
                            isListening -> "LISTENING"
                            isThinking -> "THINKING"
                            isSpeaking -> "SPEAKING"
                            else -> "READY"
                        },
                    color =
                        when {
                            isListening -> secondaryAccent
                            isThinking -> accent
                            isSpeaking -> secondaryAccent
                            else -> secondaryText
                        },
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "Create alarms and timers with Cypher's voice control. " +
                            "Manage everything currently scheduled below.",
                color = secondaryText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            LazyColumn(
                modifier =
                    Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp),
            ) {

                item {

                    SectionHeader(
                        title = "ACTIVE TIMERS",
                        count = activeTimers.size,
                        accent = secondaryAccent,
                        secondaryText = secondaryText,
                    )
                }

                if (activeTimers.isEmpty()) {

                    item {

                        EmptyCard(
                            text =
                                "No active timers. Tap Cypher and say “Set a timer for 20 minutes.”",
                            panel = panel,
                            accent = accent,
                            secondaryText = secondaryText,
                        )
                    }

                } else {

                    items(
                        items = activeTimers,
                        key = { timer -> timer.id },
                    ) { timer ->

                        TimerCard(
                            timer = timer,
                            nowMillis = nowMillis,
                            panel = panel,
                            accent = accent,
                            secondaryAccent = secondaryAccent,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            danger = danger,
                            onCancel = {

                                alarmScheduler.cancelTimer(
                                    timer.id
                                )

                                alarmRepository.markTimerInactive(
                                    timer.id
                                )

                                refresh()
                            },
                        )
                    }
                }

                item {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    SectionHeader(
                        title = "ALARMS",
                        count = alarms.count { it.enabled },
                        accent = accent,
                        secondaryText = secondaryText,
                    )
                }

                if (alarms.isEmpty()) {

                    item {

                        EmptyCard(
                            text =
                                "No alarms yet. Tap Cypher and say “Set an alarm for 6:30 AM.”",
                            panel = panel,
                            accent = accent,
                            secondaryText = secondaryText,
                        )
                    }

                } else {

                    items(
                        items = alarms,
                        key = { alarm -> alarm.id },
                    ) { alarm ->

                        AlarmCard(
                            alarm = alarm,
                            alarmScheduler = alarmScheduler,
                            panel = panel,
                            accent = accent,
                            secondaryAccent = secondaryAccent,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            danger = danger,
                            onEnabledChanged = { enabled ->

                                val updated =
                                    alarmRepository.setAlarmEnabled(
                                        alarmId = alarm.id,
                                        enabled = enabled,
                                    )

                                if (updated != null) {
                                    if (enabled) {
                                        alarmScheduler.scheduleAlarm(
                                            updated
                                        )
                                    } else {
                                        alarmScheduler.cancelAlarm(
                                            alarm.id
                                        )
                                    }
                                }

                                refresh()
                            },
                            onDelete = {
                                pendingDeleteAlarm =
                                    alarm
                            },
                        )
                    }
                }

                if (finishedTimers.isNotEmpty()) {

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text = "TIMER HISTORY",
                            color = secondaryText,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                        )
                    }

                    item {

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
                                        width = 1.dp,
                                        color =
                                            accent.copy(
                                                alpha = 0.18f
                                            ),
                                        shape =
                                            RoundedCornerShape(
                                                16.dp
                                            ),
                                    )
                                    .padding(16.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically,
                        ) {

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text =
                                        "${finishedTimers.size} finished ${
                                            if (finishedTimers.size == 1) {
                                                "timer"
                                            } else {
                                                "timers"
                                            }
                                        }",
                                    color = primaryText,
                                    fontSize = 14.sp,
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(3.dp)
                                )

                                Text(
                                    text =
                                        "Finished timers can be cleared safely.",
                                    color = secondaryText,
                                    fontSize = 12.sp,
                                )
                            }

                            Text(
                                text = "CLEAR",
                                color = danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                modifier =
                                    Modifier
                                        .clickable {

                                            alarmRepository
                                                .clearFinishedTimers()

                                            refresh()
                                        }
                                        .padding(
                                            horizontal = 8.dp,
                                            vertical = 8.dp,
                                        ),
                            )
                        }
                    }
                }

                item {
                    Spacer(
                        modifier =
                            Modifier.height(24.dp)
                    )
                }
            }
        }
    }

    val alarmToDelete =
        pendingDeleteAlarm

    if (alarmToDelete != null) {

        AlertDialog(
            onDismissRequest = {
                pendingDeleteAlarm =
                    null
            },
            title = {
                Text(
                    text = "Delete alarm?",
                    color = Color.White,
                )
            },
            text = {
                Text(
                    text =
                        "${alarmToDelete.label.ifBlank { "Alarm" }} at " +
                                formatAlarmTime(
                                    alarmToDelete.hour,
                                    alarmToDelete.minute,
                                ) +
                                " will be removed.",
                    color = secondaryText,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {

                        alarmScheduler.cancelAlarm(
                            alarmToDelete.id
                        )

                        alarmRepository.deleteAlarm(
                            alarmToDelete.id
                        )

                        pendingDeleteAlarm =
                            null

                        refresh()
                    },
                ) {

                    Text(
                        text = "DELETE",
                        color = danger,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDeleteAlarm =
                            null
                    },
                ) {

                    Text(
                        text = "CANCEL",
                        color = accent,
                    )
                }
            },
            containerColor = panel,
        )
    }
}


@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    accent: Color,
    secondaryText: Color,
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically,
    ) {

        Text(
            text = title,
            color = secondaryText,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
        )

        Text(
            text = count.toString(),
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}


@Composable
private fun EmptyCard(
    text: String,
    panel: Color,
    accent: Color,
    secondaryText: Color,
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = panel,
                    shape = RoundedCornerShape(16.dp),
                )
                .border(
                    width = 1.dp,
                    color =
                        accent.copy(
                            alpha = 0.18f
                        ),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                )
                .padding(18.dp),
    ) {

        Text(
            text = text,
            color = secondaryText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}


@Composable
private fun TimerCard(
    timer: CypherTimer,
    nowMillis: Long,
    panel: Color,
    accent: Color,
    secondaryAccent: Color,
    primaryText: Color,
    secondaryText: Color,
    danger: Color,
    onCancel: () -> Unit,
) {

    val remainingMillis =
        (
                timer.endsAtMillis -
                        nowMillis
                )
            .coerceAtLeast(
                0L
            )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = panel,
                    shape = RoundedCornerShape(18.dp),
                )
                .border(
                    width = 1.dp,
                    color =
                        secondaryAccent.copy(
                            alpha = 0.25f
                        ),
                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),
                )
                .padding(16.dp),
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically,
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        timer.label
                            .ifBlank {
                                "Timer"
                            }
                            .uppercase(
                                Locale.getDefault()
                            ),
                    color = primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Finishes ${formatClockTime(timer.endsAtMillis)}",
                    color = secondaryText,
                    fontSize = 12.sp,
                )
            }

            Text(
                text =
                    formatCountdown(
                        remainingMillis
                    ),
                color = secondaryAccent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically,
        ) {

            Text(
                text =
                    "Started for ${formatDuration(timer.durationMillis)}",
                color =
                    accent.copy(
                        alpha = 0.85f
                    ),
                fontSize = 11.sp,
            )

            Text(
                text = "CANCEL",
                color = danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier =
                    Modifier
                        .clickable {
                            onCancel()
                        }
                        .padding(
                            horizontal = 8.dp,
                            vertical = 6.dp,
                        ),
            )
        }
    }
}


@Composable
private fun AlarmCard(
    alarm: CypherAlarm,
    alarmScheduler: CypherAlarmScheduler,
    panel: Color,
    accent: Color,
    secondaryAccent: Color,
    primaryText: Color,
    secondaryText: Color,
    danger: Color,
    onEnabledChanged: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {

    val nextTrigger =
        if (alarm.enabled) {
            runCatching {
                alarmScheduler.calculateNextAlarmTime(
                    alarm
                )
            }.getOrNull()
        } else {
            null
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = panel,
                    shape = RoundedCornerShape(18.dp),
                )
                .border(
                    width = 1.dp,
                    color =
                        if (alarm.enabled) {
                            accent.copy(
                                alpha = 0.28f
                            )
                        } else {
                            secondaryText.copy(
                                alpha = 0.12f
                            )
                        },
                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),
                )
                .padding(16.dp),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text =
                    formatAlarmTime(
                        alarm.hour,
                        alarm.minute,
                    ),
                color =
                    if (alarm.enabled) {
                        primaryText
                    } else {
                        secondaryText
                    },
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    alarm.label
                        .ifBlank {
                            "Alarm"
                        }
                        .uppercase(
                            Locale.getDefault()
                        ),
                color =
                    if (alarm.enabled) {
                        accent
                    } else {
                        secondaryText
                    },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text =
                    if (alarm.repeatDays.isEmpty()) {
                        if (
                            nextTrigger !=
                            null
                        ) {
                            "Next: ${
                                formatDayAndTime(
                                    nextTrigger
                                )
                            }"
                        } else {
                            "One-time alarm"
                        }
                    } else {
                        formatRepeatDays(
                            alarm.repeatDays
                        )
                    },
                color = secondaryText,
                fontSize = 12.sp,
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = "DELETE",
                color = danger,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier =
                    Modifier
                        .clickable {
                            onDelete()
                        }
                        .padding(
                            vertical = 4.dp
                        ),
            )
        }

        Switch(
            checked =
                alarm.enabled,
            onCheckedChange =
                onEnabledChanged,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor =
                        secondaryAccent,
                    checkedTrackColor =
                        accent.copy(
                            alpha = 0.55f
                        ),
                    uncheckedThumbColor =
                        secondaryText,
                    uncheckedTrackColor =
                        Color.Black.copy(
                            alpha = 0.35f
                        ),
                ),
        )
    }
}


private fun formatAlarmTime(
    hour: Int,
    minute: Int,
): String {

    val calendar =
        Calendar
            .getInstance()
            .apply {
                set(
                    Calendar.HOUR_OF_DAY,
                    hour,
                )

                set(
                    Calendar.MINUTE,
                    minute,
                )
            }

    return SimpleDateFormat(
        "h:mm a",
        Locale.getDefault(),
    )
        .format(
            calendar.time
        )
}


private fun formatClockTime(
    millis: Long,
): String {

    return SimpleDateFormat(
        "h:mm:ss a",
        Locale.getDefault(),
    )
        .format(
            Date(
                millis
            )
        )
}


private fun formatDayAndTime(
    millis: Long,
): String {

    return SimpleDateFormat(
        "EEE h:mm a",
        Locale.getDefault(),
    )
        .format(
            Date(
                millis
            )
        )
}


private fun formatCountdown(
    millis: Long,
): String {

    val totalSeconds =
        millis /
                1000L

    val hours =
        totalSeconds /
                3600L

    val minutes =
        (
                totalSeconds %
                        3600L
                ) /
                60L

    val seconds =
        totalSeconds %
                60L

    return if (
        hours >
        0L
    ) {
        String.format(
            Locale.getDefault(),
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds,
        )
    } else {
        String.format(
            Locale.getDefault(),
            "%02d:%02d",
            minutes,
            seconds,
        )
    }
}


private fun formatDuration(
    millis: Long,
): String {

    val totalMinutes =
        (
                millis /
                        60_000L
                )
            .coerceAtLeast(
                1L
            )

    val hours =
        totalMinutes /
                60L

    val minutes =
        totalMinutes %
                60L

    return when {

        hours >
                0L &&
                minutes >
                0L ->
            "$hours h $minutes min"

        hours >
                0L ->
            "$hours h"

        else ->
            "$minutes min"
    }
}


private fun formatRepeatDays(
    days: Set<Int>,
): String {

    val weekdays =
        setOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
        )

    val everyDay =
        setOf(
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
        )

    if (
        days ==
        weekdays
    ) {
        return "Every weekday"
    }

    if (
        days ==
        everyDay
    ) {
        return "Every day"
    }

    val labels =
        listOf(
            Calendar.SUNDAY to "Sun",
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
        )
            .filter {
                    (day, _) ->
                day in days
            }
            .joinToString(
                "  "
            ) {
                it.second
            }

    return labels
        .ifBlank {
            "One-time alarm"
        }
}
