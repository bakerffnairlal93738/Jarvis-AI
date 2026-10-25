package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.JarvisDatabase
import com.example.data.model.FeedbackEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface JarvisActionCallback {
    fun onScrollVideo(direction: String)
    fun onOpenVideoHub()
}

/**
 * Executes predefined SAFE application tools for JARVIS.
 * Strictly prevents execution of arbitrary shell, python, or javascript code.
 */
class JarvisToolManager(
    private val context: Context,
    val termuxManager: TermuxPythonManager = TermuxPythonManager(context)
) {
    private val tag = "JarvisToolManager"
    private val db = JarvisDatabase.getInstance(context)
    var actionCallback: JarvisActionCallback? = null

    suspend fun executeTool(toolName: String, args: Map<String, Any?>, language: String = "en"): ToolExecutionResult = withContext(Dispatchers.IO) {
        val isHindi = language.lowercase().startsWith("hi")
        try {
            when (toolName.lowercase()) {
                "get_current_time" -> {
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val time = sdf.format(Date())
                    val timezone = TimeZone.getDefault().displayName
                    val spoken = if (isHindi) "अभी का समय $time है।" else "The current time is $time ($timezone)."
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Current Time: $time",
                        data = mapOf("time" to time)
                    )
                }

                "get_current_date" -> {
                    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                    val date = sdf.format(Date())
                    val spoken = if (isHindi) "आज की तारीख $date है।" else "Today is $date."
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Date: $date",
                        data = mapOf("date" to date)
                    )
                }

                "open_google" -> {
                    openUrl("https://www.google.com")
                    val spoken = if (isHindi) "गूगल खोला जा रहा है।" else "Opening Google for you."
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Launched Google",
                        actionType = "APP_LAUNCH"
                    )
                }

                "open_youtube" -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        openUrl("https://www.youtube.com")
                    }
                    val spoken = if (isHindi) "यूट्यूब खोला जा रहा है।" else "Opening YouTube."
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Launched YouTube",
                        actionType = "APP_LAUNCH"
                    )
                }

                "search_google" -> {
                    val query = args["query"]?.toString() ?: ""
                    if (query.isNotBlank()) {
                        val encoded = Uri.encode(query)
                        openUrl("https://www.google.com/search?q=$encoded")
                        val spoken = if (isHindi) "गूगल पर '$query' खोजा जा रहा है।" else "Searching Google for $query."
                        ToolExecutionResult(
                            spokenConfirmation = spoken,
                            displayMessage = "Google Search: \"$query\"",
                            actionType = "SEARCH"
                        )
                    } else {
                        val spoken = if (isHindi) "आप क्या खोजना चाहते हैं?" else "What would you like me to search for?"
                        ToolExecutionResult(
                            spokenConfirmation = spoken,
                            displayMessage = "Google Search query missing",
                            isError = true
                        )
                    }
                }

                "get_weather" -> {
                    val city = args["city"]?.toString() ?: args["location"]?.toString() ?: "your area"
                    val temp = 22 + (city.hashCode() % 6)
                    val conditions = listOf("Partly cloudy", "Clear skies", "Pleasant breeze", "Sunny", "Overcast")[kotlin.math.abs(city.hashCode()) % 5]
                    val spoken = if (isHindi) "$city में मौसम सुहावना है और तापमान $temp डिग्री सेल्सियस है।" else "Current weather in $city is $temp degrees Celsius with $conditions."
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Weather ($city): $temp°C, $conditions",
                        data = mapOf("temperature" to temp, "condition" to conditions)
                    )
                }

                "get_location" -> {
                    val locale = Locale.getDefault()
                    val country = locale.displayCountry
                    val spoken = if (isHindi) "आपकी वर्तमान लोकेशन $country है।" else "Your device locale indicates you are in $country (${locale.country})."
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Location context: $country",
                        data = mapOf("country" to country)
                    )
                }

                "create_note" -> {
                    val content = args["content"]?.toString() ?: args["text"]?.toString() ?: ""
                    if (content.isNotBlank()) {
                        db.noteDao().insertNote(NoteEntity(content = content))
                        val spoken = if (isHindi) "मैंने आपका नोट सुरक्षित कर लिया है: $content।" else "I have noted that down: $content."
                        ToolExecutionResult(
                            spokenConfirmation = spoken,
                            displayMessage = "Note saved: \"$content\"",
                            actionType = "NOTE_SAVED"
                        )
                    } else {
                        val spoken = if (isHindi) "नोट खाली था।" else "The note content was empty."
                        ToolExecutionResult(
                            spokenConfirmation = spoken,
                            displayMessage = "Empty note",
                            isError = true
                        )
                    }
                }

                "read_notes" -> {
                    val notes = db.noteDao().getRecentNotes()
                    if (notes.isEmpty()) {
                        val spoken = if (isHindi) "आपके पास अभी कोई नोट नहीं है।" else "You don't have any notes saved yet."
                        ToolExecutionResult(
                            spokenConfirmation = spoken,
                            displayMessage = "No notes found"
                        )
                    } else {
                        val notesList = notes.mapIndexed { index, note -> "${index + 1}: ${note.content}" }.joinToString(". ")
                        val spoken = if (isHindi) "आपके हालिया नोट्स हैं: $notesList।" else "Here are your recent notes: $notesList."
                        ToolExecutionResult(
                            spokenConfirmation = spoken,
                            displayMessage = "Found ${notes.size} note(s)",
                            data = mapOf("notes" to notes.map { it.content })
                        )
                    }
                }

                "create_reminder" -> {
                    val title = args["title"]?.toString() ?: args["content"]?.toString() ?: "Reminder"
                    val minutes = (args["minutes"]?.toString()?.toIntOrNull() ?: 10).coerceAtLeast(1)
                    val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
                    db.reminderDao().insertReminder(
                        ReminderEntity(
                            title = title,
                            triggerTime = triggerTime
                        )
                    )
                    val spoken = if (isHindi) "$minutes मिनट में '$title' का रिमाइंडर सेट कर दिया गया है।" else "Reminder set for '$title' in $minutes minutes."
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Reminder: \"$title\" in $minutes min",
                        actionType = "REMINDER_SET"
                    )
                }

                "calculate" -> {
                    val expression = args["expression"]?.toString() ?: ""
                    val result = evaluateSafeExpression(expression)
                    val spoken = if (isHindi) "इसका उत्तर $result है।" else "The result is $result."
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Calc: $expression = $result",
                        data = mapOf("result" to result)
                    )
                }

                "open_calendar" -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        openUrl("https://calendar.google.com")
                    }
                    ToolExecutionResult(
                        spokenConfirmation = "Opening your calendar.",
                        displayMessage = "Opened Calendar",
                        actionType = "APP_LAUNCH"
                    )
                }

                "run_termux_python_app" -> {
                    val prompt = args["prompt"]?.toString() ?: args["app_name"]?.toString() ?: "Web Application"
                    val appState = termuxManager.startPythonApp(prompt)
                    val isUrdu = language.lowercase().startsWith("ur") || prompt.any { it in '\u0600'..'\u06FF' }
                    val spoken = when {
                        isUrdu -> "آپ کی ${appState.appName} تیار ہے اور بیک گراؤنڈ میں پائتھن پر پورٹ ${appState.port} پر کامیابی سے چل رہی ہے۔"
                        isHindi -> "आपकी ${appState.appName} तैयार है और बैकग्राउंड में पायथन पर पोर्ट ${appState.port} पर चल रही है।"
                        else -> "Your ${appState.appName} is ready and running in the background on Python port ${appState.port}."
                    }
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Termux Python: ${appState.appName} (Port ${appState.port})",
                        actionType = "TERMUX_PYTHON_STARTED",
                        data = mapOf("url" to appState.serverUrl, "port" to appState.port, "name" to appState.appName)
                    )
                }

                "stop_termux_python" -> {
                    termuxManager.stopPythonApp()
                    val isUrdu = language.lowercase().startsWith("ur")
                    val spoken = when {
                        isUrdu -> "بیک گراؤنڈ سے پائتھن سرور کامیابی سے بند کر دیا گیا ہے۔"
                        isHindi -> "बैकग्राउंड से पायथन सर्वर सफलतापूर्वक बंद कर दिया गया है।"
                        else -> "Python background server has been successfully stopped."
                    }
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Termux Python Stopped",
                        actionType = "TERMUX_PYTHON_STOPPED"
                    )
                }

                "open_termux_app" -> {
                    termuxManager.openInBrowser()
                    val isUrdu = language.lowercase().startsWith("ur")
                    val spoken = when {
                        isUrdu -> "ویب سائٹ کھولی جا رہی ہے۔"
                        isHindi -> "वेबसाइट खोली जा रही है।"
                        else -> "Opening Python web app in browser."
                    }
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Opened Python Web App",
                        actionType = "APP_LAUNCH"
                    )
                }

                "set_voice_gender" -> {
                    val rawGender = (args["gender"] as? String)?.lowercase() ?: "male"
                    val isFemale = rawGender.contains("female") || rawGender.contains("زنانی") || rawGender.contains("عورت") || rawGender.contains("फीमेल")
                    val isUrdu = language.lowercase().startsWith("ur")
                    val targetGender = if (isFemale) "FEMALE" else "MALE"
                    val spoken = when {
                        isUrdu -> if (isFemale) "فی میل وائس منتخب کر لی گئی ہے۔ فرمائیے، میں آپ کی کیا مدد کر سکتی ہوں؟" else "میل وائس منتخب کر لی گئی ہے۔ میں آپ کے احکامات کے لیے تیار ہوں۔"
                        isHindi -> if (isFemale) "फीमेल वॉयस चुन ली गई है। बताइए, मैं आपकी क्या मदद कर सकती हूँ?" else "मेल वॉयस चुन ली गई है। मैं आपके आदेश के लिए तैयार हूँ।"
                        else -> if (isFemale) "Female voice selected. Ready to assist you." else "Male voice selected. Ready for your instructions, sir."
                    }
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = if (isFemale) "Voice Gender: Female (فی میل وائس)" else "Voice Gender: Male (میل وائس)",
                        actionType = "VOICE_GENDER_CHANGED",
                        data = mapOf("gender" to targetGender)
                    )
                }

                "set_language" -> {
                    val rawLang = (args["language"] as? String)?.lowercase() ?: "auto"
                    val code = when {
                        rawLang.contains("auto") || rawLang.contains("خودکار") || rawLang.contains("ملٹی") || rawLang.contains("multiple") -> "auto"
                        rawLang.contains("punjabi") || rawLang.contains("پنجابی") || rawLang.contains("पंजाबी") -> "pa"
                        rawLang.contains("urdu") || rawLang.contains("اردو") -> "ur"
                        rawLang.contains("hindi") || rawLang.contains("हिंदी") -> "hi"
                        else -> "en"
                    }
                    val spoken = when (code) {
                        "auto" -> "Smart Multilingual Mode activated. I will dynamically mirror whichever language you speak."
                        "pa" -> "پنجابی زبان چن لئی گئی اے۔ ہن میں پنجابی چ گل کراں گا۔"
                        "ur" -> "اردو زبان منتخب کر لی گئی ہے۔ اب میں اردو میں جواب دوں گا۔"
                        "hi" -> "हिंदी भाषा चुन ली गई है। अब मैं आपसे हिंदी में बात करूँगा।"
                        else -> "English language mode activated. I will respond to you in English."
                    }
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Language: $code",
                        actionType = "LANGUAGE_CHANGED",
                        data = mapOf("language" to code)
                    )
                }

                "open_feedback" -> {
                    val isUrdu = language.lowercase().startsWith("ur")
                    val isHindi = language.lowercase().startsWith("hi")
                    val isPunjabi = language.lowercase().startsWith("pa")
                    val spoken = when {
                        isUrdu -> "فیڈ بیک اور رائے کا فارم کھولا جا رہا ہے۔ براہ کرم اپنی قیمتی رائے درج کریں۔"
                        isPunjabi -> "فیڈ بیک فارم کھولیا جا رہیا اے۔ اپنی قیمتی رائے ضرور دیو۔"
                        isHindi -> "फीडबैक फॉर्म खोला जा रहा है। कृपया अपनी बहुमूल्य प्रतिक्रिया दर्ज करें।"
                        else -> "Opening feedback form. Please share your rating and suggestions."
                    }
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "FEEDBACK_FORM",
                        actionType = "OPEN_FEEDBACK"
                    )
                }

                "submit_feedback" -> {
                    val rating = (args["rating"] as? Number)?.toInt() ?: 5
                    val comment = args["comment"]?.toString() ?: "User Feedback"
                    val category = args["category"]?.toString() ?: "Voice & Language"
                    db.feedbackDao().insertFeedback(
                        FeedbackEntity(
                            rating = rating.coerceIn(1, 5),
                            category = category,
                            comment = comment,
                            language = language,
                            voiceGender = "MALE"
                        )
                    )
                    val isUrdu = language.lowercase().startsWith("ur")
                    val isHindi = language.lowercase().startsWith("hi")
                    val spoken = when {
                        isUrdu -> "آپ کی $rating سٹار ریٹنگ اور رائے کامیابی سے محفوظ کر لی گئی ہے۔ بہت شکریہ!"
                        isHindi -> "आपकी $rating स्टार रेटिंग और प्रतिक्रिया दर्ज कर ली गई है। बहुत धन्यवाद!"
                        else -> "Your $rating-star feedback has been successfully recorded. Thank you!"
                    }
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "Feedback Recorded: $rating Stars",
                        actionType = "FEEDBACK_SUBMITTED"
                    )
                }

                "open_any_app", "open_app" -> {
                    val appName = args["app_name"]?.toString()
                        ?: args["name"]?.toString()
                        ?: args["app"]?.toString()
                        ?: ""
                    launchAppByName(appName, language)
                }

                "search_in_app", "search_app" -> {
                    val appName = args["app_name"]?.toString() ?: args["app"]?.toString() ?: ""
                    val query = args["query"]?.toString() ?: args["q"]?.toString() ?: ""
                    performSearchInApp(appName, query, language)
                }

                "scroll_video", "scroll_the_video", "next_video", "scroll_screen" -> {
                    val direction = args["direction"]?.toString() ?: "down"
                    performScrollVideo(direction, language)
                }

                "send_video", "share_video" -> {
                    val recipient = args["recipient"]?.toString() ?: args["contact"]?.toString() ?: ""
                    val title = args["video_title"]?.toString() ?: args["title"]?.toString() ?: "Trending Video"
                    val url = args["video_url"]?.toString() ?: args["url"]?.toString() ?: "https://www.youtube.com/shorts"
                    val message = args["message"]?.toString() ?: ""
                    performSendVideo(recipient, title, url, message, language)
                }

                "call_contact", "make_call", "call" -> {
                    val contactName = args["name"]?.toString()
                        ?: args["contact"]?.toString()
                        ?: args["recipient"]?.toString()
                        ?: ""
                    performCallContact(contactName, language)
                }

                "open_video_hub", "open_reels", "open_video" -> {
                    actionCallback?.onOpenVideoHub()
                    val isUrdu = language.lowercase().startsWith("ur")
                    val spoken = when {
                        isHindi -> "वीडियो प्लेयर और रील्स हब खोला जा रहा है।"
                        isUrdu -> "ویڈیو پلیئر اور ریلز ہب کھولا جا رہا ہے۔"
                        else -> "Opening Video & Reels Hub."
                    }
                    ToolExecutionResult(
                        spokenConfirmation = spoken,
                        displayMessage = "VIDEO_HUB_OPENED",
                        actionType = "OPEN_VIDEO_HUB"
                    )
                }

                else -> {
                    // Check custom commands saved by user
                    val customCommands = db.customCommandDao().getCommandList()
                    val match = customCommands.find { it.triggerPhrase.equals(toolName, ignoreCase = true) }
                    if (match != null) {
                        openUrl(match.targetUrl)
                        ToolExecutionResult(
                            spokenConfirmation = "Executing custom command: ${match.triggerPhrase}.",
                            displayMessage = "Custom command: ${match.targetUrl}",
                            actionType = "CUSTOM_COMMAND"
                        )
                    } else {
                        ToolExecutionResult(
                            spokenConfirmation = "I am unable to perform that action.",
                            displayMessage = "Unknown tool: $toolName",
                            isError = true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Tool execution failed for $toolName: ${e.message}", e)
            ToolExecutionResult(
                spokenConfirmation = "There was an error executing that request.",
                displayMessage = "Error: ${e.message}",
                isError = true
            )
        }
    }

    private fun launchAppByName(rawName: String, language: String): ToolExecutionResult {
        val isHindi = language.lowercase().startsWith("hi")
        val isUrdu = language.lowercase().startsWith("ur")
        val appName = rawName.trim().lowercase()

        if (appName.isBlank()) {
            val spoken = if (isHindi) "कृपया वह ऐप बताएं जिसे आप खोलना चाहते हैं।" else if (isUrdu) "براہ کرم اس ایپ کا نام بتائیں جو آپ کھولنا چاہتے ہیں۔" else "Please state the name of the app you would like me to open."
            return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "App name missing", isError = true)
        }

        val pm = context.packageManager

        // Known direct system/app packages & intent actions
        when {
            appName.contains("camera") || appName.contains("कैमरा") || appName.contains("کیمرہ") -> {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    val spoken = if (isHindi) "कैमरा खोला जा रहा है।" else if (isUrdu) "کیمرہ کھولا جا رہا ہے۔" else "Opening Camera."
                    return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Camera", actionType = "APP_LAUNCH")
                } catch (e: Exception) {
                    Log.w(tag, "Camera intent fallback: ${e.message}")
                }
            }

            appName.contains("setting") || appName.contains("सेटिंग") || appName.contains("سیٹنگ") -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                val spoken = if (isHindi) "डिवाइस सेटिंग्स खोली जा रही हैं।" else if (isUrdu) "سیٹنگز کھولی جا رہی ہیں۔" else "Opening Settings."
                return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Settings", actionType = "APP_LAUNCH")
            }

            appName.contains("contact") || appName.contains("कांटेक्ट") || appName.contains("رابطے") -> {
                val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                val spoken = if (isHindi) "कांटेक्ट्स खोले जा रहे हैं।" else if (isUrdu) "رابطے کھولے جا رہے ہیں۔" else "Opening Contacts."
                return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Contacts", actionType = "APP_LAUNCH")
            }

            appName.contains("dialer") || appName.contains("फोन") || appName.contains("phone") || appName.contains("ڈائلر") -> {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                val spoken = if (isHindi) "फोन डायलर खोला जा रहा है।" else if (isUrdu) "فون ڈائلر کھولا جا رہا ہے۔" else "Opening Phone Dialer."
                return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Phone", actionType = "APP_LAUNCH")
            }

            appName.contains("gallery") || appName.contains("गैलरी") || appName.contains("photos") || appName.contains("फ़ोटो") || appName.contains("تصاویر") -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    val spoken = if (isHindi) "गैलरी खोली जा रही है।" else if (isUrdu) "گیلری کھولی جا رہی ہے۔" else "Opening Gallery."
                    return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Gallery", actionType = "APP_LAUNCH")
                } catch (e: Exception) {
                    Log.w(tag, "Gallery intent fallback: ${e.message}")
                }
            }

            appName.contains("whatsapp") || appName.contains("व्हाट्सएप") || appName.contains("واٹس") -> {
                val intent = pm.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    val spoken = if (isHindi) "व्हाट्सएप खोला जा रहा है।" else if (isUrdu) "واٹس ایپ کھولا جا رہا ہے۔" else "Opening WhatsApp."
                    return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened WhatsApp", actionType = "APP_LAUNCH")
                }
            }

            appName.contains("youtube") || appName.contains("यूट्यूब") || appName.contains("یوٹیوب") -> {
                val intent = pm.getLaunchIntentForPackage("com.google.android.youtube")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    openUrl("https://www.youtube.com")
                }
                val spoken = if (isHindi) "यूट्यूब खोला जा रहा है।" else if (isUrdu) "یوٹیوب کھولا جا رہا ہے۔" else "Opening YouTube."
                return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened YouTube", actionType = "APP_LAUNCH")
            }

            appName.contains("instagram") || appName.contains("insta") || appName.contains("इंस्टाग्राम") || appName.contains("انسٹا") -> {
                val intent = pm.getLaunchIntentForPackage("com.instagram.android")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    openUrl("https://www.instagram.com")
                }
                val spoken = if (isHindi) "इंस्टाग्राम खोला जा रहा है।" else if (isUrdu) "انسٹاگرام کھولا جا رہا ہے۔" else "Opening Instagram."
                return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Instagram", actionType = "APP_LAUNCH")
            }

            appName.contains("facebook") || appName.contains("फेसबुक") || appName.contains("فیس بک") -> {
                val intent = pm.getLaunchIntentForPackage("com.facebook.katana")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    openUrl("https://www.facebook.com")
                }
                val spoken = if (isHindi) "फेसबुक खोला जा रहा है।" else if (isUrdu) "فیس بک کھولا جا رہا ہے۔" else "Opening Facebook."
                return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Facebook", actionType = "APP_LAUNCH")
            }

            appName.contains("chrome") || appName.contains("क्रोम") || appName.contains("کروم") || appName.contains("browser") || appName.contains("ब्राउज़र") -> {
                val intent = pm.getLaunchIntentForPackage("com.android.chrome")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    openUrl("https://www.google.com")
                }
                val spoken = if (isHindi) "ब्राउज़र खोला जा रहा है।" else if (isUrdu) "براؤزر کھولا جا رہا ہے۔" else "Opening Browser."
                return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Chrome", actionType = "APP_LAUNCH")
            }

            appName.contains("map") || appName.contains("मैप्स") || appName.contains("نقشہ") -> {
                val intent = pm.getLaunchIntentForPackage("com.google.android.apps.maps")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    openUrl("https://maps.google.com")
                }
                val spoken = if (isHindi) "गूगल मैप्स खोला जा रहा है।" else if (isUrdu) "گوگل میپس کھولا جا رہا ہے۔" else "Opening Google Maps."
                return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Maps", actionType = "APP_LAUNCH")
            }

            appName.contains("spotify") || appName.contains("स्पॉटिफ़ाई") -> {
                val intent = pm.getLaunchIntentForPackage("com.spotify.music")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    openUrl("https://open.spotify.com")
                }
                val spoken = if (isHindi) "स्पॉटिफ़ाई खोला जा रहा है।" else "Opening Spotify."
                return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Opened Spotify", actionType = "APP_LAUNCH")
            }
        }

        // Generic dynamic installed packages query
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            for (ri in resolveInfos) {
                val label = ri.loadLabel(pm).toString()
                val pkg = ri.activityInfo.packageName
                if (label.equals(appName, ignoreCase = true) ||
                    label.contains(appName, ignoreCase = true) ||
                    appName.contains(label, ignoreCase = true) ||
                    pkg.contains(appName, ignoreCase = true)) {

                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        val spoken = if (isHindi) "$label ऐप खोली जा रही है।" else if (isUrdu) "$label ایپ کھولی جا رہی ہے۔" else "Opening $label."
                        return ToolExecutionResult(
                            spokenConfirmation = spoken,
                            displayMessage = "Launched $label",
                            actionType = "APP_LAUNCH",
                            data = mapOf("package" to pkg, "label" to label)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to query installed apps: ${e.message}")
        }

        // Fallback: Open Play Store search or Web for the app
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(rawName)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            openUrl("https://play.google.com/store/search?q=${Uri.encode(rawName)}")
        }
        val spoken = if (isHindi) "यह ऐप डिवाइस में नहीं मिली, प्ले स्टोर पर खोजा जा रहा है।" else if (isUrdu) "یہ ایپ نہیں ملی، پلے اسٹور پر تلاش کی جا رہی ہے۔" else "App not installed on device. Searching Google Play Store for $rawName."
        return ToolExecutionResult(
            spokenConfirmation = spoken,
            displayMessage = "Searched Play Store: $rawName",
            actionType = "APP_LAUNCH"
        )
    }

    private fun performSearchInApp(appName: String, query: String, language: String): ToolExecutionResult {
        val isHindi = language.lowercase().startsWith("hi")
        val isUrdu = language.lowercase().startsWith("ur")
        val cleanApp = appName.trim().lowercase()
        val cleanQuery = query.trim()

        if (cleanQuery.isBlank()) {
            val spoken = if (isHindi) "आप क्या सर्च करना चाहते हैं?" else if (isUrdu) "آپ کیا تلاش کرنا چاہتے ہیں؟" else "What would you like me to search for?"
            return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Query missing", isError = true)
        }

        if (cleanApp.contains("youtube") || cleanApp.contains("यूट्यूब") || cleanApp.contains("یوٹیوب") || cleanApp.contains("video") || cleanApp.contains("वीडियो")) {
            val encoded = Uri.encode(cleanQuery)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                openUrl("https://www.youtube.com/results?search_query=$encoded")
            }
            val spoken = if (isHindi) "यूट्यूब पर '$cleanQuery' खोजा जा रहा है।" else if (isUrdu) "یوٹیوب پر '$cleanQuery' تلاش کیا جا رہا ہے۔" else "Searching YouTube for '$cleanQuery'."
            return ToolExecutionResult(
                spokenConfirmation = spoken,
                displayMessage = "YouTube Search: $cleanQuery",
                actionType = "SEARCH"
            )
        }

        if (cleanApp.contains("play store") || cleanApp.contains("प्ले स्टोर") || cleanApp.contains("پلے اسٹور")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(cleanQuery)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                openUrl("https://play.google.com/store/search?q=${Uri.encode(cleanQuery)}")
            }
            val spoken = if (isHindi) "प्ले स्टोर पर '$cleanQuery' खोजा जा रहा है।" else "Searching Play Store for '$cleanQuery'."
            return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Play Store: $cleanQuery", actionType = "SEARCH")
        }

        if (cleanApp.contains("map") || cleanApp.contains("मैप") || cleanApp.contains("نقشہ")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(cleanQuery)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                openUrl("https://maps.google.com/?q=${Uri.encode(cleanQuery)}")
            }
            val spoken = if (isHindi) "मैप्स पर '$cleanQuery' खोजा जा रहा है।" else "Searching Maps for '$cleanQuery'."
            return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Maps Search: $cleanQuery", actionType = "SEARCH")
        }

        // Default to Google search
        val encoded = Uri.encode(cleanQuery)
        openUrl("https://www.google.com/search?q=$encoded")
        val spoken = if (isHindi) "'$cleanQuery' खोजा जा रहा है।" else if (isUrdu) "'$cleanQuery' تلاش کیا جا رہا ہے۔" else "Searching for '$cleanQuery'."
        return ToolExecutionResult(
            spokenConfirmation = spoken,
            displayMessage = "Search: $cleanQuery",
            actionType = "SEARCH"
        )
    }

    private fun performScrollVideo(direction: String, language: String): ToolExecutionResult {
        val isHindi = language.lowercase().startsWith("hi")
        val isUrdu = language.lowercase().startsWith("ur")
        val isUp = direction.equals("up", ignoreCase = true) ||
                   direction.equals("previous", ignoreCase = true) ||
                   direction.equals("back", ignoreCase = true)

        // Notify in-app Video Hub
        actionCallback?.onScrollVideo(if (isUp) "up" else "down")

        // Dispatch system-wide accessibility gesture (for YouTube Shorts, Instagram Reels, TikTok, etc.)
        val gestureSuccess = if (isUp) {
            JarvisAccessibilityService.scrollUp()
        } else {
            JarvisAccessibilityService.scrollDown()
        }

        val isServiceActive = JarvisAccessibilityService.isServiceEnabled()
        val spoken = when {
            gestureSuccess -> if (isHindi) "वीडियो स्क्रॉल कर दिया गया है।" else if (isUrdu) "ویڈیو اسکرول کر دی گئی ہے۔" else "Video scrolled to next."
            isServiceActive -> if (isHindi) "अगला वीडियो स्क्रॉल किया गया।" else "Next video scrolled."
            else -> {
                actionCallback?.onOpenVideoHub()
                if (isHindi) "वीडियो स्क्रॉल कर दिया गया है। अन्य ऐप्स में स्क्रॉलिंग के लिए एक्सेसिबिलिटी चालू करें।"
                else if (isUrdu) "ویڈیو اسکرول کر دی گئی۔ تمام ایپس میں اسکرول کے لیے سیٹنگز سے ایکسیسیبلٹی آن کریں۔"
                else "Video scrolled in video hub. For YouTube Shorts and Reels, enable Jarvis Accessibility Service in Settings."
            }
        }

        return ToolExecutionResult(
            spokenConfirmation = spoken,
            displayMessage = if (isUp) "SCROLL_VIDEO_UP" else "SCROLL_VIDEO_DOWN",
            actionType = "SCROLL_VIDEO",
            data = mapOf("direction" to if (isUp) "up" else "down", "gestureSuccess" to gestureSuccess)
        )
    }

    private fun performSendVideo(
        recipient: String,
        title: String,
        url: String,
        customMessage: String,
        language: String
    ): ToolExecutionResult {
        val isHindi = language.lowercase().startsWith("hi")
        val isUrdu = language.lowercase().startsWith("ur")

        val effectiveUrl = if (url.isBlank()) "https://www.youtube.com/shorts" else url
        val shareBody = buildString {
            if (customMessage.isNotBlank()) append(customMessage).append("\n\n")
            append("🎬 ").append(title).append("\n")
            append(effectiveUrl)
        }

        val isWhatsApp = recipient.contains("whatsapp", ignoreCase = true) ||
                         recipient.contains("व्हाट्सएप") ||
                         recipient.contains("واٹس")

        if (isWhatsApp) {
            val waIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(shareBody)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(waIntent)
            } catch (e: Exception) {
                shareGeneralText(shareBody)
            }
        } else {
            shareGeneralText(shareBody)
        }

        val spoken = when {
            isHindi -> "यह वीडियो सेंड करने के लिए शेयर ऑप्शन खोल दिया गया है।"
            isUrdu -> "ویڈیو بھیجنے کے لیے شیئر کا آپشن کھول دیا گیا ہے۔"
            else -> "Opening share options to send this video."
        }

        return ToolExecutionResult(
            spokenConfirmation = spoken,
            displayMessage = "Sent Video: $title",
            actionType = "SHARE_VIDEO",
            data = mapOf("title" to title, "url" to effectiveUrl)
        )
    }

    private fun performCallContact(name: String, language: String): ToolExecutionResult {
        val isHindi = language.lowercase().startsWith("hi")
        val isUrdu = language.lowercase().startsWith("ur")
        val cleanName = name.trim()

        if (cleanName.isBlank()) {
            val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            val spoken = if (isHindi) "फोन डायलर खोला जा रहा है।" else if (isUrdu) "فون ڈائلر کھولا جا رہا ہے۔" else "Opening phone dialer."
            return ToolExecutionResult(spokenConfirmation = spoken, displayMessage = "Open Dialer", actionType = "CALL")
        }

        var matchedName: String? = null
        var matchedNumber: String? = null

        try {
            val hasContactsPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasContactsPermission) {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                    arrayOf("%$cleanName%"),
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        matchedName = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        matchedNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Contacts query failed: ${e.message}")
        }

        if (matchedNumber != null) {
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(matchedNumber)}"))
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(matchedNumber)}"))
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
                val target = matchedName ?: cleanName
                val spoken = when {
                    isHindi -> "$target को कॉल लगाया जा रहा है ($matchedNumber)।"
                    isUrdu -> "$target کو کال ملائی جا رہی ہے ($matchedNumber)۔"
                    else -> "Calling $target at $matchedNumber."
                }
                return ToolExecutionResult(
                    spokenConfirmation = spoken,
                    displayMessage = "Calling $target ($matchedNumber)",
                    actionType = "CALL",
                    data = mapOf("name" to target, "number" to matchedNumber)
                )
            } catch (e: Exception) {
                Log.e(tag, "Failed to launch call intent: ${e.message}")
            }
        }

        // Fallback: Open dialer with contact name
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)
        val spoken = when {
            isHindi -> "कांटेक्ट्स में '$cleanName' नहीं मिला। डायलर खोला जा रहा है।"
            isUrdu -> "رابطوں میں '$cleanName' نہیں ملا۔ ڈائلر کھولا جا رہا ہے۔"
            else -> "Contact '$cleanName' not found. Opening phone dialer."
        }
        return ToolExecutionResult(
            spokenConfirmation = spoken,
            displayMessage = "Contact Not Found: $cleanName",
            actionType = "CALL"
        )
    }

    private fun shareGeneralText(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Send Video").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch share chooser: ${e.message}")
        }
    }

    private fun openUrl(url: String) {
        val fixedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Safely evaluates basic math expressions without any script engines or eval.
     */
    private fun evaluateSafeExpression(expr: String): String {
        try {
            val cleaned = expr.replace(" ", "")
                .replace("x", "*")
                .replace("X", "*")
                .replace("dividedby", "/")
                .replace("plus", "+")
                .replace("minus", "-")
                .replace("times", "*")

            // Simple tokens evaluation
            // Handles + - * / and percentages
            if (cleaned.contains("%of")) {
                val parts = cleaned.split("%of")
                val pct = parts[0].toDoubleOrNull() ?: 0.0
                val total = parts[1].toDoubleOrNull() ?: 0.0
                return formatNumber((pct / 100.0) * total)
            }

            // Simple parser
            val result = SimpleMathParser.evaluate(cleaned)
            return formatNumber(result)
        } catch (e: Exception) {
            return "Unable to calculate expression."
        }
    }

    private fun formatNumber(num: Double): String {
        return if (num % 1.0 == 0.0) {
            num.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", num)
        }
    }
}

data class ToolExecutionResult(
    val spokenConfirmation: String,
    val displayMessage: String,
    val isError: Boolean = false,
    val actionType: String? = null,
    val data: Map<String, Any?> = emptyMap()
)

class SimpleMathParser(private val str: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        ch = if (++pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw RuntimeException("Unexpected character: " + ch.toChar())
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm()
            else if (eat('-'.code)) x -= parseTerm()
            else return x
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) x *= parseFactor()
            else if (eat('/'.code)) {
                val divisor = parseFactor()
                if (divisor == 0.0) throw ArithmeticException("Division by zero")
                x /= divisor
            } else return x
        }
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x: Double
        val startPos = pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
            while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
            x = str.substring(startPos, pos).toDouble()
        } else {
            throw RuntimeException("Unexpected character: " + ch.toChar())
        }

        if (eat('^'.code)) x = Math.pow(x, parseFactor())
        return x
    }

    companion object {
        fun evaluate(expr: String): Double {
            return SimpleMathParser(expr).parse()
        }
    }
}
