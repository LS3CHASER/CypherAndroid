package com.shannon.cypher.personality

import java.util.Locale

sealed class CypherPersonalityCommand {
    data class SetLevel(
        val level: CypherResponseStyle.PersonalityLevel,
    ) : CypherPersonalityCommand()

    data object GetLevel : CypherPersonalityCommand()
}

object CypherPersonalityCommandParser {

    fun parse(
        message: String,
    ): CypherPersonalityCommand? {
        val text = normalise(message)

        if (text.isBlank()) return null

        if (isGetLevelRequest(text)) {
            return CypherPersonalityCommand.GetLevel
        }

        if (!hasPersonalityIntent(text)) {
            return null
        }

        return when {
            hasMaximumLevel(text) ->
                CypherPersonalityCommand.SetLevel(
                    CypherResponseStyle.PersonalityLevel.MAXIMUM_CYPHER
                )

            hasProfessionalLevel(text) ->
                CypherPersonalityCommand.SetLevel(
                    CypherResponseStyle.PersonalityLevel.PROFESSIONAL
                )

            hasWittyLevel(text) ->
                CypherPersonalityCommand.SetLevel(
                    CypherResponseStyle.PersonalityLevel.WITTY
                )

            hasBalancedLevel(text) ->
                CypherPersonalityCommand.SetLevel(
                    CypherResponseStyle.PersonalityLevel.BALANCED
                )

            else -> null
        }
    }

    private fun normalise(
        message: String,
    ): String {
        return message
            .lowercase(Locale.getDefault())
            .replace(
                Regex("[^a-z0-9\\s'-]"),
                " ",
            )
            .replace(
                Regex("\\s+"),
                " ",
            )
            .trim()
    }

    private fun isGetLevelRequest(
        text: String,
    ): Boolean {
        return text.contains("what personality") ||
                text.contains("which personality") ||
                text.contains("what personality mode") ||
                text.contains("which personality mode") ||
                text.contains("what mode are you") ||
                text.contains("which mode are you") ||
                text.contains("what style are you") ||
                text.contains("which style are you") ||
                text.contains("current personality") ||
                text.contains("personality setting") ||
                text.contains("personality level") ||
                text.contains("current mode")
    }

    private fun hasPersonalityIntent(
        text: String,
    ): Boolean {
        val explicitTarget =
            text.contains("personality") ||
                    text.contains("your style") ||
                    text.contains("your behaviour") ||
                    text.contains("your behavior")

        val directStyleCommand =
            text.startsWith("be more witty") ||
                    text.startsWith("be witty") ||
                    text.startsWith("be more professional") ||
                    text.startsWith("be professional") ||
                    text.startsWith("keep it professional") ||
                    text.startsWith("keep things professional") ||
                    text.startsWith("keep it balanced") ||
                    text.startsWith("be balanced") ||
                    text.startsWith("maximum cypher") ||
                    text.startsWith("maximum personality") ||
                    text.startsWith("full personality")

        return explicitTarget || directStyleCommand
    }

    private fun hasMaximumLevel(
        text: String,
    ): Boolean {
        return text.contains("maximum cypher") ||
                text.contains("maximum personality") ||
                text.contains("maximum mode") ||
                text.contains("full personality") ||
                text.contains("max personality") ||
                text.contains("turn it all the way up") ||
                text.contains("turn your personality all the way up")
    }

    private fun hasProfessionalLevel(
        text: String,
    ): Boolean {
        return text.contains("professional") ||
                text.contains("serious mode") ||
                text.contains("less witty") ||
                text.contains("less sarcastic") ||
                text.contains("no sarcasm")
    }

    private fun hasWittyLevel(
        text: String,
    ): Boolean {
        return text.contains("witty") ||
                text.contains("more wit") ||
                text.contains("more sarcastic") ||
                text.contains("more sarcasm") ||
                text.contains("turn up the sarcasm") ||
                text.contains("turn the sarcasm up")
    }

    private fun hasBalancedLevel(
        text: String,
    ): Boolean {
        return text.contains("balanced") ||
                text.contains("normal personality") ||
                text.contains("default personality") ||
                text.contains("normal mode")
    }
}
