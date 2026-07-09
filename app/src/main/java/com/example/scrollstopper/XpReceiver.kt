package com.example.scrollstopper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.scrollstopper.data.PreferenceManager

class XpReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "XpReceiver"
        private const val ACTION_ADD_XP = "com.example.scrollstopper.ADD_XP"
        private const val EXTRA_AMOUNT = "amount"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ADD_XP) {
            val amount = intent.getIntExtra(EXTRA_AMOUNT, 50)
            Log.d(TAG, "Received broadcast to add $amount XP")
            
            val prefManager = PreferenceManager(context)
            prefManager.xp += amount
            
            // Fully restore Hooty's mascot health when physical training XP is received!
            prefManager.mascotHp = 3
            
            Log.d(TAG, "Successfully credited $amount XP and fully restored Hooty's HP.")
        }
    }
}
