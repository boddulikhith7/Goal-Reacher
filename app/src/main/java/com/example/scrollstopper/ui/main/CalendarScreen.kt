package com.example.scrollstopper.ui.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollstopper.data.PreferenceManager
import com.example.scrollstopper.data.ScheduleData
import com.example.scrollstopper.data.TimetableBlock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefManager = remember { PreferenceManager(context) }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
    }

    // Generate dates of current week (Monday to Sunday)
    val weekDays = remember {
        val days = mutableListOf<DayInfo>()
        val cal = Calendar.getInstance()
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Adjust so Monday is first day of the week
        val offset = if (currentDayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - currentDayOfWeek
        cal.add(Calendar.DATE, offset)

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNumFormatter = SimpleDateFormat("d", Locale.getDefault())

        for (i in 0 until 7) {
            val dateStr = dateFormatter.format(cal.time)
            val dayNum = dayNumFormatter.format(cal.time)
            
            // Query preference manager for blocks completed on this date
            val completed = prefManager.getBlocksCompletedOnDate(dateStr)
            val completionType = when (completed.size) {
                3 -> DayCompletionType.FULL
                1, 2 -> DayCompletionType.PARTIAL
                else -> DayCompletionType.NONE
            }
            
            days.add(
                DayInfo(
                    dayLabel = dayNames[i],
                    dayNumber = dayNum,
                    dateString = dateStr,
                    completion = completionType,
                    isToday = dateStr == dateFormatter.format(Date())
                )
            )
            cal.add(Calendar.DATE, 1)
        }
        days
    }

    val weeklyXp = remember(weekDays, state.completedBlocks) {
        weekDays.map { day ->
            val completed = prefManager.getBlocksCompletedOnDate(day.dateString)
            var xp = 0
            if (completed.contains("BLOCK1")) xp += 20
            if (completed.contains("BLOCK2")) xp += 20
            if (completed.contains("BLOCK3")) xp += 10
            xp
        }
    }

    val dayLabels = remember(weekDays) {
        weekDays.map { it.dayLabel }
    }

    val weekPlan = state.customSyllabus.find { it.weekNumber == state.currentWeek }
        ?: if (state.customSyllabus.isNotEmpty()) state.customSyllabus[0] else ScheduleData.weeks[0]

    // Construct timeline events dynamically from custom blocks list
    val timelineEvents = remember(state.customBlocks, weekPlan) {
        val events = mutableListOf<TimelineItem>()
        // Add default wake up
        events.add(TimelineItem("5:00 AM - 6:00 AM", "Wake up & Morning Routine", TimelineType.NEUTRAL, ""))
        
        state.customBlocks.forEachIndexed { index, block ->
            val type = when (index) {
                0 -> TimelineType.BLOCK1
                1 -> TimelineType.BLOCK2
                2 -> TimelineType.BLOCK3
                else -> TimelineType.BLOCK1 // Roll over styling
            }
            val details = when (index) {
                0 -> "Topic: ${weekPlan.topic}\nSource: ${weekPlan.block1Source}"
                1 -> "Solve topic-wise questions.\nPortal: ${weekPlan.block2Source}"
                2 -> "Mon/Wed/Fri: Python | Tue/Thu: Internships\nSat: Weekly Test | Sun: Mock review"
                else -> "Custom study block. Focus on your active exam roadmap."
            }
            events.add(TimelineItem(block.timeRange, block.label, type, details))
        }
        
        // Add neutral gaps if list is small
        if (state.customBlocks.size <= 2) {
            events.add(TimelineItem("8:30 PM - 9:00 PM", "Review & Rest", TimelineType.NEUTRAL, ""))
        }
        
        events
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
                    text = "WEEKLY DISCIPLINE CALENDAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA78BFA),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = todayFormatted,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // 7-Day Day-Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly Completion Grid",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDays.forEach { day ->
                            val borderColor = if (day.isToday) Color(0xFF818CF8) else Color.Transparent
                            val bgCellColor = when (day.completion) {
                                DayCompletionType.FULL -> Color(0xFF047857) // Solid Green
                                DayCompletionType.PARTIAL -> Color(0xFFD97706) // Solid Amber
                                DayCompletionType.NONE -> Color.White.copy(alpha = 0.06f)
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = day.dayLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgCellColor)
                                        .border(
                                            width = if (day.isToday) 2.dp else 0.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.dayNumber,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (day.completion != DayCompletionType.NONE) Color.White else Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem("Green = All Blocks", Color(0xFF047857))
                        LegendItem("Amber = Partial", Color(0xFFD97706))
                        LegendItem("Empty = Not Logged", Color.White.copy(alpha = 0.15f))
                    }
                }
            }
        }

        item {
            PerformanceChart(
                weeklyXp = weeklyXp,
                dayLabels = dayLabels,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Timeline Header
        item {
            Text(
                text = "Daily Study Timeline",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Daily Timeline List
        items(timelineEvents) { event ->
            val bgTint = when (event.type) {
                TimelineType.BLOCK1 -> Color(0xFF1E3A8A).copy(alpha = 0.4f) // Blue
                TimelineType.BLOCK2 -> Color(0xFF78350F).copy(alpha = 0.4f) // Amber
                TimelineType.BLOCK3 -> Color(0xFF064E3B).copy(alpha = 0.4f) // Green
                TimelineType.NEUTRAL -> Color.White.copy(alpha = 0.02f)
            }
            val accentColor = when (event.type) {
                TimelineType.BLOCK1 -> Color(0xFF60A5FA)
                TimelineType.BLOCK2 -> Color(0xFFF59E0B)
                TimelineType.BLOCK3 -> Color(0xFF34D399)
                TimelineType.NEUTRAL -> Color.White.copy(alpha = 0.2f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgTint)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time slot
                Column(modifier = Modifier.width(120.dp)) {
                    Text(
                        text = event.time,
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Event details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (event.details.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = event.details,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Block 3 Rotation Note
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📘 Block 3 Rotation Schedule",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Mon / Wed / Fri: Python Programming (GATE & Placement support)\n" +
                               "• Tue / Thu: Internship work, updates & emails\n" +
                               "• Sat: Weekly Subject-wise Test review\n" +
                               "• Sun: Full Mock postmortems or Rest",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

data class DayInfo(
    val dayLabel: String,
    val dayNumber: String,
    val dateString: String,
    val completion: DayCompletionType,
    val isToday: Boolean
)

enum class DayCompletionType {
    NONE,
    PARTIAL,
    FULL
}

data class TimelineItem(
    val time: String,
    val title: String,
    val type: TimelineType,
    val details: String
)

enum class TimelineType {
    NEUTRAL,
    BLOCK1,
    BLOCK2,
    BLOCK3
}

@Composable
fun PerformanceChart(
    weeklyXp: List<Int>,
    dayLabels: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly XP Analytics",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    val maxVal = 50f
                    val points = weeklyXp.map { it.toFloat() }
                    
                    val stepX = width / (points.size - 1)
                    
                    // Draw horizontal grid lines
                    val gridLines = listOf(0f, 25f, 50f)
                    gridLines.forEach { g ->
                        val y = height - (g / maxVal) * height
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Build path
                    val path = Path()
                    val connectionPoints = mutableListOf<androidx.compose.ui.geometry.Offset>()
                    
                    points.forEachIndexed { i, p ->
                        val x = i * stepX
                        val y = height - (p / maxVal) * height
                        val offset = androidx.compose.ui.geometry.Offset(x, y)
                        connectionPoints.add(offset)
                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Draw line path
                    drawPath(
                        path = path,
                        color = Color(0xFF8B5CF6),
                        style = Stroke(width = 4f)
                    )

                    // Draw points
                    connectionPoints.forEachIndexed { i, offset ->
                        drawCircle(
                            color = Color(0xFF8B5CF6),
                            radius = 6f,
                            center = offset
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = offset
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // X-Axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}
