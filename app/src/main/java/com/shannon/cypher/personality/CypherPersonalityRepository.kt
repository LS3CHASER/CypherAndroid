package com.shannon.cypher.personality

import android.content.Context

class CypherPersonalityRepository(
    context: Context,
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun getPersonalityLevel(): CypherResponseStyle.PersonalityLevel {
        val savedValue =
            preferences.getString(
                KEY_PERSONALITY_LEVEL,
                null,
            )

        return savedValue
            ?.let { value ->
                runCatching {
                    CypherResponseStyle.PersonalityLevel.valueOf(value)
                }.getOrNull()
            }
            ?: CypherResponseStyle.PersonalityLevel.BALANCED
    }

    fun setPersonalityLevel(
        level: CypherResponseStyle.PersonalityLevel,
    ) {
        preferences
            .edit()
            .putString(
                KEY_PERSONALITY_LEVEL,
                level.name,
            )
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME =
            "cypher_personality_preferences"

        private const val KEY_PERSONALITY_LEVEL =
            "personality_level"
    }
}
