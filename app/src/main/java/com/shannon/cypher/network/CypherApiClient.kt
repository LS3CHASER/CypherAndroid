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
         * CypherOS -> ElevenLabs TTS -> PCM streaming path.
         *
         * The returned audio is never played.
         */
        private const val SPEECH_WARMUP_TEXT =
            "Online."

        /*
         * Maximum time a real speech request will wait for an
         * already-running startup warm-up to finish.
         *
         * This prevents the first real /speak request from racing
         * the silent /speak warm-up on a cold Render instance.
         */
        private const val WARMUP_WAIT_TIMEOUT_MS =
            15_000L

        private val warmUpLock =
            Object()

        @Volatile
        private var warmUpInProgress =
            false

        @Volatile
        private var warmUpComplete =
            false
    }


    fun warmUp() {

        /*
         * Only one Cypher speech warm-up is allowed at a time,
         * even though the Home screen and RemoteSpeaker use
         * separate CypherApiClient instances.
         */
        synchronized(
            warmUpLock
        ) {

            if (
                warmUpComplete
            ) {

                Log.d(
                    TAG,
                    "Speech warm-up already complete."
                )

                return
            }


            if (
                warmUpInProgress
            ) {

                Log.d(
                    TAG,
                    "Speech warm-up already in progress."
                )

                return
            }


            warmUpInProgress =
                true
        }


        val totalStart =
            SystemClock.elapsedRealtime()


        try {

            /*
             * Cold-start v2:
             *
             * Do NOT block speech startup on /health.
             * Warm the exact endpoint Cypher needs: /speak.
             *
             * This wakes Render and the ElevenLabs speech path
             * in one request.
             */
            Log.d(
                TAG,
                "Starting direct speech-pipeline warm-up..."
            )


            val speechReady =
                warmSpeechEngine()


            if (
                speechReady
            ) {

                warmUpComplete =
                    true
            }


            Log.d(
                TAG,
                "Direct speech-pipeline warm-up finished in " +
                        "${SystemClock.elapsedRealtime() - totalStart} ms. " +
                        "speechReady=$speechReady"
            )

        } finally {

            synchronized(
                warmUpLock
            ) {

                warmUpInProgress =
                    false

                warmUpLock.notifyAll()
            }
        }
    }






    private fun warmSpeechEngine():
            Boolean {

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
                8_000

            connection.readTimeout =
                12_000

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

                return false
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


            return true

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


            return false

        } finally {

            connection.disconnect()
        }
    }

    private fun waitForWarmUpIfNeeded() {

        val waitStartedAt =
            SystemClock.elapsedRealtime()


        synchronized(
            warmUpLock
        ) {

            if (
                !warmUpInProgress
            ) {

                return
            }


            Log.d(
                TAG,
                "Real speech request arrived while warm-up is running. " +
                        "Waiting for warm-up to finish..."
            )


            var remaining =
                WARMUP_WAIT_TIMEOUT_MS


            while (
                warmUpInProgress &&
                remaining > 0L
            ) {

                try {

                    warmUpLock.wait(
                        remaining
                    )

                } catch (
                    interrupted: InterruptedException
                ) {

                    Thread.currentThread()
                        .interrupt()

                    Log.w(
                        TAG,
                        "Warm-up wait interrupted."
                    )

                    break
                }


                remaining =
                    WARMUP_WAIT_TIMEOUT_MS -
                            (
                                    SystemClock.elapsedRealtime() -
                                            waitStartedAt
                                    )
            }
        }


        Log.d(
            TAG,
            "Warm-up coordination wait finished after " +
                    "${SystemClock.elapsedRealtime() - waitStartedAt} ms. " +
                    "warmUpComplete=$warmUpComplete, " +
                    "warmUpInProgress=$warmUpInProgress"
        )
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

        /*
         * If startup is currently warming the same remote speech
         * pipeline, let that request finish before starting the
         * user's real speech request.
         *
         * This is the cold-start race fix.
         */
        waitForWarmUpIfNeeded()


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
            30_000

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