package com.example.scrollstopper.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollstopper.data.BlockType
import com.example.scrollstopper.data.QuotesData
import com.example.scrollstopper.data.ScheduleData
import com.example.scrollstopper.data.WeekPlan
import com.example.scrollstopper.data.TimetableBlock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.media.RingtoneManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    var activeTimerBlock by remember { mutableStateOf<BlockType?>(null) }

    val weekPlan = state.customSyllabus.find { it.weekNumber == state.currentWeek }
        ?: if (state.customSyllabus.isNotEmpty()) state.customSyllabus[0] else ScheduleData.weeks[0]

    // Quote persisted for the calendar day
    val todayQuote = remember {
        val todaySeed = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()).toLongOrNull() ?: 1L
        val random = Random(todaySeed)
        QuotesData.quotes[random.nextInt(QuotesData.quotes.size)]
    }

    // Exam countdown
    val daysLeft = remember(state.examDateMillis) {
        val diff = (state.examDateMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        diff / (1000 * 60 * 60 * 24)
    }

    // Rank details
    val currentRank = remember(state.xp) {
        ScheduleData.ranks.lastOrNull { state.xp >= it.xpRequired } ?: ScheduleData.ranks[0]
    }
    val nextRank = remember(state.xp) {
        val index = ScheduleData.ranks.indexOf(currentRank)
        if (index < ScheduleData.ranks.size - 1) ScheduleData.ranks[index + 1] else null
    }

    val xpProgress = remember(state.xp, currentRank, nextRank) {
        if (nextRank != null) {
            val range = nextRank.xpRequired - currentRank.xpRequired
            val gained = state.xp - currentRank.xpRequired
            (gained.toFloat() / range).coerceIn(0f, 1f)
        } else {
            1.0f
        }
    }

    var showWeekSelector by remember { mutableStateOf(false) }
    var newErrorTopicText by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Maths") }
    var selectedReason by remember { mutableStateOf("Calculation") }
    var showSubjectDropdown by remember { mutableStateOf(false) }
    var showReasonDropdown by remember { mutableStateOf(false) }

    var filterStatus by remember { mutableStateOf("All") } // "All", "Unsolved", "Solved"
    var filterSubject by remember { mutableStateOf("All") }
    var filterReason by remember { mutableStateOf("All") }
    var showFilterSubjectDropdown by remember { mutableStateOf(false) }
    var showFilterReasonDropdown by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val completionPercent = (state.completedWeeks.size.toFloat() / 33f * 100).toInt()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.questName.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFA78BFA),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Study Dashboard",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                
                // Streak Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🔥", fontSize = 16.sp)
                        Text(
                            text = "${state.streak} Days",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                    }
                }
            }
        }

        // Accessibility Quick-Toggle Helper Card
        item {
            val context = LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isAccessibilityActive) {
                        Color(0xFF065F46).copy(alpha = 0.15f) // Emerald tinted dark green
                    } else {
                        Color(0xFF991B1B).copy(alpha = 0.15f) // Ruby tinted dark red
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (state.isAccessibilityActive) {
                        Color(0xFF10B981).copy(alpha = 0.3f)
                    } else {
                        Color(0xFFEF4444).copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (state.isAccessibilityActive) "🛡️" else "⚠️",
                        fontSize = 24.sp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isAccessibilityActive) "Scroll Shield is Active" else "Scroll Shield is Paused",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (state.isAccessibilityActive) {
                                "Paying with Paytm? Tap here to temporarily disable the accessibility service to avoid blocks."
                            } else {
                                "Tap here to enable focus protection in Android Accessibility Settings for your study blocks."
                            },
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Daily Quote Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\"${todayQuote.text}\"",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "- ${todayQuote.author}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                }
            }
        }

        // Rank Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentRank.name.uppercase(Locale.ROOT),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF818CF8)
                            )
                            Text(
                                text = currentRank.description,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        
                        Text(
                            text = "${state.xp} XP",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF818CF8),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    
                    if (nextRank != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${nextRank.xpRequired - state.xp} XP to next rank (${nextRank.name})",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    Pair("Current Focus", "Week ${state.currentWeek}/33"),
                    Pair("Days to GATE", "$daysLeft Days"),
                    Pair("Syllabus Done", "$completionPercent%")
                ).forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Week Selector Dropdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWeekSelector = !showWeekSelector },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = weekPlan.phase,
                            fontSize = 10.sp,
                            color = Color(0xFFA78BFA),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Week ${state.currentWeek}: ${weekPlan.subjects}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Topic: ${weekPlan.topic}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            if (showWeekSelector) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
                ) {
                    LazyColumn(contentPadding = PaddingValues(8.dp)) {
                        items(state.customSyllabus) { week ->
                            val isCompleted = state.completedWeeks.contains(week.weekNumber)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectWeek(week.weekNumber)
                                        showWeekSelector = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Week ${week.weekNumber}: ${week.topic}",
                                    fontSize = 13.sp,
                                    color = if (week.weekNumber == state.currentWeek) Color(0xFF818CF8) else Color.White
                                )
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Green,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Quests header
        item {
            Text(
                text = "Today's Quest Blocks",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Quest Block Cards
        items(state.customBlocks.size) { index ->
            val block = state.customBlocks[index]
            val isDone = state.completedBlocks.contains(block.id)
            val detailSource = when (index) {
                0 -> "Topic: ${weekPlan.topic}\nSource: ${weekPlan.block1Source}"
                1 -> "Solve topic-wise questions.\nPortal: ${weekPlan.block2Source}"
                2 -> when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY -> "Python Programming (Mon/Wed/Fri)"
                    Calendar.TUESDAY, Calendar.THURSDAY -> "Internship Preparation & Emails (Tue/Thu)"
                    Calendar.SATURDAY -> "Weekly Full Syllabus Test (Sat)"
                    else -> "Mock Test Postmortem / Rest (Sun)"
                }
                else -> "Custom study session. Focus on active study roadmap."
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleCustomBlock(block.id, block.xpValue) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDone) Color(0xFF065F46) else Color.White.copy(alpha = 0.05f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 2.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isDone) Color.Green else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = block.timeRange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) Color.White else Color(0xFFA78BFA)
                        )
                        Text(
                            text = block.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = detailSource,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                    }
                    Text(
                        text = "+${block.xpValue} XP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!isDone) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { 
                                    activeTimerBlock = when (index) {
                                        0 -> BlockType.BLOCK1
                                        1 -> BlockType.BLOCK2
                                        else -> BlockType.BLOCK3
                                    }
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⏱️", fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Mark Week Completed Card
        item {
            val isWeekDone = state.completedWeeks.contains(state.currentWeek)
            Button(
                onClick = { viewModel.toggleWeekCompleted(state.currentWeek) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWeekDone) Color(0xFF047857) else Color(0xFF312E81),
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isWeekDone) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    }
                    Text(
                        text = if (isWeekDone) "Week Completed! (+60 XP)" else "Mark Week Complete (+60 XP)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Advanced Error Log Section
        item {
            Text(
                text = "Advanced Error Log",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Error Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Log a conceptual / calculation mistake:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newErrorTopicText,
                        onValueChange = { newErrorTopicText = it },
                        placeholder = { Text("Mistake description (e.g. Eigenvalue formula error)", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                            focusedBorderColor = Color(0xFF818CF8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Subject Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showSubjectDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = selectedSubject, fontSize = 12.sp, maxLines = 1)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = showSubjectDropdown,
                                onDismissRequest = { showSubjectDropdown = false },
                                modifier = Modifier.background(Color(0xFF1E1B4B))
                            ) {
                                val subjectsList = listOf("Maths", "Circuits", "Electromagnetics", "Signals", "Machines", "Power Systems", "Controls", "Measurements", "Analog/Digital", "Power Electronics", "Aptitude")
                                subjectsList.forEach { subject ->
                                    DropdownMenuItem(
                                        text = { Text(subject, color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            selectedSubject = subject
                                            showSubjectDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Reason Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showReasonDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = selectedReason, fontSize = 12.sp, maxLines = 1)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = showReasonDropdown,
                                onDismissRequest = { showReasonDropdown = false },
                                modifier = Modifier.background(Color(0xFF1E1B4B))
                            ) {
                                val reasonsList = listOf("Calculation", "Conceptual", "Silly", "Speed", "Misreading")
                                reasonsList.forEach { reason ->
                                    DropdownMenuItem(
                                        text = { Text(reason, color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            selectedReason = reason
                                            showReasonDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (newErrorTopicText.isNotBlank()) {
                                viewModel.addErrorLog(newErrorTopicText, selectedSubject, selectedReason)
                                newErrorTopicText = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
                    ) {
                        Text("Add to Error Log", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Filters section
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status filter row (All / Unsolved / Solved)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statuses = listOf("All", "Unsolved", "Solved")
                    statuses.forEach { status ->
                        val isSelected = filterStatus == status
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.05f))
                                .clickable { filterStatus = status }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = status,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Dropdowns for filtering by Subject / Reason
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Filter Subject Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showFilterSubjectDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = if (filterSubject == "All") "Subj: All" else filterSubject, fontSize = 11.sp, maxLines = 1)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showFilterSubjectDropdown,
                            onDismissRequest = { showFilterSubjectDropdown = false },
                            modifier = Modifier.background(Color(0xFF1E1B4B))
                        ) {
                            val filterSubjects = listOf("All", "Maths", "Circuits", "Electromagnetics", "Signals", "Machines", "Power Systems", "Controls", "Measurements", "Analog/Digital", "Power Electronics", "Aptitude")
                            filterSubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub, color = Color.White, fontSize = 12.sp) },
                                    onClick = {
                                        filterSubject = sub
                                        showFilterSubjectDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Filter Reason Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showFilterReasonDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = if (filterReason == "All") "Reason: All" else filterReason, fontSize = 11.sp, maxLines = 1)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showFilterReasonDropdown,
                            onDismissRequest = { showFilterReasonDropdown = false },
                            modifier = Modifier.background(Color(0xFF1E1B4B))
                        ) {
                            val filterReasonsList = listOf("All", "Calculation", "Conceptual", "Silly", "Speed", "Misreading")
                            filterReasonsList.forEach { reas ->
                                DropdownMenuItem(
                                    text = { Text(reas, color = Color.White, fontSize = 12.sp) },
                                    onClick = {
                                        filterReason = reas
                                        showFilterReasonDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        val filteredLogs = state.errorLogs.filter { log ->
            val matchesStatus = when (filterStatus) {
                "Solved" -> log.isSolved
                "Unsolved" -> !log.isSolved
                else -> true
            }
            val matchesSubject = if (filterSubject == "All") true else log.subject == filterSubject
            val matchesReason = if (filterReason == "All") true else log.reason == filterReason
            matchesStatus && matchesSubject && matchesReason
        }

        if (filteredLogs.isEmpty()) {
            item {
                Text(
                    text = "No matching errors logged. All clean!",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleErrorLogSolved(log.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.isSolved) Color(0xFF065F46).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f)
                    ),
                    border = if (log.isSolved) BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.4f)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.topic,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (log.isSolved) Color.White.copy(alpha = 0.4f) else Color.White,
                                textDecoration = if (log.isSolved) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Subject Tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF312E81).copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = log.subject, fontSize = 10.sp, color = Color(0xFFC7D2FE))
                                }
                                // Reason Tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF78350F).copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = log.reason, fontSize = 10.sp, color = Color(0xFFFDE68A))
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = log.isSolved,
                                onCheckedChange = { viewModel.toggleErrorLogSolved(log.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF10B981),
                                    uncheckedColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            IconButton(
                                onClick = { viewModel.removeErrorLog(log.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete error",
                                    tint = Color.Red.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        if (activeTimerBlock != null) {
            FocusTimerDialog(
                blockType = activeTimerBlock!!,
                onClose = { activeTimerBlock = null },
                onCompleteBlock = {
                    viewModel.toggleBlock(activeTimerBlock!!)
                    activeTimerBlock = null
                }
            )
        }
    }

@Composable
fun FocusTimerDialog(
    blockType: BlockType,
    onClose: () -> Unit,
    onCompleteBlock: () -> Unit
) {
    val context = LocalContext.current
    var timeRemaining by remember { mutableStateOf(50 * 60) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timeRemaining) {
        if (isRunning && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
            
            if (timeRemaining == 0) {
                // Trigger final callbacks
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(1000)
                }

                try {
                    val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context, notificationUri)
                    r.play()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                onCompleteBlock()
            }
        }
    }

    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeStr = String.format("%02d:%02d", minutes, seconds)

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = blockType.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA78BFA)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Focus Timer",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = timeStr,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Keep your phone in distraction-free mode. The Scroll Shield is active.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play/Pause button
                Button(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFF991B1B) else Color(0xFF047857)
                    )
                ) {
                    Text(text = if (isRunning) "Pause" else "Start")
                }
                
                // Debug Skip button
                Button(
                    onClick = {
                        timeRemaining = 0
                        onCompleteBlock()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
                ) {
                    Text(text = "Skip")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(text = "Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF1E1B4B),
        shape = RoundedCornerShape(16.dp)
    )
}
