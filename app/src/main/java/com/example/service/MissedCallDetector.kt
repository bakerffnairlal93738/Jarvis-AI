package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.JarvisPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles incoming call states, detects missed calls, resolves caller names from contacts,
 * announces voice alerts, and posts actionable heads-up notifications.
 */
object MissedCallDetector {
    private const val TAG = "MissedCallDetector"
    private const val CHANNEL_ID = "jarvis_missed_calls_channel"
    private const val NOTIFICATION_ID = 2002

    private var lastState = TelephonyManager.EXTRA_STATE_IDLE
    private var isIncomingRinging = false
    private var incomingNumber: String? = null
    private var ringStartTime: Long = 0L

    fun onCallStateChanged(context: Context, state: String?, number: String?) {
        val prefs = JarvisPreferencesManager(context.applicationContext)
        // If missed call alert is disabled, do nothing
        if (!prefs.isMissedCallAlertEnabled) {
            return
        }

        Log.d(TAG, "Call state change: $state, number: $number, previousState: $lastState")

        if (!number.isNullOrBlank()) {
            incomingNumber = number
        }

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                isIncomingRinging = true
                ringStartTime = System.currentTimeMillis()
                if (!number.isNullOrBlank()) {
                    incomingNumber = number
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call was answered
                isIncomingRinging = false
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // If it was ringing and never answered, it's a missed call
                if (isIncomingRinging) {
                    val ringDuration = System.currentTimeMillis() - ringStartTime
                    // Ignore transient glitches under 1 second
                    if (ringDuration > 1000L) {
                        val callerNumber = incomingNumber ?: "Unknown"
                        handleMissedCall(context.applicationContext, callerNumber)
                    }
                }
                isIncomingRinging = false
                incomingNumber = null
            }
        }

        if (state != null) {
            lastState = state
        }
    }

    fun handleMissedCall(context: Context, number: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val callerName = resolveContactName(context, number) ?: number
            val prefs = JarvisPreferencesManager(context)
            val lang = prefs.selectedLanguage

            val voiceAnnouncement = when (lang) {
                "ur" -> "سر، آپ کو $callerName کی طرف سے مِسڈ کال آئی ہے۔"
                "hi" -> "सर, आपको $callerName की तरफ से मिस्ड कॉल आई है।"
                else -> "Sir, you have a missed call from $callerName."
            }

            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

            // 1. Save alert to JARVIS conversation memory
            val history = prefs.getConversationHistory().toMutableList()
            history.add(
                ChatMessageItem(
                    role = "model",
                    text = "📞 [MISSED CALL] $voiceAnnouncement ($timeStr)",
                    actionBadge = "MISSED_CALL"
                )
            )
            prefs.saveConversationHistory(history)

            // 2. Play speech alert if voice output enabled
            if (prefs.isVoiceOutputEnabled) {
                try {
                    val player = com.example.audio.AudioPlayer(context)
                    player.setLanguage(lang)
                    player.setSpeechVolume(prefs.speechVolume)
                    player.speakResponse(voiceAnnouncement)
                } catch (e: Exception) {
                    Log.w(TAG, "Voice announcement error: ${e.message}")
                }
            }

            // 3. Trigger vibration alert
            vibrateAlert(context)

            // 4. Show Heads-Up Notification with Quick Call Back action
            postMissedCallNotification(context, callerName, number, voiceAnnouncement)
        }
    }

    private fun resolveContactName(context: Context, number: String): String? {
        if (number.isBlank() || number.equals("Unknown", ignoreCase = true)) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx >= 0) cursor.getString(nameIdx) else null
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Contact lookup error: ${e.message}")
            null
        }
    }

    private fun vibrateAlert(context: Context) {
        try {
            val pattern = longArrayOf(0, 300, 200, 300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun postMissedCallNotification(
        context: Context,
        callerName: String,
        number: String,
        alertText: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Missed Call Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a phone call is missed with quick call-back actions"
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }

        // Open JARVIS intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Call Back intent
        val callIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val callPendingIntent = PendingIntent.getActivity(
            context,
            2,
            callIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📞 JARVIS: Missed Call Alert")
            .setContentText(alertText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Caller: $callerName\nNumber: $number\nJARVIS recorded this missed call."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "Call Back (واپس کال کریں)", callPendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}

/**
 * System BroadcastReceiver for telephony state changes.
 */
class MissedCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            @Suppress("DEPRECATION")
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            MissedCallDetector.onCallStateChanged(context, state, number)
        }
    }
}
