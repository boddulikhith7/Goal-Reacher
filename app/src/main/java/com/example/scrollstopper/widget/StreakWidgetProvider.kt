package com.example.scrollstopper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.scrollstopper.MainActivity
import com.example.scrollstopper.R
import com.example.scrollstopper.data.BlockType
import com.example.scrollstopper.data.PreferenceManager
import com.example.scrollstopper.data.ScheduleData

class StreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || intent.action == ACTION_WIDGET_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, StreakWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            onUpdate(context, appWidgetManager, ids)
        }
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.example.scrollstopper.ACTION_WIDGET_REFRESH"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, StreakWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            context.sendBroadcast(intent)
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefManager = PreferenceManager(context)
            val views = RemoteViews(context.packageName, R.layout.streak_widget_layout)

            // 1. Streak Text
            views.setTextViewText(R.id.widget_streak_text, "${prefManager.streak} Days")

            // 2. Block Dots
            val blocks = prefManager.customBlocks
            val completed = prefManager.completedBlocks
            val b1Done = if (blocks.isNotEmpty()) completed.contains(blocks[0].id) else false
            val b2Done = if (blocks.size >= 2) completed.contains(blocks[1].id) else false
            val b3Done = if (blocks.size >= 3) completed.contains(blocks[2].id) else false

            // Green color code = #34D399 (Int representation: Color.parseColor("#34D399"))
            // Gray color code = #4B5563 (Int representation: Color.parseColor("#4B5563"))
            views.setTextColor(R.id.widget_dot_b1, if (b1Done) Color.parseColor("#34D399") else Color.parseColor("#4B5563"))
            views.setTextColor(R.id.widget_dot_b2, if (b2Done) Color.parseColor("#34D399") else Color.parseColor("#4B5563"))
            views.setTextColor(R.id.widget_dot_b3, if (b3Done) Color.parseColor("#34D399") else Color.parseColor("#4B5563"))

            // 3. Duolingo-style Motivational Message
            val totalCount = blocks.size
            val completedCount = blocks.count { completed.contains(it.id) }
            val motivationMessage = when {
                totalCount == 0 -> "Set up your study blocks to start your quest! 📚"
                completedCount == totalCount -> "${prefManager.questName} is fully secured today! 🌟"
                completedCount > 0 -> {
                    val nextBlock = blocks.firstOrNull { !completed.contains(it.id) }
                    if (nextBlock != null) {
                        "Nice! Just ${nextBlock.label} left. ⚡"
                    } else {
                        "Keep it up! Almost there. 📝"
                    }
                }
                else -> {
                    val firstBlock = blocks.firstOrNull()
                    if (firstBlock != null) {
                        "${firstBlock.label} is waiting for you! 📚"
                    } else {
                        "Your quest is waiting for you! 📚"
                    }
                }
            }
            views.setTextViewText(R.id.widget_motivation_text, motivationMessage)

            // 4. Current Week Focus details
            val weekNumber = prefManager.currentWeek
            val syllabus = prefManager.customSyllabus
            val weekPlan = syllabus.find { it.weekNumber == weekNumber } ?: syllabus.firstOrNull() ?: ScheduleData.weeks[0]
            views.setTextViewText(R.id.widget_week_text, "Week $weekNumber: ${weekPlan.topic}")

            // 5. Click action to launch app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_motivation_text, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_week_text, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
