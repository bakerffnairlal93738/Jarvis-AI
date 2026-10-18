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
        return list
    }
}
