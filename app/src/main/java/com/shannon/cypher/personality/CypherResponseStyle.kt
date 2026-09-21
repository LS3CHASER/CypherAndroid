package com.shannon.cypher.personality

/**
 * Central response-personality layer for Cypher's locally handled Android skills.
 *
 * Phase 1 deliberately contains no Android dependencies and does not alter any
 * skill logic. Weather, Calendar, Tasks, Alarms and Timers can call this class
 * later after their action has already succeeded or failed.
 */
object CypherResponseStyle {

    enum class PersonalityLevel {
        PROFESSIONAL,
        BALANCED,
        WITTY,
        MAXIMUM_CYPHER,
    }

    enum class ResponseTone {
        ROUTINE,
        SUCCESS,
        INFORMATION,
        WARNING,
        FAILURE,
        SERIOUS,
    }

    /**
     * BALANCED is Cypher's default personality:
     * composed and useful first, with occasional dry wit.
     *
     * This is kept here rather than scattered through individual skills so a
     * persistent user setting can be connected later without rewriting them.
     */
    var personalityLevel: PersonalityLevel = PersonalityLevel.BALANCED
        private set

    fun setPersonalityLevel(level: PersonalityLevel) {
        personalityLevel = level
    }

    /**
     * Returns one suitable phrase from a group.
     *
     * Selection is intentionally deterministic for a supplied seed. This avoids
     * flaky tests while still allowing callers to vary phrasing naturally by
     * supplying a meaningful value such as an alarm ID, timer ID, event ID or
     * current interaction counter.
     */
    private fun choose(
        options: List<String>,
        seed: Long = System.nanoTime(),
    ): String {
        require(options.isNotEmpty())

        val index =
            ((seed xor (seed ushr 32)) and Long.MAX_VALUE)
                .rem(options.size.toLong())
                .toInt()

        return options[index]
    }

    /**
     * Serious, warning and failure responses deliberately suppress most humour.
     * Personality must never get in the way of useful information.
     */
    private fun allowsWit(tone: ResponseTone): Boolean {
        return tone == ResponseTone.ROUTINE ||
                tone == ResponseTone.SUCCESS ||
                tone == ResponseTone.INFORMATION
    }

    private fun effectiveLevel(tone: ResponseTone): PersonalityLevel {
        if (!allowsWit(tone)) {
            return PersonalityLevel.PROFESSIONAL
        }

        return personalityLevel
    }


    /**
     * Applies Cypher's personality to any locally generated Android response.
     *
     * This is the single integration point used by CypherHomeScreen. It means
     * Calendar, Tasks, Weather, Alarms, Timers and Memory can all share the
     * same personality without changing their working command logic.
     *
     * Important factual content is preserved. Failures, permission problems,
     * ambiguity prompts and serious messages remain direct and unembellished.
     */
    fun styleLocalResponse(
        text: String,
        seed: Long = System.nanoTime(),
    ): String {
        val clean = text.trim()

        if (clean.isEmpty()) {
            return clean
        }

        val lower = clean.lowercase()

        val directResponseMarkers =
            listOf(
                "couldn't",
                "could not",
                "can't",
                "cannot",
                "unavailable",
                "permission",
                "i need ",
                "please tell me",
                "please include",
                "please be more specific",
                "more than one",
                "i found the ",
                "i found an ",
                "there isn't",
                "there is no",
                "no previous",
            )

        if (directResponseMarkers.any { it in lower }) {
            return clean
        }

        val isSuccess =
            lower.startsWith("done.") ||
                    lower.startsWith("saved locally.") ||
                    lower.startsWith("got it.") ||
                    lower.startsWith("okay.") ||
                    lower.startsWith("all set.") ||
                    lower.contains("alarm set") ||
                    lower.contains("timer started") ||
                    lower.contains("marked ") && lower.contains(" complete") ||
                    lower.contains("deleted.") ||
                    lower.contains("turned on.") ||
                    lower.contains("turned off.") ||
                    lower.contains("i've added") ||
                    lower.contains("i've removed") ||
                    lower.contains("i've set a reminder") ||
                    lower.contains("is now due")

        return if (isSuccess) {
            styleSuccessResponse(
                clean,
                seed,
            )
        } else {
            styleInformationResponse(
                clean,
                seed,
            )
        }
    }

    private fun styleSuccessResponse(
        text: String,
        seed: Long,
    ): String {
        val body =
            text
                .replaceFirst(
                    Regex(
                        "^(?:Done\\.|All set\\.|That's sorted\\.|Consider it handled\\.|Handled\\.|Saved locally\\.|Got it\\.|Okay\\.)\\s*",
                        RegexOption.IGNORE_CASE,
                    ),
                    "",
                )
                .trim()

        if (body.isEmpty()) {
            return acknowledgement(
                tone = ResponseTone.SUCCESS,
                seed = seed,
            )
        }

        return when (personalityLevel) {
            PersonalityLevel.PROFESSIONAL ->
                if (text.startsWith("Done.", ignoreCase = true)) {
                    text
                } else {
                    "Done. $body"
                }

            PersonalityLevel.BALANCED -> {
                val opening =
                    choose(
                        listOf(
                            "Done.",
                            "All set.",
                            "That's sorted.",
                            "Consider it handled.",
                            "Handled.",
                        ),
                        seed,
                    )

                "$opening $body"
            }

            PersonalityLevel.WITTY -> {
                val opening =
                    choose(
                        listOf(
                            "Done.",
                            "Consider it handled.",
                            "Sorted.",
                            "Handled. Efficient, isn't it?",
                            "Done. Remarkably painless.",
                        ),
                        seed,
                    )

                "$opening $body"
            }

            PersonalityLevel.MAXIMUM_CYPHER -> {
                val opening =
                    choose(
                        listOf(
                            "Consider it handled.",
                            "Sorted. Civilization may continue.",
                            "Done. Another crisis narrowly avoided.",
                            "Handled. I do occasionally earn my keep.",
                            "That's sorted. Try to contain your astonishment.",
                        ),
                        seed,
                    )

                "$opening $body"
            }
        }
    }

    private fun styleInformationResponse(
        text: String,
        seed: Long,
    ): String {
        return when (personalityLevel) {
            PersonalityLevel.PROFESSIONAL ->
                text

            PersonalityLevel.BALANCED ->
                // Balanced Cypher keeps factual/list responses concise.
                text

            PersonalityLevel.WITTY -> {
                val lower = text.lowercase()

                if (
                    lower.startsWith("you don't have") ||
                    lower.startsWith("your calendar is clear") ||
                    lower.startsWith("your to-do list is clear") ||
                    lower.startsWith("you have no ")
                ) {
                    choose(
                        listOf(
                            text,
                            "Nothing demanding your attention there. $text",
                            "A rare moment of peace. $text",
                        ),
                        seed,
                    )
                } else {
                    text
                }
            }

            PersonalityLevel.MAXIMUM_CYPHER -> {
                val lower = text.lowercase()

                if (
                    lower.startsWith("you don't have") ||
                    lower.startsWith("your calendar is clear") ||
                    lower.startsWith("your to-do list is clear") ||
                    lower.startsWith("you have no ")
                ) {
                    choose(
                        listOf(
                            text,
                            "Against all odds, you've caught a break. $text",
                            "A suspiciously quiet result. $text",
                            "Enjoy the peace while it lasts. $text",
                        ),
                        seed,
                    )
                } else {
                    text
                }
            }
        }
    }

    fun acknowledgement(
        tone: ResponseTone = ResponseTone.SUCCESS,
        seed: Long = System.nanoTime(),
    ): String {
        return when (effectiveLevel(tone)) {
            PersonalityLevel.PROFESSIONAL ->
                choose(
                    listOf(
                        "Done.",
                        "All set.",
                        "That's sorted.",
                    ),
                    seed,
                )

            PersonalityLevel.BALANCED ->
                choose(
                    listOf(
                        "Done.",
                        "All set.",
                        "That's sorted.",
                        "Consider it handled.",
                        "Handled.",
                    ),
                    seed,
                )

            PersonalityLevel.WITTY ->
                choose(
                    listOf(
                        "Done.",
                        "Consider it handled.",
                        "That's sorted.",
                        "Handled. Efficient, isn't it?",
                        "Sorted. Remarkably painless.",
                    ),
                    seed,
                )

            PersonalityLevel.MAXIMUM_CYPHER ->
                choose(
                    listOf(
                        "Consider it handled.",
                        "Sorted. Civilization may continue.",
                        "Done. Another crisis narrowly avoided.",
                        "Handled. I do occasionally earn my keep.",
                        "That's sorted. Try to contain your astonishment.",
                    ),
                    seed,
                )
        }
    }

    fun alarmSet(
        time: String,
        repeatingDescription: String? = null,
        seed: Long = System.nanoTime(),
    ): String {
        val repeat =
            repeatingDescription
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { " $it" }
                ?: ""

        return when (effectiveLevel(ResponseTone.SUCCESS)) {
            PersonalityLevel.PROFESSIONAL ->
                "Alarm set for $time$repeat."

            PersonalityLevel.BALANCED ->
                choose(
                    listOf(
                        "Done. I've set the alarm for $time$repeat.",
                        "Alarm set for $time$repeat.",
                        "$time$repeat. Consider it handled.",
                        "All set. I'll get your attention at $time$repeat.",
                    ),
                    seed,
                )

            PersonalityLevel.WITTY ->
                choose(
                    listOf(
                        "Alarm set for $time$repeat.",
                        "$time$repeat. Consider it armed.",
                        "Done. $time$repeat is now your problem.",
                        "$time$repeat. I'll make sure you hear about it.",
                    ),
                    seed,
                )

            PersonalityLevel.MAXIMUM_CYPHER ->
                choose(
                    listOf(
                        "$time$repeat. Consider it armed and mildly threatening.",
                        "Alarm set for $time$repeat. Future you has been warned.",
                        "$time$repeat. I'll be the unpopular one when the time comes.",
                        "Done. $time$repeat is officially unavoidable.",
                    ),
                    seed,
                )
        }
    }

    fun timerStarted(
        duration: String,
        seed: Long = System.nanoTime(),
    ): String {
        return when (effectiveLevel(ResponseTone.SUCCESS)) {
            PersonalityLevel.PROFESSIONAL ->
                "Timer started for $duration."

            PersonalityLevel.BALANCED ->
                choose(
                    listOf(
                        "Done. I've started a timer for $duration.",
                        "Timer started for $duration.",
                        "$duration. Consider it handled.",
                        "All set. $duration on the clock.",
                    ),
                    seed,
                )

            PersonalityLevel.WITTY ->
                choose(
                    listOf(
                        "$duration on the clock.",
                        "Timer started for $duration. Try not to stare at it.",
                        "Done. You have $duration.",
                        "$duration. The countdown begins.",
                    ),
                    seed,
                )

            PersonalityLevel.MAXIMUM_CYPHER ->
                choose(
                    listOf(
                        "$duration. The relentless march of time has begun.",
                        "Timer started for $duration. Spend it wisely.",
                        "$duration on the clock. No pressure.",
                        "Done. $duration until I interrupt your life again.",
                    ),
                    seed,
                )
        }
    }

    fun calendarAdded(
        title: String,
        whenDescription: String,
        reminderDescription: String? = null,
        seed: Long = System.nanoTime(),
    ): String {
        val reminder =
            reminderDescription
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { " I've also set $it." }
                ?: ""

        val base =
            when (effectiveLevel(ResponseTone.SUCCESS)) {
                PersonalityLevel.PROFESSIONAL ->
                    "I've added $title to your calendar for $whenDescription."

                PersonalityLevel.BALANCED ->
                    choose(
                        listOf(
                            "Done. I've added $title to your calendar for $whenDescription.",
                            "$title is in your calendar for $whenDescription.",
                            "That's sorted. $title is booked for $whenDescription.",
                        ),
                        seed,
                    )

                PersonalityLevel.WITTY ->
                    choose(
                        listOf(
                            "$title is in the calendar for $whenDescription.",
                            "Done. $title has claimed $whenDescription.",
                            "That's sorted. $whenDescription now belongs to $title.",
                        ),
                        seed,
                    )

                PersonalityLevel.MAXIMUM_CYPHER ->
                    choose(
                        listOf(
                            "$title is booked for $whenDescription. Your future has been organised.",
                            "Done. I've surrendered $whenDescription to $title.",
                            "$title is in the calendar for $whenDescription. No pretending you forgot.",
                        ),
                        seed,
                    )
            }

        return base + reminder
    }

    fun taskAdded(
        title: String,
        seed: Long = System.nanoTime(),
    ): String {
        return when (effectiveLevel(ResponseTone.SUCCESS)) {
            PersonalityLevel.PROFESSIONAL ->
                "I've added $title to your to-do list."

            PersonalityLevel.BALANCED ->
                choose(
                    listOf(
                        "Done. I've added $title to your to-do list.",
                        "$title is on your to-do list.",
                        "That's added. One less thing to remember yourself.",
                    ),
                    seed,
                )

            PersonalityLevel.WITTY ->
                choose(
                    listOf(
                        "$title is on the list.",
                        "Added. I'll remember $title so you don't have to.",
                        "$title is officially your future problem.",
                    ),
                    seed,
                )

            PersonalityLevel.MAXIMUM_CYPHER ->
                choose(
                    listOf(
                        "$title is on the list. Procrastination may now commence.",
                        "Added. $title can no longer claim it was forgotten.",
                        "$title is officially documented. Escape routes are narrowing.",
                    ),
                    seed,
                )
        }
    }

    fun weatherIntro(
        summary: String,
        seed: Long = System.nanoTime(),
    ): String {
        return when (effectiveLevel(ResponseTone.INFORMATION)) {
            PersonalityLevel.PROFESSIONAL ->
                summary

            PersonalityLevel.BALANCED ->
                choose(
                    listOf(
                        summary,
                        "Here's the situation. $summary",
                        "Weather report: $summary",
                    ),
                    seed,
                )

            PersonalityLevel.WITTY ->
                choose(
                    listOf(
                        summary,
                        "The atmosphere has submitted its report. $summary",
                        "Outside appears to be doing this: $summary",
                    ),
                    seed,
                )

            PersonalityLevel.MAXIMUM_CYPHER ->
                choose(
                    listOf(
                        summary,
                        "I've consulted the sky on your behalf. $summary",
                        "The atmosphere remains committed to being difficult. $summary",
                    ),
                    seed,
                )
        }
    }

    fun failure(
        message: String,
    ): String {
        // Failures remain direct at every personality level.
        return message
    }

    fun warning(
        message: String,
    ): String {
        // Warnings remain direct at every personality level.
        return message
    }

    fun serious(
        message: String,
    ): String {
        // Serious information must never be decorated with humour.
        return message
    }
}
