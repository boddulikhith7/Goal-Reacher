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
            
            val totalCount = customBlocks.size
            val completedCount = yesterdayCompleted.size
            val completedDiscipline = if (totalCount >= 2) completedCount >= 2 else (totalCount > 0 && completedCount == totalCount)

            val currentStreak = streak
            if (savedDate == yesterday) {
                if (!completedDiscipline) {
                    // Decrement mascot HP instead of breaking streak instantly
                    val currentHp = mascotHp
                    if (currentHp > 1) {
                        mascotHp = currentHp - 1
                    } else {
                        mascotHp = 0
                        setStreakInternal(0) // HP hit 0, break streak
                    }
                } else {
                    // Restores mascot HP to 3 if discipline was maintained
                    mascotHp = 3
                }
            } else {
                // More than a day gap, streak broken and HP reset
                mascotHp = 0
                setStreakInternal(0)
            }

            // Reset isCompleted flags inside customBlocks list for the new day
            val list = customBlocks.map { it.copy(isCompleted = false) }
            val serialized = list.joinToString("::") { it.serialize() }
            prefs.edit().putString("custom_timetable_blocks", serialized).apply()

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

    // Dynamic Study Blocks (Fully Customizable Timetable List)
    var customBlocks: List<TimetableBlock>
        get() {
            val raw = prefs.getString("custom_timetable_blocks", "") ?: ""
            if (raw.isEmpty()) {
                val defaults = listOf(
                    TimetableBlock("1", "Block 1 (Theory)", "6:00 AM - 8:00 AM", 20, false),
                    TimetableBlock("2", "Block 2 (Practice)", "6:00 PM - 8:30 PM", 20, false),
                    TimetableBlock("3", "Block 3 (Rotation)", "9:00 PM - 10:00 PM", 10, false)
                )
                val serialized = defaults.joinToString("::") { it.serialize() }
                prefs.edit().putString("custom_timetable_blocks", serialized).apply()
                return defaults
            }
            return raw.split("::").mapNotNull { TimetableBlock.deserialize(it) }
        }
        set(value) {
            val serialized = value.joinToString("::") { it.serialize() }
            prefs.edit().putString("custom_timetable_blocks", serialized).apply()
            
            // Sync back to legacy variables for compatibility (e.g. AlarmReceiver notifications)
            if (value.isNotEmpty()) {
                prefs.edit().putString("block1_label", value[0].label).putString("block1_time", value[0].timeRange).apply()
            }
            if (value.size >= 2) {
                prefs.edit().putString("block2_label", value[1].label).putString("block2_time", value[1].timeRange).apply()
            }
            if (value.size >= 3) {
                prefs.edit().putString("block3_label", value[2].label).putString("block3_time", value[2].timeRange).apply()
            }
            
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    fun toggleCustomBlockCompleted(id: String, xpValue: Int) {
        checkDayTransition()
        val current = completedBlocks.toMutableSet()
        val isChecking = !current.contains(id)

        if (isChecking) {
            current.add(id)
            xp += xpValue
        } else {
            current.remove(id)
            xp = (xp - xpValue).coerceAtLeast(0)
        }
        completedBlocks = current

        // Also update customBlocks list isCompleted flag
        val list = customBlocks.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            list[idx] = list[idx].copy(isCompleted = isChecking)
            val serialized = list.joinToString("::") { it.serialize() }
            prefs.edit().putString("custom_timetable_blocks", serialized).apply()
        }

        // Recalculate streak contribution instantly
        val completedCount = current.size
        val totalCount = list.size
        val completedDiscipline = if (totalCount >= 2) completedCount >= 2 else (totalCount > 0 && completedCount == totalCount)
        val todayStreakCounted = prefs.getBoolean(KEY_TODAY_STREAK_COUNTED, false)

        if (completedDiscipline && !todayStreakCounted) {
            setStreakInternal(streak + 1)
            mascotHp = 3 // Fully restore HP when today's discipline goal is met!
            prefs.edit().putBoolean(KEY_TODAY_STREAK_COUNTED, true).apply()
        } else if (!completedDiscipline && todayStreakCounted) {
            setStreakInternal((streak - 1).coerceAtLeast(0))
            prefs.edit().putBoolean(KEY_TODAY_STREAK_COUNTED, false).apply()
        }
    }

    fun addTimetableBlock(label: String, timeRange: String, xp: Int) {
        val current = customBlocks.toMutableList()
        val newId = System.currentTimeMillis().toString()
        current.add(TimetableBlock(newId, label, timeRange, xp, false))
        customBlocks = current
    }

    fun deleteTimetableBlock(id: String) {
        val current = customBlocks.toMutableList()
        current.removeAll { it.id == id }
        customBlocks = current

        val completed = completedBlocks.toMutableSet()
        if (completed.contains(id)) {
            completed.remove(id)
            completedBlocks = completed
        }
    }

    fun updateTimetableBlock(id: String, label: String, timeRange: String, xp: Int) {
        val current = customBlocks.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldBlock = current[index]
            current[index] = oldBlock.copy(label = label, timeRange = timeRange, xpValue = xp)
            customBlocks = current
        }
    }

    var mascotHp: Int
        get() = prefs.getInt("mascot_hp", 3)
        set(value) {
            prefs.edit().putInt("mascot_hp", value).apply()
            com.example.scrollstopper.widget.StreakWidgetProvider.triggerUpdate(context)
        }

    var emergencyBypassUntil: Long
        get() = prefs.getLong("emergency_bypass_until", 0L)
        set(value) {
            prefs.edit().putLong("emergency_bypass_until", value).apply()
        }

    fun purchaseEmergencyPause(): Boolean {
        val currentXp = xp
        if (currentXp >= 50) {
            xp = currentXp - 50
            emergencyBypassUntil = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
            accumulatedBlockedTimeMs = 0L
            return true
        }
        return false
    }

    var flashcards: List<Flashcard>
        get() {
            val serialized = prefs.getString("flashcards_list", "") ?: ""
            if (serialized.isEmpty()) return emptyList()
            return serialized.split("::").mapNotNull { Flashcard.deserialize(it) }
        }
        set(value) {
            val serialized = value.joinToString("::") { it.serialize() }
            prefs.edit().putString("flashcards_list", serialized).apply()
        }

    var blockerQuizPool: List<BlockerQuizQuestion>
        get() {
            val serialized = prefs.getString("blocker_quiz_pool", "") ?: ""
            if (serialized.isEmpty()) return emptyList()
            return serialized.split("::").mapNotNull { BlockerQuizQuestion.deserialize(it) }
        }
        set(value) {
            val serialized = value.joinToString("::") { it.serialize() }
            prefs.edit().putString("blocker_quiz_pool", serialized).apply()
        }

    fun isTimeInBlock(timeRange: String): Boolean {
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

    val hasActiveStudyBlock: Boolean
        get() = customBlocks.any { block ->
            !block.isCompleted && isTimeInBlock(block.timeRange)
        }

    var accumulatedBlockedTimeMs: Long
        get() = prefs.getLong("accumulated_blocked_time_ms", 0L)
        set(value) = prefs.edit().putLong("accumulated_blocked_time_ms", value).apply()

    val isAppBlockingActive: Boolean
        get() {
            val now = System.currentTimeMillis()
            val isCoolDownBlocked = isCurrentlyBlocked
            val isBypassed = now < emergencyBypassUntil
            return (isCoolDownBlocked || strictMode || hasActiveStudyBlock) && !isBypassed
        }

    val isSettingsLocked: Boolean
        get() {
            val now = System.currentTimeMillis()
            val isCoolDownBlocked = isCurrentlyBlocked
            val isBypassed = now < emergencyBypassUntil
            return (isCoolDownBlocked || hasActiveStudyBlock) && !isBypassed
        }

    val isFocusActive: Boolean
        get() = isAppBlockingActive
}
