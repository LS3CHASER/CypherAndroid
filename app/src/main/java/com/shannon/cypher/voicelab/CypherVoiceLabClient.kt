package com.shannon.cypher.voicelab

import android.os.SystemClock
import android.util.Log
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class CypherVoiceLabClient {

    companion object {

        private const val BASE_URL =
            "https://cypheros-yr05.onrender.com"

        private const val TAG =
            "CypherVoiceLab"

        /*
         * Dedicated Voice Lab endpoint.
         *
         * This deliberately does NOT use Cypher's normal /speak
         * endpoint, so testing candidates cannot change or interfere
         * with the live Cypher voice.
         */
        private const val VOICE_LAB_ENDPOINT =
            "/voice-lab/speak"

        private const val CONNECT_TIMEOUT_MS =
            10_000

        private const val READ_TIMEOUT_MS =
            30_000
    }


    fun openCandidateSpeechStream(
        candidate: CypherVoiceCandidate,
        text: String =
            CypherVoiceCandidates.TEST_PHRASE,
    ): CypherVoiceLabSpeechStream {

        if (
            text.isBlank()
        ) {

            throw IllegalArgumentException(
                "Voice Lab test text cannot be blank."
            )
        }


        if (
            candidate.provider !=
            CypherVoiceProvider.OPENAI
        ) {

            throw IllegalArgumentException(
                "Voice provider ${candidate.provider} " +
                        "is not connected yet."
            )
        }


        val totalStart =
            SystemClock.elapsedRealtime()


        val url =
            URL(
                "$BASE_URL$VOICE_LAB_ENDPOINT"
            )


        val connection =
            url.openConnection()
                    as HttpURLConnection


        try {

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                CONNECT_TIMEOUT_MS

            connection.readTimeout =
                READ_TIMEOUT_MS

            connection.doOutput =
                true

            connection.useCaches =
                false


            connection.setRequestProperty(
                "Content-Type",
                "application/json",
            )


            connection.setRequestProperty(
                "Accept",
                "audio/pcm",
            )


            val requestBody =
                JSONObject()
                    .put(
                        "candidate_id",
                        candidate.id,
                    )
                    .put(
                        "text",
                        text,
                    )
                    .toString()


            Log.d(
                TAG,
                "Requesting Voice Lab candidate: ${candidate.id}"
            )


            val writeStart =
                SystemClock.elapsedRealtime()


            connection.outputStream.use {
                    outputStream ->

                outputStream.write(
                    requestBody.toByteArray(
                        Charsets.UTF_8
                    )
                )
            }


            val writeDone =
                SystemClock.elapsedRealtime()


            Log.d(
                TAG,
                "Voice Lab request body sent in " +
                        "${writeDone - writeStart} ms"
            )


            val responseWaitStart =
                SystemClock.elapsedRealtime()


            val responseCode =
                connection.responseCode


            val responseHeadersReady =
                SystemClock.elapsedRealtime()


            Log.d(
                TAG,
                "Voice Lab response headers received in " +
                        "${responseHeadersReady - responseWaitStart} ms " +
                        "(HTTP $responseCode)"
            )


            if (
                responseCode !in
                200..299
            ) {

                val errorText =
                    try {

                        connection.errorStream
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }
                            ?.take(
                                500
                            )

                    } catch (
                        _: Exception
                    ) {

                        null
                    }


                throw RuntimeException(
                    buildString {

                        append(
                            "Voice Lab endpoint returned HTTP $responseCode"
                        )


                        if (
                            !errorText.isNullOrBlank()
                        ) {

                            append(
                                ": "
                            )

                            append(
                                errorText
                            )
                        }
                    }
                )
            }


            Log.d(
                TAG,
                "Voice Lab stream ready in " +
                        "${responseHeadersReady - totalStart} ms " +
                        "for candidate ${candidate.id}"
            )


            return CypherVoiceLabSpeechStream(
                inputStream =
                    connection.inputStream,

                connection =
                    connection,

                candidateId =
                    candidate.id,
            )

        } catch (
            exception: Exception
        ) {

            connection.disconnect()

            throw exception
        }
    }
}


class CypherVoiceLabSpeechStream(
    val inputStream: InputStream,
    private val connection:
    HttpURLConnection,
    val candidateId: String,
) {

    fun close() {

        try {

            inputStream.close()

        } finally {

            connection.disconnect()
        }
    }
}