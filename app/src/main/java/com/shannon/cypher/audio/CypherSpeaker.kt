package com.shannon.cypher.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID


class CypherSpeaker(
    context: Context,
) : TextToSpeech.OnInitListener {

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private var textToSpeech: TextToSpeech? =
        TextToSpeech(
            context.applicationContext,
            this,
        )

    private var isReady = false

    private var pendingSpeech: PendingSpeech? =
        null

    private var currentOnStart:
            (() -> Unit)? = null

    private var currentOnDone:
            (() -> Unit)? = null


    private data class PendingSpeech(
        val text: String,
        val onStart: () -> Unit,
        val onDone: () -> Unit,
    )


    init {

        textToSpeech
            ?.setOnUtteranceProgressListener(
                object :
                    UtteranceProgressListener() {

                    override fun onStart(
                        utteranceId: String?,
                    ) {

                        mainHandler.post {

                            currentOnStart
                                ?.invoke()
                        }
                    }


                    override fun onDone(
                        utteranceId: String?,
                    ) {

                        mainHandler.post {

                            currentOnDone
                                ?.invoke()

                            clearCallbacks()
                        }
                    }


                    @Deprecated(
                        "Deprecated in Java"
                    )
                    override fun onError(
                        utteranceId: String?,
                    ) {

                        finishSpeech()
                    }


                    override fun onError(
                        utteranceId: String?,
                        errorCode: Int,
                    ) {

                        finishSpeech()
                    }
                }
            )
    }


    override fun onInit(
        status: Int,
    ) {

        if (
            status !=
            TextToSpeech.SUCCESS
        ) {

            isReady = false
            return
        }


        val preferredVoice =
            textToSpeech
                ?.voices
                ?.firstOrNull { voice ->

                    voice.name ==
                            "en-gb-x-gbd-local"
                }


        if (
            preferredVoice != null
        ) {

            textToSpeech
                ?.voice =
                preferredVoice

        } else {

            textToSpeech
                ?.language =
                Locale.UK
        }


        textToSpeech
            ?.setSpeechRate(
                0.90f
            )


        textToSpeech
            ?.setPitch(
                0.98f
            )


        isReady = true


        pendingSpeech
            ?.let { pending ->

                pendingSpeech = null

                speak(
                    text =
                        pending.text,

                    onStart =
                        pending.onStart,

                    onDone =
                        pending.onDone,
                )
            }
    }


    fun speak(
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {

        if (
            text.isBlank()
        ) {

            return
        }


        if (
            !isReady
        ) {

            pendingSpeech =
                PendingSpeech(
                    text = text,
                    onStart = onStart,
                    onDone = onDone,
                )

            return
        }


        currentOnStart =
            onStart

        currentOnDone =
            onDone


        val utteranceId =
            UUID
                .randomUUID()
                .toString()


        textToSpeech
            ?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId,
            )
    }


    fun stop() {

        pendingSpeech = null

        textToSpeech
            ?.stop()

        clearCallbacks()
    }


    fun destroy() {

        pendingSpeech = null

        textToSpeech
            ?.stop()

        textToSpeech
            ?.shutdown()

        textToSpeech = null

        isReady = false

        clearCallbacks()
    }


    private fun finishSpeech() {

        mainHandler.post {

            currentOnDone
                ?.invoke()

            clearCallbacks()
        }
    }


    private fun clearCallbacks() {

        currentOnStart = null
        currentOnDone = null
    }
}