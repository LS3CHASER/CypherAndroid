package com.shannon.cypher.network

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


class CypherApiClient {

    companion object {

        private const val BASE_URL =
            "http://192.168.1.26:8000"
    }


    fun sendMessage(
        message: String,
    ): String {

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


            connection.outputStream.use {
                    outputStream ->

                outputStream.write(
                    requestBody
                        .toByteArray(
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
                    "CypherOS returned HTTP $responseCode"
                )
            }


            val responseText =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }


            val responseJson =
                JSONObject(
                    responseText
                )


            return responseJson
                .getString(
                    "reply"
                )

        } finally {

            connection.disconnect()
        }
    }


    fun downloadSpeech(
        text: String,
        destinationFile: File,
    ): File {

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
                "audio/mpeg",
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
                    requestBody
                        .toByteArray(
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
                    "CypherOS speech endpoint returned HTTP $responseCode"
                )
            }


            destinationFile
                .parentFile
                ?.mkdirs()


            connection.inputStream.use {
                    inputStream ->

                destinationFile
                    .outputStream()
                    .use {
                            fileOutput ->

                        inputStream.copyTo(
                            fileOutput
                        )
                    }
            }


            return destinationFile

        } finally {

            connection.disconnect()
        }
    }
}