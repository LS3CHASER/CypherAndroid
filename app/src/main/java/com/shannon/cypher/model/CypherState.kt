package com.shannon.cypher.model

enum class CypherMode {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

data class CypherState(
    val mode: CypherMode = CypherMode.IDLE,
    val microphoneLevel: Float = 0f,
    val recognizedText: String = "",
) {
    val isListening: Boolean
        get() = mode == CypherMode.LISTENING
}
