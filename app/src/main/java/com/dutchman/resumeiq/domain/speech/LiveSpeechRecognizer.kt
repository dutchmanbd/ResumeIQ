package com.dutchman.resumeiq.domain.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

class LiveSpeechRecognizer(private val context: Context) {

    private val appContext: Context = context.applicationContext

    fun startListening(localeTag: String = Locale.getDefault().toLanguageTag()): Flow<SpeechEvent> =
        callbackFlow {
            val mainHandler = Handler(Looper.getMainLooper())
            var recognizer: SpeechRecognizer? = null
            var stopped = false

            fun buildIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            }

            fun destroyRecognizer() {
                mainHandler.post {
                    try { recognizer?.destroy() } catch (_: Exception) {}
                    recognizer = null
                }
            }

            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = extractBestText(partialResults)
                    if (text.isNotEmpty()) {
                        trySend(SpeechEvent.Partial(text))
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = extractBestText(results)
                    if (text.isNotEmpty()) {
                        trySend(SpeechEvent.Final(text))
                    }
                    if (!stopped) {
                        mainHandler.postDelayed({
                            if (!stopped && recognizer != null) {
                                try {
                                    recognizer?.startListening(buildIntent())
                                } catch (_: Exception) {}
                            }
                        }, RESTART_DELAY_MS)
                    }
                }

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (!stopped && isTransientError(error)) {
                        mainHandler.postDelayed({
                            if (!stopped && recognizer != null) {
                                try {
                                    recognizer?.startListening(buildIntent())
                                } catch (_: Exception) {}
                            }
                        }, RESTART_DELAY_MS)
                    } else if (!stopped) {
                        trySend(SpeechEvent.Error(error))
                        close()
                    }
                }
            }

            mainHandler.post {
                if (stopped) return@post
                val sr = SpeechRecognizer.createSpeechRecognizer(appContext)
                recognizer = sr
                sr.setRecognitionListener(listener)
                try {
                    sr.startListening(buildIntent())
                } catch (e: Exception) {
                    trySend(SpeechEvent.Error(SpeechRecognizer.ERROR_CLIENT))
                    close()
                }
            }

            awaitClose {
                stopped = true
                destroyRecognizer()
            }
        }

    private fun extractBestText(results: Bundle?): String =
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

    private fun isTransientError(error: Int): Boolean = error in setOf(
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
    )

    companion object {
        private const val RESTART_DELAY_MS = 100L
    }
}

sealed class SpeechEvent {
    data class Partial(val text: String) : SpeechEvent()
    data class Final(val text: String) : SpeechEvent()
    data class Error(val code: Int) : SpeechEvent()
    data object Done : SpeechEvent()
}
