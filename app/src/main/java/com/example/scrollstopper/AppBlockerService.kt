package com.example.scrollstopper

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.scrollstopper.data.PreferenceManager
import com.example.scrollstopper.theme.ScrollStopperTheme
import java.util.Calendar

class AppBlockerService : Service() {

    private lateinit var handler: Handler
    private lateinit var prefManager: PreferenceManager
    private val checkInterval = 1000L // 1 second
    private var overlayView: android.view.View? = null
    private var youtubeTimeStarted: Long = 0L

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefManager = PreferenceManager(applicationContext)
        handler = Handler(Looper.getMainLooper())
        
        // Start service as foreground
        startForegroundServiceNotification()
        
        // Start loop
        handler.post(checkRunnable)
        Log.i(TAG, "AppBlockerService created and monitoring loop started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        handler.post {
            removeBlockerOverlay()
        }
        Log.i(TAG, "AppBlockerService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundApp() {
        val currentApp = getForegroundPackageName() ?: return
        Log.i(TAG, "Foreground Check: detectedApp=$currentApp | strictMode=${prefManager.strictMode} | isBlocked=${prefManager.isCurrentlyBlocked}")

        // Targeted apps to block
        val blockedApps = listOf("com.google.android.youtube")

        if (blockedApps.contains(currentApp)) {
            val now = System.currentTimeMillis()
            val isCoolDownBlocked = prefManager.isCurrentlyBlocked
            val isBypassed = now < prefManager.emergencyBypassUntil

            // Determine if we are currently inside an active, uncompleted study block
            val inActiveStudyBlock = prefManager.customBlocks.any { block ->
                !block.isCompleted && isTimeInBlock(block.timeRange)
            }

            // In Non-Strict Mode, we track time spent in YouTube to simulate scroll limit (e.g., 10 seconds per scroll limit unit)
            if (!prefManager.strictMode && !inActiveStudyBlock && !isCoolDownBlocked && !isBypassed) {
                if (youtubeTimeStarted == 0L) {
                    youtubeTimeStarted = now
                    Log.i(TAG, "User opened YouTube. Timer started.")
                } else {
                    val timeSpentMs = now - youtubeTimeStarted
                    val limitMs = prefManager.scrollLimit * 10000L // 10s per scroll unit (e.g. 3 limit = 30 seconds)
                    Log.i(TAG, "YouTube usage: ${timeSpentMs / 1000}s / ${limitMs / 1000}s limit")
                    
                    if (timeSpentMs >= limitMs) {
                        Log.i(TAG, "Usage limit reached! Triggering cooldown block.")
                        // Activate cooldown block
                        val pauseMillis = prefManager.pauseDurationSeconds * 1000L
                        prefManager.blockUntilTimestamp = now + pauseMillis
                        prefManager.totalBlocksToday += 1
                        prefManager.timeSavedMinutes += 15 // Estimate 15 minutes saved
                        youtubeTimeStarted = 0L
                    }
                }
            } else {
                // Reset timer when blocked by study time/cooldown or strict mode is active
                youtubeTimeStarted = 0L
            }

            // Block if:
            // 1. We are in a cool-down block
            // 2. Strict Mode is enabled
            // 3. We are currently inside a scheduled, uncompleted study block
            if ((isCoolDownBlocked || prefManager.strictMode || inActiveStudyBlock) && !isBypassed) {
                Log.i(TAG, "Blocking active foreground application: $currentApp")
                handler.post {
                    showBlockerOverlay()
                }
            } else {
                handler.post {
                    removeBlockerOverlay()
                }
            }
        } else {
            // Reset timer if they exit YouTube
            youtubeTimeStarted = 0L
            
            // Remove the overlay if they exit YouTube and are on any allowed app (not our own)
            if (currentApp != packageName) {
                handler.post {
                    removeBlockerOverlay()
                }
            }
        }
    }

    private fun showBlockerOverlay() {
        if (overlayView != null) return // Already showing

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        )

        // Create standard LinearLayout layout programmatically
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#0F0A19")) // Sleek deep purple dark mode background
            setPadding(64, 64, 64, 64)
        }

        // Lock Icon Text
        val lockText = android.widget.TextView(this).apply {
            text = "🛡️"
            textSize = 64f
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(lockText)

        // Title
        val titleText = android.widget.TextView(this).apply {
            text = "Goal Reacher Focus Block"
            textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            gravity = android.view.Gravity.CENTER
            val layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 16)
            }
            this.layoutParams = layoutParams
        }
        layout.addView(titleText)

        // Subtitle / Random Quote
        val randomQuote = com.example.scrollstopper.data.QuotesData.quotes.random()
        val quoteText = android.widget.TextView(this).apply {
            text = "\"${randomQuote.text}\"\n— ${randomQuote.author}"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#A78BFA")) // Light purple accent color
            gravity = android.view.Gravity.CENTER
            val layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 48)
            }
            this.layoutParams = layoutParams
        }
        layout.addView(quoteText)

        // Exit Button
        val exitButton = android.widget.Button(this).apply {
            text = "Exit YouTube"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            
            // Set background color using dynamic gradient with rounded corners
            val shape = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(android.graphics.Color.parseColor("#8B5CF6")) // Accent purple button
            }
            background = shape
            
            val layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                144 // ~48dp height in pixels
            ).apply {
                setMargins(48, 0, 48, 0)
            }
            this.layoutParams = layoutParams
            
            setOnClickListener {
                // Redirect to home screen
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                removeBlockerOverlay()
            }
        }
        layout.addView(exitButton)

        try {
            windowManager.addView(layout, params)
            overlayView = layout
            Log.i(TAG, "Blocker overlay added to WindowManager successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding blocker overlay to WindowManager: ${e.message}", e)
        }
    }

    private fun removeBlockerOverlay() {
        val view = overlayView ?: return
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            windowManager.removeView(view)
            overlayView = null
            Log.i(TAG, "Blocker overlay removed from WindowManager successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing blocker overlay: ${e.message}", e)
        }
    }

    private fun isTimeInBlock(timeRange: String): Boolean {
        val parts = timeRange.split("-")
        if (parts.size != 2) return false
        val start = parseTime(parts[0].trim()) ?: return false
        val end = parseTime(parts[1].trim()) ?: return false
        
        val nowCalendar = Calendar.getInstance()
        val nowMinutes = nowCalendar.get(Calendar.HOUR_OF_DAY) * 60 + nowCalendar.get(Calendar.MINUTE)
        
        return if (start <= end) {
            nowMinutes in start..end
        } else {
            nowMinutes >= start || nowMinutes <= end
        }
    }

    private fun parseTime(timeStr: String): Int? {
        val clean = timeStr.uppercase().trim()
        val isPM = clean.contains("PM")
        val isAM = clean.contains("AM")
        val timeParts = clean.replace("AM", "").replace("PM", "").trim().split(":")
        if (timeParts.isEmpty()) return null
        
        var hour = timeParts[0].toIntOrNull() ?: return null
        var minute = 0
        if (timeParts.size > 1) {
            minute = timeParts[1].toIntOrNull() ?: 0
        }
        
        if (isPM && hour < 12) {
            hour += 12
        } else if (isAM && hour == 12) {
            hour = 0
        }
        
        return hour * 60 + minute
    }

    private fun getForegroundPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 60000 // Look back 60 seconds (safe against clock drifts)
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

        var latestApp: String? = null
        var latestTime = 0L
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            // Match standard MOVE_TO_FOREGROUND (1) and ACTIVITY_RESUMED (19) event states
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND || event.eventType == 19) {
                if (event.timeStamp > latestTime) {
                    latestApp = event.packageName
                    latestTime = event.timeStamp
                }
            }
        }

        // Fallback: if queryEvents returned nothing, query usage stats
        if (latestApp == null) {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                endTime - 1000 * 60 * 5, // 5 minutes lookback
                endTime
            )
            if (!stats.isNullOrEmpty()) {
                val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
                val mostRecent = sortedStats.first()
                if (endTime - mostRecent.lastTimeUsed < 15000) { // Used in the last 15 seconds
                    latestApp = mostRecent.packageName
                }
            }
        }
        return latestApp
    }

    private fun startForegroundServiceNotification() {
        val channelId = "app_blocker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Focus Shield Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Use a generic lock icon available on all versions of Android
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Focus Shield Active")
            .setContentText("Goal Reacher is protecting your study session.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(101, notification)
    }

    companion object {
        private const val TAG = "AppBlockerService"
        
        fun isUsageAccessGranted(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun isOverlayGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }
}
