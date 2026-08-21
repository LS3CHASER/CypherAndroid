package com.shannon.cypher.network

import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class CypherApiClient {

    companion object {

        private const val BASE_URL =
            "https://cypheros-yr05.onrender.com"
    }


    fun sendMessage(
        message: String,
    ): String {

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

                throw RuntimeException(
                    "CypherOS returned HTTP " +
                            responseCode
                )
            }


            val responseText =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }


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

            connection.disconnect()

            throw RuntimeException(
                "CypherOS speech endpoint " +
                        "returned HTTP $responseCode"
            )
        }


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