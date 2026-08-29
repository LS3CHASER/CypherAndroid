package com.shannon.cypher.voicelab


enum class CypherVoiceProvider {

    OPENAI,
    ELEVENLABS,
}


data class CypherVoiceCandidate(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val provider: CypherVoiceProvider,
)


object CypherVoiceCandidates {

    const val TEST_PHRASE =
        "Good evening. All systems are online and ready. " +
                "Whatever comes next, we will face it together."


    val currentCypher =
        CypherVoiceCandidate(
            id =
                "current",

            title =
                "Current Cypher",

            subtitle =
                "Control voice",

            description =
                "The current live Cypher voice. " +
                        "Use this as the reference when comparing candidates.",

            provider =
                CypherVoiceProvider.OPENAI,
        )


    val commander =
        CypherVoiceCandidate(
            id =
                "commander",

            title =
                "Commander",

            subtitle =
                "Deep • Noble • Controlled",

            description =
                "A deep, mature and resonant voice with calm authority, " +
                        "strong articulation and a composed leadership presence.",

            provider =
                CypherVoiceProvider.OPENAI,
        )


    val titan =
        CypherVoiceCandidate(
            id =
                "titan",

            title =
                "Titan",

            subtitle =
                "Heavy • Powerful • Cinematic",

            description =
                "A heavier and lower voice with substantial vocal weight, " +
                        "strong chest resonance and a large cinematic presence.",

            provider =
                CypherVoiceProvider.OPENAI,
        )


    val sentinel =
        CypherVoiceCandidate(
            id =
                "sentinel",

            title =
                "Sentinel",

            subtitle =
                "Deep • Warm • Expressive",

            description =
                "A deep authoritative voice with more warmth and emotional " +
                        "movement for natural everyday conversation.",

            provider =
                CypherVoiceProvider.OPENAI,
        )


    val prime =
        CypherVoiceCandidate(
            id =
                "prime",

            title =
                "Prime",

            subtitle =
                "Heroic • Resonant • Commanding",

            description =
                "A powerful, heroic and highly resonant voice with deliberate " +
                        "pacing, quiet strength and commanding presence.",

            provider =
                CypherVoiceProvider.OPENAI,
        )


    /*
     * We keep the ElevenLabs slot in the model now so we do not
     * have to redesign the Voice Lab later if OpenAI cannot reach
     * the depth and character we want.
     *
     * It will remain hidden from the first UI until the provider
     * is actually connected.
     */
    val designedVoice =
        CypherVoiceCandidate(
            id =
                "designed_voice",

            title =
                "Designed Voice",

            subtitle =
                "Custom voice candidate",

            description =
                "Reserved for an original custom-designed Cypher voice " +
                        "if we later connect an external voice provider.",

            provider =
                CypherVoiceProvider.ELEVENLABS,
        )


    val openAiCandidates =
        listOf(
            currentCypher,
            commander,
            titan,
            sentinel,
            prime,
        )


    fun findById(
        id: String,
    ): CypherVoiceCandidate? {

        return (
                openAiCandidates +
                        designedVoice
                )
            .firstOrNull {
                    candidate ->

                candidate.id ==
                        id
            }
    }
}