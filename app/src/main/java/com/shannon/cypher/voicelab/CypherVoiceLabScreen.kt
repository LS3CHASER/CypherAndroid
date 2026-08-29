package com.shannon.cypher.voicelab

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val TAG =
    "CypherVoiceLab"

private const val SAMPLE_RATE =
    24_000

private const val PLAYBACK_TAIL_GRACE_MS =
    750L


@Composable
fun CypherVoiceLabScreen(
    modifier: Modifier = Modifier,
) {

    val client =
        remember {
            CypherVoiceLabClient()
        }


    val coroutineScope =
        rememberCoroutineScope()


    var playingCandidateId by
    remember {
        mutableStateOf<String?>(
            null
        )
    }


    var statusText by
    remember {
        mutableStateOf(
            "Select a voice to begin."
        )
    }


    var playbackJob by
    remember {
        mutableStateOf<Job?>(
            null
        )
    }


    fun playCandidate(
        candidate: CypherVoiceCandidate,
    ) {

        /*
         * Prevent two Voice Lab candidates from
         * playing over each other.
         */
        if (
            playbackJob?.isActive ==
            true
        ) {

            return
        }


        playbackJob =
            coroutineScope.launch {

                playingCandidateId =
                    candidate.id


                statusText =
                    "Loading ${candidate.title}..."


                try {

                    withContext(
                        Dispatchers.IO
                    ) {

                        playVoiceCandidate(
                            client =
                                client,

                            candidate =
                                candidate,
                        )
                    }


                    statusText =
                        "Finished ${candidate.title}."


                } catch (
                    exception: Exception
                ) {

                    Log.e(
                        TAG,
                        "Voice Lab playback failed for " +
                                candidate.id,
                        exception,
                    )


                    statusText =
                        "Unable to play ${candidate.title}."


                } finally {

                    playingCandidateId =
                        null
                }
            }
    }


    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    20.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            ),
    ) {

        Text(
            text =
                "CYPHER VOICE LAB",

            style =
                MaterialTheme.typography
                    .headlineMedium,
        )


        Text(
            text =
                "Audition experimental Cypher voices without " +
                        "changing the live assistant voice.",

            style =
                MaterialTheme.typography
                    .bodyMedium,
        )


        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),

            colors =
                CardDefaults.cardColors(),
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

                Text(
                    text =
                        "TEST PHRASE",

                    style =
                        MaterialTheme.typography
                            .labelMedium,
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                Text(
                    text =
                        CypherVoiceCandidates.TEST_PHRASE,

                    style =
                        MaterialTheme.typography
                            .bodyLarge,
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )


        CypherVoiceCandidates
            .openAiCandidates
            .forEach {
                    candidate ->

                VoiceCandidateCard(
                    candidate =
                        candidate,

                    isPlaying =
                        playingCandidateId ==
                                candidate.id,

                    enabled =
                        playingCandidateId ==
                                null,

                    onPlay = {

                        playCandidate(
                            candidate
                        )
                    },
                )
            }


        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically,
        ) {

            if (
                playingCandidateId !=
                null
            ) {

                CircularProgressIndicator()


                Spacer(
                    modifier =
                        Modifier.padding(
                            6.dp
                        )
                )
            }


            Text(
                text =
                    statusText,

                style =
                    MaterialTheme.typography
                        .bodyMedium,
            )
        }
    }
}


@Composable
private fun VoiceCandidateCard(
    candidate: CypherVoiceCandidate,
    isPlaying: Boolean,
    enabled: Boolean,
    onPlay: () -> Unit,
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(),
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Text(
                text =
                    candidate.title,

                style =
                    MaterialTheme.typography
                        .titleLarge,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        3.dp
                    )
            )


            Text(
                text =
                    candidate.subtitle,

                style =
                    MaterialTheme.typography
                        .labelLarge,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Text(
                text =
                    candidate.description,

                style =
                    MaterialTheme.typography
                        .bodyMedium,
            )


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            Button(
                onClick =
                    onPlay,

                enabled =
                    enabled,

                modifier =
                    Modifier.fillMaxWidth(),
            ) {

                Text(
                    text =
                        if (
                            isPlaying
                        ) {

                            "PLAYING..."

                        } else {

                            "PLAY VOICE"
                        }
                )
            }
        }
    }
}


private fun playVoiceCandidate(
    client: CypherVoiceLabClient,
    candidate: CypherVoiceCandidate,
) {

    val speechStream =
        client.openCandidateSpeechStream(
            candidate =
                candidate,

            text =
                CypherVoiceCandidates
                    .TEST_PHRASE,
        )


    var audioTrack: AudioTrack? =
        null


    try {

        val minimumBufferSize =
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )


        if (
            minimumBufferSize <=
            0
        ) {

            throw RuntimeException(
                "Unable to determine AudioTrack buffer size."
            )
        }


        val audioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(
                    AudioAttributes.USAGE_MEDIA
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SPEECH
                )
                .build()


        val audioFormat =
            AudioFormat
                .Builder()
                .setEncoding(
                    AudioFormat.ENCODING_PCM_16BIT
                )
                .setSampleRate(
                    SAMPLE_RATE
                )
                .setChannelMask(
                    AudioFormat.CHANNEL_OUT_MONO
                )
                .build()


        audioTrack =
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    audioAttributes
                )
                .setAudioFormat(
                    audioFormat
                )
                .setBufferSizeInBytes(
                    maxOf(
                        minimumBufferSize * 4,
                        32_768,
                    )
                )
                .setTransferMode(
                    AudioTrack.MODE_STREAM
                )
                .build()


        audioTrack.play()


        Log.d(
            TAG,
            "Playing Voice Lab candidate " +
                    candidate.id
        )


        val buffer =
            ByteArray(
                8_192
            )


        while (
            true
        ) {

            val bytesRead =
                speechStream.inputStream
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

                var offset =
                    0


                while (
                    offset <
                    bytesRead
                ) {

                    val bytesWritten =
                        audioTrack.write(
                            buffer,
                            offset,
                            bytesRead - offset,
                        )


                    if (
                        bytesWritten <
                        0
                    ) {

                        throw RuntimeException(
                            "AudioTrack write failed: " +
                                    bytesWritten
                        )
                    }


                    offset +=
                        bytesWritten
                }
            }
        }


        /*
         * Keep the track alive briefly after the final
         * network chunk so Bluetooth does not cut off
         * the end of the sentence.
         *
         * This mirrors the working Cypher playback-tail
         * behaviour.
         */
        Thread.sleep(
            PLAYBACK_TAIL_GRACE_MS
        )


        Log.d(
            TAG,
            "Voice Lab candidate ${candidate.id} finished."
        )


    } finally {

        try {

            audioTrack?.stop()

        } catch (
            _: Exception
        ) {

            // Ignore stop errors during cleanup.
        }


        try {

            audioTrack?.release()

        } catch (
            _: Exception
        ) {

            // Ignore release errors during cleanup.
        }


        speechStream.close()
    }
}