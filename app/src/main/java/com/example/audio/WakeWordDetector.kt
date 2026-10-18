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
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lightweight Wake-Word Detector and Keyword Spotter for hands-free JARVIS initiation.
 * Continuously monitors for "Jarvis", "Hey Jarvis", "OK Jarvis", and multilingual
 * equivalents ("जार्विस", "جاروس") with trailing prompt extraction.
 */
class WakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: (wakeWord: String, trailingCommand: String?) -> Unit,
    private val onSpotterStateChanged: (Boolean) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private val tag = "WakeWordDetector"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isSpotting = false
    @Volatile
    private var isPaused = false

    var isEnabled: Boolean = true
    var sensitivity: Float = 0.5f // 0.1 to 1.0
    var languageCode: String = "en"

    private var restartAttempts = 0
    private var lastDetectionTime = 0L

    companion object {
        // Known phonetic variants and multilingual wake words
        val WAKE_KEYWORDS_ENGLISH = listOf(
            "jarvis", "hey jarvis", "ok jarvis", "okay jarvis",
            "hello jarvis", "hi jarvis", "jarves", "jarviz", "javis"
        )
        val WAKE_KEYWORDS_HINDI = listOf(
            "जार्विस", "हे जार्विस", "ओके जार्विस", "नमस्ते जार्विस", "जार्विस सुनो"
        )
        val WAKE_KEYWORDS_URDU = listOf(
            "جاروس", "ہیلو جاروس", "اوکے جاروس", "جاروس سنو", "اے جاروس"
        )
    }

    /**
     * Start continuous keyword spotting loop.
     */
    fun startSpotting() {
        if (!isEnabled) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startSpotting() }
            return
        }

        if (isSpotting && !isPaused) return
        isPaused = false

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(tag, "SpeechRecognizer not available for wake-word spotting")
            return
        }

        stopInternal()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createSpotterListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                val localeStr = when (languageCode.lowercase()) {
                    "hi", "hindi" -> "hi-IN"
                    "ur", "urdu" -> "ur-PK"
                    else -> "en-US"
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeStr)
                // Optimize for quick spotting
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            }

            speechRecognizer?.startListening(intent)
            isSpotting = true
            onSpotterStateChanged(true)
            Log.d(tag, "WakeWord spotting started in language: $languageCode")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start wake-word spotter: ${e.message}", e)
            isSpotting = false
            onSpotterStateChanged(false)
        }
    }

    /**
     * Temporarily pause spotting (e.g. while JARVIS is actively in a session or speaking).
     */
    fun pauseSpotting() {
        if (!isSpotting) return
        isPaused = true
        stopInternal()
        onSpotterStateChanged(false)
        Log.d(tag, "WakeWord spotting paused")
    }

    /**
     * Resume spotting when JARVIS returns to idle state.
     */
    fun resumeSpotting() {
        if (!isEnabled) return
        isPaused = false
        mainHandler.postDelayed({
            if (isEnabled && !isSpotting) {
                startSpotting()
            }
        }, 500)
    }

    /**
     * Permanently stop spotting.
     */
    fun stopSpotting() {
        isPaused = false
        isSpotting = false
        stopInternal()
        onSpotterStateChanged(false)
        Log.d(tag, "WakeWord spotting stopped")
    }

    private fun stopInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(tag, "Error releasing spotter: ${e.message}")
        } finally {
            speechRecognizer = null
        }
    }

    /**
     * Core Keyword Matching & Trailing Command Extraction.
     * Returns Pair(isMatched: Boolean, trailingCommand: String?)
     */
    fun checkForWakeWord(text: String): Pair<Boolean, String?> {
        val normalized = text.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return Pair(false, null)

        val allWakeWords = WAKE_KEYWORDS_ENGLISH + WAKE_KEYWORDS_HINDI + WAKE_KEYWORDS_URDU

        for (wakeWord in allWakeWords) {
            val idx = normalized.indexOf(wakeWord.lowercase(Locale.ROOT))
            if (idx != -1) {
                // Found wake word! Extract trailing command if present
                val afterWakeWord = normalized.substring(idx + wakeWord.length).trim()
                // Strip common leading punctuation
                val cleanedCommand = afterWakeWord.trimStart(',', ':', '.', '!', '؟', '۔', ' ')
                val finalTrailing = cleanedCommand.ifBlank { null }
                return Pair(true, finalTrailing)
            }
        }

        // Fuzzy phoneme fallback for "jarvis" / "जार्विस" / "جاروس"
        val words = normalized.split("\\s+".toRegex())
        for (i in words.indices) {
            val w = words[i].replace("[^a-zA-Z0-9\u0600-\u06FF\u0900-\u097F]".toRegex(), "")
            if (isPhoneticallyJarvis(w)) {
                val trailing = words.drop(i + 1).joinToString(" ").trim().ifBlank { null }
                return Pair(true, trailing)
            }
        }

        return Pair(false, null)
    }

    private fun isPhoneticallyJarvis(word: String): Boolean {
        if (word.length < 4) return false
        val w = word.lowercase(Locale.ROOT)
        return when {
            w == "jarvis" || w == "jarvis" || w == "jarwes" || w == "jarviz" || w == "javis" || w == "jarves" -> true
            w == "जार्विस" || w == "जार्विश" || w == "जारविस" -> true
            w == "جاروس" || w == "جاروس" || w == "جارویز" -> true
            // Simple Levenshtein distance <= 1 for "jarvis"
            levenshteinDistance(w, "jarvis") <= 1 -> true
            else -> false
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Acoustic energy and cadence spotter on raw PCM buffer.
     * Evaluates short-term energy bursts to detect speech syllables.
     */
    fun analyzePcmChunk(pcmData: ByteArray): Float {
        if (pcmData.isEmpty()) return 0f
        var sumSquare = 0.0
        val numSamples = pcmData.size / 2
        for (i in 0 until numSamples) {
            val sample = (pcmData[i * 2 + 1].toInt() shl 8) or (pcmData[i * 2].toInt() and 0xFF)
            sumSquare += (sample * sample).toDouble()
        }
        val rms = sqrt(sumSquare / numSamples).toFloat()
        return (rms / 32768f).coerceIn(0f, 1f)
    }

    private fun handleHypothesis(text: String) {
        val now = System.currentTimeMillis()
        if (now - lastDetectionTime < 1800) {
            // Debounce rapid re-detections
            return
        }

        val (matched, trailing) = checkForWakeWord(text)
        if (matched) {
            lastDetectionTime = now
            Log.i(tag, "Wake word DETECTED! Trailing command: '$trailing'")
            pauseSpotting()
            onWakeWordDetected("JARVIS", trailing)
        }
    }

    private fun restartSpotter() {
        if (!isEnabled || isPaused) return
        mainHandler.postDelayed({
            if (isEnabled && !isPaused) {
                startSpotting()
            }
        }, 300)
    }

    private fun createSpotterListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            // Expected timeout codes when user hasn't said wake word yet
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Normal idle timeout in spotter loop; smoothly restart
                    restartSpotter()
                }
                SpeechRecognizer.ERROR_AUDIO,
                SpeechRecognizer.ERROR_CLIENT,
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    // Back off slightly on transient errors
                    restartAttempts++
                    val delayMs = if (restartAttempts > 3) 2000L else 500L
                    mainHandler.postDelayed({ restartSpotter() }, delayMs)
                }
                else -> {
                    restartSpotter()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val candidate = matches[0]
                handleHypothesis(candidate)
            }
            if (isSpotting && !isPaused) {
                restartSpotter()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!partials.isNullOrEmpty()) {
                val candidate = partials[0]
                handleHypothesis(candidate)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
