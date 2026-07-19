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
    private var accumulatedBlockedTimeMs: Long = 0L
    private var lastActiveTime: Long = 0L
    private var lastCheckedTime: Long = 0L

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
        val currentApp = getForegroundPackageName()
        val now = System.currentTimeMillis()
        
        val blockedApps = listOf("com.google.android.youtube", "com.instagram.android")
        val settingsPackages = listOf(
            "com.android.settings",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
        )
        
        val isAppBlocked = currentApp != null && blockedApps.contains(currentApp)
        
        val isFocusActive = prefManager.isFocusActive
        val permissionsGranted = isUsageAccessGranted(this) && isOverlayGranted(this)
        
        val isSettingsBlocked = currentApp != null && settingsPackages.contains(currentApp) && isFocusActive && permissionsGranted
        
        if (isAppBlocked || isSettingsBlocked) {
            lastActiveTime = now
            
            if (isAppBlocked) {
                if (lastCheckedTime > 0L) {
                    val elapsed = now - lastCheckedTime
                    accumulatedBlockedTimeMs += elapsed
                }
                lastCheckedTime = now
                
                // In Non-Strict Mode, we track total accumulated time in blocked apps to simulate scroll limit (10s per unit)
                val inActiveStudyBlock = prefManager.hasActiveStudyBlock
                var isCoolDownBlocked = prefManager.isCurrentlyBlocked
                val isBypassed = now < prefManager.emergencyBypassUntil
                
                if (!prefManager.strictMode && !inActiveStudyBlock && !isCoolDownBlocked && !isBypassed) {
                    val limitMs = prefManager.scrollLimit * 10000L // 10s per scroll unit (e.g. 3 limit = 30 seconds)
                    Log.i(TAG, "Accumulated blocked app usage: ${accumulatedBlockedTimeMs / 1000}s / ${limitMs / 1000}s limit")
                    
                    if (accumulatedBlockedTimeMs >= limitMs) {
                        Log.i(TAG, "Usage limit reached! Triggering cooldown block.")
                        // Activate cooldown block
                        val pauseMillis = prefManager.pauseDurationSeconds * 1000L
                        prefManager.blockUntilTimestamp = now + pauseMillis
                        prefManager.totalBlocksToday += 1
                        prefManager.timeSavedMinutes += 15 // Estimate 15 minutes saved
                        accumulatedBlockedTimeMs = 0L
                    }
                } else {
                    // Reset accumulated timer when blocked by study time/cooldown or strict mode is active
                    accumulatedBlockedTimeMs = 0L
                }
            } else {
                // Settings block: do not run timer or accumulate scroll limit time
                lastCheckedTime = now
            }
            
            // Block if:
            // 1. Settings blocker condition is true
            // 2. Standard app blocker condition is true (focus is active)
            val shouldShowOverlay = isSettingsBlocked || isFocusActive
            
            if (shouldShowOverlay) {
                Log.i(TAG, "Blocking active foreground application: $currentApp")
                
                handler.post {
                    showBlockerOverlay(currentApp)
                }
            } else {
                handler.post {
                    removeBlockerOverlay()
                }
            }
        } else {
            // User is not in a blocked app
            lastCheckedTime = 0L
            
            // If they have been away from blocked apps for more than 5 seconds, reset the accumulated timer
            if (now - lastActiveTime > 5000L) {
                accumulatedBlockedTimeMs = 0L
            }
            
            // Remove the overlay if they exit the blocked app and are on any allowed app (including our own)
            handler.post {
                removeBlockerOverlay()
            }
        }
    }

    private fun showBlockerOverlay(currentApp: String) {
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

        // Request Audio Focus to instantly pause YouTube audio/video playback
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request audio focus: ${e.message}")
        }

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

        val isSettingsApp = currentApp.contains("settings") || currentApp.contains("packageinstaller")
        val appLabel = when {
            isSettingsApp -> "Settings"
            currentApp.contains("instagram") -> "Instagram"
            else -> "YouTube"
        }

        if (isSettingsApp) {
            val alertMessage = android.widget.TextView(this).apply {
                text = "System settings are locked during active focus periods to prevent force-stopping or uninstalling the blocker. Finish your session or wait for the cooldown to access settings."
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#EF4444")) // Red warning color
                gravity = android.view.Gravity.CENTER
                val layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(32, 0, 32, 48)
                }
                this.layoutParams = layoutParams
            }
            layout.addView(alertMessage)
        } else {
            // Blocker Quiz Section
            val challengeText = android.widget.TextView(this).apply {
                text = "Answer this Focus Quiz to unlock a 5-minute break pass!"
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                gravity = android.view.Gravity.CENTER
                val layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
                this.layoutParams = layoutParams
            }
            layout.addView(challengeText)

            val q = getBlockerQuestion()
            val qText = android.widget.TextView(this).apply {
                text = q.question
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                gravity = android.view.Gravity.CENTER
                val layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 0, 16, 24)
                }
                this.layoutParams = layoutParams
            }
            layout.addView(qText)

            q.options.forEachIndexed { index, option ->
                val btn = android.widget.Button(this).apply {
                    text = option
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 13f
                    gravity = android.view.Gravity.CENTER
                    
                    val shape = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 16f
                        setColor(android.graphics.Color.parseColor("#1F192F")) // Dark translucent purple
                        setStroke(2, android.graphics.Color.parseColor("#4C3B70")) // Border accent
                    }
                    background = shape
                    
                    val layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        120 // height
                    ).apply {
                        setMargins(32, 6, 32, 6)
                    }
                    this.layoutParams = layoutParams
                    
                    setOnClickListener {
                        if (index == q.correctIndex) {
                            android.widget.Toast.makeText(
                                applicationContext,
                                "Correct! You unlocked a 5-minute study break! 🎓",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            prefManager.emergencyBypassUntil = System.currentTimeMillis() + (5 * 60 * 1000L)
                            removeBlockerOverlay()
                        } else {
                            val currentHp = prefManager.mascotHp
                            if (currentHp > 1) {
                                prefManager.mascotHp = currentHp - 1
                                android.widget.Toast.makeText(
                                    applicationContext,
                                    "Incorrect! Hooty took 1 damage. HP: ${currentHp - 1}/3",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                prefManager.mascotHp = 0
                                prefManager.streak = 0
                                android.widget.Toast.makeText(
                                    applicationContext,
                                    "Hooty fainted! Streak reset to 0.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            // Set/refresh cooldown block
                            val pauseMillis = prefManager.pauseDurationSeconds * 1000L
                            prefManager.blockUntilTimestamp = System.currentTimeMillis() + pauseMillis

                            // Redirect home immediately
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            removeBlockerOverlay()
                        }
                    }
                }
                layout.addView(btn)
            }
        }

        // Give Up / Exit App Button
        val giveUpButton = android.widget.Button(this).apply {
            text = "Give Up & Exit $appLabel"
            setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
            textSize = 13f
            gravity = android.view.Gravity.CENTER
            
            val shape = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(2, android.graphics.Color.parseColor("#4B5563")) // Gray border
            }
            background = shape
            
            val layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                110
            ).apply {
                setMargins(32, 20, 32, 0)
            }
            this.layoutParams = layoutParams
            
            setOnClickListener {
                // Set/refresh cooldown block
                val pauseMillis = prefManager.pauseDurationSeconds * 1000L
                prefManager.blockUntilTimestamp = System.currentTimeMillis() + pauseMillis

                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                removeBlockerOverlay()
            }
        }
        layout.addView(giveUpButton)

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
        overlayView = null // Reset immediately to prevent concurrency duplicate-call locks
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            windowManager.removeView(view)
            Log.i(TAG, "Blocker overlay removed from WindowManager successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing blocker overlay: ${e.message}", e)
        }
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

    private fun getBlockerQuestion(): com.example.scrollstopper.data.BlockerQuizQuestion {
        val pool = prefManager.blockerQuizPool
        if (pool.isNotEmpty()) {
            return pool.random()
        }
        
        // Dynamic offline fallback questions (High quality EEE / CSE topics)
        val defaultQuestions = listOf(
            com.example.scrollstopper.data.BlockerQuizQuestion(
                "Which theorem is used to find the equivalent voltage source and series resistance?",
                listOf("Norton's Theorem", "Thevenin's Theorem", "Superposition Theorem"),
                1
            ),
            com.example.scrollstopper.data.BlockerQuizQuestion(
                "What is the power factor of a purely capacitive circuit?",
                listOf("Zero leading", "Zero lagging", "Unity"),
                0
            ),
            com.example.scrollstopper.data.BlockerQuizQuestion(
                "In an Operational Amplifier, what is the ideal input impedance?",
                listOf("Zero", "One", "Infinite"),
                2
            ),
            com.example.scrollstopper.data.BlockerQuizQuestion(
                "Which damping technique is commonly used in moving coil instruments?",
                listOf("Air friction", "Eddy current", "Fluid friction"),
                1
            ),
            com.example.scrollstopper.data.BlockerQuizQuestion(
                "What does a P-N junction diode conduct when forward biased?",
                listOf("Majority carriers", "Minority carriers", "Neither"),
                0
            )
        )
        return defaultQuestions.random()
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
