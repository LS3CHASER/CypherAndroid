package com.shannon.cypher.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer


class CypherSpeechRecognizer(
    context: Context,
) {

    private val speechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(
            context.applicationContext
        )

    private var onListeningChanged:
            ((Boolean) -> Unit)? = null

    private var onLevelChanged:
            ((Float) -> Unit)? = null

    private var onTextRecognized:
            ((String) -> Unit)? = null


    init {

        speechRecognizer.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?,
                ) {

                    onListeningChanged?.invoke(
                        true
                    )
                }


                override fun onBeginningOfSpeech() {
                }


                override fun onRmsChanged(
                    rmsdB: Float,
                ) {

                    val normalizedLevel =
                        ((rmsdB + 2f) / 12f)
                            .coerceIn(
                                0f,
                                1f,
                            )

                    onLevelChanged?.invoke(
                        normalizedLevel
                    )
                }


                override fun onBufferReceived(
                    buffer: ByteArray?,
                ) {
                }


                override fun onEndOfSpeech() {

                    onLevelChanged?.invoke(
                        0f
                    )
                }


                override fun onError(
                    error: Int,
                ) {

                    onLevelChanged?.invoke(
                        0f
                    )

                    onListeningChanged?.invoke(
                        false
                    )
                }


                override fun onResults(
                    results: Bundle?,
                ) {

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val recognizedText =
                        matches
                            ?.firstOrNull()
                            .orEmpty()

                    onTextRecognized?.invoke(
                        recognizedText
                    )

                    onLevelChanged?.invoke(
                        0f
                    )

                    onListeningChanged?.invoke(
                        false
                    )
                }


                override fun onPartialResults(
                    partialResults: Bundle?,
                ) {
                }


                override fun onEvent(
                    eventType: Int,
                    params: Bundle?,
                ) {
                }
            }
        )
    }


    fun start(
        onListeningChanged:
            (Boolean) -> Unit,
        onLevelChanged:
            (Float) -> Unit,
        onTextRecognized:
            (String) -> Unit,
    ) {

        this.onListeningChanged =
            onListeningChanged

        this.onLevelChanged =
            onLevelChanged

        this.onTextRecognized =
            onTextRecognized


        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-AU",
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true,
            )

            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3,
            )
        }


        speechRecognizer.startListening(
            intent
        )
    }


    fun stop() {

        speechRecognizer.stopListening()
    }


    fun cancel() {

        speechRecognizer.cancel()

        onLevelChanged?.invoke(
            0f
        )

        onListeningChanged?.invoke(
            false
        )
    }


    fun destroy() {

        speechRecognizer.destroy()
    }
}
