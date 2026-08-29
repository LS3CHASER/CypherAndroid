package com.shannon.cypher.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class CypherRemoteSpeaker(
    context: Context,
) {

    companion object {

        private const val TAG =
            "CypherAudio"

        private const val DIAGNOSTIC_TAG =
            "CypherAudioDiag"

        private const val PRE_BUFFER_BYTES =
            4_096

        private const val DIAGNOSTIC_LOG_NAME =
            "cypher_audio_diagnostics.log"

        private const val MAX_DIAGNOSTIC_LOG_BYTES =
            1_000_000L

        private const val PLAYBACK_TAIL_GRACE_MS =
            750L
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


    private val diagnosticLogFile =
        File(
            appContext.filesDir,
            DIAGNOSTIC_LOG_NAME,
        )


    private val diagnosticLock =
        Any()


    private var currentSessionId =
        "none"


    private val speechAudioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(
                AudioAttributes.USAGE_MEDIA
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

                    diagnostic(
                        "Audio focus LOST."
                    )

                    stop()
                }


                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {

                    diagnostic(
                        "Audio focus LOST_TRANSIENT."
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

                    diagnostic(
                        "Audio focus LOSS_TRANSIENT_CAN_DUCK."
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

                    diagnostic(
                        "Audio focus GAIN."
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


        currentSessionId =
            System.currentTimeMillis()
                .toString()


        stopRequested =
            false


        val totalStartMillis =
            SystemClock.elapsedRealtime()


        diagnostic(
            "=================================================="
        )

        diagnostic(
            "NEW SPEECH SESSION"
        )

        diagnostic(
            "Speech length: ${text.length} characters"
        )

        diagnosticAudioState(
            "Session start"
        )


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


                diagnostic(
                    "Opening /speak stream..."
                )


                speechStream =
                    apiClient.openSpeechStream(
                        text
                    )


                val streamOpenedAt =
                    SystemClock.elapsedRealtime()


                diagnostic(
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


                diagnostic(
                    "AudioTrack config: " +
                            "sampleRate=$sampleRate, " +
                            "channel=MONO, " +
                            "format=PCM_16BIT, " +
                            "minimumBuffer=$minimumBufferSize, " +
                            "trackBuffer=$audioTrackBufferSize"
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


                diagnostic(
                    "AudioTrack created. " +
                            "state=${audioTrackStateName(track.state)}"
                )

                diagnosticTrackRoute(
                    "Track route after creation",
                    track,
                )


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


                diagnostic(
                    "Initial PCM ready: " +
                            "$preBufferBytes bytes after " +
                            "${firstAudioReadyAt - firstAudioWaitStart} ms"
                )


                if (
                    stopRequested ||
                    preBufferBytes <= 0
                ) {

                    diagnostic(
                        "Playback cancelled before audio start. " +
                                "stopRequested=$stopRequested, " +
                                "preBufferBytes=$preBufferBytes"
                    )

                    return@withContext
                }


                diagnosticAudioState(
                    "Before audio focus request"
                )


                val focusGranted =
                    requestSpeechAudioFocus()


                diagnostic(
                    "Audio focus request: " +
                            if (
                                focusGranted
                            ) {
                                "GRANTED"
                            } else {
                                "NOT GRANTED - playback will still be attempted"
                            }
                )


                diagnosticAudioState(
                    "After audio focus request"
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


                diagnosticTrackRoute(
                    "Track route before play()",
                    track,
                )


                track.play()


                diagnostic(
                    "AudioTrack.play() invoked."
                )


                Thread.sleep(
                    100
                )


                diagnosticTrackRoute(
                    "Track route 100 ms after play()",
                    track,
                )


                diagnosticAudioState(
                    "100 ms after play()"
                )


                withContext(
                    Dispatchers.Main
                ) {

                    onStart()
                }


                diagnostic(
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


                /*
                 * AudioTrack's playback head tells us Android has
                 * consumed the PCM frames, but the final audio can
                 * still be travelling through the device or
                 * Bluetooth output pipeline.
                 *
                 * Give that downstream buffer time to finish before
                 * stop/release so the final words are not clipped.
                 */
                if (
                    !stopRequested
                ) {

                    diagnostic(
                        "Waiting ${PLAYBACK_TAIL_GRACE_MS} ms for output tail to drain."
                    )

                    Thread.sleep(
                        PLAYBACK_TAIL_GRACE_MS
                    )
                }


                diagnosticTrackRoute(
                    "Track route at playback completion",
                    track,
                )


                diagnostic(
                    "Speech playback complete. " +
                            "Total speaker time: " +
                            "${SystemClock.elapsedRealtime() - totalStartMillis} ms"
                )


            } catch (
                exception: Exception
            ) {

                diagnostic(
                    "Remote speaker exception: " +
                            "${exception.javaClass.simpleName}: " +
                            "${exception.message}"
                )


                throw exception

            } finally {

                diagnostic(
                    "Beginning remote speaker cleanup."
                )


                try {

                    speechStream
                        ?.close()

                } catch (
                    exception: Exception
                ) {

                    diagnostic(
                        "Speech stream close failed: " +
                                "${exception.javaClass.simpleName}: " +
                                "${exception.message}"
                    )
                }


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


                diagnosticAudioState(
                    "After playback cleanup"
                )


                withContext(
                    Dispatchers.Main
                ) {

                    onDone()
                }


                diagnostic(
                    "END SPEECH SESSION"
                )

                diagnostic(
                    "=================================================="
                )
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


            diagnostic(
                "Audio focus abandoned."
            )

        } catch (
            exception: Exception
        ) {

            diagnostic(
                "Failed to abandon audio focus: " +
                        "${exception.javaClass.simpleName}: " +
                        "${exception.message}"
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

                diagnostic(
                    "AudioTrack.write() stopped with result=$written"
                )

                break
            }


            offset +=
                written
        }
    }


    fun stop() {

        stopRequested =
            true


        diagnostic(
            "stop() requested."
        )


        diagnosticTrackRoute(
            "Track route when stop() requested",
            audioTrack,
        )


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

        diagnostic(
            "destroy() requested."
        )

        stop()
    }


    private fun diagnosticAudioState(
        label: String,
    ) {

        val mode =
            audioModeName(
                audioManager.mode
            )


        val outputs =
            audioManager
                .getDevices(
                    AudioManager.GET_DEVICES_OUTPUTS
                )
                .joinToString(
                    separator = " | "
                ) {
                        device ->

                    describeDevice(
                        device
                    )
                }
                .ifBlank {
                    "none"
                }


        val communicationDevice =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                audioManager
                    .communicationDevice
                    ?.let {
                        describeDevice(
                            it
                        )
                    }
                    ?: "none"

            } else {

                "not-supported"
            }


        @Suppress(
            "DEPRECATION"
        )
        val bluetoothScoOn =
            audioManager.isBluetoothScoOn


        @Suppress(
            "DEPRECATION"
        )
        val speakerphoneOn =
            audioManager.isSpeakerphoneOn


        diagnostic(
            "$label :: " +
                    "mode=$mode, " +
                    "musicActive=${audioManager.isMusicActive}, " +
                    "speakerphoneOn=$speakerphoneOn, " +
                    "bluetoothScoOn=$bluetoothScoOn, " +
                    "communicationDevice=$communicationDevice"
        )


        diagnostic(
            "$label :: outputDevices=$outputs"
        )
    }


    private fun diagnosticTrackRoute(
        label: String,
        track: AudioTrack?,
    ) {

        if (
            track == null
        ) {

            diagnostic(
                "$label :: track=null"
            )

            return
        }


        val routedDevice =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M
            ) {

                track.routedDevice
                    ?.let {
                        describeDevice(
                            it
                        )
                    }
                    ?: "none"

            } else {

                "not-supported"
            }


        diagnostic(
            "$label :: " +
                    "state=${audioTrackStateName(track.state)}, " +
                    "playState=${audioTrackPlayStateName(track.playState)}, " +
                    "routedDevice=$routedDevice"
        )
    }


    private fun describeDevice(
        device: AudioDeviceInfo,
    ): String {

        val product =
            device.productName
                ?.toString()
                ?.ifBlank {
                    "unknown"
                }
                ?: "unknown"


        val address =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                device.address
                    .ifBlank {
                        "none"
                    }

            } else {

                "unavailable"
            }


        return (
                "${audioDeviceTypeName(device.type)}" +
                        "(id=${device.id}," +
                        "product=$product," +
                        "address=$address)"
                )
    }


    private fun audioDeviceTypeName(
        type: Int,
    ): String {

        return when (
            type
        ) {

            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ->
                "BUILTIN_SPEAKER"

            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE ->
                "BUILTIN_EARPIECE"

            AudioDeviceInfo.TYPE_WIRED_HEADPHONES ->
                "WIRED_HEADPHONES"

            AudioDeviceInfo.TYPE_WIRED_HEADSET ->
                "WIRED_HEADSET"

            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ->
                "BLUETOOTH_A2DP"

            AudioDeviceInfo.TYPE_BLUETOOTH_SCO ->
                "BLUETOOTH_SCO"

            AudioDeviceInfo.TYPE_USB_DEVICE ->
                "USB_DEVICE"

            AudioDeviceInfo.TYPE_USB_HEADSET ->
                "USB_HEADSET"

            AudioDeviceInfo.TYPE_HDMI ->
                "HDMI"

            AudioDeviceInfo.TYPE_DOCK ->
                "DOCK"

            AudioDeviceInfo.TYPE_REMOTE_SUBMIX ->
                "REMOTE_SUBMIX"

            else -> {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
                ) {

                    when (
                        type
                    ) {

                        AudioDeviceInfo.TYPE_BLE_HEADSET ->
                            "BLE_HEADSET"

                        AudioDeviceInfo.TYPE_BLE_SPEAKER ->
                            "BLE_SPEAKER"

                        AudioDeviceInfo.TYPE_BLE_BROADCAST ->
                            "BLE_BROADCAST"

                        else ->
                            "TYPE_$type"
                    }

                } else {

                    "TYPE_$type"
                }
            }
        }
    }


    private fun audioModeName(
        mode: Int,
    ): String {

        return when (
            mode
        ) {

            AudioManager.MODE_NORMAL ->
                "MODE_NORMAL"

            AudioManager.MODE_RINGTONE ->
                "MODE_RINGTONE"

            AudioManager.MODE_IN_CALL ->
                "MODE_IN_CALL"

            AudioManager.MODE_IN_COMMUNICATION ->
                "MODE_IN_COMMUNICATION"

            else ->
                "MODE_$mode"
        }
    }


    private fun audioTrackStateName(
        state: Int,
    ): String {

        return when (
            state
        ) {

            AudioTrack.STATE_INITIALIZED ->
                "INITIALIZED"

            AudioTrack.STATE_UNINITIALIZED ->
                "UNINITIALIZED"

            AudioTrack.STATE_NO_STATIC_DATA ->
                "NO_STATIC_DATA"

            else ->
                "STATE_$state"
        }
    }


    private fun audioTrackPlayStateName(
        state: Int,
    ): String {

        return when (
            state
        ) {

            AudioTrack.PLAYSTATE_STOPPED ->
                "STOPPED"

            AudioTrack.PLAYSTATE_PAUSED ->
                "PAUSED"

            AudioTrack.PLAYSTATE_PLAYING ->
                "PLAYING"

            else ->
                "PLAYSTATE_$state"
        }
    }


    private fun diagnostic(
        message: String,
    ) {

        Log.d(
            DIAGNOSTIC_TAG,
            "[$currentSessionId] $message"
        )


        val timestamp =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS",
                Locale.US,
            ).format(
                Date()
            )


        val line =
            "$timestamp [$currentSessionId] $message\n"


        try {

            synchronized(
                diagnosticLock
            ) {

                if (
                    diagnosticLogFile.exists() &&
                    diagnosticLogFile.length() >
                    MAX_DIAGNOSTIC_LOG_BYTES
                ) {

                    diagnosticLogFile.writeText(
                        "Cypher audio diagnostics log rotated.\n"
                    )
                }


                diagnosticLogFile.appendText(
                    line
                )
            }

        } catch (
            exception: Exception
        ) {

            Log.w(
                TAG,
                "Unable to write persistent audio diagnostic log.",
                exception,
            )
        }
    }
}
