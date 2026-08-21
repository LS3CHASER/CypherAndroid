package com.shannon.cypher.audio

import android.content.Context
import android.media.MediaPlayer
import com.shannon.cypher.network.CypherApiClient
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CypherRemoteSpeaker(
    context: Context,
) {

    private val appContext =
        context.applicationContext

    private val apiClient =
        CypherApiClient()

    private var mediaPlayer:
            MediaPlayer? = null

    private var currentAudioFile:
            File? = null


    suspend fun speak(
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {

        if (text.isBlank()) {
            return
        }


        val audioFile =
            withContext(
                Dispatchers.IO
            ) {

                val destinationFile =
                    File(
                        appContext.cacheDir,
                        "cypher_speech_${System.currentTimeMillis()}.mp3",
                    )

                apiClient.downloadSpeech(
                    text = text,
                    destinationFile =
                        destinationFile,
                )
            }


        withContext(
            Dispatchers.Main
        ) {

            stop()

            currentAudioFile =
                audioFile


            mediaPlayer =
                MediaPlayer().apply {

                    setDataSource(
                        audioFile.absolutePath
                    )


                    setOnPreparedListener {
                            player ->

                        onStart()

                        player.start()
                    }


                    setOnCompletionListener {

                        releasePlayer()

                        deleteCurrentAudio()

                        onDone()
                    }


                    setOnErrorListener {
                            _,
                            _,
                            _ ->

                        releasePlayer()

                        deleteCurrentAudio()

                        onDone()

                        true
                    }


                    prepareAsync()
                }
        }
    }


    fun stop() {

        try {

            mediaPlayer
                ?.stop()

        } catch (
            _: IllegalStateException
        ) {
        }


        releasePlayer()

        deleteCurrentAudio()
    }


    fun destroy() {

        stop()
    }


    private fun releasePlayer() {

        mediaPlayer
            ?.release()

        mediaPlayer =
            null
    }


    private fun deleteCurrentAudio() {

        try {

            currentAudioFile
                ?.takeIf {
                    it.exists()
                }
                ?.delete()

        } catch (
            _: Exception
        ) {
        }


        currentAudioFile =
            null
    }
}