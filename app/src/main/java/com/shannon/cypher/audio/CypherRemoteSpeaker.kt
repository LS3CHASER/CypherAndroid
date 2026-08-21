package com.shannon.cypher.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.shannon.cypher.network.CypherApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CypherRemoteSpeaker(
    context: Context,
) {

    private val appContext =
        context.applicationContext

    private val apiClient =
        CypherApiClient()


    private var audioTrack:
            AudioTrack? = null


    @Volatile
    private var stopRequested =
        false


    suspend fun speak(
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {

        if (text.isBlank()) {
            return
        }


        stop()


        stopRequested =
            false


        withContext(
            Dispatchers.IO
        ) {

            var speechStream:
                    com.shannon.cypher.network
                    .SpeechStream? = null

            var track:
                    AudioTrack? = null


            try {

                speechStream =
                    apiClient.openSpeechStream(
                        text
                    )


                val sampleRate =
                    24_000

                val channelConfig =
                    AudioFormat.CHANNEL_OUT_MONO

                val audioFormat =
                    AudioFormat.ENCODING_PCM_16BIT


                val minimumBufferSize =
                    AudioTrack.getMinBufferSize(
                        sampleRate,
                        channelConfig,
                        audioFormat,
                    )


                val bufferSize =
                    maxOf(
                        minimumBufferSize,
                        8192,
                    )


                track =
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes
                                .Builder()
                                .setUsage(
                                    AudioAttributes
                                        .USAGE_ASSISTANT
                                )
                                .setContentType(
                                    AudioAttributes
                                        .CONTENT_TYPE_SPEECH
                                )
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat
                                .Builder()
                                .setEncoding(
                                    audioFormat
                                )
                                .setSampleRate(
                                    sampleRate
                                )
                                .setChannelMask(
                                    channelConfig
                                )
                                .build()
                        )
                        .setBufferSizeInBytes(
                            bufferSize
                        )
                        .setTransferMode(
                            AudioTrack.MODE_STREAM
                        )
                        .build()


                audioTrack =
                    track


                val buffer =
                    ByteArray(
                        4096
                    )


                var started =
                    false


                while (
                    !stopRequested
                ) {

                    val bytesRead =
                        speechStream
                            .inputStream
                            .read(
                                buffer
                            )


                    if (
                        bytesRead == -1
                    ) {
                        break
                    }


                    if (
                        bytesRead > 0
                    ) {

                        if (
                            !started
                        ) {

                            track.play()

                            started =
                                true


                            withContext(
                                Dispatchers.Main
                            ) {

                                onStart()
                            }
                        }


                        track.write(
                            buffer,
                            0,
                            bytesRead,
                            AudioTrack.WRITE_BLOCKING,
                        )
                    }
                }


                if (
                    started &&
                    !stopRequested
                ) {

                    try {

                        track.stop()

                    } catch (
                        _: IllegalStateException
                    ) {
                    }
                }

            } finally {

                speechStream
                    ?.close()


                try {

                    track
                        ?.release()

                } catch (
                    _: Exception
                ) {
                }


                if (
                    audioTrack === track
                ) {

                    audioTrack =
                        null
                }


                withContext(
                    Dispatchers.Main
                ) {

                    onDone()
                }
            }
        }
    }


    fun stop() {

        stopRequested =
            true


        try {

            audioTrack
                ?.pause()

        } catch (
            _: IllegalStateException
        ) {
        }


        try {

            audioTrack
                ?.flush()

        } catch (
            _: IllegalStateException
        ) {
        }


        try {

            audioTrack
                ?.stop()

        } catch (
            _: IllegalStateException
        ) {
        }
    }


    fun destroy() {

        stop()
    }
}