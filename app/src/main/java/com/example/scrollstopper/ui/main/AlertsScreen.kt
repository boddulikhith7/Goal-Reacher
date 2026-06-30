package com.example.scrollstopper.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.scrollstopper.ScrollStopperAccessibilityService
import com.example.scrollstopper.data.BlockType
import com.example.scrollstopper.notifications.AlarmReceiver
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Notification Permission Check (For Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            AlarmReceiver.scheduleAlarms(context)
        }
    }

    // Live generated alert logic
    val alerts = remember(state.completedBlocks, state.completedWeeks, state.currentWeek) {
        val list = mutableListOf<AlertItem>()
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val daysToSunday = when (dayOfWeek) {
            Calendar.MONDAY -> 6
            Calendar.TUESDAY -> 5
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 2
            Calendar.SATURDAY -> 1
            Calendar.SUNDAY -> 0
            else -> 0
        }

        val isWeekComplete = state.completedWeeks.contains(state.currentWeek)
        val completedBlockCount = state.completedBlocks.size

        // 1. Week Completion Alerts
        if (isWeekComplete) {
            list.add(
                AlertItem(
                    title = "Week Goals Secured!",
                    message = "You have marked the current week complete. You've successfully banked time and got ahead of pace!",
                    severity = AlertSeverity.SUCCESS
                )
            )
        } else {
            when (daysToSunday) {
                0 -> list.add(
                    AlertItem(
                        title = "Week Deadline Today!",
                        message = "Sunday night is the deadline. Review your topic and mark the week complete before midnight!",
                        severity = AlertSeverity.DANGER
                    )
                )
                1, 2 -> list.add(
                    AlertItem(
                        title = "Week Deadline Looming",
                        message = "Only $daysToSunday days left to finish this week's study blocks and topic. Keep the pace up!",
                        severity = AlertSeverity.DANGER
                    )
                )
                3, 4 -> list.add(
                    AlertItem(
                        title = "Mid-week Progress Check",
                        message = "We are halfway through the study week ($daysToSunday days left). Complete remaining blocks to avoid a weekend lag.",
                        severity = AlertSeverity.WARNING
                    )
                )
                else -> list.add(
                    AlertItem(
                        title = "Week Fresh Start",
                        message = "Good start! You have $daysToSunday days left until the Sunday night schedule deadline.",
                        severity = AlertSeverity.SUCCESS
                    )
                )
            }
        }

        // 2. Daily Block Alerts
        if (completedBlockCount == 3) {
            list.add(
                AlertItem(
                    title = "Perfect Day Achieved!",
                    message = "All 3 study blocks are marked complete today. Max +50 XP claimed!",
                    severity = AlertSeverity.SUCCESS
                )
            )
        } else {
            val missingBlocks = 3 - completedBlockCount
            if (missingBlocks >= 2) {
                list.add(
                    AlertItem(
                        title = "Discipline Warning",
                        message = "You are missing $missingBlocks study blocks today. Wake up and complete remaining blocks to save your streak!",
                        severity = AlertSeverity.DANGER
                    )
                )
            } else {
                list.add(
                    AlertItem(
                        title = "Almost There",
                        message = "Just 1 study block missing today. Complete it to log a perfect day!",
                        severity = AlertSeverity.WARNING
                    )
                )
            }
        }

        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "SYSTEM ALERTS & FOCUS SHIELD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA78BFA),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Shield & Alerts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // Push Notifications Permission Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔔 Local Reminder Alarms",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Allows the app to send exact reminders on study block starts (6:00 AM, 6:00 PM, 9:00 PM) even when closed.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
                        ) {
                            Text(text = "Enable Reminders", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF065F46).copy(alpha = 0.15f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓ Reminders & Alarms are Active",
                                color = Color(0xFF34D399),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Scroll Shield status card
        item {
            val isActive = state.isAccessibilityActive
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🛡️ Scroll Shield (ScrollStopper)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isActive) "Status: Shield is Active" else "Status: Shield is Off",
                                fontSize = 11.sp,
                                color = if (isActive) Color.Green else Color.Red.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) Color.White.copy(alpha = 0.08f) else Color(0xFF8B5CF6)
                            )
                        ) {
                            Text(
                                text = if (isActive) "Configure" else "Turn On",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Shield limits sliders
                    Text(
                        text = "Swipe/Scroll Limit: ${state.scrollLimit} scrolls",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = state.scrollLimit.toFloat(),
                        onValueChange = { viewModel.updateScrollLimit(it.toInt()) },
                        valueRange = 3f..20f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8B5CF6),
                            activeTrackColor = Color(0xFF8B5CF6),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Mindful Pause Duration: ${state.pauseDurationSeconds}s",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = state.pauseDurationSeconds.toFloat(),
                        onValueChange = { viewModel.updatePauseDuration(it.toInt()) },
                        valueRange = 5f..120f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8B5CF6),
                            activeTrackColor = Color(0xFF8B5CF6),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Strict Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Strict Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Lock settings modifications during active blocking periods.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = state.strictMode,
                            onCheckedChange = { viewModel.toggleStrictMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF8B5CF6),
                                checkedTrackColor = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.3f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Blocker Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "SCROLLS BLOCKED TODAY", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            Text(text = "${state.totalBlocksToday} Times", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "ESTIMATED TIME SAVED", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            Text(text = "${state.timeSavedMinutes} Minutes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        }
                    }
                }
            }
        }

        // Quest & Timetable Editor card
        item {
            var tempQuestName by remember(state.questName) { mutableStateOf(state.questName) }
            var tempApiKey by remember(state.geminiApiKey) { mutableStateOf(state.geminiApiKey) }
            
            var b1Label by remember(state.block1Label) { mutableStateOf(state.block1Label) }
            var b1Time by remember(state.block1Time) { mutableStateOf(state.block1Time) }
            var b2Label by remember(state.block2Label) { mutableStateOf(state.block2Label) }
            var b2Time by remember(state.block2Time) { mutableStateOf(state.block2Time) }
            var b3Label by remember(state.block3Label) { mutableStateOf(state.block3Label) }
            var b3Time by remember(state.block3Time) { mutableStateOf(state.block3Time) }

            // Target Date configuration (Format: YYYY-MM-DD)
            val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
            var tempDateStr by remember(state.examDateMillis) {
                mutableStateOf(sdf.format(java.util.Date(state.examDateMillis)))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚙️ Quest Settings & Timetable Editor",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Customize the app title, countdown, and daily study blocks.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Quest Title
                    OutlinedTextField(
                        value = tempQuestName,
                        onValueChange = { tempQuestName = it },
                        label = { Text("Active Quest Name (e.g. GATE EEE Quest)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Exam Target Date
                    OutlinedTextField(
                        value = tempDateStr,
                        onValueChange = { tempDateStr = it },
                        label = { Text("Exam Date (Format: YYYY-MM-DD)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gemini API Key
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("Gemini API Key (Optional)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Edit Study Blocks",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Block 1 Customization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = b1Label,
                            onValueChange = { b1Label = it },
                            label = { Text("Block 1 Label", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = b1Time,
                            onValueChange = { b1Time = it },
                            label = { Text("Block 1 Time", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Block 2 Customization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = b2Label,
                            onValueChange = { b2Label = it },
                            label = { Text("Block 2 Label", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = b2Time,
                            onValueChange = { b2Time = it },
                            label = { Text("Block 2 Time", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Block 3 Customization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = b3Label,
                            onValueChange = { b3Label = it },
                            label = { Text("Block 3 Label", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = b3Time,
                            onValueChange = { b3Time = it },
                            label = { Text("Block 3 Time", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val parsedDate = try {
                                sdf.parse(tempDateStr)?.time ?: state.examDateMillis
                            } catch (e: Exception) {
                                state.examDateMillis
                            }
                            viewModel.updateQuestConfig(tempQuestName, parsedDate)
                            viewModel.updateGeminiKey(tempApiKey)
                            viewModel.updateBlockConfig(1, b1Label, b1Time)
                            viewModel.updateBlockConfig(2, b2Label, b2Time)
                            viewModel.updateBlockConfig(3, b3Label, b3Time)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("Save Configuration", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Alerts section header
        item {
            Text(
                text = "Active Live Status Alerts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // List of active live alerts
        items(alerts) { alert ->
            val bgTint = when (alert.severity) {
                AlertSeverity.DANGER -> Color(0xFF991B1B).copy(alpha = 0.15f) // Red
                AlertSeverity.WARNING -> Color(0xFF92400E).copy(alpha = 0.15f) // Amber
                AlertSeverity.SUCCESS -> Color(0xFF065F46).copy(alpha = 0.15f) // Green
            }
            val accentColor = when (alert.severity) {
                AlertSeverity.DANGER -> Color(0xFFF87171)
                AlertSeverity.WARNING -> Color(0xFFFBBF24)
                AlertSeverity.SUCCESS -> Color(0xFF34D399)
            }
            val borderTint = accentColor.copy(alpha = 0.3f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = bgTint)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = alert.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alert.message,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

data class AlertItem(
    val title: String,
    val message: String,
    val severity: AlertSeverity
)

enum class AlertSeverity {
    DANGER,
    WARNING,
    SUCCESS
}
