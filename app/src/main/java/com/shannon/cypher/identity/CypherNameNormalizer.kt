package com.shannon.cypher.identity


object CypherNameNormalizer {

    private val nameVariants = setOf(
        "cypher",
        "cipher",
        "cyper",
        "cyber",
        "sifer",
        "sypher",
    )


    fun normalize(
        message: String,
    ): String {

        val words = message
            .trim()
            .split(
                Regex("\\s+")
            )
            .toMutableList()

        if (words.isEmpty()) {
            return message
        }


        fun cleanWord(
            word: String,
        ): String {

            return word
                .lowercase()
                .trim(
                    '.',
                    ',',
                    '!',
                    '?',
                    ':',
                    ';',
                )
        }


        for (index in words.indices) {

            val clean =
                cleanWord(
                    words[index]
                )

            if (clean in nameVariants) {

                val punctuation =
                    words[index]
                        .dropWhile {
                            it.isLetter()
                        }

                words[index] =
                    "Cypher$punctuation"
            }
        }


        return words.joinToString(
            separator = " "
        )
    }
}
