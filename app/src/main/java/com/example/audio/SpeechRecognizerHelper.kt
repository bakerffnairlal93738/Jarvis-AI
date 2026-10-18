package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Android SpeechRecognizer helper with multilingual support (English, Hindi, Urdu)
 * and real-time RMS/amplitude feedback for the HUD visualizer.
 */
class SpeechRecognizerHelper(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private val tag = "SpeechRecognizerHelper"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var selectedLanguageCode = "en"

    fun setLanguage(langCode: String) {
        selectedLanguageCode = langCode
    }

    fun startListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening() }
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service is not available on this device.")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                when (selectedLanguageCode.lowercase()) {
                    "auto" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "ur-PK", "hi-IN", "pa-IN"))
                    }
                    "hi", "hindi" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    }
                    "ur", "urdu" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK")
                    }
                    "pa", "punjabi" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pa-IN")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pa-IN")
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("pa-IN", "ur-PK"))
                    }
                    else -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                    }
                }
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            onListeningStateChanged(true)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start speech recognition: ${e.message}", e)
            onError("Recognition start error: ${e.message}")
            stopListening()
        }
    }

    fun stopListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stopListening() }
            return
        }

        isListening = false
        onListeningStateChanged(false)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(tag, "Error destroying speech recognizer: ${e.message}")
        } finally {
            speechRecognizer = null
            onAmplitudeChanged(0f)
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            onListeningStateChanged(true)
        }

        override fun onBeginningOfSpeech() {
            isListening = true
            onListeningStateChanged(true)
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Convert rmsdB (-2 to 10) to 0.0 .. 1.0 range
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            onAmplitudeChanged(normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            onListeningStateChanged(false)
        }

        override fun onError(error: Int) {
            isListening = false
            onListeningStateChanged(false)
            onAmplitudeChanged(0f)

            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                else -> "Recognition error ($error)"
            }
            Log.w(tag, "Speech recognition error: $errorMsg ($error)")
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                onError(errorMsg)
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            onListeningStateChanged(false)
            onAmplitudeChanged(0f)

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim()
            if (!recognizedText.isNullOrBlank()) {
                onFinalResult(recognizedText)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull()?.trim()
            if (!partialText.isNullOrBlank()) {
                onPartialResult(partialText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
