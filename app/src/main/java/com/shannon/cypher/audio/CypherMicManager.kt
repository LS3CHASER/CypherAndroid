package com.shannon.cypher.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlin.math.sqrt


class CypherMicManager {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var keepRecording = false

    private val mainHandler = Handler(
        Looper.getMainLooper()
    )


    @SuppressLint("MissingPermission")
    fun start(
        onLevelChanged: (Float) -> Unit,
    ) {

        if (keepRecording) {
            return
        }

        val sampleRate = 16000

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        if (bufferSize <= 0) {
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )

        keepRecording = true

        audioRecord?.startRecording()

        recordingThread = Thread {

            val buffer = ShortArray(
                bufferSize
            )

            while (keepRecording) {

                val samplesRead =
                    audioRecord?.read(
                        buffer,
                        0,
                        buffer.size,
                    ) ?: 0

                if (samplesRead > 0) {

                    var sum = 0.0

                    for (index in 0 until samplesRead) {

                        val sample =
                            buffer[index].toDouble()

                        sum += sample * sample
                    }

                    val rms = sqrt(
                        sum / samplesRead
                    )

                    val normalisedLevel =
                        (rms / 6000.0)
                            .coerceIn(
                                0.0,
                                1.0,
                            )
                            .toFloat()

                    mainHandler.post {

                        onLevelChanged(
                            normalisedLevel
                        )
                    }
                }
            }
        }

        recordingThread?.start()
    }


    fun stop() {

        keepRecording = false

        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        }

        audioRecord?.release()
        audioRecord = null

        recordingThread?.interrupt()
        recordingThread = null
    }
}