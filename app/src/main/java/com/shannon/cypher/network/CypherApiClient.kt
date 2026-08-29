package com.shannon.cypher.network

import android.os.SystemClock
import android.util.Log
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class CypherApiClient {

    companion object {

        private const val BASE_URL =
            "https://cypheros-yr05.onrender.com"

        private const val TAG =
            "CypherNetwork"

        /*
         * Tiny phrase generated only to warm the complete
         * CypherOS -> OpenAI TTS -> PCM streaming path.
         *
         * The returned audio is never played.
         */
        private const val SPEECH_WARMUP_TEXT =
            "Online."
    }


    fun warmUp() {

        val totalStart =
            SystemClock.elapsedRealtime()


        Log.d(
            TAG,
            "Starting complete CypherOS warm-up..."
        )


        val renderReady =
            warmRender()


        if (
            !renderReady
        ) {

            Log.w(
                TAG,
                "Render warm-up failed. " +
                        "Skipping speech warm-up."
            )

            return
        }


        /*
         * Render is now awake.
         *
         * Warm the actual speech generation path as well.
         * This produces a tiny PCM response but never sends
         * it to AudioTrack or Android's audio system.
         */
        warmSpeechEngine()


        Log.d(
            TAG,
            "Complete CypherOS warm-up finished in " +
                    "${SystemClock.elapsedRealtime() - totalStart} ms"
        )
    }


    private fun warmRender():
            Boolean {

        val totalStart =
            SystemClock.elapsedRealtime()


        val url =
            URL(
                "$BASE_URL/health"
            )


        val connection =
            url.openConnection()
                    as HttpURLConnection


        return try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                15_000

            connection.readTimeout =
                30_000

            connection.useCaches =
                false


            Log.d(
                TAG,
                "Starting silent Render/CypherOS warm-up..."
            )


            val responseCode =
                connection.responseCode


            val successful =
                responseCode in
                        200..299


            Log.d(
                TAG,
                "Render/CypherOS warm-up completed in " +
                        "${SystemClock.elapsedRealtime() - totalStart} ms " +
                        "(HTTP $responseCode)"
            )


            successful

        } catch (
            exception: Exception
        ) {

            Log.w(
                TAG,
                "Render/CypherOS warm-up failed after " +
                        "${SystemClock.elapsedRealtime() - totalStart} ms.",
                exception,
            )


            false

        } finally {

            connection.disconnect()
        }
    }


    private fun warmSpeechEngine() {

        val totalStart =
            SystemClock.elapsedRealtime()


        val url =
            URL(
                "$BASE_URL/speak"
            )


        val connection =
            url.openConnection()
                    as HttpURLConnection


        try {

            connection.requestMethod =
                "POST"

            /*
             * This request runs silently in the background.
             *
             * Give a genuinely cold Render/OpenAI speech
             * session enough time to wake without falling
             * into Android TTS fallback.
             */
            connection.connectTimeout =
                15_000

            connection.readTimeout =
                30_000

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
                        "text",
                        SPEECH_WARMUP_TEXT,
                    )
                    .toString()


            Log.d(
                TAG,
                "Starting silent speech-engine warm-up..."
            )


            connection.outputStream.use {
                    outputStream ->

                outputStream.write(
                    requestBody.toByteArray(
                        Charsets.UTF_8
                    )
                )
            }


            val responseCode =
                connection.responseCode


            if (
                responseCode !in
                200..299
            ) {

                Log.w(
                    TAG,
                    "Speech-engine warm-up returned " +
                            "HTTP $responseCode"
                )

                return
            }


            /*
             * IMPORTANT:
             *
             * Fully consume the PCM stream so the OpenAI TTS
             * generation request actually completes.
             *
             * These bytes are deliberately thrown away.
             *
             * No AudioTrack is created.
             * No audio focus is requested.
             * Nothing is played through the phone,
             * headphones, Bluetooth or car audio.
             */
            val discardBuffer =
                ByteArray(
                    8_192
                )


            var totalBytesDiscarded =
                0L


            connection.inputStream.use {
                    inputStream ->

                while (
                    true
                ) {

                    val bytesRead =
                        inputStream.read(
                            discardBuffer
                        )


                    if (
                        bytesRead == -1
                    ) {

                        break
                    }


                    if (
                        bytesRead > 0
                    ) {

                        totalBytesDiscarded +=
                            bytesRead
                    }
                }
            }


            Log.d(
                TAG,
                "Silent speech-engine warm-up completed in " +
                        "${SystemClock.elapsedRealtime() - totalStart} ms. " +
                        "Discarded $totalBytesDiscarded PCM bytes."
            )

        } catch (
            exception: Exception
        ) {

            /*
             * Warm-up failure must never prevent Cypher from
             * starting normally. It is purely an optimisation.
             */
            Log.w(
                TAG,
                "Silent speech-engine warm-up failed after " +
                        "${SystemClock.elapsedRealtime() - totalStart} ms.",
                exception,
            )

        } finally {

            connection.disconnect()
        }
    }


    fun sendMessage(
        message: String,
    ): String {

        val totalStart =
            SystemClock.elapsedRealtime()


        val url =
            URL(
                "$BASE_URL/message"
            )


        val connection =
            url.openConnection()
                    as HttpURLConnection


        try {

            connection.requestMethod =
                "POST"

            connection.connectTimeout =
                10_000

            connection.readTimeout =
                30_000

            connection.doOutput =
                true


            connection.setRequestProperty(
                "Content-Type",
                "application/json",
            )


            connection.setRequestProperty(
                "Accept",
                "application/json",
            )


            val requestBody =
                JSONObject()
                    .put(
                        "message",
                        message,
                    )
                    .toString()


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
                "/message request body sent in " +
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
                "/message response headers received in " +
                        "${responseHeadersReady - responseWaitStart} ms " +
                        "(HTTP $responseCode)"
            )


            if (
                responseCode !in
                200..299
            ) {

                throw RuntimeException(
                    "CypherOS returned HTTP " +
                            responseCode
                )
            }


            val bodyReadStart =
                SystemClock.elapsedRealtime()


            val responseText =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }


            val bodyReadDone =
                SystemClock.elapsedRealtime()


            Log.d(
                TAG,
                "/message response body read in " +
                        "${bodyReadDone - bodyReadStart} ms"
            )


            Log.d(
                TAG,
                "/message total time: " +
                        "${bodyReadDone - totalStart} ms"
            )


            return JSONObject(
                responseText
            ).getString(
                "reply"
            )

        } finally {

            connection.disconnect()
        }
    }


    fun openSpeechStream(
        text: String,
    ): SpeechStream {

        val totalStart =
            SystemClock.elapsedRealtime()


        val url =
            URL(
                "$BASE_URL/speak"
            )


        val connection =
            url.openConnection()
                    as HttpURLConnection


        connection.requestMethod =
            "POST"

        /*
         * Keep normal speech requests responsive.
         *
         * The longer cold-start allowance belongs to the
         * silent background warm-up above.
         */
        connection.connectTimeout =
            8_000

        connection.readTimeout =
            10_000

        connection.doOutput =
            true


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
                    "text",
                    text,
                )
                .toString()


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
            "/speak request body sent in " +
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
            "/speak response headers received in " +
                    "${responseHeadersReady - responseWaitStart} ms " +
                    "(HTTP $responseCode)"
        )


        if (
            responseCode !in
            200..299
        ) {

            connection.disconnect()


            throw RuntimeException(
                "CypherOS speech endpoint " +
                        "returned HTTP $responseCode"
            )
        }


        Log.d(
            TAG,
            "/speak stream connection ready in " +
                    "${responseHeadersReady - totalStart} ms"
        )


        return SpeechStream(
            inputStream =
                connection.inputStream,

            connection =
                connection,
        )
    }
}


class SpeechStream(
    val inputStream: InputStream,
    private val connection:
    HttpURLConnection,
) {

    fun close() {

        try {

            inputStream.close()

        } finally {

            connection.disconnect()
        }
    }
}