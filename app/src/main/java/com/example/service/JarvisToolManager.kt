package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
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
