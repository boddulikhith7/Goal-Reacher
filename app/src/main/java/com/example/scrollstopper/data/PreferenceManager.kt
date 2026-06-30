package com.example.scrollstopper.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PreferenceManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gate_quest_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_XP = "xp"
        private const val KEY_STREAK = "streak"
        private const val KEY_CURRENT_WEEK = "current_week"
        private const val KEY_COMPLETED_WEEKS = "completed_weeks"
        private const val KEY_WEAK_TOPICS = "weak_topics"
        private const val KEY_TODAY_DATE = "today_date"
        private const val KEY_COMPLETED_BLOCKS = "completed_blocks"
        private const val KEY_TODAY_STREAK_COUNTED = "today_streak_counted"
        private const val KEY_TOTAL_BLOCKS_TODAY = "total_blocks_today"
        private const val KEY_TIME_SAVED_MINUTES = "time_saved_minutes"

        // Scroll Stopper settings
        private const val KEY_SCROLL_LIMIT = "scroll_limit"
        private const val KEY_PAUSE_DURATION = "pause_duration"
        private const val KEY_STRICT_MODE = "strict_mode"
        private const val KEY_BLOCK_UNTIL = "block_until"
    }

    init {
        checkDayTransition()
    }

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    val todayDate: String
        get() = prefs.getString(KEY_TODAY_DATE, "") ?: ""

    fun checkDayTransition() {
        val today = getTodayString()
        val savedDate = prefs.getString(KEY_TODAY_DATE, "") ?: ""

        if (savedDate.isEmpty()) {
            prefs.edit().putString(KEY_TODAY_DATE, today).apply()
            return
        }

        if (savedDate != today) {
            val yesterday = getYesterdayString()
            // Check if streak was maintained yesterday
            val yesterdayCompleted = prefs.getStringSet(KEY_COMPLETED_BLOCKS, emptySet()) ?: emptySet()
            val completedBoth = yesterdayCompleted.contains("BLOCK1") && yesterdayCompleted.contains("BLOCK2")

            val currentStreak = streak
            if (savedDate == yesterday) {
                if (!completedBoth) {
                    // Broke the streak yesterday
                    setStreakInternal(0)
                }
            } else {
                // More than a day gap, streak broken
                setStreakInternal(0)
            }

            // Reset daily variables
            prefs.edit()
                .putString(KEY_TODAY_DATE, today)
                .putStringSet(KEY_COMPLETED_BLOCKS, emptySet())
                .putBoolean(KEY_TODAY_STREAK_COUNTED, false)
                .apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }
    }

    // XP
    var xp: Int
        get() = prefs.getInt(KEY_XP, 0)
        set(value) = prefs.edit().putInt(KEY_XP, value).apply()

    // Streak
    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) = setStreakInternal(value)

    private fun setStreakInternal(value: Int) {
        prefs.edit().putInt(KEY_STREAK, value).apply()
        com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
    }

    // Current Week Focus (1..33)
    var currentWeek: Int
        get() = prefs.getInt(KEY_CURRENT_WEEK, 1)
        set(value) {
            prefs.edit().putInt(KEY_CURRENT_WEEK, value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    // Completed Weeks (stored as set of string representation of week numbers)
    var completedWeeks: Set<Int>
        get() = (prefs.getStringSet(KEY_COMPLETED_WEEKS, emptySet()) ?: emptySet())
            .mapNotNull { it.toIntOrNull() }.toSet()
        set(value) = prefs.edit().putStringSet(KEY_COMPLETED_WEEKS, value.map { it.toString() }.toSet()).apply()

    fun isWeekCompleted(weekNumber: Int): Boolean {
        return completedWeeks.contains(weekNumber)
    }

    fun toggleWeekCompleted(weekNumber: Int) {
        val current = completedWeeks.toMutableSet()
        if (current.contains(weekNumber)) {
            current.remove(weekNumber)
            xp = (xp - 60).coerceAtLeast(0)
        } else {
            current.add(weekNumber)
            xp += 60
        }
        completedWeeks = current
    }

    // Error Logs (stored as a list of serialized items joined by ::)
    var errorLogs: List<ErrorLogItem>
        get() {
            val raw = prefs.getString("error_logs_list", "") ?: ""
            if (raw.isEmpty()) return emptyList()
            return raw.split("::").mapNotNull { ErrorLogItem.deserialize(it) }
        }
        set(value) {
            val serialized = value.joinToString("::") { it.serialize() }
            prefs.edit().putString("error_logs_list", serialized).apply()
        }

    fun addErrorLog(topic: String, subject: String, reason: String) {
        val current = errorLogs.toMutableList()
        val newItem = ErrorLogItem(
            id = System.currentTimeMillis().toString(),
            topic = topic,
            subject = subject,
            reason = reason,
            isSolved = false
        )
        current.add(newItem)
        errorLogs = current
    }

    fun removeErrorLog(id: String) {
        val current = errorLogs.toMutableList()
        current.removeAll { it.id == id }
        errorLogs = current
    }

    fun toggleErrorLogSolved(id: String) {
        val current = errorLogs.map {
            if (it.id == id) it.copy(isSolved = !it.isSolved) else it
        }
        errorLogs = current
    }

    // Today's Completed Blocks
    var completedBlocks: Set<String>
        get() {
            checkDayTransition()
            return prefs.getStringSet(KEY_COMPLETED_BLOCKS, emptySet()) ?: emptySet()
        }
        private set(value) {
            val today = getTodayString()
            prefs.edit()
                .putStringSet(KEY_COMPLETED_BLOCKS, value)
                .putStringSet("hist_$today", value)
                .apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    fun getBlocksCompletedOnDate(dateStr: String): Set<String> {
        return prefs.getStringSet("hist_$dateStr", emptySet()) ?: emptySet()
    }

    fun isBlockCompleted(blockType: BlockType): Boolean {
        return completedBlocks.contains(blockType.name)
    }

    fun toggleBlockCompleted(blockType: BlockType) {
        checkDayTransition()
        val current = completedBlocks.toMutableSet()
        val isChecking = !current.contains(blockType.name)

        if (isChecking) {
            current.add(blockType.name)
            xp += blockType.xpValue
        } else {
            current.remove(blockType.name)
            xp = (xp - blockType.xpValue).coerceAtLeast(0)
        }
        completedBlocks = current

        // Recalculate streak contribution instantly
        val completedBoth = current.contains("BLOCK1") && current.contains("BLOCK2")
        val todayStreakCounted = prefs.getBoolean(KEY_TODAY_STREAK_COUNTED, false)

        if (completedBoth && !todayStreakCounted) {
            setStreakInternal(streak + 1)
            prefs.edit().putBoolean(KEY_TODAY_STREAK_COUNTED, true).apply()
        } else if (!completedBoth && todayStreakCounted) {
            setStreakInternal((streak - 1).coerceAtLeast(0))
            prefs.edit().putBoolean(KEY_TODAY_STREAK_COUNTED, false).apply()
        }
    }

    // Stats
    var totalBlocksToday: Int
        get() = prefs.getInt(KEY_TOTAL_BLOCKS_TODAY, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_BLOCKS_TODAY, value).apply()

    var timeSavedMinutes: Int
        get() = prefs.getInt(KEY_TIME_SAVED_MINUTES, 0)
        set(value) = prefs.edit().putInt(KEY_TIME_SAVED_MINUTES, value).apply()

    // Scroll Stopper Settings
    var scrollLimit: Int
        get() = prefs.getInt(KEY_SCROLL_LIMIT, 5)
        set(value) = prefs.edit().putInt(KEY_SCROLL_LIMIT, value).apply()

    var pauseDurationSeconds: Int
        get() = prefs.getInt(KEY_PAUSE_DURATION, 15)
        set(value) = prefs.edit().putInt(KEY_PAUSE_DURATION, value).apply()

    var strictMode: Boolean
        get() = prefs.getBoolean(KEY_STRICT_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_STRICT_MODE, value).apply()

    var blockUntilTimestamp: Long
        get() = prefs.getLong(KEY_BLOCK_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_BLOCK_UNTIL, value).apply()

    val isCurrentlyBlocked: Boolean
        get() = System.currentTimeMillis() < blockUntilTimestamp

    // Quest Config
    var questName: String
        get() = prefs.getString("quest_name", "GATE EEE Quest") ?: "GATE EEE Quest"
        set(value) {
            prefs.edit().putString("quest_name", value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    var examDateMillis: Long
        get() {
            // Default is Feb 7, 2027
            val defaultCal = java.util.Calendar.getInstance().apply {
                set(2027, java.util.Calendar.FEBRUARY, 7, 0, 0, 0)
            }
            return prefs.getLong("exam_date_millis", defaultCal.timeInMillis)
        }
        set(value) {
            prefs.edit().putLong("exam_date_millis", value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value).apply()

    // Dynamic Study Blocks Config
    var block1Label: String
        get() = prefs.getString("block1_label", "Block 1 (Theory)") ?: "Block 1 (Theory)"
        set(value) {
            prefs.edit().putString("block1_label", value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    var block1Time: String
        get() = prefs.getString("block1_time", "6:00 AM - 8:00 AM") ?: "6:00 AM - 8:00 AM"
        set(value) {
            prefs.edit().putString("block1_time", value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    var block2Label: String
        get() = prefs.getString("block2_label", "Block 2 (Practice)") ?: "Block 2 (Practice)"
        set(value) {
            prefs.edit().putString("block2_label", value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    var block2Time: String
        get() = prefs.getString("block2_time", "6:00 PM - 8:30 PM") ?: "6:00 PM - 8:30 PM"
        set(value) {
            prefs.edit().putString("block2_time", value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    var block3Label: String
        get() = prefs.getString("block3_label", "Block 3 (Rotation)") ?: "Block 3 (Rotation)"
        set(value) {
            prefs.edit().putString("block3_label", value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    var block3Time: String
        get() = prefs.getString("block3_time", "9:00 PM - 10:00 PM") ?: "9:00 PM - 10:00 PM"
        set(value) {
            prefs.edit().putString("block3_time", value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    // Dynamic Syllabus (List of WeekPlan serialized by ^, separated by ::)
    var customSyllabus: List<WeekPlan>
        get() {
            val raw = prefs.getString("custom_syllabus_list", "") ?: ""
            if (raw.isEmpty()) return ScheduleData.weeks
            return raw.split("::").mapNotNull { WeekPlan.deserialize(it) }
        }
        set(value) {
            val serialized = value.joinToString("::") { it.serialize() }
            prefs.edit().putString("custom_syllabus_list", serialized).apply()
        }
}
