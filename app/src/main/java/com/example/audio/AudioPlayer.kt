package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Handles real-time PCM audio playback (24kHz from Gemini Live API)
 * as well as device Text-to-Speech with full multilingual (Hindi, English, Urdu) support,
 * voice output muting, pitch/rate controls, and amplitude animation.
 */
class AudioPlayer(
    private val context: Context,
    private val onPlaybackStarted: () -> Unit = {},
    private val onPlaybackFinished: () -> Unit = {},
    private val onAmplitude: (Float) -> Unit = {}
) {
    private val tag = "AudioPlayer"
    private val sampleRate = 24000 // Gemini Live output sample rate: 24kHz

    private var audioTrack: AudioTrack? = null
    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private var playbackJob: Job? = null
    private var ttsAmplitudeJob: Job? = null
    @Volatile
    private var isPlaying = false

    // Device TextToSpeech fallback
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var currentLanguageCode: String = "en"
    var isVoiceOutputEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                stopPlayback()
            }
        }
    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 1.05f
    private var speechVolume: Float = 1.0f

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(speechPitch)
                applyTtsLanguage(currentLanguageCode)
                setupTtsListener()
            } else {
                Log.e(tag, "TTS initialization failed: $status")
            }
        }
    }

    private fun setupTtsListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isPlaying = true
                onPlaybackStarted()
            }

            override fun onDone(utteranceId: String?) {
                isPlaying = false
                ttsAmplitudeJob?.cancel()
                ttsAmplitudeJob = null
                onPlaybackFinished()
                onAmplitude(0f)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isPlaying = false
                ttsAmplitudeJob?.cancel()
                ttsAmplitudeJob = null
                onPlaybackFinished()
                onAmplitude(0f)
            }
        })
    }

    fun setLanguage(langCode: String) {
        currentLanguageCode = langCode
        applyTtsLanguage(langCode)
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun setSpeechPitch(pitch: Float) {
        speechPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(speechPitch)
    }

    fun setSpeechVolume(volume: Float) {
        speechVolume = volume.coerceIn(0.05f, 1.0f)
        try {
            audioTrack?.setVolume(speechVolume)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (maxVol * speechVolume).toInt().coerceIn(1, maxVol)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            }
        } catch (e: Exception) {
            Log.w(tag, "AudioPlayer setSpeechVolume error: ${e.message}")
        }
    }

    fun getSpeechVolume(): Float = speechVolume

    /**
     * Plays a pleasant, futuristic two-tone sci-fi wake-word chime (880Hz -> 1320Hz).
     */
    fun playWakeChime() {
        try {
            val sRate = 24000
            val durationMs = 180
            val numSamples = (sRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val freq = if (i < numSamples / 2) 880.0 else 1320.0
                val angle = 2.0 * Math.PI * i / (sRate / freq)
                val envelope = Math.sin(Math.PI * i / numSamples)
                buffer[i] = (Math.sin(angle) * envelope * 12000).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
        } catch (e: Exception) {
            Log.w(tag, "Chime play error: ${e.message}")
        }
    }

    enum class VoiceGender {
        MALE, FEMALE
    }

    private var currentVoiceGender: VoiceGender = VoiceGender.MALE

    fun setVoiceGender(gender: VoiceGender) {
        currentVoiceGender = gender
        if (gender == VoiceGender.FEMALE) {
            speechPitch = 1.25f
        } else {
            speechPitch = 0.92f
        }
        applyTtsVoiceAndPitch()
    }

    fun getVoiceGender(): VoiceGender = currentVoiceGender

    private fun applyTtsVoiceAndPitch() {
        if (!isTtsInitialized) return
        try {
            val voices = tts?.voices
            if (!voices.isNullOrEmpty()) {
                val targetKeyword = if (currentVoiceGender == VoiceGender.FEMALE) "female" else "male"
                val match = voices.firstOrNull { voice ->
                    voice.name.contains(targetKeyword, ignoreCase = true) &&
                    (voice.locale.language.equals(currentLanguageCode, ignoreCase = true) || voice.locale.language == Locale.getDefault().language)
                } ?: voices.firstOrNull { voice ->
                    voice.name.contains(targetKeyword, ignoreCase = true)
                }
                if (match != null) {
                    tts?.voice = match
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "Could not set custom TTS voice: ${e.message}")
        }
        tts?.setPitch(speechPitch)
        tts?.setSpeechRate(speechRate)
    }

    fun detectLanguage(text: String): String {
        var urduArabicCount = 0
        var devanagariCount = 0
        var gurmukhiCount = 0
        var latinCount = 0

        for (ch in text) {
            when (ch) {
                in '\u0600'..'\u06FF', in '\u0750'..'\u077F', in '\uFB50'..'\uFDFF', in '\uFE70'..'\uFEFF' -> urduArabicCount++
                in '\u0900'..'\u097F' -> devanagariCount++
                in '\u0A00'..'\u0A7F' -> gurmukhiCount++
                in 'a'..'z', in 'A'..'Z' -> latinCount++
            }
        }

        if (gurmukhiCount > 0 && gurmukhiCount >= devanagariCount && gurmukhiCount >= urduArabicCount) {
            return "pa"
        }

        val lower = text.lowercase()
        val isPunjabiShahmukhi = lower.contains("کی حال") || lower.contains("کی کر رہے") || lower.contains("تسی") ||
                lower.contains("تہاڈا") || lower.contains("اساں") || lower.contains("کیتا") ||
                lower.contains("ہانجی") || lower.contains("چنگا") || lower.contains("ہور") ||
                lower.contains("پیا اے") || lower.contains("پئی اے") || lower.contains("کجھ") ||
                lower.contains("کیویں") || lower.contains("کتھے")

        if (isPunjabiShahmukhi) {
            return "pa"
        }

        val isRomanPunjabi = lower.contains("ki haal") || lower.contains("tussi") || lower.contains("change") ||
                lower.contains("hor dasso") || lower.contains("kiddan") || lower.contains("sat sri akal")

        if (isRomanPunjabi) {
            return "pa"
        }

        return when {
            devanagariCount > urduArabicCount && devanagariCount > latinCount -> "hi"
            urduArabicCount > devanagariCount && urduArabicCount > latinCount -> "ur"
            else -> "en"
        }
    }

    private fun applyTtsLanguage(langCode: String) {
        if (!isTtsInitialized) return
        val target = if (langCode.equals("auto", ignoreCase = true)) "en" else langCode.lowercase()
        val locale = when (target) {
            "hi", "hindi" -> Locale.forLanguageTag("hi-IN")
            "ur", "urdu" -> Locale.forLanguageTag("ur-PK")
            "pa", "punjabi" -> Locale.forLanguageTag("pa-IN")
            "ar", "arabic" -> Locale.forLanguageTag("ar-SA")
            "es", "spanish" -> Locale.forLanguageTag("es-ES")
            "fr", "french" -> Locale.forLanguageTag("fr-FR")
            "de", "german" -> Locale.forLanguageTag("de-DE")
            else -> Locale.US
        }

        var result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (target == "pa") {
                // Fallback to Hindi or Urdu engine voice which produces close phonetic sound for Punjabi
                result = tts?.setLanguage(Locale.forLanguageTag("hi-IN"))
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(tag, "Language $target not fully supported by device TTS, falling back to default")
                tts?.setLanguage(Locale.US)
            }
        }
        applyTtsVoiceAndPitch()
    }

    /**
     * Prepares AudioTrack for 24kHz 16-bit PCM streaming.
     */
    private fun ensureAudioTrack(): AudioTrack {
        audioTrack?.let {
            if (it.state == AudioTrack.STATE_INITIALIZED) return it
        }

        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 4800) // ~100ms at 24kHz

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        track.play()
        try {
            track.setVolume(speechVolume)
        } catch (e: Exception) {
            Log.w(tag, "AudioTrack initial setVolume error: ${e.message}")
        }
        audioTrack = track
        return track
    }

    /**
     * Enqueues incoming PCM chunks (24kHz, 16-bit LE) from Gemini Live and plays them immediately.
     */
    fun enqueueAudioChunk(pcmChunk: ByteArray, coroutineScope: CoroutineScope) {
        if (!isVoiceOutputEnabled) return

        audioQueue.offer(pcmChunk)

        if (!isPlaying) {
            startPlaybackLoop(coroutineScope)
        }
    }

    private fun startPlaybackLoop(coroutineScope: CoroutineScope) {
        isPlaying = true
        onPlaybackStarted()

        playbackJob = coroutineScope.launch(Dispatchers.IO) {
            val track = ensureAudioTrack()
            try {
                while (isActive && isPlaying) {
                    val chunk = audioQueue.poll(150, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (chunk != null) {
                        track.write(chunk, 0, chunk.size)

                        // Compute RMS amplitude for playback animation
                        val amp = calculateNormalizedRms(chunk, chunk.size)
                        onAmplitude(amp)
                    } else {
                        // Queue drained
                        if (audioQueue.isEmpty()) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "AudioTrack playback error: ${e.message}", e)
            } finally {
                isPlaying = false
                onPlaybackFinished()
                onAmplitude(0f)
            }
        }
    }

    /**
     * Implements speakResponse(text) using device SpeechSynthesis (TTS):
     * 1. Checks if voice output is enabled.
     * 2. Auto-detects Hindi/Urdu/English script.
     * 3. Speaks through device speakers.
     * 4. Drives real-time animated waveform during speech.
     */
    fun speakResponse(text: String, coroutineScope: CoroutineScope? = null) {
        stopPlayback()
        if (text.isBlank() || !isVoiceOutputEnabled) return

        val targetLang = when {
            currentLanguageCode.equals("auto", ignoreCase = true) -> detectLanguage(text)
            text.any { it in '\u0A00'..'\u0A7F' } -> "pa"
            detectLanguage(text) == "pa" && currentLanguageCode != "hi" && currentLanguageCode != "ur" -> "pa"
            text.any { it in '\u0900'..'\u097F' } && currentLanguageCode == "en" -> "hi"
            text.any { it in '\u0600'..'\u06FF' } && currentLanguageCode == "en" -> "ur"
            else -> currentLanguageCode
        }
        applyTtsLanguage(targetLang)

        if (isTtsInitialized) {
            val params = android.os.Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, speechVolume)
            val utteranceId = "JARVIS_${System.currentTimeMillis()}"

            // Start simulated amplitude waveform during TTS speaking
            coroutineScope?.let { scope ->
                ttsAmplitudeJob?.cancel()
                ttsAmplitudeJob = scope.launch(Dispatchers.Default) {
                    while (isActive && isPlaying) {
                        val baseAmp = 0.35f + Random.nextFloat() * 0.45f
                        onAmplitude(baseAmp)
                        delay(90)
                    }
                    onAmplitude(0f)
                }
            }

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            Log.w(tag, "TTS not ready yet")
        }
    }

    /**
     * Barge-in / Interrupt: Stops all audio immediately, flushes queues, and resets hardware.
     */
    fun stopPlayback() {
        isPlaying = false
        audioQueue.clear()
        playbackJob?.cancel()
        playbackJob = null
        ttsAmplitudeJob?.cancel()
        ttsAmplitudeJob = null

        // Stop AudioTrack
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.pause()
                    it.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping AudioTrack: ${e.message}")
        }

        // Stop TTS
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS: ${e.message}")
        }

        onPlaybackFinished()
        onAmplitude(0f)
    }

    fun isCurrentlySpeaking(): Boolean {
        return isPlaying || (tts?.isSpeaking == true)
    }

    fun release() {
        stopPlayback()
        try {
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(tag, "Error releasing AudioTrack: ${e.message}")
        }
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS: ${e.message}")
        }
    }

    private fun calculateNormalizedRms(pcmBytes: ByteArray, bytesRead: Int): Float {
        val shortBuffer = ByteBuffer.wrap(pcmBytes, 0, bytesRead)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()

        var sumSquare = 0.0
        val numSamples = shortBuffer.remaining()
        if (numSamples == 0) return 0f

        while (shortBuffer.hasRemaining()) {
            val sample = shortBuffer.get().toDouble()
            sumSquare += sample * sample
        }

        val rms = sqrt(sumSquare / numSamples)
        val normalized = (rms / 32767.0).toFloat()
        return normalized.coerceIn(0f, 1f)
    }
}
