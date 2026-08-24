package com.shannon.cypher.tasks

import android.content.Context
import kotlin.math.max


class CypherTaskRepository(
    context: Context,
) {

    private val database =
        CypherTaskDatabase
            .getInstance(
                context
            )


    private val taskDao =
        database.taskDao()


    suspend fun addTask(
        title: String,
        dueAtMillis: Long? = null,
    ): Long {

        val cleanTitle =
            cleanText(
                title
            )


        if (
            cleanTitle.isBlank()
        ) {

            return -1
        }


        val task =
            CypherTaskEntity(
                title =
                    cleanTitle,

                isCompleted =
                    false,

                createdAtMillis =
                    System.currentTimeMillis(),

                completedAtMillis =
                    null,

                dueAtMillis =
                    dueAtMillis,
            )


        return taskDao.insert(
            task
        )
    }


    suspend fun getOpenTasks():
            List<CypherTaskEntity> {

        return taskDao
            .getAllTasks()
            .filter {
                !it.isCompleted
            }
            .sortedWith(
                compareBy<CypherTaskEntity> {
                    it.dueAtMillis == null
                }.thenBy {
                    it.dueAtMillis
                        ?: Long.MAX_VALUE
                }.thenBy {
                    it.createdAtMillis
                }
            )
    }


    suspend fun getCompletedTasks():
            List<CypherTaskEntity> {

        return taskDao
            .getAllTasks()
            .filter {
                it.isCompleted
            }
            .sortedByDescending {
                it.completedAtMillis
                    ?: 0L
            }
    }


    suspend fun findOpenTasks(
        searchText: String,
    ): List<CypherTaskEntity> {

        val cleanedSearch =
            normaliseForMatching(
                searchText
            )


        if (
            cleanedSearch.isBlank()
        ) {

            return emptyList()
        }


        val openTasks =
            getOpenTasks()


        if (
            openTasks.isEmpty()
        ) {

            return emptyList()
        }


        /*
         * 1. Exact / partial match after speech normalisation.
         */
        val directMatches =
            openTasks.filter {
                    task ->

                val taskText =
                    normaliseForMatching(
                        task.title
                    )


                taskText == cleanedSearch ||
                        taskText.contains(
                            cleanedSearch
                        ) ||
                        cleanedSearch.contains(
                            taskText
                        )
            }


        if (
            directMatches.isNotEmpty()
        ) {

            return directMatches
        }


        /*
         * 2. Score every open task using several voice-friendly
         *    representations:
         *
         *    - normalised words
         *    - joined words
         *    - consonant / phonetic skeleton
         *    - word-by-word fuzzy similarity
         *
         * This helps with speech recognition mistakes such as:
         *
         * buy coolant -> by coolant
         * buy coolant -> bicolon
         * buy filters -> by filters
         * four bolts -> for bolts
         */
        val scored =
            openTasks
                .map {
                        task ->

                    task to taskMatchScore(
                        task.title,
                        searchText,
                    )
                }
                .sortedByDescending {
                    it.second
                }


        val best =
            scored.firstOrNull()
                ?: return emptyList()


        val secondBestScore =
            scored
                .getOrNull(
                    1
                )
                ?.second
                ?: 0.0


        /*
         * If there is only one open task, we can be more tolerant
         * because there is no competing task to confuse it with.
         */
        if (
            openTasks.size == 1 &&
            best.second >= 0.46
        ) {

            return listOf(
                best.first
            )
        }


        /*
         * With multiple tasks, require a stronger match and a
         * useful gap over the second-best candidate.
         */
        if (
            best.second >= 0.62 &&
            (
                    best.second -
                            secondBestScore
                    ) >= 0.08
        ) {

            return listOf(
                best.first
            )
        }


        /*
         * If several tasks score strongly and similarly, return
         * all plausible matches rather than guessing.
         */
        return scored
            .filter {
                it.second >= 0.70
            }
            .map {
                it.first
            }
    }


    suspend fun getTaskById(
        taskId: Long,
    ): CypherTaskEntity? {

        return taskDao.getById(
            taskId
        )
    }


    suspend fun completeTask(
        taskId: Long,
    ): Boolean {

        val task =
            taskDao.getById(
                taskId
            )
                ?: return false


        if (
            task.isCompleted
        ) {

            return true
        }


        taskDao.update(
            task.copy(
                isCompleted =
                    true,

                completedAtMillis =
                    System.currentTimeMillis(),
            )
        )


        return true
    }


    suspend fun completeTask(
        searchText: String,
    ): CypherTaskEntity? {

        val matches =
            findOpenTasks(
                searchText
            )


        if (
            matches.size != 1
        ) {

            return null
        }


        val task =
            matches.first()


        return if (
            completeTask(
                task.id
            )
        ) {

            task

        } else {

            null
        }
    }


    suspend fun deleteTask(
        taskId: Long,
    ): Boolean {

        return taskDao.deleteById(
            taskId
        ) > 0
    }


    suspend fun deleteTask(
        searchText: String,
    ): CypherTaskEntity? {

        val matches =
            findOpenTasks(
                searchText
            )


        if (
            matches.size != 1
        ) {

            return null
        }


        val task =
            matches.first()


        return if (
            deleteTask(
                task.id
            )
        ) {

            task

        } else {

            null
        }
    }


    suspend fun clearCompletedTasks() {

        taskDao.clearCompleted()
    }


    private fun cleanText(
        text: String,
    ): String {

        return text
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
    }


    private fun normaliseForMatching(
        text: String,
    ): String {

        val replacements =
            mapOf(
                "by" to "buy",
                "bye" to "buy",

                "four" to "for",
                "fore" to "for",

                "two" to "to",
                "too" to "to",

                "won" to "one",

                "ate" to "eight",
            )


        return text
            .lowercase()
            .replace(
                Regex("[^a-z0-9\\s]"),
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
            .split(
                " "
            )
            .filter {
                it.isNotBlank()
            }
            .joinToString(
                " "
            ) {
                    word ->

                replacements[
                    word
                ]
                    ?: word
            }
    }


    private fun taskMatchScore(
        storedTitle: String,
        spokenTitle: String,
    ): Double {

        val stored =
            normaliseForMatching(
                storedTitle
            )

        val spoken =
            normaliseForMatching(
                spokenTitle
            )


        if (
            stored == spoken
        ) {

            return 1.0
        }


        val storedJoined =
            stored.replace(
                " ",
                ""
            )

        val spokenJoined =
            spoken.replace(
                " ",
                ""
            )


        val storedPhonetic =
            phoneticSkeleton(
                storedJoined
            )

        val spokenPhonetic =
            phoneticSkeleton(
                spokenJoined
            )


        val phraseScore =
            stringSimilarity(
                stored,
                spoken,
            )


        val joinedScore =
            stringSimilarity(
                storedJoined,
                spokenJoined,
            )


        val phoneticScore =
            stringSimilarity(
                storedPhonetic,
                spokenPhonetic,
            )


        val wordScore =
            wordSetSimilarity(
                stored,
                spoken,
            )


        /*
         * Weighted toward whole-phrase and phonetic similarity.
         * The phonetic component helps when speech recognition
         * collapses several spoken words into one strange word.
         */
        return (
                phraseScore * 0.25
                ) + (
                joinedScore * 0.25
                ) + (
                phoneticScore * 0.35
                ) + (
                wordScore * 0.15
                )
    }


    private fun wordSetSimilarity(
        first: String,
        second: String,
    ): Double {

        val firstWords =
            first
                .split(
                    " "
                )
                .filter {
                    it.isNotBlank()
                }


        val secondWords =
            second
                .split(
                    " "
                )
                .filter {
                    it.isNotBlank()
                }


        if (
            firstWords.isEmpty() ||
            secondWords.isEmpty()
        ) {

            return 0.0
        }


        var total =
            0.0


        for (
        word in firstWords
        ) {

            val best =
                secondWords
                    .maxOfOrNull {
                            otherWord ->

                        stringSimilarity(
                            word,
                            otherWord,
                        )
                    }
                    ?: 0.0


            total +=
                best
        }


        return total /
                max(
                    firstWords.size,
                    secondWords.size,
                ).toDouble()
    }


    private fun phoneticSkeleton(
        text: String,
    ): String {

        if (
            text.isBlank()
        ) {

            return ""
        }


        var result =
            text
                .lowercase()
                .replace(
                    "ph",
                    "f"
                )
                .replace(
                    "ck",
                    "k"
                )
                .replace(
                    "qu",
                    "k"
                )
                .replace(
                    "c",
                    "k"
                )
                .replace(
                    "q",
                    "k"
                )
                .replace(
                    "x",
                    "ks"
                )
                .replace(
                    Regex("[aeiou]"),
                    ""
                )


        result =
            result.replace(
                Regex("(.)\\1+"),
                "$1"
            )


        return result
    }


    private fun stringSimilarity(
        first: String,
        second: String,
    ): Double {

        if (
            first == second
        ) {

            return 1.0
        }


        if (
            first.isBlank() ||
            second.isBlank()
        ) {

            return 0.0
        }


        val distance =
            levenshteinDistance(
                first,
                second,
            )


        return (
                1.0 -
                        (
                                distance.toDouble() /
                                        max(
                                            first.length,
                                            second.length,
                                        ).toDouble()
                                )
                ).coerceIn(
                0.0,
                1.0,
            )
    }


    private fun levenshteinDistance(
        first: String,
        second: String,
    ): Int {

        if (
            first.isEmpty()
        ) {

            return second.length
        }


        if (
            second.isEmpty()
        ) {

            return first.length
        }


        var previous =
            IntArray(
                second.length + 1
            ) {
                it
            }


        var current =
            IntArray(
                second.length + 1
            )


        for (
        firstIndex in first.indices
        ) {

            current[0] =
                firstIndex + 1


            for (
            secondIndex in second.indices
            ) {

                val cost =
                    if (
                        first[firstIndex] ==
                        second[secondIndex]
                    ) {
                        0
                    } else {
                        1
                    }


                current[
                    secondIndex + 1
                ] =
                    minOf(
                        current[
                            secondIndex
                        ] + 1,

                        previous[
                            secondIndex + 1
                        ] + 1,

                        previous[
                            secondIndex
                        ] + cost,
                    )
            }


            val swap =
                previous

            previous =
                current

            current =
                swap
        }


        return previous[
            second.length
        ]
    }
}