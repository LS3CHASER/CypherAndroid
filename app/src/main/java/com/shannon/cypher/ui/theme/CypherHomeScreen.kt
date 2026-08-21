package com.shannon.cypher.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shannon.cypher.R
import com.shannon.cypher.audio.CypherRemoteSpeaker
import com.shannon.cypher.audio.CypherSpeaker
import com.shannon.cypher.audio.CypherSpeechRecognizer
import com.shannon.cypher.calendar.CypherCalendarEvent
import com.shannon.cypher.calendar.CypherCalendarManager
import com.shannon.cypher.identity.CypherNameNormalizer
import com.shannon.cypher.network.CypherApiClient
import com.shannon.cypher.ui.theme.CypherTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private data class CalendarCreateRequest(
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val confirmationDay: String,
    val confirmationTime: String,
)


@Composable
fun CypherHomeScreen() {

    val background =
        Color(0xFF070509)

    val panel =
        Color(0xFF110D16)

    val accent =
        Color(0xFF8A2BE2)

    val secondaryAccent =
        Color(0xFF76FF03)

    val secondaryText =
        Color(0xFFA99AAF)


    val context =
        LocalContext.current

    val isPreview =
        LocalInspectionMode.current

    val coroutineScope =
        rememberCoroutineScope()


    var isListening by remember {
        mutableStateOf(false)
    }

    var isThinking by remember {
        mutableStateOf(false)
    }

    var isSpeaking by remember {
        mutableStateOf(false)
    }

    var microphoneLevel by remember {
        mutableStateOf(0f)
    }

    var recognizedText by remember {
        mutableStateOf("")
    }

    var cypherReply by remember {
        mutableStateOf("")
    }


    var pendingCalendarTarget by remember {
        mutableStateOf("today")
    }


    var pendingCalendarCreateRequest by remember {
        mutableStateOf<CalendarCreateRequest?>(null)
    }


    /*
     * Remembers the last upcoming calendar
     * event Cypher told us about.
     *
     * This enables:
     *
     * "What's my next appointment?"
     *
     * followed by:
     *
     * "What's after that?"
     */
    var lastCalendarEventStartMillis by remember {
        mutableStateOf<Long?>(null)
    }


    val speechRecognizer =
        remember {

            if (isPreview) {

                null

            } else {

                CypherSpeechRecognizer(
                    context
                )
            }
        }


    val remoteSpeaker =
        remember {

            if (isPreview) {

                null

            } else {

                CypherRemoteSpeaker(
                    context
                )
            }
        }


    val fallbackSpeaker =
        remember {

            if (isPreview) {

                null

            } else {

                CypherSpeaker(
                    context
                )
            }
        }


    val apiClient =
        remember {

            CypherApiClient()
        }


    val calendarManager =
        remember {

            if (isPreview) {

                null

            } else {

                CypherCalendarManager(
                    context
                )
            }
        }


    DisposableEffect(Unit) {

        onDispose {

            speechRecognizer
                ?.destroy()

            remoteSpeaker
                ?.destroy()

            fallbackSpeaker
                ?.destroy()
        }
    }


    fun stopSpeaking() {

        remoteSpeaker
            ?.stop()

        fallbackSpeaker
            ?.stop()

        isSpeaking =
            false
    }


    fun speakReply(
        reply: String,
    ) {

        if (
            reply.isBlank()
        ) {

            return
        }


        coroutineScope.launch {

            try {

                remoteSpeaker?.speak(

                    text =
                        reply,

                    onStart = {

                        isSpeaking =
                            true
                    },

                    onDone = {

                        isSpeaking =
                            false
                    },
                )

            } catch (
                error: Exception
            ) {

                fallbackSpeaker?.speak(

                    text =
                        reply,

                    onStart = {

                        isSpeaking =
                            true
                    },

                    onDone = {

                        isSpeaking =
                            false
                    },
                )
            }
        }
    }


    fun weekdayNumber(
        weekday: String,
    ): Int? {

        return when (
            weekday
        ) {

            "monday" ->
                Calendar.MONDAY

            "tuesday" ->
                Calendar.TUESDAY

            "wednesday" ->
                Calendar.WEDNESDAY

            "thursday" ->
                Calendar.THURSDAY

            "friday" ->
                Calendar.FRIDAY

            "saturday" ->
                Calendar.SATURDAY

            "sunday" ->
                Calendar.SUNDAY

            else ->
                null
        }
    }


    fun getCalendarTarget(
        message: String,
    ): String {

        val lowerMessage =
            message.lowercase()


        if (
            lowerMessage.contains(
                "tomorrow"
            )
        ) {

            return "tomorrow"
        }


        if (
            lowerMessage.contains(
                "today"
            )
        ) {

            return "today"
        }


        val weekdays =
            listOf(
                "monday",
                "tuesday",
                "wednesday",
                "thursday",
                "friday",
                "saturday",
                "sunday",
            )


        for (
        weekday in weekdays
        ) {

            if (
                lowerMessage.contains(
                    weekday
                )
            ) {

                return weekday
            }
        }


        return "today"
    }


    fun isNextAfterThatRequest(
        message: String,
    ): Boolean {

        val lowerMessage =
            message
                .lowercase()
                .trim()


        val phrases =
            listOf(

                "next appointment after that",

                "next event after that",

                "next meeting after that",

                "what's after that",

                "whats after that",

                "what is after that",

                "what's the one after that",

                "whats the one after that",

                "what is the one after that",

                "what about the one after that",

                "and after that",

                "after that",

                "next one",

                "the next one",

                "and the next one",
            )


        return phrases.any {
                phrase ->

            lowerMessage.contains(
                phrase
            )
        }
    }


    fun isNextAppointmentRequest(
        message: String,
    ): Boolean {

        val lowerMessage =
            message.lowercase()


        val phrases =
            listOf(

                "next appointment",

                "next event",

                "next meeting",

                "next calendar event",

                "what's my next appointment",

                "whats my next appointment",

                "what is my next appointment",

                "what's my next event",

                "whats my next event",

                "what is my next event",

                "what's next on my calendar",

                "whats next on my calendar",

                "what is next on my calendar",

                "what have i got next",

                "what do i have next",
            )


        return phrases.any {
                phrase ->

            lowerMessage.contains(
                phrase
            )
        }
    }


    fun isCalendarCreateRequest(
        message: String,
    ): Boolean {

        val lowerMessage =
            message.lowercase()


        val creationWords =
            listOf(
                "add",
                "create",
                "put",
                "schedule",
                "book",
                "make",
                "set",
            )


        val eventWords =
            listOf(
                "appointment",
                "meeting",
                "event",
                "booking",
            )


        val dayWords =
            listOf(
                "today",
                "tomorrow",
                "monday",
                "tuesday",
                "wednesday",
                "thursday",
                "friday",
                "saturday",
                "sunday",
            )


        val readQuestionPhrases =
            listOf(
                "what's on",
                "whats on",
                "what is on",
                "what have i got",
                "what do i have",
                "what am i doing",
                "show me",
                "tell me what's",
                "tell me whats",
                "do i have",
            )


        val hasCreationWord =
            creationWords.any {
                    word ->

                Regex(
                    "\\b${Regex.escape(word)}\\b"
                ).containsMatchIn(
                    lowerMessage
                )
            }


        val hasEventWord =
            eventWords.any {
                    word ->

                Regex(
                    "\\b${Regex.escape(word)}\\b"
                ).containsMatchIn(
                    lowerMessage
                )
            }


        val hasDay =
            dayWords.any {
                    day ->

                lowerMessage.contains(
                    day
                )
            }


        val normalisedTimeText =
            lowerMessage.replace(
                ".",
                ""
            )


        val hasTime =
            Regex(
                "\\b\\d{1,2}" +
                        "(?::\\d{2})?" +
                        "\\s*(am|pm)\\b",
                RegexOption.IGNORE_CASE,
            ).containsMatchIn(
                normalisedTimeText
            )


        val soundsLikeReadQuestion =
            readQuestionPhrases.any {
                    phrase ->

                lowerMessage.contains(
                    phrase
                )
            }


        if (
            hasCreationWord &&
            hasEventWord
        ) {

            return true
        }


        if (
            hasEventWord &&
            hasDay &&
            hasTime &&
            !soundsLikeReadQuestion
        ) {

            return true
        }


        return false
    }


    fun isCalendarReadRequest(
        message: String,
    ): Boolean {

        val lowerMessage =
            message.lowercase()


        val readPhrases =
            listOf(

                "what's on my calendar",

                "whats on my calendar",

                "what is on my calendar",

                "what have i got on",

                "what do i have on",

                "what am i doing",

                "what's on today",

                "whats on today",

                "what is on today",

                "what's on tomorrow",

                "whats on tomorrow",

                "what is on tomorrow",

                "appointments today",

                "appointments tomorrow",

                "events today",

                "events tomorrow",

                "show me my calendar",

                "tell me what's on my calendar",

                "tell me whats on my calendar",
            )


        if (
            readPhrases.any {
                    phrase ->

                lowerMessage.contains(
                    phrase
                )
            }
        ) {

            return true
        }


        val weekdays =
            listOf(
                "monday",
                "tuesday",
                "wednesday",
                "thursday",
                "friday",
                "saturday",
                "sunday",
            )


        val hasWeekday =
            weekdays.any {
                    day ->

                lowerMessage.contains(
                    day
                )
            }


        val questionWords =
            listOf(
                "what",
                "what's",
                "whats",
                "have i",
                "do i have",
                "am i doing",
                "show me",
                "tell me",
            )


        val looksLikeQuestion =
            questionWords.any {
                    phrase ->

                lowerMessage.contains(
                    phrase
                )
            }


        return (
                hasWeekday &&
                        looksLikeQuestion &&
                        lowerMessage.contains(
                            "calendar"
                        )
                )
    }


    fun getCalendarDayRange(
        target: String,
    ): Pair<Long, Long> {

        val targetDay =
            Calendar.getInstance()


        when (
            target
        ) {

            "tomorrow" -> {

                targetDay.add(
                    Calendar.DAY_OF_YEAR,
                    1
                )
            }


            "today" -> {
            }


            else -> {

                val requestedWeekday =
                    weekdayNumber(
                        target
                    )


                if (
                    requestedWeekday != null
                ) {

                    val currentWeekday =
                        targetDay.get(
                            Calendar.DAY_OF_WEEK
                        )


                    val daysAhead =
                        (
                                requestedWeekday -
                                        currentWeekday +
                                        7
                                ) % 7


                    if (
                        daysAhead > 0
                    ) {

                        targetDay.add(
                            Calendar.DAY_OF_YEAR,
                            daysAhead,
                        )
                    }
                }
            }
        }


        val startOfDay =
            targetDay.clone()
                    as Calendar


        startOfDay.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        startOfDay.set(
            Calendar.MINUTE,
            0
        )

        startOfDay.set(
            Calendar.SECOND,
            0
        )

        startOfDay.set(
            Calendar.MILLISECOND,
            0
        )


        val endOfDay =
            targetDay.clone()
                    as Calendar


        endOfDay.set(
            Calendar.HOUR_OF_DAY,
            23
        )

        endOfDay.set(
            Calendar.MINUTE,
            59
        )

        endOfDay.set(
            Calendar.SECOND,
            59
        )

        endOfDay.set(
            Calendar.MILLISECOND,
            999
        )


        return Pair(
            startOfDay.timeInMillis,
            endOfDay.timeInMillis,
        )
    }


    fun calendarTargetDescription(
        target: String,
    ): String {

        return when (
            target
        ) {

            "today" ->
                "today"

            "tomorrow" ->
                "tomorrow"

            else ->

                target.replaceFirstChar {

                    if (
                        it.isLowerCase()
                    ) {

                        it.titlecase(
                            Locale.getDefault()
                        )

                    } else {

                        it.toString()
                    }
                }
        }
    }


    fun getEventsForCalendarTarget(
        target: String,
    ): List<CypherCalendarEvent> {

        val manager =
            calendarManager
                ?: return emptyList()


        return when (
            target
        ) {

            "today" ->
                manager.getTodayEvents()


            "tomorrow" ->
                manager.getTomorrowEvents()


            else -> {

                val range =
                    getCalendarDayRange(
                        target
                    )


                manager.getEventsBetween(
                    startMillis =
                        range.first,

                    endMillis =
                        range.second,
                )
            }
        }
    }


    /*
     * Search for the first upcoming event
     * AFTER the supplied timestamp.
     */
    fun getNextCalendarEvent(
        afterMillis: Long,
    ): CypherCalendarEvent? {

        val manager =
            calendarManager
                ?: return null


        val searchEnd =
            Calendar
                .getInstance()
                .apply {

                    timeInMillis =
                        afterMillis

                    add(
                        Calendar.YEAR,
                        1
                    )
                }
                .timeInMillis


        return manager
            .getEventsBetween(
                startMillis =
                    afterMillis + 1,

                endMillis =
                    searchEnd,
            )
            .firstOrNull {
                    event ->

                event.startTimeMillis >
                        afterMillis
            }
    }


    fun formatCalendarEvents(
        events: List<CypherCalendarEvent>,
        target: String,
    ): String {

        val description =
            calendarTargetDescription(
                target
            )


        if (
            events.isEmpty()
        ) {

            return when (
                target
            ) {

                "today" ->
                    "You have nothing on your calendar today."

                "tomorrow" ->
                    "You have nothing on your calendar tomorrow."

                else ->
                    "You have nothing on your calendar for $description."
            }
        }


        val timeFormat =
            SimpleDateFormat(
                "h:mm a",
                Locale.getDefault(),
            )


        val eventDescriptions =
            events.map {
                    event ->

                if (
                    event.allDay
                ) {

                    event.title

                } else {

                    val startTime =
                        timeFormat.format(
                            event.startTimeMillis
                        )


                    "${event.title} at $startTime"
                }
            }


        val count =
            if (
                events.size == 1
            ) {

                "one event"

            } else {

                "${events.size} events"
            }


        val intro =
            when (
                target
            ) {

                "today" ->
                    "You have $count today."

                "tomorrow" ->
                    "You have $count tomorrow."

                else ->
                    "You have $count on $description."
            }


        return (
                intro +
                        " " +
                        eventDescriptions.joinToString(
                            separator = ", "
                        ) +
                        "."
                )
    }


    fun readCalendar(
        target: String,
    ) {

        val events =
            getEventsForCalendarTarget(
                target
            )


        val reply =
            formatCalendarEvents(
                events =
                    events,

                target =
                    target,
            )


        cypherReply =
            reply


        speakReply(
            reply
        )
    }


    /*
     * Speak an upcoming event and
     * remember it for "after that".
     */
    fun speakUpcomingEvent(
        event: CypherCalendarEvent?,
        isFollowUp: Boolean,
    ) {

        val reply =
            if (
                event == null
            ) {

                if (
                    isFollowUp
                ) {

                    "You have no later appointments on your calendar."

                } else {

                    "You have no upcoming appointments."
                }

            } else {

                lastCalendarEventStartMillis =
                    event.startTimeMillis


                val dateFormat =
                    SimpleDateFormat(
                        "EEEE d MMMM",
                        Locale.getDefault(),
                    )


                val date =
                    dateFormat.format(
                        event.startTimeMillis
                    )


                if (
                    event.allDay
                ) {

                    if (
                        isFollowUp
                    ) {

                        (
                                "After that, your next event is " +
                                        "${event.title} on $date."
                                )

                    } else {

                        (
                                "Your next event is " +
                                        "${event.title} on $date."
                                )
                    }

                } else {

                    val timeFormat =
                        SimpleDateFormat(
                            "h:mm a",
                            Locale.getDefault(),
                        )


                    val time =
                        timeFormat.format(
                            event.startTimeMillis
                        )


                    if (
                        isFollowUp
                    ) {

                        (
                                "After that, your next event is " +
                                        "${event.title} on $date " +
                                        "at $time."
                                )

                    } else {

                        (
                                "Your next event is " +
                                        "${event.title} on $date " +
                                        "at $time."
                                )
                    }
                }
            }


        cypherReply =
            reply


        speakReply(
            reply
        )
    }


    fun readNextAppointment() {

        val event =
            getNextCalendarEvent(
                afterMillis =
                    System.currentTimeMillis()
            )


        speakUpcomingEvent(
            event =
                event,

            isFollowUp =
                false,
        )
    }


    fun readAppointmentAfterThat() {

        val previousEventTime =
            lastCalendarEventStartMillis


        if (
            previousEventTime == null
        ) {

            val reply =
                (
                        "I don't have a previous appointment " +
                                "to continue from. Ask me for your " +
                                "next appointment first."
                        )


            cypherReply =
                reply


            speakReply(
                reply
            )

            return
        }


        val event =
            getNextCalendarEvent(
                afterMillis =
                    previousEventTime
            )


        speakUpcomingEvent(
            event =
                event,

            isFollowUp =
                true,
        )
    }


    fun calculateEventDate(
        target: String,
        hour: Int,
        minute: Int,
    ): Calendar {

        val result =
            Calendar.getInstance()


        when (
            target
        ) {

            "tomorrow" -> {

                result.add(
                    Calendar.DAY_OF_YEAR,
                    1
                )
            }


            "today" -> {
            }


            else -> {

                val requestedWeekday =
                    weekdayNumber(
                        target
                    )


                if (
                    requestedWeekday != null
                ) {

                    val currentWeekday =
                        result.get(
                            Calendar.DAY_OF_WEEK
                        )


                    var daysAhead =
                        (
                                requestedWeekday -
                                        currentWeekday +
                                        7
                                ) % 7


                    if (
                        daysAhead == 0
                    ) {

                        val requestedMinutes =
                            hour * 60 +
                                    minute


                        val currentMinutes =
                            result.get(
                                Calendar.HOUR_OF_DAY
                            ) * 60 +
                                    result.get(
                                        Calendar.MINUTE
                                    )


                        if (
                            requestedMinutes <=
                            currentMinutes
                        ) {

                            daysAhead =
                                7
                        }
                    }


                    if (
                        daysAhead > 0
                    ) {

                        result.add(
                            Calendar.DAY_OF_YEAR,
                            daysAhead,
                        )
                    }
                }
            }
        }


        result.set(
            Calendar.HOUR_OF_DAY,
            hour
        )

        result.set(
            Calendar.MINUTE,
            minute
        )

        result.set(
            Calendar.SECOND,
            0
        )

        result.set(
            Calendar.MILLISECOND,
            0
        )


        return result
    }


    fun extractEventTitle(
        message: String,
    ): String {

        var title =
            message
                .replace(
                    Regex(
                        "(?i)\\bcypher\\b"
                    ),
                    "",
                )
                .trim()


        title =
            title.replace(
                Regex(
                    "(?i)^(please\\s+)?(add|create|put|schedule|book|make|set)\\s+"
                ),
                "",
            )


        title =
            title.replace(
                Regex(
                    "(?i)\\b(to|on|in)\\s+(my\\s+)?calendar\\b"
                ),
                "",
            )


        title =
            title.replace(
                Regex(
                    "(?i)\\b(today|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b.*$"
                ),
                "",
            )


        title =
            title.replace(
                Regex(
                    "(?i)\\bfrom\\s+\\d{1,2}(:\\d{2})?\\s*(am|pm)?.*$"
                ),
                "",
            )


        title =
            title.replace(
                Regex(
                    "(?i)\\bat\\s+\\d{1,2}(:\\d{2})?\\s*(am|pm)?.*$"
                ),
                "",
            )


        title =
            title.replace(
                Regex(
                    "(?i)\\bfor\\s+(half\\s+an\\s+hour|an\\s+hour|one\\s+hour|\\d+\\s+hours?|\\d+\\s+minutes?).*$"
                ),
                "",
            )


        title =
            title
                .replace(
                    Regex(
                        "\\s+"
                    ),
                    " ",
                )
                .trim(
                    ' ',
                    ',',
                    '.',
                )


        if (
            title.isBlank()
        ) {

            return "Calendar event"
        }


        return title.replaceFirstChar {

            if (
                it.isLowerCase()
            ) {

                it.titlecase(
                    Locale.getDefault()
                )

            } else {

                it.toString()
            }
        }
    }


    fun convertTo24Hour(
        rawHour: Int,
        period: String,
    ): Int {

        var hour =
            rawHour


        if (
            period == "am" &&
            rawHour == 12
        ) {

            hour =
                0
        }


        if (
            period == "pm" &&
            rawHour != 12
        ) {

            hour =
                rawHour + 12
        }


        return hour
    }


    fun calculateDurationEnd(
        normalizedMessage: String,
        start: Calendar,
        startPeriod: String,
    ): Calendar {

        val end =
            start.clone()
                    as Calendar


        end.add(
            Calendar.HOUR_OF_DAY,
            1
        )


        if (
            Regex(
                "\\bfor\\s+half\\s+an\\s+hour\\b",
                RegexOption.IGNORE_CASE,
            ).containsMatchIn(
                normalizedMessage
            )
        ) {

            end.timeInMillis =
                start.timeInMillis


            end.add(
                Calendar.MINUTE,
                30
            )


            return end
        }


        if (
            Regex(
                "\\bfor\\s+(an|one)\\s+hour\\b",
                RegexOption.IGNORE_CASE,
            ).containsMatchIn(
                normalizedMessage
            )
        ) {

            end.timeInMillis =
                start.timeInMillis


            end.add(
                Calendar.HOUR_OF_DAY,
                1
            )


            return end
        }


        val minuteDuration =
            Regex(
                "\\bfor\\s+(\\d+)\\s+minutes?\\b",
                RegexOption.IGNORE_CASE,
            ).find(
                normalizedMessage
            )


        if (
            minuteDuration != null
        ) {

            val minutes =
                minuteDuration
                    .groupValues[1]
                    .toIntOrNull()


            if (
                minutes != null &&
                minutes > 0
            ) {

                end.timeInMillis =
                    start.timeInMillis


                end.add(
                    Calendar.MINUTE,
                    minutes
                )


                return end
            }
        }


        val hourDuration =
            Regex(
                "\\bfor\\s+(\\d+)\\s+hours?\\b",
                RegexOption.IGNORE_CASE,
            ).find(
                normalizedMessage
            )


        if (
            hourDuration != null
        ) {

            val hours =
                hourDuration
                    .groupValues[1]
                    .toIntOrNull()


            if (
                hours != null &&
                hours > 0
            ) {

                end.timeInMillis =
                    start.timeInMillis


                end.add(
                    Calendar.HOUR_OF_DAY,
                    hours
                )


                return end
            }
        }


        val untilMatch =
            Regex(
                "\\b(?:until|till)\\s+" +
                        "(\\d{1,2})" +
                        "(?::(\\d{2}))?" +
                        "\\s*(am|pm)?\\b",
                RegexOption.IGNORE_CASE,
            ).find(
                normalizedMessage
            )


        if (
            untilMatch != null
        ) {

            val rawEndHour =
                untilMatch
                    .groupValues[1]
                    .toIntOrNull()


            val endMinute =
                untilMatch
                    .groupValues[2]
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.toIntOrNull()
                    ?: 0


            val explicitEndPeriod =
                untilMatch
                    .groupValues[3]
                    .lowercase()


            if (
                rawEndHour != null &&
                rawEndHour in 1..12 &&
                endMinute in 0..59
            ) {

                val endHour24 =
                    if (
                        explicitEndPeriod.isNotBlank()
                    ) {

                        convertTo24Hour(
                            rawHour =
                                rawEndHour,

                            period =
                                explicitEndPeriod,
                        )

                    } else {

                        var inferredHour =
                            convertTo24Hour(
                                rawHour =
                                    rawEndHour,

                                period =
                                    startPeriod,
                            )


                        val startMinutes =
                            start.get(
                                Calendar.HOUR_OF_DAY
                            ) * 60 +
                                    start.get(
                                        Calendar.MINUTE
                                    )


                        val endMinutes =
                            inferredHour * 60 +
                                    endMinute


                        if (
                            endMinutes <=
                            startMinutes
                        ) {

                            inferredHour +=
                                12


                            if (
                                inferredHour >= 24
                            ) {

                                inferredHour -=
                                    24
                            }
                        }


                        inferredHour
                    }


                end.timeInMillis =
                    start.timeInMillis


                end.set(
                    Calendar.HOUR_OF_DAY,
                    endHour24
                )

                end.set(
                    Calendar.MINUTE,
                    endMinute
                )

                end.set(
                    Calendar.SECOND,
                    0
                )

                end.set(
                    Calendar.MILLISECOND,
                    0
                )


                if (
                    end.timeInMillis <=
                    start.timeInMillis
                ) {

                    end.add(
                        Calendar.DAY_OF_YEAR,
                        1
                    )
                }


                return end
            }
        }


        return end
    }


    fun parseCalendarCreateRequest(
        message: String,
    ): CalendarCreateRequest? {

        val normalizedMessage =
            message
                .lowercase()
                .replace(
                    ".",
                    ""
                )
                .replace(
                    ",",
                    ""
                )
                .replace(
                    Regex("\\s+"),
                    " ",
                )
                .trim()


        val target =
            getCalendarTarget(
                normalizedMessage
            )


        val match =
            Regex(
                "\\b(\\d{1,2})" +
                        "(?::(\\d{2}))?" +
                        "\\s*(am|pm)\\b",
                RegexOption.IGNORE_CASE,
            ).find(
                normalizedMessage
            )
                ?: return null


        val rawHour =
            match
                .groupValues[1]
                .toIntOrNull()
                ?: return null


        val minute =
            match
                .groupValues[2]
                .takeIf {
                    it.isNotBlank()
                }
                ?.toIntOrNull()
                ?: 0


        val period =
            match
                .groupValues[3]
                .lowercase()


        if (
            rawHour !in 1..12 ||
            minute !in 0..59
        ) {

            return null
        }


        val hour24 =
            convertTo24Hour(
                rawHour =
                    rawHour,

                period =
                    period,
            )


        val start =
            calculateEventDate(
                target =
                    target,

                hour =
                    hour24,

                minute =
                    minute,
            )


        val end =
            calculateDurationEnd(
                normalizedMessage =
                    normalizedMessage,

                start =
                    start,

                startPeriod =
                    period,
            )


        val title =
            extractEventTitle(
                normalizedMessage
            )


        val displayTime =
            SimpleDateFormat(
                "h:mm a",
                Locale.getDefault(),
            ).format(
                start.time
            )


        val displayDay =
            calendarTargetDescription(
                target
            )


        return CalendarCreateRequest(

            title =
                title,

            startTimeMillis =
                start.timeInMillis,

            endTimeMillis =
                end.timeInMillis,

            confirmationDay =
                displayDay,

            confirmationTime =
                displayTime,
        )
    }


    fun createCalendarEvent(
        request: CalendarCreateRequest,
    ) {

        val manager =
            calendarManager


        if (
            manager == null
        ) {

            val reply =
                "I couldn't access your calendar."


            cypherReply =
                reply


            speakReply(
                reply
            )


            return
        }


        val eventId =
            manager.createEvent(

                title =
                    request.title,

                startTimeMillis =
                    request.startTimeMillis,

                endTimeMillis =
                    request.endTimeMillis,
            )


        val reply =
            if (
                eventId != null
            ) {

                (
                        "Done. I've added " +
                                "${request.title} to your calendar " +
                                "for ${request.confirmationDay} " +
                                "at ${request.confirmationTime}."
                        )

            } else {

                (
                        "I couldn't add that event. " +
                                "I may not have access to a writable calendar."
                        )
            }


        cypherReply =
            reply


        speakReply(
            reply
        )
    }


    val calendarReadPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) {
                granted ->


            if (
                granted
            ) {

                when (
                    pendingCalendarTarget
                ) {

                    "next" -> {

                        readNextAppointment()
                    }


                    "next_after" -> {

                        readAppointmentAfterThat()
                    }


                    else -> {

                        readCalendar(
                            pendingCalendarTarget
                        )
                    }
                }

            } else {

                cypherReply =
                    "Calendar permission was not granted."
            }
        }


    fun requestNextAppointment() {

        val permissionGranted =
            ContextCompat
                .checkSelfPermission(
                    context,

                    Manifest
                        .permission
                        .READ_CALENDAR,
                ) ==
                    PackageManager
                        .PERMISSION_GRANTED


        if (
            permissionGranted
        ) {

            readNextAppointment()

        } else {

            pendingCalendarTarget =
                "next"


            calendarReadPermissionLauncher
                .launch(
                    Manifest
                        .permission
                        .READ_CALENDAR
                )
        }
    }


    fun requestAppointmentAfterThat() {

        val permissionGranted =
            ContextCompat
                .checkSelfPermission(
                    context,

                    Manifest
                        .permission
                        .READ_CALENDAR,
                ) ==
                    PackageManager
                        .PERMISSION_GRANTED


        if (
            permissionGranted
        ) {

            readAppointmentAfterThat()

        } else {

            pendingCalendarTarget =
                "next_after"


            calendarReadPermissionLauncher
                .launch(
                    Manifest
                        .permission
                        .READ_CALENDAR
                )
        }
    }


    val calendarWritePermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestMultiplePermissions()
        ) {
                permissions ->


            val readGranted =
                permissions[
                    Manifest.permission.READ_CALENDAR
                ] ==
                        true ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CALENDAR,
                        ) ==
                        PackageManager.PERMISSION_GRANTED


            val writeGranted =
                permissions[
                    Manifest.permission.WRITE_CALENDAR
                ] ==
                        true ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_CALENDAR,
                        ) ==
                        PackageManager.PERMISSION_GRANTED


            if (
                readGranted &&
                writeGranted
            ) {

                pendingCalendarCreateRequest
                    ?.let {
                            request ->

                        createCalendarEvent(
                            request
                        )
                    }

            } else {

                val reply =
                    "Calendar write permission was not granted."


                cypherReply =
                    reply


                speakReply(
                    reply
                )
            }


            pendingCalendarCreateRequest =
                null
        }


    fun handleCalendarReadRequest(
        message: String,
    ) {

        val target =
            getCalendarTarget(
                message
            )


        pendingCalendarTarget =
            target


        val permissionGranted =
            ContextCompat
                .checkSelfPermission(
                    context,

                    Manifest
                        .permission
                        .READ_CALENDAR,
                ) ==
                    PackageManager
                        .PERMISSION_GRANTED


        if (
            permissionGranted
        ) {

            readCalendar(
                target
            )

        } else {

            calendarReadPermissionLauncher
                .launch(
                    Manifest
                        .permission
                        .READ_CALENDAR
                )
        }
    }


    fun handleCalendarCreateRequest(
        message: String,
    ) {

        val request =
            parseCalendarCreateRequest(
                message
            )


        if (
            request == null
        ) {

            val reply =
                (
                        "I can add that, but please include " +
                                "a day and time, for example, " +
                                "add dentist appointment Monday at 2 PM."
                        )


            cypherReply =
                reply


            speakReply(
                reply
            )


            return
        }


        val readGranted =
            ContextCompat
                .checkSelfPermission(
                    context,

                    Manifest
                        .permission
                        .READ_CALENDAR,
                ) ==
                    PackageManager
                        .PERMISSION_GRANTED


        val writeGranted =
            ContextCompat
                .checkSelfPermission(
                    context,

                    Manifest
                        .permission
                        .WRITE_CALENDAR,
                ) ==
                    PackageManager
                        .PERMISSION_GRANTED


        if (
            readGranted &&
            writeGranted
        ) {

            createCalendarEvent(
                request
            )

        } else {

            pendingCalendarCreateRequest =
                request


            calendarWritePermissionLauncher
                .launch(
                    arrayOf(
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR,
                    )
                )
        }
    }


    fun sendMessageToCypherOS(
        message: String,
    ) {

        if (
            message.isBlank()
        ) {

            return
        }


        isThinking =
            true

        isSpeaking =
            false

        cypherReply =
            ""


        coroutineScope.launch {

            try {

                val reply =
                    withContext(
                        Dispatchers.IO
                    ) {

                        apiClient.sendMessage(
                            message
                        )
                    }


                cypherReply =
                    reply


                isThinking =
                    false


                speakReply(
                    reply
                )

            } catch (
                error: Exception
            ) {

                isThinking =
                    false


                cypherReply =
                    "I couldn't connect to CypherOS."
            }
        }
    }


    fun startListening() {

        recognizedText =
            ""

        cypherReply =
            ""


        stopSpeaking()


        speechRecognizer?.start(

            onListeningChanged = {
                    listening ->


                isListening =
                    listening


                if (
                    !listening
                ) {

                    microphoneLevel =
                        0f
                }
            },


            onLevelChanged = {
                    level ->


                microphoneLevel =
                    level
            },


            onTextRecognized = {
                    text ->


                val normalizedText =
                    CypherNameNormalizer
                        .normalize(
                            text
                        )


                recognizedText =
                    normalizedText


                when {

                    /*
                     * IMPORTANT:
                     * Check "after that" BEFORE
                     * generic "next appointment".
                     */
                    isNextAfterThatRequest(
                        normalizedText
                    ) -> {

                        requestAppointmentAfterThat()
                    }


                    isCalendarCreateRequest(
                        normalizedText
                    ) -> {

                        handleCalendarCreateRequest(
                            normalizedText
                        )
                    }


                    isNextAppointmentRequest(
                        normalizedText
                    ) -> {

                        requestNextAppointment()
                    }


                    isCalendarReadRequest(
                        normalizedText
                    ) -> {

                        handleCalendarReadRequest(
                            normalizedText
                        )
                    }


                    else -> {

                        sendMessageToCypherOS(
                            normalizedText
                        )
                    }
                }
            },
        )
    }


    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) {
                granted ->


            if (
                granted
            ) {

                startListening()

            } else {

                isListening =
                    false
            }
        }


    val currentHour =
        Calendar
            .getInstance()
            .get(
                Calendar.HOUR_OF_DAY
            )


    val greeting =
        when (
            currentHour
        ) {

            in 5..11 ->
                "Good morning, Shannon."

            in 12..16 ->
                "Good afternoon, Shannon."

            else ->
                "Good evening, Shannon."
        }


    val infiniteTransition =
        rememberInfiniteTransition(
            label =
                "CypherCoreAnimation"
        )


    val idlePulse by
    infiniteTransition.animateFloat(

        initialValue =
            0.96f,

        targetValue =
            1.04f,

        animationSpec =
            infiniteRepeatable(

                animation =
                    tween(
                        durationMillis =
                            1800,

                        easing =
                            FastOutSlowInEasing,
                    ),

                repeatMode =
                    RepeatMode.Reverse,
            ),

        label =
            "IdlePulse",
    )


    val middleRotation by
    infiniteTransition.animateFloat(

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
                                    1800

                                isListening ->
                                    3000

                                isSpeaking ->
                                    4500

                                else ->
                                    9000
                            },
                    ),

                repeatMode =
                    RepeatMode.Restart,
            ),

        label =
            "MiddleRotation",
    )


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
                    .padding(
                        horizontal =
                            24.dp,

                        vertical =
                            40.dp,
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {


            Text(
                text =
                    "C Y P H E R",

                color =
                    accent,

                fontSize =
                    28.sp,

                fontWeight =
                    FontWeight.Light,

                letterSpacing =
                    8.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Text(
                text =
                    when {

                        isListening ->
                            "LISTENING"

                        isThinking ->
                            "THINKING"

                        isSpeaking ->
                            "SPEAKING"

                        else ->
                            "SYSTEM ONLINE"
                    },

                color =
                    when {

                        isListening ->
                            secondaryAccent

                        isThinking ->
                            accent

                        isSpeaking ->
                            secondaryAccent

                        else ->
                            secondaryText
                    },

                fontSize =
                    12.sp,

                letterSpacing =
                    3.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        56.dp
                    )
            )


            Box(
                modifier =
                    Modifier
                        .size(
                            220.dp
                        )
                        .scale(

                            if (
                                isListening
                            ) {

                                1.0f

                            } else {

                                idlePulse
                            }
                        ),

                contentAlignment =
                    Alignment.Center,
            ) {


                Canvas(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    val centerX =
                        size.width /
                                2f


                    val centerY =
                        size.height /
                                2f


                    val baseRadius =
                        (
                                size.minDimension /
                                        2f
                                ) -
                                6.dp.toPx()


                    val waveStrength =
                        if (
                            isListening
                        ) {

                            microphoneLevel *
                                    18.dp.toPx()

                        } else {

                            0f
                        }


                    val path =
                        Path()


                    val points =
                        180


                    for (
                    index in
                    0..points
                    ) {

                        val angle =
                            (
                                    index.toFloat() /
                                            points.toFloat()
                                    ) *
                                    (
                                            Math.PI *
                                                    2.0
                                            )


                        val wave =
                            sin(
                                angle *
                                        12.0
                            )
                                .toFloat()


                        val radius =
                            baseRadius +
                                    (
                                            wave *
                                                    waveStrength
                                            )


                        val x =
                            centerX +
                                    cos(
                                        angle
                                    )
                                        .toFloat() *
                                    radius


                        val y =
                            centerY +
                                    sin(
                                        angle
                                    )
                                        .toFloat() *
                                    radius


                        if (
                            index == 0
                        ) {

                            path.moveTo(
                                x,
                                y
                            )

                        } else {

                            path.lineTo(
                                x,
                                y
                            )
                        }
                    }


                    path.close()


                    drawPath(
                        path =
                            path,

                        color =
                            if (
                                isListening
                            ) {

                                secondaryAccent

                            } else {

                                accent
                            },

                        style =
                            Stroke(
                                width =
                                    2.dp.toPx(),

                                cap =
                                    StrokeCap.Round,
                            ),
                    )
                }


                Box(
                    modifier =
                        Modifier.size(
                            160.dp
                        ),

                    contentAlignment =
                        Alignment.Center,
                ) {


                    Canvas(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .rotate(
                                    middleRotation
                                )
                    ) {

                        val strokeWidth =
                            3.dp.toPx()


                        val arcSize =
                            Size(

                                width =
                                    size.width -
                                            strokeWidth,

                                height =
                                    size.height -
                                            strokeWidth,
                            )


                        val topLeft =
                            Offset(

                                x =
                                    strokeWidth /
                                            2,

                                y =
                                    strokeWidth /
                                            2,
                            )


                        drawArc(
                            color =
                                accent,

                            startAngle =
                                -90f,

                            sweepAngle =
                                70f,

                            useCenter =
                                false,

                            topLeft =
                                topLeft,

                            size =
                                arcSize,

                            style =
                                Stroke(
                                    width =
                                        strokeWidth,

                                    cap =
                                        StrokeCap.Round,
                                ),
                        )


                        drawArc(
                            color =
                                accent,

                            startAngle =
                                20f,

                            sweepAngle =
                                85f,

                            useCenter =
                                false,

                            topLeft =
                                topLeft,

                            size =
                                arcSize,

                            style =
                                Stroke(
                                    width =
                                        strokeWidth,

                                    cap =
                                        StrokeCap.Round,
                                ),
                        )


                        drawArc(
                            color =
                                secondaryAccent,

                            startAngle =
                                145f,

                            sweepAngle =
                                45f,

                            useCenter =
                                false,

                            topLeft =
                                topLeft,

                            size =
                                arcSize,

                            style =
                                Stroke(
                                    width =
                                        strokeWidth,

                                    cap =
                                        StrokeCap.Round,
                                ),
                        )


                        drawArc(
                            color =
                                accent,

                            startAngle =
                                220f,

                            sweepAngle =
                                95f,

                            useCenter =
                                false,

                            topLeft =
                                topLeft,

                            size =
                                arcSize,

                            style =
                                Stroke(
                                    width =
                                        strokeWidth,

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
                            "Cypher",

                        modifier =
                            Modifier.size(
                                105.dp
                            ),
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        38.dp
                    )
            )


            Text(
                text =
                    greeting,

                color =
                    Color.White,

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Medium,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Text(
                text =
                    when {

                        isListening ->
                            "I'm listening..."

                        isThinking ->
                            "Thinking..."

                        cypherReply
                            .isNotBlank() ->
                            cypherReply

                        recognizedText
                            .isNotBlank() ->
                            recognizedText

                        else ->
                            "Awaiting your command."
                    },

                color =
                    when {

                        isListening ->
                            secondaryAccent

                        isThinking ->
                            accent

                        isSpeaking ->
                            secondaryAccent

                        else ->
                            secondaryText
                    },

                fontSize =
                    14.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        42.dp
                    )
            )


            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color =
                                panel,

                            shape =
                                RoundedCornerShape(
                                    18.dp
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
                                    18.dp
                                ),
                        )
                        .padding(
                            20.dp
                        ),
            ) {

                Column {


                    Text(
                        text =
                            "SYSTEM STATUS",

                        color =
                            accent,

                        fontSize =
                            12.sp,

                        letterSpacing =
                            2.sp,
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                18.dp
                            )
                    )


                    StatusRow(
                        label =
                            "LOCATION",

                        value =
                            "Taree",

                        valueColor =
                            Color.White,
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )


                    StatusRow(
                        label =
                            "STATUS",

                        value =
                            if (
                                cypherReply ==
                                "I couldn't connect to CypherOS."
                            ) {

                                "OFFLINE"

                            } else {

                                "ONLINE"
                            },

                        valueColor =
                            if (
                                cypherReply ==
                                "I couldn't connect to CypherOS."
                            ) {

                                Color.Red

                            } else {

                                secondaryAccent
                            },
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )


            Text(
                text =
                    "TAP TO SPEAK",

                color =
                    secondaryText,

                fontSize =
                    11.sp,

                letterSpacing =
                    2.sp,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Box(
                modifier =
                    Modifier
                        .size(
                            76.dp
                        )
                        .clickable {

                            when {

                                isListening -> {

                                    speechRecognizer
                                        ?.cancel()
                                }


                                isSpeaking -> {

                                    stopSpeaking()
                                }


                                !isThinking -> {

                                    val permissionGranted =
                                        ContextCompat
                                            .checkSelfPermission(
                                                context,

                                                Manifest
                                                    .permission
                                                    .RECORD_AUDIO,
                                            ) ==
                                                PackageManager
                                                    .PERMISSION_GRANTED


                                    if (
                                        permissionGranted
                                    ) {

                                        startListening()

                                    } else {

                                        microphonePermissionLauncher
                                            .launch(
                                                Manifest
                                                    .permission
                                                    .RECORD_AUDIO
                                            )
                                    }
                                }
                            }
                        }
                        .background(
                            color =
                                accent.copy(
                                    alpha =
                                        0.12f
                                ),

                            shape =
                                CircleShape,
                        )
                        .border(
                            width =
                                if (
                                    isListening ||
                                    isSpeaking
                                ) {

                                    2.dp

                                } else {

                                    1.dp
                                },

                            color =
                                if (
                                    isListening ||
                                    isSpeaking
                                ) {

                                    secondaryAccent

                                } else {

                                    accent
                                },

                            shape =
                                CircleShape,
                        ),

                contentAlignment =
                    Alignment.Center,
            ) {


                Text(
                    text =
                        when {

                            isListening ->
                                "STOP"

                            isThinking ->
                                "..."

                            isSpeaking ->
                                "STOP"

                            else ->
                                "MIC"
                        },

                    color =
                        secondaryAccent,

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Bold,
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )
        }
    }
}


@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color,
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,
    ) {

        Text(
            text =
                label,

            color =
                Color(
                    0xFF607D8B
                ),

            fontSize =
                12.sp,
        )


        Text(
            text =
                value,

            color =
                valueColor,

            fontSize =
                13.sp,

            fontWeight =
                FontWeight.Medium,
        )
    }
}


@Preview(
    showBackground = true,
    backgroundColor = 0xFF070509,
)
@Composable
fun CypherHomeScreenPreview() {

    CypherTheme {

        CypherHomeScreen()
    }
}