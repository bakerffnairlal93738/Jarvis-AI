package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.audio.SpeechRecognizerHelper
import com.example.data.JarvisDatabase
import com.example.data.model.CustomCommandEntity
import com.example.data.model.FeedbackEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ReminderEntity
import com.example.audio.WakeWordDetector
import com.example.service.ChatMessageItem
import com.example.service.GeminiLiveService
import com.example.service.GeminiRestService
import com.example.service.JarvisToolManager
import com.example.service.TermuxPythonAppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AssistantStatus {
    READY,
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    DISCONNECTED,
    RECONNECTING,
    ERROR
}

data class JarvisUiState(
    val status: AssistantStatus = AssistantStatus.READY,
    val amplitude: Float = 0f,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val isContinuousListening: Boolean = true,
    val isWakeWordEnabled: Boolean = true,
    val isWakeWordListening: Boolean = false,
    val wakeWordSensitivity: Float = 0.5f,
    val lastWakeWordSpotted: String? = null,
    val selectedLanguage: String = "auto", // "auto", "en", "ur", "pa", "hi"
    val selectedMode: String = "LIVE", // "LIVE" or "VOICE"
    val selectedVoiceGender: String = "MALE", // "MALE" or "FEMALE"
    val selectedVoice: String = "Kore",
    val bargeInSensitivity: Float = 0.35f,
    val liveUserSpeech: String = "",
    val messages: List<ChatMessageItem> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val customCommands: List<CustomCommandEntity> = emptyList(),
    val errorMessage: String? = null,
    val apiKey: String = "",
    val selectedModelPersona: String = "JARVIS", // "JARVIS", "Gemini Live"
    val isVoiceOutputEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.05f,
    val termuxState: TermuxPythonAppState = TermuxPythonAppState(),
    val isTermuxSheetOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isCommandsOpen: Boolean = false,
    val isNotesOpen: Boolean = false,
    val isFeedbackOpen: Boolean = false,
    val feedbackList: List<FeedbackEntity> = emptyList(),
    val feedbackSubmittedMessage: String? = null
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "JarvisViewModel"
    private val database = JarvisDatabase.getInstance(application)
    private val toolManager = JarvisToolManager(application)
    private val restService = GeminiRestService(toolManager)

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null
    private var speechRecognizerHelper: SpeechRecognizerHelper? = null
    private var liveService: GeminiLiveService? = null
    private var wakeWordDetector: WakeWordDetector? = null

    init {
        // Load API key from BuildConfig safely
        val initialKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        _uiState.update { it.copy(apiKey = initialKey) }

        setupAudioComponents()
        observeDatabase()
        observeTermuxState()
        if (_uiState.value.isWakeWordEnabled) {
            wakeWordDetector?.startSpotting()
        }
    }

    private fun setupAudioComponents() {
        val app = getApplication<Application>()

        // Audio Player (24kHz PCM for Gemini Live + TTS)
        audioPlayer = AudioPlayer(
            context = app,
            onPlaybackStarted = {
                _uiState.update { it.copy(isSpeaking = true, status = AssistantStatus.SPEAKING) }
                audioRecorder?.isAssistantSpeaking = true
            },
            onPlaybackFinished = {
                _uiState.update { it.copy(isSpeaking = false) }
                audioRecorder?.isAssistantSpeaking = false
                handlePlaybackEnded()
            },
            onAmplitude = { amp ->
                if (_uiState.value.isSpeaking) {
                    _uiState.update { it.copy(amplitude = amp) }
                }
            }
        )

        // Audio Recorder (16kHz PCM for Gemini Live)
        audioRecorder = AudioRecorder(
            sampleRate = 16000,
            onAudioChunk = { chunk ->
                if (!_uiState.value.isMuted) {
                    liveService?.sendAudioChunk(chunk)
                }
            },
            onAmplitude = { amp ->
                if (!_uiState.value.isSpeaking) {
                    _uiState.update { it.copy(amplitude = amp) }
                }
            },
            onBargeInDetected = {
                handleBargeIn()
            }
        )

        // Gemini Live Service (WebSocket)
        liveService = GeminiLiveService(
            toolManager = toolManager,
            onAudioOutputReceived = { pcmChunk ->
                audioPlayer?.enqueueAudioChunk(pcmChunk, viewModelScope)
            },
            onTranscriptReceived = { text, isUser ->
                viewModelScope.launch(Dispatchers.Main) {
                    val role = if (isUser) "user" else "model"
                    val currentList = _uiState.value.messages.toMutableList()
                    val badge = if (text.startsWith("[ACTION]")) text.removePrefix("[ACTION] ").trim() else null
                    currentList.add(ChatMessageItem(role = role, text = text, actionBadge = badge))
                    _uiState.update { it.copy(messages = currentList) }
                }
            },
            onStatusChanged = { connState ->
                viewModelScope.launch(Dispatchers.Main) {
                    val newStatus = when (connState) {
                        GeminiLiveService.ConnectionState.CONNECTED -> {
                            if (audioRecorder?.isCurrentlyRecording() == true) AssistantStatus.LISTENING else AssistantStatus.READY
                        }
                        GeminiLiveService.ConnectionState.CONNECTING -> AssistantStatus.CONNECTING
                        GeminiLiveService.ConnectionState.RECONNECTING -> AssistantStatus.RECONNECTING
                        GeminiLiveService.ConnectionState.DISCONNECTED -> AssistantStatus.DISCONNECTED
                        GeminiLiveService.ConnectionState.ERROR -> AssistantStatus.ERROR
                    }
                    _uiState.update { it.copy(status = newStatus) }
                }
            },
            onInterrupted = {
                handleBargeIn()
            }
        )

        // Speech Recognizer (multilingual STT for Assistant mode)
        speechRecognizerHelper = SpeechRecognizerHelper(
            context = app,
            onPartialResult = { partial ->
                _uiState.update { it.copy(liveUserSpeech = partial) }
            },
            onFinalResult = { finalResult ->
                _uiState.update { it.copy(liveUserSpeech = "") }
                processUserPrompt(finalResult)
            },
            onAmplitudeChanged = { amp ->
                if (!_uiState.value.isSpeaking) {
                    _uiState.update { it.copy(amplitude = amp) }
                }
            },
            onListeningStateChanged = { listening ->
                _uiState.update {
                    it.copy(status = if (listening) AssistantStatus.LISTENING else AssistantStatus.READY)
                }
            },
            onError = { errMsg ->
                _uiState.update { it.copy(errorMessage = errMsg, status = AssistantStatus.ERROR) }
            }
        )

        // Wake Word Spotter for hands-free JARVIS initiation
        wakeWordDetector = WakeWordDetector(
            context = app,
            onWakeWordDetected = { wakeWord, trailingCommand ->
                handleWakeWordTriggered(wakeWord, trailingCommand)
            },
            onSpotterStateChanged = { spotting ->
                _uiState.update { it.copy(isWakeWordListening = spotting) }
            },
            onError = { errMsg ->
                Log.w(tag, "WakeWord error: $errMsg")
            }
        ).apply {
            isEnabled = _uiState.value.isWakeWordEnabled
            sensitivity = _uiState.value.wakeWordSensitivity
            languageCode = _uiState.value.selectedLanguage
        }
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            database.noteDao().getAllNotes().collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }
        viewModelScope.launch {
            database.reminderDao().getAllReminders().collect { reminders ->
                _uiState.update { it.copy(reminders = reminders) }
            }
        }
        viewModelScope.launch {
            database.customCommandDao().getAllCommands().collect { commands ->
                _uiState.update { it.copy(customCommands = commands) }
            }
        }
        viewModelScope.launch {
            database.feedbackDao().getAllFeedback().collect { feedback ->
                _uiState.update { it.copy(feedbackList = feedback) }
            }
        }
    }

    /**
     * Start/Stop primary conversation flow based on selectedMode ("LIVE" vs "VOICE").
     */
    fun toggleLiveSession() {
        val currentState = _uiState.value
        if (currentState.isSpeaking) {
            handleBargeIn()
            return
        }

        if (currentState.status == AssistantStatus.LISTENING || currentState.status == AssistantStatus.CONNECTING) {
            stopSession()
        } else {
            startSession()
        }
    }

    private fun startSession() {
        wakeWordDetector?.pauseSpotting()
        val mode = _uiState.value.selectedMode
        val key = _uiState.value.apiKey

        if (mode == "LIVE") {
            _uiState.update { it.copy(status = AssistantStatus.CONNECTING) }
            liveService?.setVoice(_uiState.value.selectedVoice)
            liveService?.connect(key, viewModelScope)
            audioRecorder?.startRecording(viewModelScope)
            _uiState.update { it.copy(status = AssistantStatus.LISTENING) }
        } else {
            // Conversational Voice mode: start SpeechRecognizer
            speechRecognizerHelper?.setLanguage(_uiState.value.selectedLanguage)
            speechRecognizerHelper?.startListening()
            _uiState.update { it.copy(status = AssistantStatus.LISTENING) }
        }
    }

    fun stopSession() {
        audioRecorder?.stopRecording()
        speechRecognizerHelper?.stopListening()
        liveService?.disconnect()
        audioPlayer?.stopPlayback()
        _uiState.update {
            it.copy(
                status = AssistantStatus.READY,
                isSpeaking = false,
                amplitude = 0f,
                liveUserSpeech = ""
            )
        }
        if (_uiState.value.isWakeWordEnabled) {
            wakeWordDetector?.resumeSpotting()
        }
    }

    /**
     * Interruption / Barge-in: immediately stops playback, clears audio buffer, and resumes listening.
     */
    fun handleBargeIn() {
        audioPlayer?.stopPlayback()
        _uiState.update {
            it.copy(
                isSpeaking = false,
                status = AssistantStatus.INTERRUPTED,
                amplitude = 0f
            )
        }

        viewModelScope.launch {
            delay(400)
            if (_uiState.value.selectedMode == "LIVE") {
                _uiState.update { it.copy(status = AssistantStatus.LISTENING) }
            } else {
                if (_uiState.value.isContinuousListening) {
                    speechRecognizerHelper?.startListening()
                } else {
                    _uiState.update { it.copy(status = AssistantStatus.READY) }
                    if (_uiState.value.isWakeWordEnabled) {
                        wakeWordDetector?.resumeSpotting()
                    }
                }
            }
        }
    }

    private fun handlePlaybackEnded() {
        if (_uiState.value.selectedMode == "VOICE" && _uiState.value.isContinuousListening) {
            viewModelScope.launch {
                delay(600)
                speechRecognizerHelper?.startListening()
            }
        } else {
            _uiState.update { it.copy(status = AssistantStatus.READY) }
            if (_uiState.value.isWakeWordEnabled) {
                wakeWordDetector?.resumeSpotting()
            }
        }
    }

    /**
     * Complete voice pipeline:
     * User speech → Recognized text → Gemini API → Gemini Response displayed → Spoken aloud through speakers.
     */
    fun processUserPrompt(prompt: String) {
        if (prompt.isBlank()) return

        _uiState.update {
            it.copy(
                status = AssistantStatus.THINKING,
                messages = it.messages + ChatMessageItem(role = "user", text = prompt)
            )
        }

        viewModelScope.launch {
            val key = _uiState.value.apiKey
            val result = restService.sendMessage(
                userPrompt = prompt,
                apiKey = key,
                voiceName = _uiState.value.selectedVoice,
                modelPersona = _uiState.value.selectedModelPersona,
                language = _uiState.value.selectedLanguage
            )

            if (result.isError) {
                _uiState.update {
                    it.copy(
                        status = AssistantStatus.ERROR,
                        errorMessage = result.replyText,
                        messages = it.messages + ChatMessageItem(role = "model", text = result.replyText)
                    )
                }
            } else {
                if (result.actionSummary?.contains("Voice Gender:") == true) {
                    val g = if (result.actionSummary.contains("Female")) "FEMALE" else "MALE"
                    setVoiceGender(g, speakConfirmation = false)
                }
                if (result.actionSummary?.contains("Language:") == true) {
                    val code = result.actionSummary.substringAfter("Language:").trim()
                    setLanguage(code, speakConfirmation = false)
                }
                if (result.actionSummary?.contains("FEEDBACK_FORM", ignoreCase = true) == true ||
                    result.actionSummary?.contains("OPEN_FEEDBACK", ignoreCase = true) == true) {
                    openFeedback(true)
                } else if (result.actionSummary?.contains("FEEDBACK_SUBMITTED", ignoreCase = true) == true ||
                           result.actionSummary?.contains("Feedback Recorded", ignoreCase = true) == true) {
                    _uiState.update { it.copy(feedbackSubmittedMessage = "Thank you! Your feedback has been received.") }
                } else if (result.actionSummary?.contains("FEEDBACK", ignoreCase = true) == true) {
                    openFeedback(true)
                }

                _uiState.update {
                    it.copy(
                        status = AssistantStatus.SPEAKING,
                        messages = it.messages + ChatMessageItem(
                            role = "model",
                            text = result.replyText,
                            actionBadge = result.actionSummary
                        )
                    )
                }

                // VERY IMPORTANT: Speak response out loud through device speakers with live waveform!
                audioPlayer?.setLanguage(_uiState.value.selectedLanguage)
                audioPlayer?.speakResponse(result.replyText, viewModelScope)
            }
        }
    }

    fun setModelPersona(persona: String) {
        _uiState.update { it.copy(selectedModelPersona = persona) }
    }

    fun toggleVoiceOutput() {
        val newEnabled = !_uiState.value.isVoiceOutputEnabled
        _uiState.update { it.copy(isVoiceOutputEnabled = newEnabled) }
        audioPlayer?.isVoiceOutputEnabled = newEnabled
    }

    fun setSpeechRate(rate: Float) {
        _uiState.update { it.copy(speechRate = rate) }
        audioPlayer?.setSpeechRate(rate)
    }

    fun setSpeechPitch(pitch: Float) {
        _uiState.update { it.copy(speechPitch = pitch) }
        audioPlayer?.setSpeechPitch(pitch)
    }

    fun stopSpeaking() {
        audioPlayer?.stopPlayback()
        _uiState.update { it.copy(isSpeaking = false, status = AssistantStatus.READY) }
        if (_uiState.value.isWakeWordEnabled) {
            wakeWordDetector?.resumeSpotting()
        }
    }

    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = newMuted) }
    }

    fun setLanguage(langCode: String, speakConfirmation: Boolean = false) {
        _uiState.update { it.copy(selectedLanguage = langCode) }
        audioPlayer?.setLanguage(langCode)
        speechRecognizerHelper?.setLanguage(langCode)
        wakeWordDetector?.languageCode = langCode

        if (speakConfirmation) {
            val confirm = when (langCode.lowercase()) {
                "auto" -> "Smart Multilingual Mode enabled. I will mirror whatever language you speak: English, Urdu, Punjabi, or Hindi."
                "en" -> "English language selected. Ready for your instructions, sir."
                "ur" -> "اردو زبان منتخب کر لی گئی ہے۔ فرمائیے، میں آپ کی کیا مدد کر سکتا ہوں؟"
                "pa" -> "پنجابی زبان چن لئی گئی اے۔ دسو کیہڑا کم کرنا اے؟"
                "hi" -> "हिंदी भाषा चुन ली गई है। बताइए, मैं आपकी क्या सहायता कर सकता हूँ?"
                else -> "$langCode language selected."
            }
            audioPlayer?.speakResponse(confirm, viewModelScope)
        }
    }

    fun setMode(mode: String) {
        stopSession()
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun setVoice(voice: String) {
        _uiState.update { it.copy(selectedVoice = voice) }
        liveService?.setVoice(voice)
    }

    fun setVoiceGender(gender: String, speakConfirmation: Boolean = true) {
        val normalized = if (gender.equals("FEMALE", ignoreCase = true)) "FEMALE" else "MALE"
        val targetVoice = if (normalized == "FEMALE") "Aoede" else "Kore"
        val targetPitch = if (normalized == "FEMALE") 1.25f else 0.92f

        _uiState.update {
            it.copy(
                selectedVoiceGender = normalized,
                selectedVoice = targetVoice,
                speechPitch = targetPitch
            )
        }
        audioPlayer?.setVoiceGender(if (normalized == "FEMALE") AudioPlayer.VoiceGender.FEMALE else AudioPlayer.VoiceGender.MALE)
        audioPlayer?.setSpeechPitch(targetPitch)
        liveService?.setVoice(targetVoice)

        if (speakConfirmation) {
            val sampleText = when (_uiState.value.selectedLanguage) {
                "ur" -> if (normalized == "FEMALE") {
                    "فی میل وائس منتخب کر لی گئی ہے۔ فرمائیے، میں آپ کی کیا مدد کر سکتی ہوں؟"
                } else {
                    "میل وائس منتخب کر لی گئی ہے۔ میں آپ کے احکامات کے لیے تیار ہوں۔"
                }
                "hi" -> if (normalized == "FEMALE") {
                    "फीमेल वॉयस चुन ली गई है। बताइए, मैं आपकी क्या मदद कर सकती हूँ?"
                } else {
                    "मेल वॉयस चुन ली गई है। मैं आपके आदेश के लिए तैयार हूँ।"
                }
                else -> if (normalized == "FEMALE") {
                    "Female voice selected. Ready to assist you."
                } else {
                    "Male voice selected. Ready for your instructions, sir."
                }
            }
            audioPlayer?.setLanguage(_uiState.value.selectedLanguage)
            audioPlayer?.speakResponse(sampleText, viewModelScope)
        }
    }

    fun setBargeInSensitivity(sensitivity: Float) {
        _uiState.update { it.copy(bargeInSensitivity = sensitivity) }
        audioRecorder?.setBargeInSensitivity(sensitivity)
    }

    fun setContinuousListening(enabled: Boolean) {
        _uiState.update { it.copy(isContinuousListening = enabled) }
    }

    fun updateApiKey(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey) }
    }

    fun replayMessageSpeech(text: String) {
        audioPlayer?.setLanguage(_uiState.value.selectedLanguage)
        audioPlayer?.speakResponse(text, viewModelScope)
    }

    fun openSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun openCommands(open: Boolean) {
        _uiState.update { it.copy(isCommandsOpen = open) }
    }

    fun openNotes(open: Boolean) {
        _uiState.update { it.copy(isNotesOpen = open) }
    }

    fun openFeedback(open: Boolean) {
        _uiState.update { it.copy(isFeedbackOpen = open) }
    }

    fun submitFeedback(rating: Int, category: String, comment: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = FeedbackEntity(
                rating = rating.coerceIn(1, 5),
                category = category,
                comment = comment.trim(),
                language = _uiState.value.selectedLanguage,
                voiceGender = _uiState.value.selectedVoiceGender
            )
            database.feedbackDao().insertFeedback(entity)
            _uiState.update {
                it.copy(
                    isFeedbackOpen = false,
                    feedbackSubmittedMessage = "Thank you! Your feedback has been received."
                )
            }

            val thanksMsg = when (_uiState.value.selectedLanguage) {
                "ur" -> "آپ کی رائے اور فیڈ بیک کا بہت بہت شکریہ! اس سے مجھے مزید بہتر بنانے میں مدد ملے گی۔"
                "pa" -> "تہاڈے فیڈ بیک دا بہت بہت شکریہ! ایہدے نال مینوں ہور چنگا بنان چ مدد ملے گی۔"
                "hi" -> "आपकी प्रतिक्रिया के लिए बहुत धन्यवाद! इससे मुझे और बेहतर बनने में मदद मिलेगी।"
                else -> "Thank you for your valuable feedback! It helps improve my neural capabilities."
            }
            audioPlayer?.speakResponse(thanksMsg, viewModelScope)
        }
    }

    fun deleteFeedback(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.feedbackDao().deleteFeedback(id)
        }
    }

    fun clearFeedbackSubmittedMessage() {
        _uiState.update { it.copy(feedbackSubmittedMessage = null) }
    }

    fun addCustomCommand(trigger: String, url: String, desc: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.customCommandDao().insertCommand(
                CustomCommandEntity(triggerPhrase = trigger, targetUrl = url, description = desc)
            )
        }
    }

    fun deleteCustomCommand(command: CustomCommandEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.customCommandDao().deleteCommand(command)
        }
    }

    fun testCustomCommand(command: CustomCommandEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = toolManager.executeTool(command.triggerPhrase, emptyMap(), _uiState.value.selectedLanguage)
            audioPlayer?.speakResponse(res.spokenConfirmation, viewModelScope)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.noteDao().deleteNote(note)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.reminderDao().deleteReminder(reminder)
        }
    }

    fun readNotesAloud() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = toolManager.executeTool("read_notes", emptyMap(), _uiState.value.selectedLanguage)
            audioPlayer?.speakResponse(res.spokenConfirmation, viewModelScope)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeTermuxState() {
        viewModelScope.launch {
            toolManager.termuxManager.state.collect { tState ->
                _uiState.update { it.copy(termuxState = tState) }
            }
        }
    }

    fun openTermuxSheet(open: Boolean) {
        _uiState.update { it.copy(isTermuxSheetOpen = open) }
    }

    fun startTermuxPython(prompt: String) {
        processUserPrompt("ٹرمکس میں پائتھن ایپ بناؤ: $prompt")
    }

    fun stopTermuxPython() {
        processUserPrompt("بیک گراؤنڈ سے پائتھن بند کر دو")
    }

    fun openTermuxWebApp() {
        toolManager.termuxManager.openInBrowser()
    }

    fun toggleWakeWord(enabled: Boolean) {
        _uiState.update { it.copy(isWakeWordEnabled = enabled) }
        wakeWordDetector?.isEnabled = enabled
        if (enabled) {
            if (_uiState.value.status == AssistantStatus.READY) {
                wakeWordDetector?.startSpotting()
            }
        } else {
            wakeWordDetector?.stopSpotting()
        }
    }

    fun setWakeWordSensitivity(sensitivity: Float) {
        _uiState.update { it.copy(wakeWordSensitivity = sensitivity) }
        wakeWordDetector?.sensitivity = sensitivity
    }

    private fun handleWakeWordTriggered(wakeWord: String, trailingCommand: String?) {
        viewModelScope.launch(Dispatchers.Main) {
            // Futuristic sonic acknowledgment chime
            audioPlayer?.playWakeChime()

            _uiState.update {
                it.copy(
                    lastWakeWordSpotted = wakeWord,
                    status = AssistantStatus.LISTENING
                )
            }

            if (!trailingCommand.isNullOrBlank()) {
                // Immediate hands-free command in one breath
                processUserPrompt(trailingCommand)
            } else {
                // Standby wake up: voice greeting acknowledging user
                val greeting = when (_uiState.value.selectedLanguage) {
                    "ur" -> "جی، فرمائیے؟"
                    "hi" -> "जी, बताइए?"
                    else -> "Online, sir. How can I help you?"
                }
                val currentMessages = _uiState.value.messages.toMutableList()
                currentMessages.add(ChatMessageItem(role = "user", text = wakeWord))
                currentMessages.add(ChatMessageItem(role = "model", text = greeting))
                _uiState.update {
                    it.copy(messages = currentMessages, status = AssistantStatus.SPEAKING)
                }
                audioPlayer?.setLanguage(_uiState.value.selectedLanguage)
                audioPlayer?.speakResponse(greeting, viewModelScope)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder?.stopRecording()
        audioPlayer?.release()
        speechRecognizerHelper?.stopListening()
        wakeWordDetector?.stopSpotting()
        liveService?.disconnect()
    }
}
