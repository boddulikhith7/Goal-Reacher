package com.example.scrollstopper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrollstopper.data.PreferenceManager

class ScrollStopperAccessibilityService : AccessibilityService() {

    private lateinit var prefManager: PreferenceManager
    private var lastScrollTime = 0L
    private var scrollCount = 0

    companion object {
        private const val TAG = "ScrollStopperService"
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

        fun isServiceEnabled(context: Context): Boolean {
            val expectedComponentName = "${context.packageName}/${ScrollStopperAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = enabledServicesSetting.split(":")
            for (service in colonSplitter) {
                if (service.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefManager = PreferenceManager(applicationContext)
        Log.d(TAG, "ScrollStopperAccessibilityService created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // We only care about YouTube
        if (packageName != YOUTUBE_PACKAGE) return

        val rootNode = rootInActiveWindow ?: return

        // Check if whitelisted educational channel is on screen
        if (containsWhitelistChannel(rootNode)) {
            Log.d(TAG, "Whitelisted educational channel detected. Bypassing scroll check.")
            scrollCount = 0
            return
        }

        // Check if user is looking at YouTube Shorts
        val isShorts = detectShorts(rootNode)

        if (isShorts) {
            // Case 1: Cool-down block is currently active -> enforce it immediately
            if (prefManager.isCurrentlyBlocked) {
                triggerBlock()
                return
            }

            // Case 2: Handle active scroll events to check if they exceed the limit
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                val now = System.currentTimeMillis()
                // Debounce scrolls (multiple scroll events might fire for one swipe, e.g., within 500ms)
                if (now - lastScrollTime > 800) {
                    lastScrollTime = now
                    scrollCount++
                    Log.d(TAG, "Shorts Scroll Detected! Count: $scrollCount / ${prefManager.scrollLimit}")

                    if (scrollCount >= prefManager.scrollLimit) {
                        scrollCount = 0
                        // Activate cool-down block
                        val pauseMillis = prefManager.pauseDurationSeconds * 1000L
                        prefManager.blockUntilTimestamp = System.currentTimeMillis() + pauseMillis
                        prefManager.totalBlocksToday += 1
                        prefManager.timeSavedMinutes += 15 // Estimate 15 mins saved per block
                        
                        triggerBlock()
                    }
                }
            }
        } else {
            // Reset scroll count if they navigate away from Shorts but stay in YouTube
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                scrollCount = 0
            }
        }
    }

    private fun containsWhitelistChannel(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val text = node.text?.toString()
        val contentDesc = node.contentDescription?.toString()
        val whitelist = listOf("Gate Smashers", "Neso Academy", "Ravindrababu", "NPTEL")

        for (channel in whitelist) {
            if (text != null && text.contains(channel, ignoreCase = true)) {
                return true
            }
            if (contentDesc != null && contentDesc.contains(channel, ignoreCase = true)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = containsWhitelistChannel(child)
                child.recycle()
                if (found) return true
            }
        }
        return false
    }

    private fun isIgnoredContainer(node: AccessibilityNodeInfo?): Boolean {
        var current = node?.parent
        while (current != null) {
            val id = current.viewIdResourceName
            if (id != null && (
                id.contains("pivot") ||
                id.contains("navigation") ||
                id.contains("tab") ||
                id.contains("bar") ||
                id.contains("menu") ||
                id.contains("shelf") ||
                id.contains("header")
            )) {
                current.recycle()
                return true
            }
            val parent = current.parent
            current.recycle()
            current = parent
        }
        return false
    }

    private fun detectShorts(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // Check resource IDs commonly used by YouTube Shorts video players and containers
        val viewId = node.viewIdResourceName
        if (viewId != null && (
            viewId.contains("reel_watch_fragment") ||
            viewId.contains("reel_recycler") ||
            viewId.contains("reel_container") ||
            viewId.contains("shorts_player") ||
            viewId.contains("reel_watch_layout") ||
            viewId.contains("shorts_video_player")
        )) {
            // Exclude bottom navigation bar items
            if (!viewId.contains("pivot_bar") && !viewId.contains("navigation")) {
                return true
            }
        }

        // Fallback: Check text or content description for "Shorts" or "Reels"
        // provided it's not inside a navigation bar or shelf container.
        val contentDesc = node.contentDescription?.toString()
        if (contentDesc != null && (contentDesc.equals("Shorts", ignoreCase = true) || contentDesc.contains("Reels"))) {
            if (!isIgnoredContainer(node)) {
                return true
            }
        }

        val text = node.text?.toString()
        if (text != null && text.equals("Shorts", ignoreCase = true)) {
            if (!isIgnoredContainer(node)) {
                return true
            }
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val isShortsChild = detectShorts(child)
                child.recycle()
                if (isShortsChild) {
                    return true
                }
            }
        }

        return false
    }

    private fun triggerBlock() {
        Log.d(TAG, "Enforcing study block: launching BlockerActivity")
        val intent = Intent(this, BlockerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "ScrollStopperAccessibilityService interrupted")
    }
}
