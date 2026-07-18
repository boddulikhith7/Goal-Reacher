package com.example.scrollstopper

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.scrollstopper.data.PreferenceManager
import java.util.Calendar

class AppBlockerService : Service() {

    private lateinit var handler: Handler
    private lateinit var prefManager: PreferenceManager
    private val checkInterval = 1000L // 1 second

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
        Log.d(TAG, "AppBlockerService created and monitoring loop started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        Log.d(TAG, "AppBlockerService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundApp() {
        val currentApp = getForegroundPackageName() ?: return

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

            // Block if:
            // 1. We are in a cool-down block
            // 2. Strict Mode is enabled
            // 3. We are currently inside a scheduled, uncompleted study block
            if ((isCoolDownBlocked || prefManager.strictMode || inActiveStudyBlock) && !isBypassed) {
                Log.d(TAG, "Blocking active foreground application: $currentApp")
                
                val blockerIntent = Intent(this, BlockerActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(blockerIntent)
            }
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
