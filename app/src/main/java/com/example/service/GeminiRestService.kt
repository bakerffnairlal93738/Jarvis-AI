package com.example.service

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiRestResult(
    val replyText: String,
    val audioBytes: ByteArray? = null,
    val actionSummary: String? = null,
    val isError: Boolean = false
)

data class ChatMessageItem(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionBadge: String? = null
)

class GeminiRestService(
    private val toolManager: JarvisToolManager
) {
    private val tag = "GeminiRestService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val conversationHistory = mutableListOf<ChatMessageItem>()

    fun getHistory(): List<ChatMessageItem> = conversationHistory.toList()

    fun addHistoryItem(item: ChatMessageItem) {
        conversationHistory.add(item)
        if (conversationHistory.size > 20) {
            conversationHistory.removeAt(0)
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    /**
     * Sends user prompt to Gemini REST API with tool calling and conversation history.
     */
    suspend fun sendMessage(
        userPrompt: String,
        apiKey: String,
        voiceName: String = "Kore",
        modelPersona: String = "JARVIS",
        language: String = "en"
    ): GeminiRestResult = withContext(Dispatchers.IO) {
        val isHindi = language.lowercase().startsWith("hi") || userPrompt.any { it in '\u0900'..'\u097F' }
        val lower = userPrompt.trim().lowercase()

        // First check local direct command intents (e.g. "sleep", "wake up", "stop listening", "exit")
        when {
            lower == "sleep" || lower == "stop listening" || lower == "सो जाओ" || lower == "सुनना बंद करो" -> {
                val reply = if (isHindi) "स्टैंडबाय मोड शुरू किया जा रहा है। जब भी ज़रूरत हो, रिएक्टर को स्पर्श करें।" else "Entering standby mode. Tap the reactor whenever you need me."
                return@withContext GeminiRestResult(
                    replyText = reply,
                    actionSummary = "STANDBY"
                )
            }
            lower == "wake up" || lower == "जाग जाओ" || lower == "उठो जार्विस" -> {
                val reply = if (isHindi) "सभी सिस्टम ऑनलाइन हैं। जार्विस आपकी सेवा में तैयार है।" else "Systems online and all sensors nominal. How can I assist you?"
                return@withContext GeminiRestResult(
                    replyText = reply,
                    actionSummary = "ONLINE"
                )
            }
            lower == "exit" || lower == "बंद करो" -> {
                val reply = if (isHindi) "सक्रिय वॉयस सेवा बंद की जा रही है। अलविदा!" else "Shutting down active voice subroutines. Goodbye."
                return@withContext GeminiRestResult(
                    replyText = reply,
                    actionSummary = "EXIT"
                )
            }
        }

        // Check if user utterance directly matches predefined tools
        val directResult = checkDirectCommand(userPrompt, if (isHindi) "hi" else language)
        if (directResult != null) {
            addHistoryItem(ChatMessageItem(role = "user", text = userPrompt))
            addHistoryItem(ChatMessageItem(role = "model", text = directResult.spokenConfirmation, actionBadge = directResult.displayMessage))
            return@withContext GeminiRestResult(
                replyText = directResult.spokenConfirmation,
                actionSummary = directResult.displayMessage
            )
        }

        if (apiKey.isBlank()) {
            val keyError = if (isHindi) "Gemini API key कॉन्फ़िगर नहीं है। कृपया सेटिंग्स या सीक्रेट्स पैनल में API key दर्ज करें।" else "Gemini API key is not configured. Please enter your API key in the Secrets panel or Settings."
            return@withContext GeminiRestResult(
                replyText = keyError,
                isError = true
            )
        }

        try {
            // Build contents array with conversation history
            val contentsJson = JSONArray()
            for (item in conversationHistory) {
                val turn = JSONObject().apply {
                    put("role", item.role)
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", item.text) })
                    }
                    put("parts", parts)
                }
                contentsJson.put(turn)
            }

            // Add current user prompt
            contentsJson.put(JSONObject().apply {
                put("role", "user")
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", userPrompt) })
                }
                put("parts", parts)
            })

            val langRule = when (language.lowercase()) {
                "en" -> "The user selected English mode. Always respond in natural, professional English."
                "ur" -> "The user selected Urdu mode. Always respond in fluent, polite Urdu (اردو)."
                "hi" -> "The user selected Hindi mode. Always respond in fluent, polite Hindi (हिन्दी)."
                "pa" -> "The user selected Punjabi mode. Always respond in authentic, natural Punjabi (پنجابی / ਪੰਜਾਬੀ)."
                else -> "MANDATORY MULTILINGUAL RULE: Automatically mirror the exact language used by the user! If the user speaks English, reply in English. If the user speaks Urdu, reply in Urdu. If the user speaks Punjabi (e.g. 'کی حال اے', 'کی کر رہے ہو', 'تسی کیویں او', 'ਕੀ ਹਾਲ ਹੈ'), reply in authentic, natural Punjabi. If the user speaks Hindi, reply in Hindi. If the user mixes languages or uses Roman transliteration, mirror their language and dialect naturally."
            }

            // System instructions: JARVIS Voice Assistant
            val systemPrompt = "You are JARVIS, an advanced, highly capable, and intelligent AI voice assistant built with real-time voice input and speech output. " +
                "You have native, fluent comprehension and natural spoken delivery across English, Urdu (اردو), Punjabi (پنجابی / ਪੰਜਾਬੀ), Hindi (हिंदी), and multiple languages. " +
                "$langRule " +
                "Keep spoken voice responses concise (1 to 3 sentences ideally), conversational, and direct for immediate voice output. " +
                "Use the provided tools whenever an action is needed."

            val systemInstruction = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                }
                put("parts", parts)
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsJson)
                put("systemInstruction", systemInstruction)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                }
                put("generationConfig", genConfig)

                // Tools
                val toolsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("functionDeclarations", createToolDeclarations())
                    })
                }
                put("tools", toolsArray)
            }

            // Using gemini-3.5-flash for fast and robust conversational replies
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(tag, "Gemini REST error ${response.code}: $responseBody")
                return@withContext GeminiRestResult(
                    replyText = "I encountered an issue connecting to Gemini. (HTTP ${response.code})",
                    isError = true
                )
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var replyText = ""
            var actionSummary: String? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        replyText += part.getString("text") + " "
                    }

                    // Handle tool call if requested by model
                    if (part.has("functionCall")) {
                        val fnCall = part.getJSONObject("functionCall")
                        val fnName = fnCall.getString("name")
                        val argsObj = fnCall.optJSONObject("args")
                        val argsMap = mutableMapOf<String, Any?>()
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                argsMap[k] = argsObj.get(k)
                            }
                        }

                        val execution = toolManager.executeTool(fnName, argsMap)
                        actionSummary = execution.displayMessage
                        replyText = execution.spokenConfirmation
                    }
                }
            }

            replyText = replyText.trim()
            if (replyText.isBlank()) {
                replyText = "I am at your service."
            }

            // Update history
            addHistoryItem(ChatMessageItem(role = "user", text = userPrompt))
            addHistoryItem(ChatMessageItem(role = "model", text = replyText, actionBadge = actionSummary))

            GeminiRestResult(
                replyText = replyText,
                actionSummary = actionSummary
            )
        } catch (e: Exception) {
            Log.e(tag, "Gemini REST call failed: ${e.message}", e)
            GeminiRestResult(
                replyText = "Pardon me, I encountered an error: ${e.message}",
                isError = true
            )
        }
    }

    private suspend fun checkDirectCommand(prompt: String, language: String = "en"): ToolExecutionResult? {
        val lower = prompt.trim().lowercase()

        // Greeting checks
        if (lower == "hello jarvis" || lower == "hey jarvis" || lower == "नमस्ते जार्विस" || lower == "नमस्ते" || lower == "हेलो जार्विस") {
            val spoken = if (language.startsWith("hi")) {
                "नमस्ते! मैं जार्विस (JARVIS) हूँ। आप मुझसे कोई भी सवाल पूछ सकते हैं या कोई काम करने को कह सकते हैं।"
            } else {
                "Hello! I am JARVIS, your AI voice assistant. How may I be of service to you today?"
            }
            return ToolExecutionResult(
                spokenConfirmation = spoken,
                displayMessage = "GREETING",
                actionType = "VOICE_GREETING"
            )
        }

        // JARVIS identity check
        if (lower.contains("तुम कौन हो") || lower.contains("who are you") || lower.contains("तुम्हारा नाम क्या है") || lower == "what is your name") {
            val spoken = if (language.startsWith("hi")) {
                "मैं जार्विस (JARVIS) हूँ, आपका आधुनिक एआई वॉयस असिस्टेंट। मैं वॉयस इनपुट और वॉयस आउटपुट के साथ आपकी हर सहायता के लिए तत्पर हूँ।"
            } else {
                "I am JARVIS, your advanced AI voice assistant, equipped with real-time voice input, voice output, and system actions."
            }
            return ToolExecutionResult(
                spokenConfirmation = spoken,
                displayMessage = "JARVIS Identity",
                actionType = "MODEL_INFO"
            )
        }

        return when {
            lower.contains("time is it") || lower == "what is the time" || lower.contains("समय क्या हुआ") || lower.contains("टाइम क्या है") || lower.contains("कितने बजे") -> {
                toolManager.executeTool("get_current_time", emptyMap(), language)
            }
            lower.contains("today's date") || lower.contains("what is today's date") || lower.contains("tell me the date") || lower.contains("आज क्या तारीख") || lower.contains("आज की तारीख") -> {
                toolManager.executeTool("get_current_date", emptyMap(), language)
            }
            // Video Scrolling Voice Commands
            lower.contains("scroll the video") || lower.contains("scroll video") ||
            lower.contains("स्क्रॉल द वीडियो") || lower.contains("स्क्रॉल वीडियो") ||
            lower.contains("वीडियो स्क्रॉल") || lower.contains("اگلی ویڈیو") ||
            lower.contains("next video") || lower.contains("ویڈیو اسکرول") ||
            lower.contains("ویڈیو سکرول") || lower.contains("سکرول دی ویڈیو") ||
            lower.contains("اسکرول دی ویڈیو") || lower.contains("سکڑول دی ویڈیو") ||
            lower.contains("اگلا ویڈیو") || lower.contains("अगली वीडियो") ||
            lower.contains("अगला वीडियो") || lower.contains("रील स्क्रॉल") ||
            lower.contains("शॉर्ट्स स्क्रॉल") -> {
                toolManager.executeTool("scroll_video", mapOf("direction" to "down"), language)
            }

            lower.contains("previous video") || lower.contains("पिछला वीडियो") ||
            lower.contains("پچھلی ویڈیو") || lower.contains("scroll up video") ||
            lower.contains("स्क्रॉल अप") || lower.contains("اوپر اسکرول") ||
            lower.contains("اوپر سکرول") -> {
                toolManager.executeTool("scroll_video", mapOf("direction" to "up"), language)
            }

            // Send / Share Video Voice Commands
            lower.contains("سینڈ کر دو") || lower.contains("سینڈ کرو") ||
            lower.contains("سینڈ کر دے") || lower.contains("سیند کر دو") ||
            lower.contains("send this video") || lower.contains("send video") ||
            lower.contains("share video") || lower.contains("شیئر کرو") ||
            lower.contains("شیئر کر دو") || lower.contains("ویڈیو شیئر") ||
            lower.contains("ویڈیو بھیجو") || lower.contains("ویڈیو بھیج دو") ||
            lower.contains("सेंड कर दो") || lower.contains("सेंड करो") ||
            lower.contains("वीडियो भेजो") || lower.contains("वीडियो सेंड") ||
            (lower.contains("ویڈیو") && (lower.contains("بھیج") || lower.contains("سینڈ"))) ||
            (lower.contains("वीडियो") && (lower.contains("भेज") || lower.contains("सेंड"))) -> {
                val recipient = when {
                    lower.contains("whatsapp") || lower.contains("व्हाट्सएप") || lower.contains("واٹس") -> "whatsapp"
                    else -> ""
                }
                toolManager.executeTool("send_video", mapOf("recipient" to recipient), language)
            }

            // Call Contact Voice Commands
            lower.contains("کانٹیکٹ میں سے کال") || lower.contains("کانٹیکٹ سے کال") ||
            lower.contains("کانٹیکٹ میں سے") || lower.contains("کانٹیکٹ سے") ||
            lower.contains("कांटेक्ट में से कॉल") || lower.contains("कांटेक्ट से कॉल") ||
            lower.contains("कॉल लगाओ") || lower.contains("कॉल करो") ||
            lower.contains("फोन मिलाओ") || lower.contains("فون ملاؤ") ||
            lower.contains("کال ملاؤ") || lower.contains("کال کرو") ||
            lower.contains("کال لگاؤ") || lower.contains("کال لگا دے") ||
            lower.startsWith("call ") || lower.contains("make a call to ") -> {
                val name = when {
                    lower.startsWith("call ") -> prompt.substringAfter("call ").trim()
                    lower.contains("make a call to ") -> prompt.substringAfter("make a call to ").trim()
                    lower.contains("کانٹیکٹ میں سے") || lower.contains("کانٹیکٹ سے") -> {
                        prompt.replaceFirst("(?i)کانٹیکٹ( میں)? سے ".toRegex(), "")
                            .replaceFirst("(?i)(کو)?\\s*(کال لگاؤ|کال لگا دے|کال کرو|فون ملاؤ|کال ملاؤ)".toRegex(), "")
                            .trim()
                    }
                    lower.contains("कांटेक्ट में से") || lower.contains("कांटेक्ट से") -> {
                        prompt.replaceFirst("(?i)कांटेक्ट( में)? से ".toRegex(), "")
                            .replaceFirst("(?i)(को)?\\s*(कॉल लगाओ|कॉल करो|फोन मिलाओ)".toRegex(), "")
                            .trim()
                    }
                    lower.contains("کال لگاؤ") -> prompt.replaceFirst("(?i)(کو)?\\s*کال لگاؤ".toRegex(), "").trim()
                    lower.contains("کال لگا دے") -> prompt.replaceFirst("(?i)(کو)?\\s*کال لگا دے".toRegex(), "").trim()
                    lower.contains("کال ملاؤ") -> prompt.replaceFirst("(?i)(کو)?\\s*کال ملاؤ".toRegex(), "").trim()
                    lower.contains("کال کرو") -> prompt.replaceFirst("(?i)(کو)?\\s*کال کرو".toRegex(), "").trim()
                    lower.contains("कॉल लगाओ") -> prompt.replaceFirst("(?i)(को)?\\s*कॉल लगाओ".toRegex(), "").trim()
                    lower.contains("कॉल करो") -> prompt.replaceFirst("(?i)(को)?\\s*कॉल करो".toRegex(), "").trim()
                    lower.contains("फोन मिलाओ") -> prompt.replaceFirst("(?i)(को)?\\s*फोन मिलाओ".toRegex(), "").trim()
                    else -> ""
                }
                toolManager.executeTool("call_contact", mapOf("name" to name), language)
            }

            // Search in App Voice Commands (YouTube, Play Store, etc.)
            (lower.contains("यूट्यूब") || lower.contains("یوٹیوب") || lower.contains("youtube")) &&
            (lower.contains("सर्च") || lower.contains("खोजो") || lower.contains("تلاش") || lower.contains("search") || lower.contains("سرچ")) -> {
                val q = prompt.replaceFirst("(?i)(यूट्यूब पर|یوٹیوب پر|on youtube|youtube per)\\s*".toRegex(), "")
                    .replaceFirst("(?i)(سर्च करो|سर्च|खोजो|search for|search|تلاش کرو|سرچ کرو|سرچ)\\s*".toRegex(), "")
                    .replaceFirst("(?i)(سर्च करो|سर्च|खोजो|تلاش کرو|سرچ کرو|سرچ)\\s*".toRegex(), "")
                    .trim()
                toolManager.executeTool("search_in_app", mapOf("app_name" to "youtube", "query" to q), language)
            }

            // General search in named app: e.g. "[App] پر [Query] سرچ کرو" or "[App] पर [Query] सर्च करो"
            lower.contains(" پر ") && (lower.contains("سرچ کرو") || lower.contains("تلاش کرو") || lower.contains("سرچ") || lower.contains("سर्च करो") || lower.contains("सर्च")) -> {
                val parts = prompt.split(" پر ")
                val appPart = parts.getOrNull(0)?.trim() ?: ""
                val rest = parts.getOrNull(1)?.replaceFirst("(?i)(سرچ کرو|تلاش کرو|سرچ|سर्च करो|सर्च|खोजो)".toRegex(), "")?.trim() ?: ""
                toolManager.executeTool("search_in_app", mapOf("app_name" to appPart, "query" to rest), language)
            }

            lower.contains(" पर ") && (lower.contains("सर्च करो") || lower.contains("खोजो") || lower.contains("सर्च")) -> {
                val parts = prompt.split(" पर ")
                val appPart = parts.getOrNull(0)?.trim() ?: ""
                val rest = parts.getOrNull(1)?.replaceFirst("(?i)(सर्च करो|सर्च|खोजो)".toRegex(), "")?.trim() ?: ""
                toolManager.executeTool("search_in_app", mapOf("app_name" to appPart, "query" to rest), language)
            }

            // Open Video & Reels Hub
            lower.contains("video hub") || lower.contains("ویڈیو ہب") || lower.contains("वीडियो हब") || lower.contains("ریلس ہب") || lower.contains("रील्स हب") -> {
                toolManager.executeTool("open_video_hub", emptyMap(), language)
            }

            // Open ANY app by name (e.g. "WhatsApp کھولو", "یہ والی ایپ اوپن کر دے", "Open Instagram", "Camera चलाओ")
            lower.startsWith("open ") -> {
                val appName = prompt.substringAfter("open ").trim()
                toolManager.executeTool("open_any_app", mapOf("app_name" to appName), language)
            }
            lower.contains("اوپن کر دے") || lower.contains("اوپن کر دو") || lower.contains("اوپن کرو") -> {
                val appName = prompt.replaceFirst("(?i)(یہ والی ایپ|یہ ایپ|کوئی بھی ایپ|اوپن کر دے|اوپن کر دو|اوپن کرو|app open kar de|open kar do|open kar de)\\s*".toRegex(), "")
                    .replaceFirst("(?i)(اوپن کر دے|اوپن کر دو|اوپن کرو)$".toRegex(), "")
                    .trim()
                toolManager.executeTool("open_any_app", mapOf("app_name" to appName), language)
            }
            lower.endsWith(" खोलो") || lower.endsWith(" खोल दो") || lower.endsWith(" चलाओ") || lower.endsWith(" ओपन करो") || lower.endsWith(" کھولو") || lower.endsWith(" کھول دو") -> {
                val appName = prompt.replaceFirst("(?i)\\s*(खोलो|खोल दो|चलाओ|ओपन करो|کھولو|کھول دو)$".toRegex(), "").trim()
                toolManager.executeTool("open_any_app", mapOf("app_name" to appName), language)
            }

            lower == "open youtube" || lower == "यूट्यूब खोलो" || lower == "यूट्यूब चलाओ" -> {
                toolManager.executeTool("open_youtube", emptyMap(), language)
            }
            lower == "open google" || lower == "गूगल खोलो" || lower == "गूगल चलाओ" -> {
                toolManager.executeTool("open_google", emptyMap(), language)
            }
            lower.startsWith("search google for ") -> {
                val q = prompt.substringAfter("search google for ").trim()
                toolManager.executeTool("search_google", mapOf("query" to q), language)
            }
            lower.startsWith("गूगल पर सर्च करो ") || lower.startsWith("गूगल पर खोजो ") -> {
                val q = prompt.replaceFirst("(?i)गूगल पर (सर्च करो|खोजो) ".toRegex(), "").trim()
                toolManager.executeTool("search_google", mapOf("query" to q), "hi")
            }
            lower.contains("what is my location") || lower.contains("where am i") || lower.contains("मेरी लोकेशन") -> {
                toolManager.executeTool("get_location", emptyMap(), language)
            }
            lower.contains("what's the weather") || lower.contains("what is the weather") || lower.contains("weather kaisa hai") || lower.contains("मौसम कैसा है") || lower.contains("आज का मौसम") -> {
                toolManager.executeTool("get_weather", mapOf("city" to if (language.startsWith("hi")) "आपके क्षेत्र" else "your location"), language)
            }
            lower.startsWith("create a note ") || lower.startsWith("create note ") -> {
                val noteText = prompt.replaceFirst("(?i)create a note ".toRegex(), "")
                    .replaceFirst("(?i)create note ".toRegex(), "").trim()
                toolManager.executeTool("create_note", mapOf("content" to noteText), language)
            }
            lower.startsWith("नोट बनाओ ") || lower.startsWith("नोट लिखो ") -> {
                val noteText = prompt.replaceFirst("(?i)(नोट बनाओ|नोट लिखो) ".toRegex(), "").trim()
                toolManager.executeTool("create_note", mapOf("content" to noteText), "hi")
            }
            lower == "read my notes" || lower == "read notes" || lower == "नोट्स पढ़ो" || lower == "मेरे नोट्स" -> {
                toolManager.executeTool("read_notes", emptyMap(), language)
            }
            lower.startsWith("remind me in ") -> {
                val afterIn = lower.substringAfter("remind me in ")
                val mins = afterIn.substringBefore(" ").toIntOrNull() ?: 10
                val title = if (afterIn.contains(" to ")) afterIn.substringAfter(" to ").trim() else "Reminder"
                toolManager.executeTool("create_reminder", mapOf("title" to title, "minutes" to mins), language)
            }
            lower.startsWith("calculate ") -> {
                val expr = prompt.substringAfter("calculate ").trim()
                toolManager.executeTool("calculate", mapOf("expression" to expr), language)
            }
            lower.startsWith("हिसाब करो ") || lower.startsWith("जोड़ो ") -> {
                val expr = prompt.replaceFirst("(?i)(हिसाब करो|जोड़ो) ".toRegex(), "").trim()
                toolManager.executeTool("calculate", mapOf("expression" to expr), "hi")
            }

            // Voice Gender Switch Commands (میل وائس یا فی میل وائس)
            lower.contains("female voice") || lower.contains("فی میل وائس") ||
            lower.contains("زنانی آواز") || lower.contains("फीमेल वॉयस") || lower.contains("लड़की की आवाज") -> {
                toolManager.executeTool("set_voice_gender", mapOf("gender" to "female"), language)
            }
            lower.contains("male voice") || lower.contains("میل وائس") ||
            lower.contains("مردانہ آواز") || lower.contains("मेल वॉयस") || lower.contains("लड़के की आवाज") -> {
                toolManager.executeTool("set_voice_gender", mapOf("gender" to "male"), language)
            }

            // Language Selection Voice Commands
            lower.contains("speak in punjabi") || lower.contains("پنجابی بولو") || lower.contains("پنجابی چ گل") || lower.contains("पंजाबी बोलो") -> {
                toolManager.executeTool("set_language", mapOf("language" to "pa"), language)
            }
            lower.contains("speak in urdu") || lower.contains("اردو بولو") || lower.contains("اردو میں بات") || lower.contains("उर्दू बोलो") -> {
                toolManager.executeTool("set_language", mapOf("language" to "ur"), language)
            }
            lower.contains("speak in hindi") || lower.contains("हिंदी बोलो") || lower.contains("ہندی بولو") -> {
                toolManager.executeTool("set_language", mapOf("language" to "hi"), language)
            }
            lower.contains("speak in english") || lower.contains("انگلش بولو") || lower.contains("अंग्रेजी बोलो") -> {
                toolManager.executeTool("set_language", mapOf("language" to "en"), language)
            }
            lower.contains("auto language") || lower.contains("multiple language") || lower.contains("خودکار زبان") || lower.contains("آٹو زبان") -> {
                toolManager.executeTool("set_language", mapOf("language" to "auto"), language)
            }
            lower.contains("feedback") || lower.contains("فیڈ بیک") || lower.contains("رائے") ||
            lower.contains("فीडबैक") || lower.contains("प्रतिक्रिया") || lower.contains("ریٹنگ") ||
            lower.contains("रेटिंग") || lower.contains("rate this") || lower.contains("rate jarvis") ||
            lower.contains("rate app") -> {
                toolManager.executeTool("open_feedback", emptyMap(), language)
            }
            // Termux Python Stop Command
            lower.contains("پائتھن بند") || (lower.contains("بیک گراؤنڈ") && lower.contains("پائتھن")) ||
            lower.contains("پائتھن روکو") || lower.contains("ٹرمکس بند") ||
            lower.contains("पायथन बंद") || lower.contains("stop python") ||
            lower.contains("kill python") || lower.contains("stop background python") ||
            lower.contains("stop termux") -> {
                toolManager.executeTool("stop_termux_python", emptyMap(), language)
            }

            // Open Termux Python Web App
            lower.contains("ویب سائٹ کھولو") || lower.contains("پائتھن ایپ کھولو") ||
            lower.contains("اوپن پائتھن") || lower == "open python app" ||
            lower == "open termux app" || lower == "open web app" -> {
                toolManager.executeTool("open_termux_app", emptyMap(), language)
            }

            // Termux Python Create & Run in Background
            (lower.contains("ٹرمکس") && (lower.contains("پائتھن") || lower.contains("ایپ") || lower.contains("ویب سائٹ") || lower.contains("رن"))) ||
            (lower.contains("پائتھن") && (lower.contains("رن") || lower.contains("چلاؤ") || lower.contains("بناؤ") || lower.contains("سرور"))) ||
            (lower.contains("ٹرمکس") || lower.contains("termux")) ||
            lower.contains("run python") || lower.contains("start python") ||
            lower.contains("create python app") || lower.contains("create website in python") ||
            lower.contains("make a website in termux") -> {
                toolManager.executeTool("run_termux_python_app", mapOf("prompt" to prompt), language)
            }

            lower.contains("open calendar") || lower.contains("कैलेंडर खोलो") || lower.contains("کیلنڈر کھولو") -> {
                toolManager.executeTool("open_calendar", emptyMap(), language)
            }
            else -> null
        }
    }

    private fun createToolDeclarations(): JSONArray {
        val list = JSONArray()
        list.put(JSONObject().apply {
            put("name", "get_current_time")
            put("description", "Get current time")
        })
        list.put(JSONObject().apply {
            put("name", "get_current_date")
            put("description", "Get current date")
        })
        list.put(JSONObject().apply {
            put("name", "open_youtube")
            put("description", "Open YouTube app or web")
        })
        list.put(JSONObject().apply {
            put("name", "open_google")
            put("description", "Open Google search homepage")
        })
        list.put(JSONObject().apply {
            put("name", "run_termux_python_app")
            put("description", "Generate a web app or website, run Python in Termux background, and notify when ready")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("prompt", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Description or type of app/website to generate (e.g. portfolio, todo, weather, calculator)")
                    })
                })
                put("required", JSONArray().apply { put("prompt") })
            })
        })
        list.put(JSONObject().apply {
            put("name", "stop_termux_python")
            put("description", "Stop the background Python server and Termux process")
        })
        list.put(JSONObject().apply {
            put("name", "open_termux_app")
            put("description", "Open the running Python web application in mobile browser")
        })
        list.put(JSONObject().apply {
            put("name", "search_google")
            put("description", "Search Google with query")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search query")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })
        list.put(JSONObject().apply {
            put("name", "get_weather")
            put("description", "Get weather for location")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("city", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "City name")
                    })
                })
            })
        })
        list.put(JSONObject().apply {
            put("name", "create_note")
            put("description", "Save a note")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Content of note")
                    })
                })
                put("required", JSONArray().apply { put("content") })
            })
        })
        list.put(JSONObject().apply {
            put("name", "read_notes")
            put("description", "Read saved notes")
        })
        list.put(JSONObject().apply {
            put("name", "create_reminder")
            put("description", "Create a reminder")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Reminder title")
                    })
                    put("minutes", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Minutes")
                    })
                })
                put("required", JSONArray().apply { put("title") })
            })
        })
        list.put(JSONObject().apply {
            put("name", "calculate")
            put("description", "Calculate math expression")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("expression", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Expression")
                    })
                })
                put("required", JSONArray().apply { put("expression") })
            })
        })
        list.put(JSONObject().apply {
            put("name", "open_calendar")
            put("description", "Open calendar")
        })
        list.put(JSONObject().apply {
            put("name", "open_any_app")
            put("description", "Open any installed application or system tool by name (e.g. WhatsApp, Camera, YouTube, Instagram, Chrome, Maps, Settings, etc.)")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("app_name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the app to launch (e.g. WhatsApp, YouTube, Instagram, Camera, Maps)")
                    })
                })
                put("required", JSONArray().apply { put("app_name") })
            })
        })
        list.put(JSONObject().apply {
            put("name", "search_in_app")
            put("description", "Search for videos or queries inside a specific app (such as YouTube, Google, Play Store, Maps)")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("app_name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "App to search inside (e.g. YouTube, Google, Play Store, Maps)")
                    })
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search query or video topic to search for")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })
        list.put(JSONObject().apply {
            put("name", "scroll_video")
            put("description", "Scroll to next or previous video in video feeds, reels, shorts, or in-app video hub")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("direction", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'down' for next video, 'up' for previous video")
                    })
                })
            })
        })
        list.put(JSONObject().apply {
            put("name", "send_video")
            put("description", "Send or share the current video to someone via WhatsApp or system share")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("recipient", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Recipient name or 'whatsapp'")
                    })
                    put("video_title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Title of the video being shared")
                    })
                    put("video_url", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "URL of the video")
                    })
                })
            })
        })
        list.put(JSONObject().apply {
            put("name", "call_contact")
            put("description", "Call someone from phone contacts by person name or contact name")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Contact name to call (e.g. Rahul, Papa, Mom, Amit)")
                    })
                })
                put("required", JSONArray().apply { put("name") })
            })
        })
        list.put(JSONObject().apply {
            put("name", "open_video_hub")
            put("description", "Open the in-app video and reels hub for viewing and voice-controlled scrolling")
        })
        return list
    }
}
