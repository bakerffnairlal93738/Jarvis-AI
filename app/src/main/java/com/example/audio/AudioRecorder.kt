package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Captures microphone audio as 16kHz, 16-bit Mono Linear PCM in little-endian format,
 * perfectly matching Google Gemini Multimodal Live API specifications.
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

    // Barge-in detection threshold
    private var bargeInThreshold = 0.35f
    var isAssistantSpeaking = false

    @SuppressLint("MissingPermission")
    fun startRecording(coroutineScope: CoroutineScope) {
        if (isRecording) return

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 3200) // ~100ms chunks at 16kHz

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord initialization failed")
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = coroutineScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(bufferSize)
                while (isActive && isRecording) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        val chunk = buffer.copyOf(readBytes)
                        onAudioChunk(chunk)

                        // Calculate RMS Amplitude
                        val amplitude = calculateNormalizedRms(chunk, readBytes)
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

    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
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
