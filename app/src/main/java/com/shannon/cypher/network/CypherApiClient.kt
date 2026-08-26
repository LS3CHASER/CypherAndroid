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
    }



    fun warmUp() {

        val totalStart =
            SystemClock.elapsedRealtime()


        val url =
            URL(BASE_URL)


        val connection =
            url.openConnection()
                    as HttpURLConnection


        try {

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
                "Starting silent CypherOS warm-up..."
            )


            /*
             * Any HTTP response is enough for our purpose here.
             * Even if the root route returns 404, Render has still
             * had to wake the service and answer the request.
             */
            val responseCode =
                connection.responseCode


            Log.d(
                TAG,
                "CypherOS warm-up completed in " +
                        "${SystemClock.elapsedRealtime() - totalStart} ms " +
                        "(HTTP $responseCode)"
            )

        } catch (
            exception: Exception
        ) {

            Log.w(
                TAG,
                "CypherOS warm-up failed after " +
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
            URL("$BASE_URL/message")


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
            URL("$BASE_URL/speak")


        val connection =
            url.openConnection()
                    as HttpURLConnection


        connection.requestMethod =
            "POST"

        connection.connectTimeout =
            10_000

        connection.readTimeout =
            60_000

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
