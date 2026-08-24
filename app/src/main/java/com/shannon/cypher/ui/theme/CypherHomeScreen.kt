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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.shannon.cypher.calendar.CypherCalendarDateParser
import com.shannon.cypher.calendar.CypherCalendarEvent
import com.shannon.cypher.calendar.CypherCalendarManager
import com.shannon.cypher.memory.CypherMemoryRepository
import com.shannon.cypher.tasks.CypherTaskRepository
import com.shannon.cypher.identity.CypherNameNormalizer
import com.shannon.cypher.network.CypherApiClient
import com.shannon.cypher.navigation.CypherScreen
import com.shannon.cypher.ui.navigation.CypherMenuOverlay
import com.shannon.cypher.ui.tasks.CypherTaskScreen
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
    val reminderMinutes: Int? = null,
)


@Composable
fun CypherHomeScreen() {

    val background = Color(0xFF070509)
    val panel = Color(0xFF110D16)
    val accent = Color(0xFF8A2BE2)
    val secondaryAccent = Color(0xFF76FF03)
    val secondaryText = Color(0xFFA99AAF)

    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var microphoneLevel by remember { mutableStateOf(0f) }
    var recognizedText by remember { mutableStateOf("") }
    var cypherReply by remember { mutableStateOf("") }

    var currentScreen by
    remember {
        mutableStateOf(
            CypherScreen.HOME
        )
    }

    var isMenuOpen by
    remember {
        mutableStateOf(
            false
        )
    }

    var pendingCalendarTarget by remember { mutableStateOf("today") }
    var pendingCalendarCreateRequest by remember {
        mutableStateOf<CalendarCreateRequest?>(null)
    }
    var lastCalendarEventStartMillis by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteEvent by remember { mutableStateOf<CypherCalendarEvent?>(null) }

    val speechRecognizer = remember {
        if (isPreview) null else CypherSpeechRecognizer(context)
    }

    val remoteSpeaker = remember {
        if (isPreview) null else CypherRemoteSpeaker(context)
    }

    val fallbackSpeaker = remember {
        if (isPreview) null else CypherSpeaker(context)
    }

    val apiClient = remember { CypherApiClient() }

    val calendarManager = remember {
        if (isPreview) null else CypherCalendarManager(context)
    }


    val memoryRepository =
        remember {
            if (isPreview) {
                null
            } else {
                CypherMemoryRepository(
                    context
                )
            }
        }

    val taskRepository =
        remember {
            if (isPreview) {
                null
            } else {
                CypherTaskRepository(
                    context
                )
            }
        }


    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            remoteSpeaker?.destroy()
            fallbackSpeaker?.destroy()
        }
    }


    fun stopSpeaking() {
        remoteSpeaker?.stop()
        fallbackSpeaker?.stop()
        isSpeaking = false
    }


    fun speakReply(reply: String) {
        if (reply.isBlank()) return

        coroutineScope.launch {
            try {
                remoteSpeaker?.speak(
                    text = reply,
                    onStart = { isSpeaking = true },
                    onDone = { isSpeaking = false },
                )
            } catch (_: Exception) {
                fallbackSpeaker?.speak(
                    text = reply,
                    onStart = { isSpeaking = true },
                    onDone = { isSpeaking = false },
                )
            }
        }
    }


    fun reply(text: String) {
        cypherReply = text
        speakReply(text)
    }


    fun hasReadCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun hasWriteCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun weekdayNumber(weekday: String): Int? {
        return when (weekday.lowercase()) {
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            "sunday" -> Calendar.SUNDAY
            else -> null
        }
    }


    fun getRelativeCalendarDate(message: String): Calendar {
        val lower = message.lowercase()
        val result = Calendar.getInstance()

        if ("tomorrow" in lower) {
            result.add(Calendar.DAY_OF_YEAR, 1)
            return result
        }

        if ("today" in lower) {
            return result
        }

        val weekdays = listOf(
            "monday", "tuesday", "wednesday", "thursday",
            "friday", "saturday", "sunday",
        )

        val requested = weekdays.firstOrNull { it in lower }
        if (requested != null) {
            val requestedNumber = weekdayNumber(requested)!!
            val currentNumber = result.get(Calendar.DAY_OF_WEEK)
            var daysAhead = (requestedNumber - currentNumber + 7) % 7
            if (daysAhead == 0) daysAhead = 7
            result.add(Calendar.DAY_OF_YEAR, daysAhead)
        }

        return result
    }


    fun getCalendarDate(message: String): Calendar {
        return CypherCalendarDateParser.parseSpecificDate(message)
            ?: getRelativeCalendarDate(message)
    }


    fun getCalendarDayRange(message: String): Pair<Long, Long> {
        return CypherCalendarDateParser.dayRange(
            getCalendarDate(message)
        )
    }


    fun formatDate(date: Calendar): String {
        return SimpleDateFormat(
            "EEEE d MMMM",
            Locale.getDefault(),
        ).format(date.time)
    }


    fun formatTime(timeMillis: Long): String {
        return SimpleDateFormat(
            "h:mm a",
            Locale.getDefault(),
        ).format(timeMillis)
    }


    fun isNextAfterThatRequest(message: String): Boolean {
        val lower = message.lowercase().trim()
        val phrases = listOf(
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
        return phrases.any { it in lower }
    }


    fun isNextAppointmentRequest(message: String): Boolean {
        val lower = message.lowercase()
        val phrases = listOf(
            "next appointment",
            "next event",
            "next meeting",
            "next calendar event",
            "what's next on my calendar",
            "whats next on my calendar",
            "what is next on my calendar",
            "what have i got next",
            "what do i have next",
        )
        return phrases.any { it in lower }
    }


    fun hasCalendarDateWords(message: String): Boolean {
        val lower = message.lowercase()
        val relative = listOf(
            "today", "tomorrow", "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday", "sunday",
        ).any { it in lower }

        return relative || CypherCalendarDateParser.containsSpecificDate(message)
    }


    fun isCalendarCreateRequest(message: String): Boolean {
        val lower = message.lowercase()
        val creationWords = listOf(
            "add", "create", "put", "schedule", "book", "make", "set",
        )
        val hasCreationWord = creationWords.any {
            Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower)
        }
        val hasTime = Regex(
            "\\b\\d{1,2}(?::\\d{2})?\\s*(?:a\\.?m\\.?|p\\.?m\\.?)\\b",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(lower)

        return hasCreationWord && hasCalendarDateWords(message) && hasTime
    }


    fun isCalendarDeleteRequest(message: String): Boolean {
        val lower = message.lowercase()
        val deleteWords = listOf("delete", "remove", "cancel")
        return deleteWords.any {
            Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower)
        } && hasCalendarDateWords(message)
    }


    fun isCalendarEditRequest(message: String): Boolean {
        val lower = message.lowercase()
        val editWords = listOf("move", "change", "reschedule")
        return editWords.any {
            Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower)
        } && hasCalendarDateWords(message)
    }


    fun isStandaloneReminderRequest(message: String): Boolean {
        val lower = message.lowercase()
        return (
                ("remind me" in lower || "set a reminder" in lower || "add a reminder" in lower) &&
                        "before" in lower &&
                        hasCalendarDateWords(message) &&
                        !isCalendarCreateRequest(message)
                )
    }


    fun isCalendarReadRequest(
        message: String,
    ): Boolean {

        val lowerMessage =
            message
                .lowercase()
                .replace("?", "")
                .trim()

        if (isCalendarCreateRequest(message)) return false
        if (isCalendarDeleteRequest(message)) return false
        if (isCalendarEditRequest(message)) return false
        if (isStandaloneReminderRequest(message)) return false

        val specificDate =
            CypherCalendarDateParser
                .parseSpecificDate(
                    message
                )

        if (specificDate != null) {

            val specificDateReadPhrases =
                listOf(
                    "calendar",
                    "appointment",
                    "appointments",
                    "meeting",
                    "meetings",
                    "event",
                    "events",
                    "schedule",
                    "what's on",
                    "whats on",
                    "what is on",
                    "anything on",
                    "what do i have",
                    "what have i got",
                    "do i have",
                    "have i got",
                )

            if (
                specificDateReadPhrases.any { phrase ->
                    lowerMessage.contains(phrase)
                }
            ) {
                return true
            }
        }

        val calendarWords =
            listOf(
                "calendar",
                "appointment",
                "appointments",
                "meeting",
                "meetings",
                "event",
                "events",
                "schedule",
                "anything",
            )

        val readWords =
            listOf(
                "what",
                "what's",
                "whats",
                "what is",
                "what do i have",
                "what have i got",
                "have i got",
                "do i have",
                "anything",
                "what am i doing",
                "show me",
                "tell me",
            )

        val hasCalendarWord =
            calendarWords.any { word ->
                lowerMessage.contains(word)
            }

        val hasReadIntent =
            readWords.any { phrase ->
                lowerMessage.contains(phrase)
            }

        return (
                hasCalendarDateWords(message) &&
                        hasReadIntent &&
                        (
                                hasCalendarWord ||
                                        specificDate != null
                                )
                )
    }


    fun isDeleteConfirmation(message: String): Boolean {
        val lower = message.lowercase().trim()
        return lower in setOf(
            "yes", "yes please", "confirm", "confirmed", "do it",
            "delete it", "remove it", "yep", "yeah",
        )
    }


    fun isDeleteCancellation(message: String): Boolean {
        val lower = message.lowercase().trim()
        return lower in setOf(
            "no", "no thanks", "cancel", "don't", "dont", "never mind",
            "nevermind", "leave it",
        )
    }


    fun normaliseTimeText(message: String): String {
        return message
            .lowercase()
            .replace("a.m.", "am")
            .replace("p.m.", "pm")
            .replace("a.m", "am")
            .replace("p.m", "pm")
            .replace("a m", "am")
            .replace("p m", "pm")
    }


    fun parseExplicitTimes(message: String): List<Triple<Int, Int, String>> {
        val normalised = normaliseTimeText(message)
        val regex = Regex(
            "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b",
            RegexOption.IGNORE_CASE,
        )

        return regex.findAll(normalised).mapNotNull { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val minute = match.groupValues[2].ifBlank { "0" }.toIntOrNull()
                ?: return@mapNotNull null
            val period = match.groupValues[3].lowercase()
            if (hour !in 1..12 || minute !in 0..59) return@mapNotNull null
            Triple(hour, minute, period)
        }.toList()
    }


    fun convertTo24Hour(rawHour: Int, period: String): Int {
        return when {
            period == "am" && rawHour == 12 -> 0
            period == "pm" && rawHour != 12 -> rawHour + 12
            else -> rawHour
        }
    }


    fun calendarWithTime(
        baseDate: Calendar,
        rawHour: Int,
        minute: Int,
        period: String,
    ): Calendar {
        return (baseDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, convertTo24Hour(rawHour, period))
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }


    fun parseReminderMinutes(message: String): Int? {
        val lower = message.lowercase()

        if (Regex("\\bhalf\\s+an\\s+hour\\s+before\\b").containsMatchIn(lower)) {
            return 30
        }

        if (Regex("\\b(?:an|one)\\s+hour\\s+before\\b").containsMatchIn(lower)) {
            return 60
        }

        if (Regex("\\b(?:a|one)\\s+day\\s+before\\b").containsMatchIn(lower)) {
            return 1440
        }

        Regex("\\b(\\d+)\\s+minutes?\\s+before\\b").find(lower)?.let {
            return it.groupValues[1].toIntOrNull()
        }

        Regex("\\b(\\d+)\\s+hours?\\s+before\\b").find(lower)?.let {
            return it.groupValues[1].toIntOrNull()?.times(60)
        }

        Regex("\\b(\\d+)\\s+days?\\s+before\\b").find(lower)?.let {
            return it.groupValues[1].toIntOrNull()?.times(1440)
        }

        return null
    }


    fun reminderDescription(minutes: Int): String {
        return when {
            minutes == 30 -> "30 minutes before"
            minutes == 60 -> "1 hour before"
            minutes == 1440 -> "1 day before"
            minutes % 1440 == 0 -> "${minutes / 1440} days before"
            minutes % 60 == 0 -> "${minutes / 60} hours before"
            else -> "$minutes minutes before"
        }
    }


    fun calculateDurationEnd(
        message: String,
        start: Calendar,
    ): Calendar {
        val lower = normaliseTimeText(message)
        val end = start.clone() as Calendar
        end.add(Calendar.HOUR_OF_DAY, 1)

        if (Regex("\\bfor\\s+half\\s+an\\s+hour\\b").containsMatchIn(lower)) {
            return (start.clone() as Calendar).apply {
                add(Calendar.MINUTE, 30)
            }
        }

        if (Regex("\\bfor\\s+(?:an|one)\\s+hour\\b").containsMatchIn(lower)) {
            return (start.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }

        Regex("\\bfor\\s+(\\d+)\\s+minutes?\\b").find(lower)?.let { match ->
            val minutes = match.groupValues[1].toIntOrNull()
            if (minutes != null && minutes > 0) {
                return (start.clone() as Calendar).apply {
                    add(Calendar.MINUTE, minutes)
                }
            }
        }

        Regex("\\bfor\\s+(\\d+)\\s+hours?\\b").find(lower)?.let { match ->
            val hours = match.groupValues[1].toIntOrNull()
            if (hours != null && hours > 0) {
                return (start.clone() as Calendar).apply {
                    add(Calendar.HOUR_OF_DAY, hours)
                }
            }
        }

        val explicitTimes = parseExplicitTimes(message)
        if (("until" in lower || "till" in lower) && explicitTimes.size >= 2) {
            val last = explicitTimes.last()
            val finish = calendarWithTime(
                start,
                last.first,
                last.second,
                last.third,
            )
            if (finish.timeInMillis <= start.timeInMillis) {
                finish.add(Calendar.DAY_OF_YEAR, 1)
            }
            return finish
        }

        return end
    }


    fun stripDateAndTimeLanguage(
        message: String,
    ): String {

        var text =
            message.lowercase()


        /*
         * Numeric day followed by month.
         *
         * Examples:
         * 27 August
         * 27th August
         * the 27th of August
         * 3rd of September 2026
         */
        text =
            text.replace(
                Regex(
                    "\\b(?:the\\s+)?\\d{1,2}" +
                            "(?:st|nd|rd|th)?" +
                            "(?:\\s+of)?\\s+" +
                            "(?:january|february|march|april|may|june|july|august|" +
                            "september|october|november|december)" +
                            "(?:\\s+\\d{4})?\\b",
                    RegexOption.IGNORE_CASE,
                ),
                " ",
            )


        /*
         * Month followed by numeric day.
         *
         * Examples:
         * August 27
         * August 27th
         * August the 27th
         */
        text =
            text.replace(
                Regex(
                    "\\b(?:january|february|march|april|may|june|july|august|" +
                            "september|october|november|december)\\s+" +
                            "(?:the\\s+)?\\d{1,2}" +
                            "(?:st|nd|rd|th)?" +
                            "(?:\\s+\\d{4})?\\b",
                    RegexOption.IGNORE_CASE,
                ),
                " ",
            )


        /*
         * Spoken ordinal followed by month.
         *
         * Examples:
         * twenty seventh of August
         * the third of September
         */
        text =
            text.replace(
                Regex(
                    "\\b(?:the\\s+)?" +
                            "(?:first|second|third|fourth|fifth|sixth|seventh|" +
                            "eighth|ninth|tenth|eleventh|twelfth|thirteenth|" +
                            "fourteenth|fifteenth|sixteenth|seventeenth|" +
                            "eighteenth|nineteenth|twentieth|" +
                            "twenty\\s+first|twenty\\s+second|twenty\\s+third|" +
                            "twenty\\s+fourth|twenty\\s+fifth|twenty\\s+sixth|" +
                            "twenty\\s+seventh|twenty\\s+eighth|twenty\\s+ninth|" +
                            "thirtieth|thirty\\s+first)" +
                            "(?:\\s+of)?\\s+" +
                            "(?:january|february|march|april|may|june|july|august|" +
                            "september|october|november|december)" +
                            "(?:\\s+\\d{4})?\\b",
                    RegexOption.IGNORE_CASE,
                ),
                " ",
            )


        /*
         * Relative dates / weekdays.
         */
        text =
            text.replace(
                Regex(
                    "\\b(today|tomorrow|monday|tuesday|wednesday|thursday|" +
                            "friday|saturday|sunday)\\b",
                    RegexOption.IGNORE_CASE,
                ),
                " ",
            )


        /*
         * Times.
         */
        text =
            text.replace(
                Regex(
                    "\\b(?:at|from|to|until|till)?\\s*" +
                            "\\d{1,2}(?::\\d{2})?\\s*" +
                            "(?:a\\.?m\\.?|p\\.?m\\.?)\\b",
                    RegexOption.IGNORE_CASE,
                ),
                " ",
            )


        /*
         * Reminder wording.
         */
        text =
            text.replace(
                Regex(
                    "\\b(?:and\\s+)?" +
                            "remind\\s+me\\s+.*?\\s+before\\b",
                    RegexOption.IGNORE_CASE,
                ),
                " ",
            )


        /*
         * Event duration.
         */
        text =
            text.replace(
                Regex(
                    "\\bfor\\s+" +
                            "(?:half\\s+an\\s+hour|" +
                            "an\\s+hour|" +
                            "one\\s+hour|" +
                            "\\d+\\s+minutes?|" +
                            "\\d+\\s+hours?)\\b",
                    RegexOption.IGNORE_CASE,
                ),
                " ",
            )


        return text
            .replace(
                Regex("\\s+"),
                " ",
            )
            .trim()
    }


    fun extractCreateTitle(message: String): String {
        var title = stripDateAndTimeLanguage(message)

        title = title.replace(Regex("(?i)\\bcypher\\b"), " ")
        title = title.replace(
            Regex("(?i)\\b(add|create|put|schedule|book|make|set)\\b"),
            " ",
        )
        title = title.replace(
            Regex("(?i)\\b(to|on|in)?\\s*(my\\s+)?calendar\\b"),
            " ",
        )
        title = title.replace(
            Regex("(?i)\\b(?:and\\s+)?remind\\s+me.*$"),
            " ",
        )

        title = title.replace(Regex("\\s+"), " ").trim(' ', ',', '.')

        return if (title.isBlank()) {
            "Calendar event"
        } else {
            title.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }


    fun extractLookupTitle(message: String): String {
        var title = stripDateAndTimeLanguage(message)

        title = title.replace(Regex("(?i)\\bcypher\\b"), " ")
        title = title.replace(
            Regex(
                "(?i)\\b(delete|remove|cancel|move|change|reschedule|" +
                        "remind|reminder|set|add|please|appointment|event|meeting)\\b"
            ),
            " ",
        )
        title = title.replace(
            Regex("(?i)\\b(me|my|the|a|an|calendar|before|for|on|at|to|from|minute|minutes|hour|hours|day|days)\\b"),
            " ",
        )
        title = title.replace(Regex("\\b\\d+\\b"), " ")
        title = title.replace(Regex("\\s+"), " ").trim(' ', ',', '.')

        return title
    }


    fun parseCalendarCreateRequest(message: String): CalendarCreateRequest? {
        val times = parseExplicitTimes(message)
        val firstTime = times.firstOrNull() ?: return null
        val date = getCalendarDate(message)
        val start = calendarWithTime(
            date,
            firstTime.first,
            firstTime.second,
            firstTime.third,
        )
        val end = calculateDurationEnd(message, start)
        val reminderMinutes = parseReminderMinutes(message)

        return CalendarCreateRequest(
            title = extractCreateTitle(message),
            startTimeMillis = start.timeInMillis,
            endTimeMillis = end.timeInMillis,
            confirmationDay = formatDate(start),
            confirmationTime = formatTime(start.timeInMillis),
            reminderMinutes = reminderMinutes,
        )
    }


    fun getEventsForMessage(message: String): List<CypherCalendarEvent> {
        val manager = calendarManager ?: return emptyList()
        val range = getCalendarDayRange(message)
        return manager.getEventsBetween(
            startMillis = range.first,
            endMillis = range.second,
        )
    }


    fun formatCalendarEvents(
        events: List<CypherCalendarEvent>,
        message: String,
    ): String {
        val date = getCalendarDate(message)
        val dateDescription = formatDate(date)

        if (events.isEmpty()) {
            return "You have nothing on your calendar for $dateDescription."
        }

        val eventDescriptions = events.map { event ->
            if (event.allDay) {
                event.title
            } else {
                "${event.title} at ${formatTime(event.startTimeMillis)}"
            }
        }

        val count = if (events.size == 1) "one event" else "${events.size} events"

        return "You have $count on $dateDescription. " +
                eventDescriptions.joinToString(", ") + "."
    }


    fun readCalendar(message: String) {
        val events = getEventsForMessage(message)
        reply(formatCalendarEvents(events, message))
    }


    fun getNextCalendarEvent(afterMillis: Long): CypherCalendarEvent? {
        val manager = calendarManager ?: return null
        val searchEnd = Calendar.getInstance().apply {
            timeInMillis = afterMillis
            add(Calendar.YEAR, 1)
        }.timeInMillis

        return manager.getEventsBetween(
            startMillis = afterMillis + 1,
            endMillis = searchEnd,
        ).firstOrNull { it.startTimeMillis > afterMillis }
    }


    fun speakUpcomingEvent(
        event: CypherCalendarEvent?,
        isFollowUp: Boolean,
    ) {
        if (event == null) {
            reply(
                if (isFollowUp) {
                    "You have no later appointments on your calendar."
                } else {
                    "You have no upcoming appointments."
                }
            )
            return
        }

        lastCalendarEventStartMillis = event.startTimeMillis
        val date = SimpleDateFormat(
            "EEEE d MMMM",
            Locale.getDefault(),
        ).format(event.startTimeMillis)

        val prefix = if (isFollowUp) "After that, your next event is" else "Your next event is"

        val text = if (event.allDay) {
            "$prefix ${event.title} on $date."
        } else {
            "$prefix ${event.title} on $date at ${formatTime(event.startTimeMillis)}."
        }

        reply(text)
    }


    fun readNextAppointment() {
        speakUpcomingEvent(
            getNextCalendarEvent(System.currentTimeMillis()),
            isFollowUp = false,
        )
    }


    fun readAppointmentAfterThat() {
        val previous = lastCalendarEventStartMillis
        if (previous == null) {
            reply(
                "I don't have a previous appointment to continue from. " +
                        "Ask me for your next appointment first."
            )
            return
        }

        speakUpcomingEvent(
            getNextCalendarEvent(previous),
            isFollowUp = true,
        )
    }


    fun createCalendarEvent(request: CalendarCreateRequest) {
        val manager = calendarManager
        if (manager == null) {
            reply("I couldn't access your calendar.")
            return
        }

        val eventId = manager.createEvent(
            title = request.title,
            startTimeMillis = request.startTimeMillis,
            endTimeMillis = request.endTimeMillis,
            reminderMinutes = request.reminderMinutes,
        )

        if (eventId == null) {
            reply(
                "I couldn't add that event. I may not have access to a writable calendar."
            )
            return
        }

        val reminderText = request.reminderMinutes?.let {
            " I've also set a reminder ${reminderDescription(it)}."
        } ?: ""

        reply(
            "Done. I've added ${request.title} to your calendar for " +
                    "${request.confirmationDay} at ${request.confirmationTime}." +
                    reminderText
        )
    }


    fun findMatchingEvents(message: String): List<CypherCalendarEvent> {
        val manager = calendarManager ?: return emptyList()
        val range = getCalendarDayRange(message)
        val title = extractLookupTitle(message)

        if (title.isBlank()) {
            return manager.getEventsBetween(range.first, range.second)
        }

        return manager.findEvents(
            titleWords = title,
            startMillis = range.first,
            endMillis = range.second,
        )
    }


    fun describeMatches(
        matches: List<CypherCalendarEvent>,
    ): String {
        return matches.take(4).joinToString(", ") { event ->
            if (event.allDay) event.title
            else "${event.title} at ${formatTime(event.startTimeMillis)}"
        }
    }


    fun handleDeleteRequest(message: String) {
        val matches = findMatchingEvents(message)

        when {
            matches.isEmpty() -> {
                reply("I couldn't find a matching calendar event on that date.")
            }

            matches.size > 1 -> {
                reply(
                    "I found more than one matching event: ${describeMatches(matches)}. " +
                            "Please tell me which one you want to delete."
                )
            }

            else -> {
                val event = matches.first()
                pendingDeleteEvent = event
                val date = SimpleDateFormat(
                    "EEEE d MMMM",
                    Locale.getDefault(),
                ).format(event.startTimeMillis)

                reply(
                    "I found ${event.title} on $date at ${formatTime(event.startTimeMillis)}. " +
                            "Do you want me to delete it?"
                )
            }
        }
    }


    fun confirmDelete() {
        val event = pendingDeleteEvent
        if (event == null) {
            reply("There isn't a calendar deletion waiting for confirmation.")
            return
        }

        val deleted = calendarManager?.deleteEvent(event.id) == true
        pendingDeleteEvent = null

        if (deleted) {
            reply("Done. I've deleted ${event.title} from your calendar.")
        } else {
            reply("I couldn't delete that calendar event.")
        }
    }


    fun cancelDelete() {
        pendingDeleteEvent = null
        reply("Okay. I won't delete it.")
    }


    fun handleEditRequest(message: String) {
        val matches = findMatchingEvents(message)

        if (matches.isEmpty()) {
            reply("I couldn't find a matching calendar event on that date.")
            return
        }

        if (matches.size > 1) {
            reply(
                "I found more than one matching event: ${describeMatches(matches)}. " +
                        "Please be more specific."
            )
            return
        }

        val newTime = parseExplicitTimes(message).lastOrNull()
        if (newTime == null) {
            reply("I found the event, but I need the new time you want to move it to.")
            return
        }

        val event = matches.first()
        val newDate = getCalendarDate(message)
        val newStart = calendarWithTime(
            newDate,
            newTime.first,
            newTime.second,
            newTime.third,
        )

        val oldDuration = (event.endTimeMillis - event.startTimeMillis).coerceAtLeast(60_000L)
        val newEnd = newStart.timeInMillis + oldDuration

        val updated = calendarManager?.updateEventTime(
            eventId = event.id,
            startTimeMillis = newStart.timeInMillis,
            endTimeMillis = newEnd,
        ) == true

        if (updated) {
            reply(
                "Done. I've moved ${event.title} to ${formatDate(newStart)} " +
                        "at ${formatTime(newStart.timeInMillis)}."
            )
        } else {
            reply("I couldn't update that calendar event.")
        }
    }


    fun handleStandaloneReminderRequest(message: String) {
        val reminderMinutes = parseReminderMinutes(message)
        if (reminderMinutes == null) {
            reply(
                "I found the reminder request, but I need how long before the event, " +
                        "for example 30 minutes before."
            )
            return
        }

        val matches = findMatchingEvents(message)

        if (matches.isEmpty()) {
            reply("I couldn't find a matching calendar event on that date.")
            return
        }

        if (matches.size > 1) {
            reply(
                "I found more than one matching event: ${describeMatches(matches)}. " +
                        "Please tell me which one you want the reminder for."
            )
            return
        }

        val event = matches.first()
        val set = calendarManager?.replaceReminder(
            eventId = event.id,
            minutesBefore = reminderMinutes,
        ) == true

        if (set) {
            reply(
                "Done. I've set a reminder ${reminderDescription(reminderMinutes)} " +
                        "for ${event.title}."
            )
        } else {
            reply("I couldn't set that calendar reminder.")
        }
    }


    val calendarReadPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                when (pendingCalendarTarget) {
                    "next" -> readNextAppointment()
                    "next_after" -> readAppointmentAfterThat()
                    else -> readCalendar(pendingCalendarTarget)
                }
            } else {
                reply("Calendar permission was not granted.")
            }
        }


    val calendarWritePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val readGranted =
                permissions[Manifest.permission.READ_CALENDAR] == true ||
                        hasReadCalendarPermission()

            val writeGranted =
                permissions[Manifest.permission.WRITE_CALENDAR] == true ||
                        hasWriteCalendarPermission()

            if (readGranted && writeGranted) {
                pendingCalendarCreateRequest?.let { createCalendarEvent(it) }
            } else {
                reply("Calendar write permission was not granted.")
            }

            pendingCalendarCreateRequest = null
        }


    fun requestCalendarRead(message: String) {
        pendingCalendarTarget = message

        if (hasReadCalendarPermission()) {
            readCalendar(message)
        } else {
            calendarReadPermissionLauncher.launch(
                Manifest.permission.READ_CALENDAR
            )
        }
    }


    fun requestNextAppointment() {
        pendingCalendarTarget = "next"

        if (hasReadCalendarPermission()) {
            readNextAppointment()
        } else {
            calendarReadPermissionLauncher.launch(
                Manifest.permission.READ_CALENDAR
            )
        }
    }


    fun requestAppointmentAfterThat() {
        pendingCalendarTarget = "next_after"

        if (hasReadCalendarPermission()) {
            readAppointmentAfterThat()
        } else {
            calendarReadPermissionLauncher.launch(
                Manifest.permission.READ_CALENDAR
            )
        }
    }


    fun requestCalendarCreate(message: String) {
        val request = parseCalendarCreateRequest(message)

        if (request == null) {
            reply(
                "I can add that, but please include a date or day and time, " +
                        "for example, add dentist appointment on 27 August at 2 PM."
            )
            return
        }

        if (hasReadCalendarPermission() && hasWriteCalendarPermission()) {
            createCalendarEvent(request)
        } else {
            pendingCalendarCreateRequest = request
            calendarWritePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                )
            )
        }
    }


    fun runCalendarWriteAction(action: () -> Unit) {
        if (hasReadCalendarPermission() && hasWriteCalendarPermission()) {
            action()
        } else {
            reply(
                "I need calendar read and write permission for that. " +
                        "Please allow calendar access and try the command again."
            )
        }
    }



    fun isMemoryRememberRequest(
        message: String,
    ): Boolean {

        val lower =
            message
                .lowercase()
                .trim()

        return (
                lower.startsWith("remember that ") ||
                        lower.startsWith("remember my ") ||
                        lower.startsWith("remember i ") ||
                        lower.startsWith("remember i'm ") ||
                        lower.startsWith("remember im ") ||
                        lower.startsWith("actually, my ") ||
                        lower.startsWith("actually my ") ||
                        lower.startsWith("my ") ||
                        lower.startsWith("change my ") ||
                        lower.startsWith("update my ")
                )
    }


    fun isMemoryRecallRequest(
        message: String,
    ): Boolean {

        val lower =
            message
                .lowercase()
                .trim()

        return (
                lower.startsWith(
                    "what's my "
                ) ||
                        lower.startsWith(
                            "whats my "
                        ) ||
                        lower.startsWith(
                            "what is my "
                        ) ||
                        lower.startsWith(
                            "do you remember my "
                        ) ||
                        lower.startsWith(
                            "what did i tell you about "
                        )
                )
    }


    fun isMemoryListRequest(
        message: String,
    ): Boolean {

        val lower =
            message
                .lowercase()
                .trim()

        return (
                "what do you remember about me" in lower ||
                        "what do you remember" in lower ||
                        "what have you remembered" in lower
                )
    }


    fun isMemoryForgetRequest(
        message: String,
    ): Boolean {

        val lower =
            message
                .lowercase()
                .trim()

        return (
                lower.startsWith(
                    "forget my "
                ) ||
                        lower.startsWith(
                            "forget that "
                        )
                )
    }


    fun parseMemoryRememberRequest(
        message: String,
    ): Pair<String, String>? {

        var cleaned =
            message
                .trim()
                .trimEnd(
                    '.',
                    '!',
                    '?',
                )


        cleaned =
            cleaned.replace(
                Regex(
                    "(?i)^actually,?\\s+"
                ),
                ""
            )


        cleaned =
            cleaned.replace(
                Regex(
                    "(?i)^remember(?:\\s+that)?\\s+"
                ),
                ""
            )


        cleaned =
            cleaned.replace(
                Regex(
                    "(?i)^(change|update)\\s+my\\s+"
                ),
                "my "
            )


        val patterns =
            listOf(
                Regex(
                    "(?i)^my\\s+(.+?)\\s+is\\s+(.+)$"
                ),

                Regex(
                    "(?i)^my\\s+(.+?)\\s+are\\s+(.+)$"
                ),

                Regex(
                    "(?i)^my\\s+(.+?)\\s+to\\s+(.+)$"
                ),

                Regex(
                    "(?i)^i(?:'m|\\s+am)\\s+(.+)$"
                ),
            )


        for (
        pattern in patterns
        ) {

            val match =
                pattern.find(
                    cleaned
                )
                    ?: continue


            if (
                match.groupValues.size == 2
            ) {

                val value =
                    match
                        .groupValues[1]
                        .trim()
                        .removeSuffix(
                            " now"
                        )
                        .trim()


                if (
                    value.isBlank()
                ) {

                    return null
                }


                return Pair(
                    "i am",
                    value,
                )
            }


            val key =
                match
                    .groupValues[1]
                    .trim()


            var value =
                match
                    .groupValues[2]
                    .trim()


            value =
                value
                    .removeSuffix(
                        " now"
                    )
                    .trim()


            if (
                key.isBlank() ||
                value.isBlank()
            ) {

                return null
            }


            return Pair(
                key,
                value,
            )
        }


        return null
    }


    fun parseMemoryRecallKey(
        message: String,
    ): String? {

        val cleaned =
            message
                .lowercase()
                .trim()
                .trimEnd(
                    '.',
                    '!',
                    '?',
                )


        val patterns =
            listOf(
                Regex(
                    "^what's my (.+)$"
                ),

                Regex(
                    "^whats my (.+)$"
                ),

                Regex(
                    "^what is my (.+)$"
                ),

                Regex(
                    "^do you remember my (.+)$"
                ),

                Regex(
                    "^what did i tell you about (.+)$"
                ),
            )


        for (
        pattern in patterns
        ) {

            val match =
                pattern.find(
                    cleaned
                )


            if (
                match != null
            ) {

                return match
                    .groupValues[1]
                    .trim()
            }
        }


        return null
    }


    fun parseMemoryForgetKey(
        message: String,
    ): String? {

        val cleaned =
            message
                .lowercase()
                .trim()
                .trimEnd(
                    '.',
                    '!',
                    '?',
                )


        val patterns =
            listOf(
                Regex(
                    "^forget my (.+)$"
                ),

                Regex(
                    "^forget that (.+)$"
                ),
            )


        for (
        pattern in patterns
        ) {

            val match =
                pattern.find(
                    cleaned
                )


            if (
                match != null
            ) {

                return match
                    .groupValues[1]
                    .trim()
            }
        }


        return null
    }


    fun handleMemoryRemember(
        message: String,
    ) {

        val repository =
            memoryRepository
                ?: return


        val request =
            parseMemoryRememberRequest(
                message
            )


        if (
            request == null
        ) {

            reply(
                "I understood that as a memory request, " +
                        "but I couldn't work out what you wanted me to remember."
            )

            return
        }


        coroutineScope.launch {

            repository.remember(
                key =
                    request.first,

                value =
                    request.second,
            )


            reply(
                "Got it. I'll remember that your " +
                        "${request.first} is ${request.second}."
            )
        }
    }


    fun handleMemoryRecall(
        message: String,
    ) {

        val repository =
            memoryRepository
                ?: return


        val key =
            parseMemoryRecallKey(
                message
            )


        if (
            key == null
        ) {

            reply(
                "I couldn't work out which memory you wanted."
            )

            return
        }


        coroutineScope.launch {

            val value =
                repository.recall(
                    key
                )


            if (
                value == null
            ) {

                reply(
                    "I don't have anything saved for your $key."
                )

            } else {

                reply(
                    "Your $key is $value."
                )
            }
        }
    }


    fun handleMemoryList() {

        val repository =
            memoryRepository
                ?: return


        coroutineScope.launch {

            val memories =
                repository.getAll()


            if (
                memories.isEmpty()
            ) {

                reply(
                    "I don't have any saved memories yet."
                )

                return@launch
            }


            val description =
                memories
                    .entries
                    .joinToString(
                        "; "
                    ) { entry ->

                        "your ${entry.key} is ${entry.value}"
                    }


            reply(
                "I remember that $description."
            )
        }
    }


    fun handleMemoryForget(
        message: String,
    ) {

        val repository =
            memoryRepository
                ?: return


        val key =
            parseMemoryForgetKey(
                message
            )


        if (
            key == null
        ) {

            reply(
                "I couldn't work out which memory you wanted me to forget."
            )

            return
        }


        coroutineScope.launch {

            val removed =
                repository.forget(
                    key
                )


            if (
                removed
            ) {

                reply(
                    "Done. I've forgotten your $key."
                )

            } else {

                reply(
                    "I don't have a saved memory for your $key."
                )
            }
        }
    }


    fun isTaskAddRequest(message: String): Boolean {
        val lower = message.lowercase().trim()
        val hasTarget = listOf("to-do list", "to do list", "todo list", "task list", "my tasks").any { it in lower }
        val hasIntent = listOf("add ", "create ", "put ").any { lower.startsWith(it) }
        return hasTarget && hasIntent
    }


    fun isTaskScreenRequest(
        message: String,
    ): Boolean {

        val lower =
            message
                .lowercase()
                .trim()

        return listOf(
            "show me my to-do list",
            "show me my to do list",
            "show me my todo list",
            "show my to-do list",
            "show my to do list",
            "show my todo list",
            "open my to-do list",
            "open my to do list",
            "open my todo list",
            "show me my tasks",
            "show my tasks",
            "open my tasks",
            "open task list",
            "open my task list",
        ).any { phrase ->
            phrase in lower
        }
    }


    fun isTaskListRequest(message: String): Boolean {
        val lower = message.lowercase().trim()
        return listOf(
            "what's on my to-do list",
            "whats on my to-do list",
            "what is on my to-do list",
            "what's on my to do list",
            "whats on my to do list",
            "what is on my to do list",
            "what's on my todo list",
            "whats on my todo list",
            "what is on my todo list",
            "what tasks do i have",
            "what tasks have i got",
            "what are my tasks",
            "show my tasks",
            "show me my tasks",
            "show my to-do list",
            "show my to do list",
            "show my todo list",
            "read my tasks",
            "read my to-do list",
            "read my to do list",
            "read my todo list",
        ).any { it in lower }
    }


    fun isTaskCompleteRequest(message: String): Boolean {
        val lower = message.lowercase().trim()
        return (
                lower.startsWith("mark ") &&
                        (
                                " as done" in lower ||
                                        " as complete" in lower ||
                                        " as completed" in lower
                                )
                ) ||
                lower.startsWith("complete ") ||
                lower.startsWith("finish ")
    }


    fun isTaskDeleteRequest(message: String): Boolean {
        val lower = message.lowercase().trim()
        val deleteIntent = lower.startsWith("remove ") || lower.startsWith("delete ")
        val taskTarget = listOf(
            "from my to-do list",
            "from my to do list",
            "from my todo list",
            "from my task list",
            "from my tasks",
        ).any { it in lower }
        return deleteIntent && taskTarget
    }


    fun extractTaskAddTitle(message: String): String {
        var title = message.trim().trimEnd('.', '!', '?')
        title = title.replace(Regex("(?i)^(add|create|put)\\s+"), "")
        title = title.replace(
            Regex("(?i)\\s+(?:to|on|in)\\s+my\\s+(?:to-do|to\\s+do|todo|task)\\s+list$"),
            ""
        )
        title = title.replace(
            Regex("(?i)\\s+(?:to|on|in)\\s+my\\s+tasks$"),
            ""
        )
        return title.replace(Regex("\\s+"), " ").trim()
    }


    fun extractTaskCompleteTitle(message: String): String {
        var title = message.trim().trimEnd('.', '!', '?')
        title = title.replace(Regex("(?i)^mark\\s+"), "")
        title = title.replace(
            Regex("(?i)\\s+as\\s+(?:done|complete|completed)$"),
            ""
        )
        title = title.replace(Regex("(?i)^(complete|finish)\\s+"), "")
        title = title.replace(Regex("(?i)^(the\\s+)?task\\s+"), "")
        return title.replace(Regex("\\s+"), " ").trim()
    }


    fun extractTaskDeleteTitle(message: String): String {
        var title = message.trim().trimEnd('.', '!', '?')
        title = title.replace(Regex("(?i)^(remove|delete)\\s+"), "")
        title = title.replace(
            Regex("(?i)\\s+from\\s+my\\s+(?:to-do|to\\s+do|todo|task)\\s+list$"),
            ""
        )
        title = title.replace(
            Regex("(?i)\\s+from\\s+my\\s+tasks$"),
            ""
        )
        title = title.replace(Regex("(?i)^(the\\s+)?task\\s+"), "")
        return title.replace(Regex("\\s+"), " ").trim()
    }


    fun handleTaskAdd(message: String) {
        val repository = taskRepository ?: return
        val title = extractTaskAddTitle(message)

        if (title.isBlank()) {
            reply("I couldn't work out what you wanted me to add to your to-do list.")
            return
        }

        coroutineScope.launch {
            val taskId =
                repository.addTask(
                    title
                )

            val savedTask =
                if (
                    taskId > 0
                ) {
                    repository.getTaskById(
                        taskId
                    )
                } else {
                    null
                }

            if (
                savedTask != null
            ) {
                reply(
                    "Saved locally. I've added ${savedTask.title} to your to-do list."
                )
            } else {
                reply(
                    "I couldn't save that task locally."
                )
            }
        }
    }


    fun handleTaskList() {
        val repository = taskRepository ?: return

        coroutineScope.launch {
            val tasks = repository.getOpenTasks()

            if (tasks.isEmpty()) {
                reply("Your to-do list is empty.")
                return@launch
            }

            val description =
                tasks
                    .take(10)
                    .mapIndexed { index, task ->
                        "${index + 1}. ${task.title}"
                    }
                    .joinToString(". ")

            val extra =
                if (tasks.size > 10) {
                    " You also have ${tasks.size - 10} more tasks."
                } else {
                    ""
                }

            reply(
                "You have ${tasks.size} open ${if (tasks.size == 1) "task" else "tasks"}. " +
                        "$description.$extra"
            )
        }
    }


    fun handleTaskComplete(message: String) {
        val repository = taskRepository ?: return
        val title = extractTaskCompleteTitle(message)

        if (title.isBlank()) {
            reply("I couldn't work out which task you wanted to complete.")
            return
        }

        coroutineScope.launch {
            val task = repository.completeTask(title)

            if (task == null) {
                reply("I couldn't find an open task matching $title.")
            } else {
                reply("Done. I've marked ${task.title} as complete.")
            }
        }
    }


    fun handleTaskDelete(message: String) {
        val repository = taskRepository ?: return
        val title = extractTaskDeleteTitle(message)

        if (title.isBlank()) {
            reply("I couldn't work out which task you wanted to remove.")
            return
        }

        coroutineScope.launch {
            val task = repository.deleteTask(title)

            if (task == null) {
                reply("I couldn't find an open task matching $title.")
            } else {
                reply("Done. I've removed ${task.title} from your to-do list.")
            }
        }
    }


    fun sendMessageToCypherOS(message: String) {
        if (message.isBlank()) return

        isThinking = true
        isSpeaking = false
        cypherReply = ""

        coroutineScope.launch {
            try {
                val answer = withContext(Dispatchers.IO) {
                    apiClient.sendMessage(message)
                }

                cypherReply = answer
                isThinking = false
                speakReply(answer)

            } catch (_: Exception) {
                isThinking = false
                cypherReply = "I couldn't connect to CypherOS."
            }
        }
    }


    fun startListening() {
        recognizedText = ""
        cypherReply = ""
        stopSpeaking()

        speechRecognizer?.start(
            onListeningChanged = { listening ->
                isListening = listening
                if (!listening) microphoneLevel = 0f
            },
            onLevelChanged = { level ->
                microphoneLevel = level
            },
            onTextRecognized = { text ->
                val normalizedText =
                    CypherNameNormalizer.normalize(text)

                recognizedText = normalizedText

                when {
                    pendingDeleteEvent != null && isDeleteConfirmation(normalizedText) -> {
                        runCalendarWriteAction { confirmDelete() }
                    }

                    pendingDeleteEvent != null && isDeleteCancellation(normalizedText) -> {
                        cancelDelete()
                    }

                    isNextAfterThatRequest(normalizedText) -> {
                        requestAppointmentAfterThat()
                    }

                    isCalendarDeleteRequest(normalizedText) -> {
                        runCalendarWriteAction { handleDeleteRequest(normalizedText) }
                    }

                    isCalendarEditRequest(normalizedText) -> {
                        runCalendarWriteAction { handleEditRequest(normalizedText) }
                    }

                    isStandaloneReminderRequest(normalizedText) -> {
                        runCalendarWriteAction {
                            handleStandaloneReminderRequest(normalizedText)
                        }
                    }

                    isCalendarCreateRequest(normalizedText) -> {
                        requestCalendarCreate(normalizedText)
                    }


                    isTaskAddRequest(
                        normalizedText
                    ) -> {
                        handleTaskAdd(
                            normalizedText
                        )
                    }

                    isTaskScreenRequest(
                        normalizedText
                    ) -> {

                        currentScreen =
                            CypherScreen.TASKS

                        isMenuOpen =
                            false

                        reply(
                            "Opening your to-do list."
                        )
                    }


                    isTaskListRequest(
                        normalizedText
                    ) -> {
                        handleTaskList()
                    }

                    isTaskCompleteRequest(
                        normalizedText
                    ) -> {
                        handleTaskComplete(
                            normalizedText
                        )
                    }

                    isTaskDeleteRequest(
                        normalizedText
                    ) -> {
                        handleTaskDelete(
                            normalizedText
                        )
                    }


                    isMemoryRememberRequest(
                        normalizedText
                    ) -> {

                        handleMemoryRemember(
                            normalizedText
                        )
                    }

                    isMemoryRecallRequest(
                        normalizedText
                    ) -> {

                        handleMemoryRecall(
                            normalizedText
                        )
                    }

                    isMemoryListRequest(
                        normalizedText
                    ) -> {

                        handleMemoryList()
                    }

                    isMemoryForgetRequest(
                        normalizedText
                    ) -> {

                        handleMemoryForget(
                            normalizedText
                        )
                    }

                    isNextAppointmentRequest(normalizedText) -> {
                        requestNextAppointment()
                    }

                    isCalendarReadRequest(normalizedText) -> {
                        requestCalendarRead(normalizedText)
                    }

                    else -> {
                        sendMessageToCypherOS(normalizedText)
                    }
                }
            },
        )
    }


    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startListening()
            } else {
                isListening = false
            }
        }


    fun handleMicClick() {

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
                            Manifest.permission.RECORD_AUDIO,
                        ) ==
                            PackageManager.PERMISSION_GRANTED

                if (
                    permissionGranted
                ) {
                    startListening()
                } else {
                    microphonePermissionLauncher
                        .launch(
                            Manifest.permission.RECORD_AUDIO
                        )
                }
            }
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


    Box(
        modifier =
            Modifier.fillMaxSize(),
    ) {

        when (
            currentScreen
        ) {

            CypherScreen.HOME -> {

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


                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        min = 40.dp,
                                        max = 140.dp,
                                    )
                                    .verticalScroll(
                                        rememberScrollState()
                                    ),
                        ) {

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
                        }


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


                /*
                 * Home screen menu button.
                 * This overlays the existing Home design so the
                 * original layout is not shifted or redesigned.
                 */
                Box(
                    modifier =
                        Modifier
                            .padding(
                                start =
                                    12.dp,

                                top =
                                    24.dp,
                            )
                            .size(
                                52.dp
                            )
                            .clickable {
                                isMenuOpen =
                                    true
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
            }


            CypherScreen.TASKS -> {

                val repository =
                    taskRepository

                if (
                    repository != null
                ) {

                    CypherTaskScreen(
                        taskRepository =
                            repository,

                        isListening =
                            isListening,

                        isThinking =
                            isThinking,

                        isSpeaking =
                            isSpeaking,

                        onMenuClick = {
                            isMenuOpen =
                                true
                        },

                        onMicClick = {
                            handleMicClick()
                        },
                    )

                } else {

                    Surface(
                        modifier =
                            Modifier.fillMaxSize(),

                        color =
                            background,
                    ) {

                        Box(
                            modifier =
                                Modifier.fillMaxSize(),

                            contentAlignment =
                                Alignment.Center,
                        ) {

                            Text(
                                text =
                                    "Tasks are unavailable in preview mode.",

                                color =
                                    secondaryText,
                            )
                        }
                    }
                }
            }
        }


        if (
            isMenuOpen
        ) {

            CypherMenuOverlay(
                currentScreen =
                    currentScreen,

                onScreenSelected = { screen ->
                    currentScreen =
                        screen
                },

                onDismiss = {
                    isMenuOpen =
                        false
                },
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