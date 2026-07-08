package com.example.scrollstopper.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrollstopper.ScrollStopperAccessibilityService
import com.example.scrollstopper.data.BlockType
import com.example.scrollstopper.data.ErrorLogItem
import com.example.scrollstopper.data.PreferenceManager
import com.example.scrollstopper.data.WeekPlan
import com.example.scrollstopper.data.TimetableBlock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val xp: Int = 0,
    val streak: Int = 0,
    val currentWeek: Int = 1,
    val completedWeeks: Set<Int> = emptySet(),
    val errorLogs: List<ErrorLogItem> = emptyList(),
    val completedBlocks: Set<String> = emptySet(),
    val totalBlocksToday: Int = 0,
    val timeSavedMinutes: Int = 0,
    val scrollLimit: Int = 5,
    val pauseDurationSeconds: Int = 15,
    val strictMode: Boolean = false,
    val isAccessibilityActive: Boolean = false,
    val questName: String = "GATE EEE Quest",
    val examDateMillis: Long = 0L,
    val geminiApiKey: String = "",
    val customSyllabus: List<WeekPlan> = emptyList(),
    val customBlocks: List<TimetableBlock> = emptyList()
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val prefManager = PreferenceManager(application)
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        prefManager.checkDayTransition()
        val isActive = ScrollStopperAccessibilityService.isServiceEnabled(getApplication())
        _uiState.update {
            MainUiState(
                xp = prefManager.xp,
                streak = prefManager.streak,
                currentWeek = prefManager.currentWeek,
                completedWeeks = prefManager.completedWeeks,
                errorLogs = prefManager.errorLogs,
                completedBlocks = prefManager.completedBlocks,
                totalBlocksToday = prefManager.totalBlocksToday,
                timeSavedMinutes = prefManager.timeSavedMinutes,
                scrollLimit = prefManager.scrollLimit,
                pauseDurationSeconds = prefManager.pauseDurationSeconds,
                strictMode = prefManager.strictMode,
                isAccessibilityActive = isActive,
                questName = prefManager.questName,
                examDateMillis = prefManager.examDateMillis,
                geminiApiKey = prefManager.geminiApiKey,
                customSyllabus = prefManager.customSyllabus,
                customBlocks = prefManager.customBlocks
            )
        }
    }

    fun toggleBlock(blockType: BlockType) {
        prefManager.toggleBlockCompleted(blockType)
        refreshState()
    }

    fun toggleCustomBlock(id: String, xpValue: Int) {
        prefManager.toggleCustomBlockCompleted(id, xpValue)
        refreshState()
    }

    fun selectWeek(weekNumber: Int) {
        prefManager.currentWeek = weekNumber
        refreshState()
    }

    fun toggleWeekCompleted(weekNumber: Int) {
        prefManager.toggleWeekCompleted(weekNumber)
        refreshState()
    }

    fun addErrorLog(topic: String, subject: String, reason: String) {
        if (topic.isNotBlank()) {
            prefManager.addErrorLog(topic, subject, reason)
            refreshState()
        }
    }

    fun removeErrorLog(id: String) {
        prefManager.removeErrorLog(id)
        refreshState()
    }

    fun toggleErrorLogSolved(id: String) {
        prefManager.toggleErrorLogSolved(id)
        refreshState()
    }

    fun updateScrollLimit(limit: Int) {
        // Prevent editing during active block in Strict Mode
        if (prefManager.strictMode && prefManager.isCurrentlyBlocked) return
        prefManager.scrollLimit = limit
        refreshState()
    }

    fun updatePauseDuration(seconds: Int) {
        if (prefManager.strictMode && prefManager.isCurrentlyBlocked) return
        prefManager.pauseDurationSeconds = seconds
        refreshState()
    }

    fun toggleStrictMode(enabled: Boolean) {
        if (prefManager.strictMode && prefManager.isCurrentlyBlocked) return
        prefManager.strictMode = enabled
        refreshState()
    }
    fun updateQuestConfig(name: String, dateMillis: Long) {
        prefManager.questName = name
        prefManager.examDateMillis = dateMillis
        refreshState()
    }

    fun updateGeminiKey(key: String) {
        prefManager.geminiApiKey = key
        refreshState()
    }

    fun addTimetableBlock(label: String, timeRange: String, xp: Int) {
        prefManager.addTimetableBlock(label, timeRange, xp)
        refreshState()
    }

    fun deleteTimetableBlock(id: String) {
        prefManager.deleteTimetableBlock(id)
        refreshState()
    }

    fun updateTimetableBlock(id: String, label: String, timeRange: String, xp: Int) {
        prefManager.updateTimetableBlock(id, label, timeRange, xp)
        refreshState()
    }

    fun updateCustomSyllabus(plans: List<WeekPlan>) {
        prefManager.customSyllabus = plans
        prefManager.currentWeek = 1
        refreshState()
    }
}
