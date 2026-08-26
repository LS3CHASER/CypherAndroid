package com.shannon.cypher.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.shannon.cypher.network.CypherApiClient
import com.shannon.cypher.network.SpeechStream
import java.io.BufferedInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CypherRemoteSpeaker(
    context: Context,
) {

    companion object {

        private const val TAG =
            "CypherAudio"

        private const val PRE_BUFFER_BYTES =
            4_096
    }


    private val appContext =
        context.applicationContext


    private val apiClient =
        CypherApiClient()


    private val audioManager =
        appContext.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager


    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )


    private val speechAudioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(
                AudioAttributes.USAGE_ASSISTANT
            )
            .setContentType(
                AudioAttributes.CONTENT_TYPE_SPEECH
            )
            .build()


    private var audioTrack:
            AudioTrack? = null


    private var audioFocusRequest:
            AudioFocusRequest? = null


    private var hasAudioFocus =
        false


    @Volatile
    private var stopRequested =
        false


    private val audioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener {
                focusChange ->

            when (
                focusChange
            ) {

                AudioManager.AUDIOFOCUS_LOSS -> {

                    Log.d(
                        TAG,
                        "Audio focus lost."
                    )

                    stop()
                }


                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {

                    Log.d(
                        TAG,
                        "Audio focus lost temporarily."
                    )

                    try {

                        audioTrack
                            ?.pause()

                    } catch (
                        _: IllegalStateException
                    ) {
                    }
                }


                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {

                    Log.d(
                        TAG,
                        "Audio focus loss: can duck."
                    )

                    try {

                        audioTrack
                            ?.setVolume(
                                0.65f
                            )

                    } catch (
                        _: Exception
                    ) {
                    }
                }


                AudioManager.AUDIOFOCUS_GAIN -> {

                    Log.d(
                        TAG,
                        "Audio focus gained."
                    )

                    try {

                        audioTrack
                            ?.setVolume(
                                1.0f
                            )

                        if (
                            audioTrack
                                ?.playState ==
                            AudioTrack.PLAYSTATE_PAUSED
                        ) {

                            audioTrack
                                ?.play()
                        }

                    } catch (
                        _: IllegalStateException
                    ) {
                    }
                }
            }
        }


    suspend fun speak(
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {

        if (
            text.isBlank()
        ) {

            return
        }


        stop()

        stopRequested =
            false


        val totalStartMillis =
            SystemClock.elapsedRealtime()


        withContext(
            Dispatchers.IO
        ) {

            var speechStream:
                    SpeechStream? = null

            var track:
                    AudioTrack? = null


            try {

                val streamRequestStart =
                    SystemClock.elapsedRealtime()


                Log.d(
                    TAG,
                    "Opening /speak stream..."
                )


                speechStream =
                    apiClient.openSpeechStream(
                        text
                    )


                val streamOpenedAt =
                    SystemClock.elapsedRealtime()


                Log.d(
                    TAG,
                    "/speak stream opened in " +
                            "${streamOpenedAt - streamRequestStart} ms"
                )


                val input =
                    BufferedInputStream(
                        speechStream.inputStream,
                        8_192,
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
                        minimumBufferSize * 2,
                        16_384,
                    )


                track =
                    AudioTrack
                        .Builder()
                        .setAudioAttributes(
                            speechAudioAttributes
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


                val firstAudioWaitStart =
                    SystemClock.elapsedRealtime()


                val preBuffer =
                    ByteArray(
                        PRE_BUFFER_BYTES
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


                val firstAudioReadyAt =
                    SystemClock.elapsedRealtime()


                Log.d(
                    TAG,
                    "Initial PCM ready: " +
                            "$preBufferBytes bytes after " +
                            "${firstAudioReadyAt - firstAudioWaitStart} ms"
                )


                if (
                    stopRequested ||
                    preBufferBytes <= 0
                ) {

                    return@withContext
                }


                val focusGranted =
                    requestSpeechAudioFocus()


                Log.d(
                    TAG,
                    "Audio focus request: " +
                            if (
                                focusGranted
                            ) {
                                "GRANTED"
                            } else {
                                "NOT GRANTED - attempting playback anyway"
                            }
                )


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

                    initialBytes -=
                        1
                }


                val playRequestedAt =
                    SystemClock.elapsedRealtime()


                track.play()


                withContext(
                    Dispatchers.Main
                ) {

                    onStart()
                }


                Log.d(
                    TAG,
                    "AudioTrack.play() called after " +
                            "${playRequestedAt - totalStartMillis} ms"
                )


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

                        playbackBytes -=
                            1
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


                val totalFramesWritten =
                    totalBytesWritten / 2L


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


                Log.d(
                    TAG,
                    "Speech playback complete. Total speaker time: " +
                            "${SystemClock.elapsedRealtime() - totalStartMillis} ms"
                )


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


                abandonSpeechAudioFocus()


                withContext(
                    Dispatchers.Main
                ) {

                    onDone()
                }
            }
        }
    }


    private fun requestSpeechAudioFocus():
            Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val request =
                AudioFocusRequest
                    .Builder(
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                    )
                    .setAudioAttributes(
                        speechAudioAttributes
                    )
                    .setOnAudioFocusChangeListener(
                        audioFocusChangeListener,
                        mainHandler,
                    )
                    .setAcceptsDelayedFocusGain(
                        false
                    )
                    .setWillPauseWhenDucked(
                        true
                    )
                    .build()


            audioFocusRequest =
                request


            val result =
                audioManager.requestAudioFocus(
                    request
                )


            hasAudioFocus =
                result ==
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED


            hasAudioFocus

        } else {

            @Suppress(
                "DEPRECATION"
            )
            val result =
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                )


            hasAudioFocus =
                result ==
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED


            hasAudioFocus
        }
    }


    private fun abandonSpeechAudioFocus() {

        if (
            !hasAudioFocus
        ) {

            audioFocusRequest =
                null

            return
        }


        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                audioFocusRequest
                    ?.let {
                            request ->

                        audioManager
                            .abandonAudioFocusRequest(
                                request
                            )
                    }

            } else {

                @Suppress(
                    "DEPRECATION"
                )
                audioManager
                    .abandonAudioFocus(
                        audioFocusChangeListener
                    )
            }

        } catch (
            exception: Exception
        ) {

            Log.w(
                TAG,
                "Failed to abandon audio focus.",
                exception,
            )

        } finally {

            hasAudioFocus =
                false

            audioFocusRequest =
                null
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


        abandonSpeechAudioFocus()
    }


    fun destroy() {

        stop()
    }
}
