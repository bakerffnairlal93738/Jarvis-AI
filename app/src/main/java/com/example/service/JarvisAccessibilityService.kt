package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility service for JARVIS that enables system-wide voice-controlled gestures
 * such as auto-scrolling videos/shorts/reels in apps like YouTube, Instagram, TikTok, etc.
 */
class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "JarvisAccessibilityService connected and ready for voice gestures.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events can be monitored if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "JarvisAccessibilityService interrupted.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "JarvisAccessService"

        @Volatile
        var instance: JarvisAccessibilityService? = null

        fun isServiceEnabled(): Boolean = instance != null

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open accessibility settings: ${e.message}")
            }
        }

        /**
         * Dispatches a swipe gesture to scroll down to the next video (swipe up on screen).
         */
        fun scrollDown(): Boolean {
            val service = instance ?: return false
            val metrics = service.resources.displayMetrics
            val width = metrics.widthPixels.toFloat()
            val height = metrics.heightPixels.toFloat()

            val startX = width / 2f
            val startY = height * 0.78f
            val endX = startX
            val endY = height * 0.22f

            return service.performSwipeGesture(startX, startY, endX, endY, 260L)
        }

        /**
         * Dispatches a swipe gesture to scroll up to previous video (swipe down on screen).
         */
        fun scrollUp(): Boolean {
            val service = instance ?: return false
            val metrics = service.resources.displayMetrics
            val width = metrics.widthPixels.toFloat()
            val height = metrics.heightPixels.toFloat()

            val startX = width / 2f
            val startY = height * 0.22f
            val endX = startX
            val endY = height * 0.78f

            return service.performSwipeGesture(startX, startY, endX, endY, 260L)
        }
    }

    private fun performSwipeGesture(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            var dispatched = false
            dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "Swipe gesture completed successfully")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "Swipe gesture cancelled")
                }
            }, null)

            if (dispatched) return true
        }

        // Fallback: try node-based scrolling on active window
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val scrolled = rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            @Suppress("DEPRECATION")
            rootNode.recycle()
            return scrolled
        }

        return false
    }
}
