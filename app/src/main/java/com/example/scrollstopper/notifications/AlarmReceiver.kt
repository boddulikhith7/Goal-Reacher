package com.example.scrollstopper.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.scrollstopper.data.BlockType
import com.example.scrollstopper.data.PreferenceManager
import com.example.scrollstopper.data.ScheduleData
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        private const val ACTION_BLOCK_REMINDER = "com.example.scrollstopper.ACTION_BLOCK_REMINDER"
        private const val EXTRA_BLOCK_TYPE = "extra_block_type"

        fun scheduleAlarms(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            scheduleBlockAlarm(context, alarmManager, 1, 6, 0, BlockType.BLOCK1)   // 6:00 AM
            scheduleBlockAlarm(context, alarmManager, 2, 18, 0, BlockType.BLOCK2)  // 6:00 PM
            scheduleBlockAlarm(context, alarmManager, 3, 21, 0, BlockType.BLOCK3)  // 9:00 PM
        }

        private fun scheduleBlockAlarm(
            context: Context,
            alarmManager: AlarmManager,
            requestCode: Int,
            hour: Int,
            minute: Int,
            blockType: BlockType
        ) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_BLOCK_REMINDER
                putExtra(EXTRA_BLOCK_TYPE, blockType.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            // Set repeating daily alarm
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Scheduled alarm for $blockType at $hour:$minute")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted. Rescheduling alarms...")
            scheduleAlarms(context)
            return
        }

        if (intent.action == ACTION_BLOCK_REMINDER) {
            val blockTypeName = intent.getStringExtra(EXTRA_BLOCK_TYPE) ?: return
            val blockType = try {
                BlockType.valueOf(blockTypeName)
            } catch (e: Exception) {
                return
            }

            val prefManager = PreferenceManager(context)
            val weekNumber = prefManager.currentWeek
            val syllabus = prefManager.customSyllabus
            val weekPlan = syllabus.find { it.weekNumber == weekNumber } ?: return

            // If user already completed this block today, do not notify!
            if (prefManager.isBlockCompleted(blockType)) {
                Log.d(TAG, "Block $blockType is already completed. Skipping notification.")
                return
            }

            val helper = NotificationHelper(context)
            val blockLabel = when (blockType) {
                BlockType.BLOCK1 -> prefManager.block1Label
                BlockType.BLOCK2 -> prefManager.block2Label
                BlockType.BLOCK3 -> prefManager.block3Label
            }
            val title = "${prefManager.questName} - $blockLabel"
            val message = when (blockType) {
                BlockType.BLOCK1 -> "Time for $blockLabel: Study '${weekPlan.topic}'. Source: ${weekPlan.block1Source}."
                BlockType.BLOCK2 -> "Time for $blockLabel: Solve questions on '${weekPlan.topic}'. Portal: ${weekPlan.block2Source}."
                BlockType.BLOCK3 -> {
                    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    val rotationTask = when (dayOfWeek) {
                        Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY -> "Python Programming"
                        Calendar.TUESDAY, Calendar.THURSDAY -> "Internship preparation & emails"
                        Calendar.SATURDAY -> "Weekly full-length test analysis"
                        else -> "Mock test review or Rest"
                    }
                    "Focus on: $rotationTask."
                }
            }
            helper.sendNotification(title, message)
        }
    }
}
