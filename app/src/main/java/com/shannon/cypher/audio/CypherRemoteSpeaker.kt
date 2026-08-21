package com.shannon.cypher.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.shannon.cypher.network.CypherApiClient
import com.shannon.cypher.network.SpeechStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream


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
                    SpeechStream? = null

            var track:
                    AudioTrack? = null


            try {

                speechStream =
                    apiClient.openSpeechStream(
                        text
                    )


                val input =
                    BufferedInputStream(
                        speechStream.inputStream,
                        32_768,
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


                val audioTrackBufferSize =
                    maxOf(
                        minimumBufferSize * 4,
                        32_768,
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
                            audioTrackBufferSize
                        )
                        .setTransferMode(
                            AudioTrack.MODE_STREAM
                        )
                        .build()


                audioTrack =
                    track


                val preBufferTarget =
                    16_384


                val preBuffer =
                    ByteArray(
                        preBufferTarget
                    )


                var preBufferBytes =
                    0


                while (
                    preBufferBytes <
                    preBuffer.size &&
                    !stopRequested
                ) {

                    val bytesRead =
                        input.read(
                            preBuffer,
                            preBufferBytes,
                            preBuffer.size -
                                    preBufferBytes,
                        )


                    if (
                        bytesRead == -1
                    ) {
                        break
                    }


                    preBufferBytes +=
                        bytesRead
                }


                if (
                    stopRequested ||
                    preBufferBytes <= 0
                ) {

                    return@withContext
                }


                var leftoverByte:
                        Byte? = null


                var initialBytes =
                    preBufferBytes


                if (
                    initialBytes % 2 != 0
                ) {

                    leftoverByte =
                        preBuffer[
                            initialBytes - 1
                        ]

                    initialBytes -= 1
                }


                track.play()


                withContext(
                    Dispatchers.Main
                ) {

                    onStart()
                }


                if (
                    initialBytes > 0
                ) {

                    writeFully(
                        track = track,
                        buffer = preBuffer,
                        length = initialBytes,
                    )
                }


                val networkBuffer =
                    ByteArray(
                        8_192
                    )


                val playbackBuffer =
                    ByteArray(
                        8_194
                    )


                var totalBytesWritten =
                    initialBytes.toLong()


                while (
                    !stopRequested
                ) {

                    val bytesRead =
                        input.read(
                            networkBuffer
                        )


                    if (
                        bytesRead == -1
                    ) {
                        break
                    }


                    if (
                        bytesRead <= 0
                    ) {
                        continue
                    }


                    var playbackBytes =
                        0


                    if (
                        leftoverByte != null
                    ) {

                        playbackBuffer[0] =
                            leftoverByte

                        System.arraycopy(
                            networkBuffer,
                            0,
                            playbackBuffer,
                            1,
                            bytesRead,
                        )

                        playbackBytes =
                            bytesRead + 1

                        leftoverByte =
                            null

                    } else {

                        System.arraycopy(
                            networkBuffer,
                            0,
                            playbackBuffer,
                            0,
                            bytesRead,
                        )

                        playbackBytes =
                            bytesRead
                    }


                    if (
                        playbackBytes % 2 != 0
                    ) {

                        leftoverByte =
                            playbackBuffer[
                                playbackBytes - 1
                            ]

                        playbackBytes -= 1
                    }


                    if (
                        playbackBytes > 0
                    ) {

                        writeFully(
                            track = track,
                            buffer = playbackBuffer,
                            length = playbackBytes,
                        )

                        totalBytesWritten +=
                            playbackBytes
                    }
                }


                /*
                 * PCM16 mono:
                 * 2 bytes = 1 sample/frame.
                 */
                val totalFramesWritten =
                    totalBytesWritten / 2L


                /*
                 * Wait until AudioTrack has actually
                 * played every frame we queued.
                 */
                while (
                    !stopRequested &&
                    track.playState ==
                    AudioTrack.PLAYSTATE_PLAYING
                ) {

                    val playedFrames =
                        track.playbackHeadPosition
                            .toLong() and
                                0xFFFFFFFFL


                    if (
                        playedFrames >=
                        totalFramesWritten
                    ) {

                        break
                    }


                    Thread.sleep(
                        20
                    )
                }


            } finally {

                speechStream
                    ?.close()


                try {

                    track
                        ?.stop()

                } catch (
                    _: IllegalStateException
                ) {
                }


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


    private fun writeFully(
        track: AudioTrack,
        buffer: ByteArray,
        length: Int,
    ) {

        var offset =
            0


        while (
            offset < length &&
            !stopRequested
        ) {

            val written =
                track.write(
                    buffer,
                    offset,
                    length - offset,
                    AudioTrack.WRITE_BLOCKING,
                )


            if (
                written <= 0
            ) {
                break
            }


            offset +=
                written
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