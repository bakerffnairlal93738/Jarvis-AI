package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Captures microphone audio as 16kHz, 16-bit Mono Linear PCM in little-endian format,
 * perfectly matching Google Gemini Multimodal Live API specifications.
 * Features hardware NoiseSuppressor, AcousticEchoCanceler, and intelligent software DSP noise gate.
 */
class AudioRecorder(
    private val sampleRate: Int = 16000,
    private val onAudioChunk: (ByteArray) -> Unit,
    private val onAmplitude: (Float) -> Unit,
    private val onBargeInDetected: () -> Unit = {}
) {
    private val tag = "AudioRecorder"
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    // Background noise suppression settings
    var isNoiseSuppressionEnabled: Boolean = true
    var noiseSuppressionLevel: Float = 0.70f // 0.10f (mild) to 1.0f (aggressive)

    // Barge-in detection threshold
    private var bargeInThreshold = 0.35f
    var isAssistantSpeaking = false

    // High-pass filter state to remove sub-audio rumble (< 80Hz)
    private var prevSample = 0f
    private var prevFiltered = 0f

    fun setNoiseSuppression(enabled: Boolean, level: Float = noiseSuppressionLevel) {
        isNoiseSuppressionEnabled = enabled
        noiseSuppressionLevel = level.coerceIn(0.1f, 1.0f)
        try {
            noiseSuppressor?.enabled = enabled
            echoCanceler?.enabled = enabled
        } catch (e: Exception) {
            Log.w(tag, "Failed to toggle hardware noise suppressor: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording(coroutineScope: CoroutineScope) {
        if (isRecording) return

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 3200) // ~100ms chunks at 16kHz

        // Use VOICE_RECOGNITION for built-in OS beamforming noise rejection if enabled, fallback to MIC
        val audioSource = if (isNoiseSuppressionEnabled) {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        } else {
            MediaRecorder.AudioSource.MIC
        }

        try {
            audioRecord = try {
                AudioRecord(
                    audioSource,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            } catch (e: Exception) {
                // Fallback to standard MIC if VOICE_RECOGNITION fails on device
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord initialization failed")
                audioRecord?.release()
                audioRecord = null
                return
            }

            // Attach hardware NoiseSuppressor and AcousticEchoCanceler if available
            val sessionId = audioRecord?.audioSessionId ?: 0
            if (sessionId != 0 && isNoiseSuppressionEnabled) {
                if (NoiseSuppressor.isAvailable()) {
                    try {
                        noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                            enabled = true
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "NoiseSuppressor init error: ${e.message}")
                    }
                }
                if (AcousticEchoCanceler.isAvailable()) {
                    try {
                        echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                            enabled = true
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "AcousticEchoCanceler init error: ${e.message}")
                    }
                }
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = coroutineScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(bufferSize)
                while (isActive && isRecording) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        val rawChunk = buffer.copyOf(readBytes)

                        // Process audio with software noise suppression & gate if enabled
                        val chunkToSend = if (isNoiseSuppressionEnabled) {
                            applyDSpNoiseSuppression(rawChunk, readBytes)
                        } else {
                            rawChunk
                        }

                        onAudioChunk(chunkToSend)

                        // Calculate RMS Amplitude
                        val amplitude = calculateNormalizedRms(chunkToSend, readBytes)
                        onAmplitude(amplitude)

                        // Check for Barge-in / interruption if JARVIS is speaking
                        if (isAssistantSpeaking && amplitude > bargeInThreshold) {
                            onBargeInDetected()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error starting AudioRecord: ${e.message}", e)
            stopRecording()
        }
    }

    /**
     * Digital Signal Processing (DSP) Noise Suppression:
     * 1. Dynamic Noise Gate: Eliminates background fan hum, room air-conditioner, hiss, and ambient chatter below the threshold.
     * 2. High-pass filter: Cuts low-frequency rumble and mains AC hum below 80Hz.
     */
    private fun applyDSpNoiseSuppression(pcmBytes: ByteArray, bytesRead: Int): ByteArray {
        val shortBuffer = ByteBuffer.wrap(pcmBytes, 0, bytesRead)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()

        val numSamples = shortBuffer.remaining()
        if (numSamples == 0) return pcmBytes

        // Measure raw chunk energy
        var sumSquare = 0.0
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val s = shortBuffer.get()
            samples[i] = s
            sumSquare += (s.toDouble() * s.toDouble())
        }
        val rawRms = sqrt(sumSquare / numSamples) / 32767.0

        // Dynamic noise gate threshold scaled by suppression level (0.012f to 0.057f)
        val noiseGate = 0.012f + (noiseSuppressionLevel * 0.045f)

        val outBuffer = ByteArray(bytesRead)
        val outShorts = ByteBuffer.wrap(outBuffer)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()

        if (rawRms < noiseGate) {
            // Ambient noise / silence: gate out completely to avoid background audio leaking
            outShorts.put(ShortArray(numSamples))
        } else {
            // Speech detected: apply 80Hz high-pass filter to clean up rumble & hum
            val alpha = 0.94f // 80Hz cutoff at 16kHz
            for (i in 0 until numSamples) {
                val input = samples[i].toFloat()
                val filtered = alpha * (prevFiltered + input - prevSample)
                prevSample = input
                prevFiltered = filtered
                outShorts.put(filtered.coerceIn(-32768f, 32767f).toInt().toShort())
            }
        }

        return outBuffer
    }

    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
            echoCanceler?.release()
            echoCanceler = null
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
            onAmplitude(0f)
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    fun setBargeInSensitivity(threshold: Float) {
        bargeInThreshold = threshold.coerceIn(0.1f, 0.9f)
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
        // Normalize 16-bit range (max 32767) to 0.0..1.0
        val normalized = (rms / 32767.0).toFloat()
        return normalized.coerceIn(0f, 1f)
    }
}
