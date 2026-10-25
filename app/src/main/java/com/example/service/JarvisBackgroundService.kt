package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.JarvisPreferencesManager

/**
 * Foreground Service for JARVIS that keeps background tasks, missed call alerts,
 * wake word detection, and the local Termux Python server alive and responsive.
 * When toggled OFF, this service stops completely and releases all resources.
 */
class JarvisBackgroundService : Service() {

    private val tag = "JarvisBgService"
    private var wakeLock: PowerManager.WakeLock? = null
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: Any? = null // TelephonyCallback or PhoneStateListener

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        registerPhoneStateListener()
        Log.i(tag, "JarvisBackgroundService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.i(tag, "JarvisBackgroundService running in foreground.")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start foreground service: ${e.message}")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterPhoneStateListener()
        releaseWakeLock()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)

        Log.i(tag, "JarvisBackgroundService destroyed & background execution completely stopped.")
        super.onDestroy()
    }

    private fun registerPhoneStateListener() {
        try {
            val hasPhonePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPhonePermission) {
                Log.w(tag, "READ_PHONE_STATE permission not granted for background service listener.")
                return
            }

            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        val stateStr = when (state) {
                            TelephonyManager.CALL_STATE_RINGING -> TelephonyManager.EXTRA_STATE_RINGING
                            TelephonyManager.CALL_STATE_OFFHOOK -> TelephonyManager.EXTRA_STATE_OFFHOOK
                            else -> TelephonyManager.EXTRA_STATE_IDLE
                        }
                        MissedCallDetector.onCallStateChanged(applicationContext, stateStr, null)
                    }
                }
                telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        val stateStr = when (state) {
                            TelephonyManager.CALL_STATE_RINGING -> TelephonyManager.EXTRA_STATE_RINGING
                            TelephonyManager.CALL_STATE_OFFHOOK -> TelephonyManager.EXTRA_STATE_OFFHOOK
                            else -> TelephonyManager.EXTRA_STATE_IDLE
                        }
                        MissedCallDetector.onCallStateChanged(applicationContext, stateStr, phoneNumber)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                telephonyCallback = listener
            }
            Log.i(tag, "Background telephony state listener registered.")
        } catch (e: Exception) {
            Log.e(tag, "Failed to register background phone listener: ${e.message}")
        }
    }

    private fun unregisterPhoneStateListener() {
        try {
            telephonyCallback?.let { cb ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && cb is TelephonyCallback) {
                    telephonyManager?.unregisterTelephonyCallback(cb)
                } else if (cb is PhoneStateListener) {
                    @Suppress("DEPRECATION")
                    telephonyManager?.listen(cb, PhoneStateListener.LISTEN_NONE)
                }
            }
            telephonyCallback = null
            telephonyManager = null
        } catch (e: Exception) {
            Log.w(tag, "Error unregistering phone listener: ${e.message}")
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "JARVIS::BackgroundExecutionLock"
            )?.apply {
                acquire(10 * 60 * 1000L) // 10 minutes timeout per acquire cycle
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to release wake lock: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS AI Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps JARVIS AI, missed call alerts, and Termux Python active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prefs = JarvisPreferencesManager(applicationContext)
        val subtitle = if (prefs.isMissedCallAlertEnabled) {
            "Background Active • Missed Call Alerts & Termux Ready"
        } else {
            "Background Service Active • Voice & Termux Ready"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS AI • Neural Core Active")
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "jarvis_background_channel"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            try {
                val intent = Intent(context, JarvisBackgroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("JarvisBgService", "Error starting background service: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, JarvisBackgroundService::class.java)
                context.stopService(intent)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.cancel(NOTIFICATION_ID)
            } catch (e: Exception) {
                Log.e("JarvisBgService", "Error stopping background service: ${e.message}")
            }
        }

        fun requestBatteryOptimizationExemption(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val packageName = context.packageName
                    if (powerManager?.isIgnoringBatteryOptimizations(packageName) == false) {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } else {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e("JarvisBgService", "Battery optimization intent error: ${e.message}")
            }
        }
    }
}
