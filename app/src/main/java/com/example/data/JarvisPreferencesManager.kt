package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.service.ChatMessageItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent storage for all JARVIS settings, credentials, voice configurations,
 * and conversation history across app restarts and task clears.
 */
class JarvisPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_persistent_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "key_gemini_api_key"
        private const val KEY_VOICE = "key_selected_voice"
        private const val KEY_VOICE_GENDER = "key_voice_gender"
        private const val KEY_LANGUAGE = "key_selected_language"
        private const val KEY_MODEL_PERSONA = "key_model_persona"
        private const val KEY_VOICE_OUTPUT_ENABLED = "key_voice_output_enabled"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_CONTINUOUS_LISTENING = "key_continuous_listening"
        private const val KEY_BARGE_IN_SENSITIVITY = "key_barge_in_sensitivity"
        private const val KEY_WAKE_WORD_ENABLED = "key_wake_word_enabled"
        private const val KEY_WAKE_WORD_SENSITIVITY = "key_wake_word_sensitivity"
        private const val KEY_SPEECH_VOLUME = "key_speech_volume"
        private const val KEY_SELECTED_MODE = "key_selected_mode"
        private const val KEY_NOISE_SUPPRESSION_ENABLED = "key_noise_suppression_enabled"
        private const val KEY_NOISE_SUPPRESSION_LEVEL = "key_noise_suppression_level"
        private const val KEY_RUN_IN_BACKGROUND = "key_run_in_background"
        private const val KEY_MISSED_CALL_ALERT_ENABLED = "key_missed_call_alert_enabled"
        private const val KEY_SELECTED_AI_MODEL = "key_selected_ai_model"
        private const val KEY_CONVERSATION_HISTORY = "key_conversation_history"
    }

    // API Key: default fallback to BuildConfig if not yet saved in prefs
    var apiKey: String
        get() {
            val saved = prefs.getString(KEY_API_KEY, null)
            if (!saved.isNullOrBlank()) return saved
            return try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                ""
            }
        }
        set(value) {
            prefs.edit().putString(KEY_API_KEY, value.trim()).commit()
        }

    var selectedVoice: String
        get() = prefs.getString(KEY_VOICE, "Kore") ?: "Kore"
        set(value) { prefs.edit().putString(KEY_VOICE, value).commit() }

    var selectedVoiceGender: String
        get() = prefs.getString(KEY_VOICE_GENDER, "MALE") ?: "MALE"
        set(value) { prefs.edit().putString(KEY_VOICE_GENDER, value).commit() }

    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "auto") ?: "auto"
        set(value) { prefs.edit().putString(KEY_LANGUAGE, value).commit() }

    var selectedModelPersona: String
        get() = prefs.getString(KEY_MODEL_PERSONA, "JARVIS") ?: "JARVIS"
        set(value) { prefs.edit().putString(KEY_MODEL_PERSONA, value).commit() }

    var selectedAiModel: String
        get() = prefs.getString(KEY_SELECTED_AI_MODEL, "Gemini 2.5 Flash") ?: "Gemini 2.5 Flash"
        set(value) { prefs.edit().putString(KEY_SELECTED_AI_MODEL, value).commit() }

    var isVoiceOutputEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_OUTPUT_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_VOICE_OUTPUT_ENABLED, value).commit() }

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) { prefs.edit().putFloat(KEY_SPEECH_RATE, value).commit() }

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.05f)
        set(value) { prefs.edit().putFloat(KEY_SPEECH_PITCH, value).commit() }

    var isContinuousListening: Boolean
        get() = prefs.getBoolean(KEY_CONTINUOUS_LISTENING, true)
        set(value) { prefs.edit().putBoolean(KEY_CONTINUOUS_LISTENING, value).commit() }

    var bargeInSensitivity: Float
        get() = prefs.getFloat(KEY_BARGE_IN_SENSITIVITY, 0.35f)
        set(value) { prefs.edit().putFloat(KEY_BARGE_IN_SENSITIVITY, value).commit() }

    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).commit() }

    var wakeWordSensitivity: Float
        get() = prefs.getFloat(KEY_WAKE_WORD_SENSITIVITY, 0.5f)
        set(value) { prefs.edit().putFloat(KEY_WAKE_WORD_SENSITIVITY, value).commit() }

    var speechVolume: Float
        get() = prefs.getFloat(KEY_SPEECH_VOLUME, 1.0f)
        set(value) { prefs.edit().putFloat(KEY_SPEECH_VOLUME, value.coerceIn(0.05f, 1.0f)).commit() }

    var selectedMode: String
        get() = prefs.getString(KEY_SELECTED_MODE, "LIVE") ?: "LIVE"
        set(value) { prefs.edit().putString(KEY_SELECTED_MODE, value).commit() }

    var isNoiseSuppressionEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOISE_SUPPRESSION_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_NOISE_SUPPRESSION_ENABLED, value).commit() }

    var noiseSuppressionLevel: Float
        get() = prefs.getFloat(KEY_NOISE_SUPPRESSION_LEVEL, 0.70f)
        set(value) { prefs.edit().putFloat(KEY_NOISE_SUPPRESSION_LEVEL, value.coerceIn(0.1f, 1.0f)).commit() }

    var isRunInBackgroundEnabled: Boolean
        get() = prefs.getBoolean(KEY_RUN_IN_BACKGROUND, true)
        set(value) { prefs.edit().putBoolean(KEY_RUN_IN_BACKGROUND, value).commit() }

    var isMissedCallAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_MISSED_CALL_ALERT_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_MISSED_CALL_ALERT_ENABLED, value).commit() }

    // Conversation History Persistence
    fun getConversationHistory(): List<ChatMessageItem> {
        val raw = prefs.getString(KEY_CONVERSATION_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<ChatMessageItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ChatMessageItem(
                        role = obj.optString("role", "user"),
                        text = obj.optString("text", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        actionBadge = if (obj.has("actionBadge")) obj.optString("actionBadge") else null
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return list
    }

    fun saveConversationHistory(messages: List<ChatMessageItem>) {
        val arr = JSONArray()
        // Save last 60 messages for speed and longevity
        for (m in messages.takeLast(60)) {
            val obj = JSONObject().apply {
                put("role", m.role)
                put("text", m.text)
                put("timestamp", m.timestamp)
                if (m.actionBadge != null) put("actionBadge", m.actionBadge)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CONVERSATION_HISTORY, arr.toString()).commit()
    }

    fun clearConversationHistory() {
        prefs.edit().remove(KEY_CONVERSATION_HISTORY).commit()
    }
}
