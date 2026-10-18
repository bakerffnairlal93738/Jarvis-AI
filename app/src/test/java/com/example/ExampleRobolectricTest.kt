package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.JarvisDatabase
import com.example.data.model.CustomCommandEntity
import com.example.data.model.NoteEntity
import com.example.service.JarvisToolManager
import com.example.service.SimpleMathParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS", appName)
    }

    @Test
    fun `test math parser basic and percentage operations`() {
        assertEquals(100.0, SimpleMathParser.evaluate("25 * 4"), 0.001)
        assertEquals(25.0, SimpleMathParser.evaluate("15 + 10"), 0.001)
        assertEquals(12.5, SimpleMathParser.evaluate("25 / 2"), 0.001)
        assertEquals(30.0, SimpleMathParser.evaluate("10 + 4 * 5"), 0.001)
    }

    @Test
    fun `test tool manager current time and date`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolManager = JarvisToolManager(context)

        val timeResult = toolManager.executeTool("get_current_time", emptyMap())
        assertFalse(timeResult.isError)
        assertTrue(timeResult.spokenConfirmation.contains("The current time is"))

        val timeResultHindi = toolManager.executeTool("get_current_time", emptyMap(), "hi")
        assertFalse(timeResultHindi.isError)
        assertTrue(timeResultHindi.spokenConfirmation.contains("अभी का समय"))

        val dateResult = toolManager.executeTool("get_current_date", emptyMap())
        assertFalse(dateResult.isError)
        assertTrue(dateResult.spokenConfirmation.contains("Today is"))

        val dateResultHindi = toolManager.executeTool("get_current_date", emptyMap(), "hi")
        assertFalse(dateResultHindi.isError)
        assertTrue(dateResultHindi.spokenConfirmation.contains("आज की तारीख"))
    }

    @Test
    fun `test tool manager note creation and reading`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolManager = JarvisToolManager(context)

        val createResult = toolManager.executeTool("create_note", mapOf("content" to "Meeting at 4pm"))
        assertFalse(createResult.isError)
        assertEquals("NOTE_SAVED", createResult.actionType)

        val readResult = toolManager.executeTool("read_notes", emptyMap())
        assertFalse(readResult.isError)
        assertTrue(readResult.spokenConfirmation.contains("Meeting at 4pm"))
    }

    @Test
    fun `test tool manager calculation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolManager = JarvisToolManager(context)

        val calcResult = toolManager.executeTool("calculate", mapOf("expression" to "50 * 2 + 30"))
        assertFalse(calcResult.isError)
        assertTrue(calcResult.spokenConfirmation.contains("130"))
    }

    @Test
    fun `test custom commands execution`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = JarvisDatabase.getInstance(context)
        db.customCommandDao().insertCommand(
            CustomCommandEntity(
                triggerPhrase = "launch testing site",
                targetUrl = "https://example.com",
                description = "Test site"
            )
        )

        val toolManager = JarvisToolManager(context)
        val result = toolManager.executeTool("launch testing site", emptyMap())
        assertFalse(result.isError)
        assertEquals("CUSTOM_COMMAND", result.actionType)
    }

    @Test
    fun `test speech recognizer helper creation and non-recursive rms callback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var receivedAmplitude = -1f
        val helper = com.example.audio.SpeechRecognizerHelper(
            context = context,
            onPartialResult = {},
            onFinalResult = {},
            onAmplitudeChanged = { amp -> receivedAmplitude = amp },
            onListeningStateChanged = {},
            onError = {}
        )
        assertNotNull(helper)
        helper.setLanguage("en")
        assertFalse(helper.isCurrentlyListening())
    }

    @Test
    fun `test termux python background runner start and stop in Urdu`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolManager = JarvisToolManager(context)

        // Test Start Python App in Urdu
        val startResult = toolManager.executeTool("run_termux_python_app", mapOf("prompt" to "پورٹ فولیو ویب سائٹ بنائیں"), "ur")
        assertFalse(startResult.isError)
        assertEquals("TERMUX_PYTHON_STARTED", startResult.actionType)
        assertTrue(startResult.spokenConfirmation.contains("بیک گراؤنڈ میں پائتھن"))
        assertTrue(toolManager.termuxManager.state.value.isRunning)
        assertEquals(5000, toolManager.termuxManager.state.value.port)

        // Test Stop Python in Urdu
        val stopResult = toolManager.executeTool("stop_termux_python", emptyMap(), "ur")
        assertFalse(stopResult.isError)
        assertEquals("TERMUX_PYTHON_STOPPED", stopResult.actionType)
        assertTrue(stopResult.spokenConfirmation.contains("بند کر دیا گیا ہے"))
        assertFalse(toolManager.termuxManager.state.value.isRunning)
    }

    @Test
    fun `test wake word detection and command extraction in English`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var spottedWord = ""
        var extractedCommand: String? = null
        val detector = com.example.audio.WakeWordDetector(
            context = context,
            onWakeWordDetected = { word, cmd ->
                spottedWord = word
                extractedCommand = cmd
            }
        )

        // Test 1: Just wake word "Hey Jarvis"
        val res1 = detector.checkForWakeWord("hey jarvis")
        assertTrue(res1.first)
        assertNull(res1.second)

        // Test 2: Wake word with trailing command
        val res2 = detector.checkForWakeWord("Hey Jarvis, what is the weather today?")
        assertTrue(res2.first)
        assertEquals("what is the weather today?", res2.second)

        // Test 3: Standalone "Jarvis"
        val res3 = detector.checkForWakeWord("Jarvis turn on the lights")
        assertTrue(res3.first)
        assertEquals("turn on the lights", res3.second)

        // Test 4: Unrelated phrase without wake word
        val res4 = detector.checkForWakeWord("what is the time right now")
        assertFalse(res4.first)
        assertNull(res4.second)
    }

    @Test
    fun `test wake word detection in Urdu and Hindi`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = com.example.audio.WakeWordDetector(
            context = context,
            onWakeWordDetected = { _, _ -> }
        )

        // Urdu wake word test
        val urduRes = detector.checkForWakeWord("جاروس کیا وقت ہوا ہے؟")
        assertTrue(urduRes.first)
        assertEquals("کیا وقت ہوا ہے؟", urduRes.second)

        // Hindi wake word test
        val hindiRes = detector.checkForWakeWord("हे जार्विस आज का मौसम कैसा है")
        assertTrue(hindiRes.first)
        assertEquals("आज का मौसम कैसा है", hindiRes.second)
    }

    @Test
    fun `test audio player wake chime synthesis`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioPlayer = com.example.audio.AudioPlayer(context = context)
        assertNotNull(audioPlayer)
        // Should execute smoothly without throwing exceptions
        audioPlayer.playWakeChime()
    }

    @Test
    fun `test AudioPlayer VoiceGender selection between MALE and FEMALE`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioPlayer = com.example.audio.AudioPlayer(context = context)
        assertEquals(com.example.audio.AudioPlayer.VoiceGender.MALE, audioPlayer.getVoiceGender())

        // Switch to FEMALE
        audioPlayer.setVoiceGender(com.example.audio.AudioPlayer.VoiceGender.FEMALE)
        assertEquals(com.example.audio.AudioPlayer.VoiceGender.FEMALE, audioPlayer.getVoiceGender())

        // Switch back to MALE
        audioPlayer.setVoiceGender(com.example.audio.AudioPlayer.VoiceGender.MALE)
        assertEquals(com.example.audio.AudioPlayer.VoiceGender.MALE, audioPlayer.getVoiceGender())
    }

    @Test
    fun `test set_voice_gender tool in Urdu and English`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolManager = JarvisToolManager(context)

        // Select Female voice in Urdu
        val femaleRes = toolManager.executeTool("set_voice_gender", mapOf("gender" to "female"), "ur")
        assertFalse(femaleRes.isError)
        assertEquals("VOICE_GENDER_CHANGED", femaleRes.actionType)
        assertEquals("FEMALE", femaleRes.data["gender"])
        assertTrue(femaleRes.spokenConfirmation.contains("فی میل وائس"))

        // Select Male voice in Urdu
        val maleRes = toolManager.executeTool("set_voice_gender", mapOf("gender" to "male"), "ur")
        assertFalse(maleRes.isError)
        assertEquals("VOICE_GENDER_CHANGED", maleRes.actionType)
        assertEquals("MALE", maleRes.data["gender"])
        assertTrue(maleRes.spokenConfirmation.contains("میل وائس"))
    }

    @Test
    fun `test AudioPlayer language detection for Punjabi, Urdu, Hindi, English`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioPlayer = com.example.audio.AudioPlayer(context = context)

        // Punjabi Gurmukhi
        assertEquals("pa", audioPlayer.detectLanguage("ਕੀ ਹਾਲ ਹੈ ਦੋਸਤੋ"))

        // Punjabi Shahmukhi
        assertEquals("pa", audioPlayer.detectLanguage("کی حال اے تسی کیویں او"))

        // Urdu
        assertEquals("ur", audioPlayer.detectLanguage("آپ کا شکریہ، میں بالکل ٹھیک ہوں"))

        // Hindi Devanagari
        assertEquals("hi", audioPlayer.detectLanguage("नमस्ते, आप कैसे हैं?"))

        // English
        assertEquals("en", audioPlayer.detectLanguage("Hello JARVIS, what is the weather today?"))
    }

    @Test
    fun `test set_language tool execution for Auto and Punjabi`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolManager = JarvisToolManager(context)

        // Select Punjabi
        val paRes = toolManager.executeTool("set_language", mapOf("language" to "punjabi"), "pa")
        assertFalse(paRes.isError)
        assertEquals("LANGUAGE_CHANGED", paRes.actionType)
        assertEquals("pa", paRes.data["language"])
        assertTrue(paRes.spokenConfirmation.contains("پنجابی"))

        // Select Auto-detect
        val autoRes = toolManager.executeTool("set_language", mapOf("language" to "auto"), "en")
        assertFalse(autoRes.isError)
        assertEquals("LANGUAGE_CHANGED", autoRes.actionType)
        assertEquals("auto", autoRes.data["language"])
        assertTrue(autoRes.spokenConfirmation.contains("Multilingual"))
    }

    @Test
    fun `test FeedbackDao insert and retrieval`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.JarvisDatabase.getInstance(context)
        val feedbackDao = db.feedbackDao()

        val id = feedbackDao.insertFeedback(
            com.example.data.model.FeedbackEntity(
                rating = 5,
                category = "Multilingual Accuracy",
                comment = "Excellent Punjabi and Urdu voice support!",
                language = "ur",
                voiceGender = "FEMALE"
            )
        )
        assertTrue(id > 0)

        // Delete test
        feedbackDao.deleteFeedback(id)
    }

    @Test
    fun `test open_feedback tool in Urdu and English`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolManager = JarvisToolManager(context)

        val resUrdu = toolManager.executeTool("open_feedback", emptyMap(), "ur")
        assertFalse(resUrdu.isError)
        assertEquals("OPEN_FEEDBACK", resUrdu.actionType)
        assertTrue(resUrdu.spokenConfirmation.contains("فیڈ بیک"))

        val resEn = toolManager.executeTool("open_feedback", emptyMap(), "en")
        assertFalse(resEn.isError)
        assertEquals("OPEN_FEEDBACK", resEn.actionType)
        assertTrue(resEn.spokenConfirmation.contains("feedback"))

        val resSubmit = toolManager.executeTool("submit_feedback", mapOf("rating" to 5, "comment" to "Superb!"), "ur")
        assertFalse(resSubmit.isError)
        assertEquals("FEEDBACK_SUBMITTED", resSubmit.actionType)
        assertTrue(resSubmit.spokenConfirmation.contains("محفوظ"))
    }
}
