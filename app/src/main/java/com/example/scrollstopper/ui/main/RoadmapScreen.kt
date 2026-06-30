package com.example.scrollstopper.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollstopper.data.ScheduleData
import com.example.scrollstopper.data.WeekPlan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun RoadmapScreen(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    onNavigateToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    var targetExamText by remember { mutableStateOf("") }
    var targetDateText by remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        // Default target date is 6 months from now
        mutableStateOf(sdf.format(java.util.Date(System.currentTimeMillis() + 180L * 24 * 60 * 60 * 1000)))
    }
    var isPlanning by remember { mutableStateOf(false) }
    var plannerStatusText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Helper functions for AI Planner
    fun queryGeminiAPI(apiKey: String, examName: String): String? {
        val prompt = "Create a structured weekly study schedule for the exam: '$examName'. " +
                "Provide the response strictly in JSON format. The JSON should be an array of objects where each object has exactly these fields: " +
                "\"weekNumber\" (integer starting from 1), \"phase\" (string, e.g. Phase 1: Foundations), \"subjects\" (string, e.g. Mathematics), " +
                "\"topic\" (string, e.g. Linear Algebra), \"block1Source\" (string containing a recommended YouTube channel/lecture title for theory), " +
                "\"block2Source\" (string containing a recommended online practice portal/website). " +
                "Provide between 8 to 12 weeks of detailed syllabus. Return ONLY the raw JSON array. Do not include markdown code block formatting (like ```json)."

        val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        var conn: java.net.HttpURLConnection? = null
        return try {
            val url = java.net.URL(urlStr)
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", prompt)
                    ))
                ))
            }

            val os = conn.outputStream
            os.write(body.toString().toByteArray())
            os.close()

            val responseCode = conn.responseCode
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val br = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                br.close()
                sb.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun parseGeminiResponse(rawResponse: String): List<WeekPlan>? {
        return try {
            val root = JSONObject(rawResponse)
            val candidates = root.getJSONArray("candidates")
            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = parts.getJSONObject(0).getString("text")

            val cleanText = text.replace("```json", "").replace("```", "").trim()
            val jsonArray = JSONArray(cleanText)

            val plans = mutableListOf<WeekPlan>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                plans.add(
                    WeekPlan(
                        weekNumber = obj.getInt("weekNumber"),
                        phase = obj.getString("phase"),
                        subjects = obj.getString("subjects"),
                        topic = obj.getString("topic"),
                        block1Source = obj.getString("block1Source"),
                        block2Source = obj.getString("block2Source")
                    )
                )
            }
            plans
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateSchedule(examName: String, examDateStr: String) {
        if (examName.isBlank()) return
        isPlanning = true
        plannerStatusText = "Analyzing exam requirements..."
        
        scope.launch(Dispatchers.IO) {
            try {
                val cleanName = examName.trim().lowercase()
                var resolvedPlans: List<WeekPlan>? = null
                var resolvedQuestName = examName
                var parsedDateMillis = System.currentTimeMillis() + (180L * 24 * 60 * 60 * 1000)
                
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                try {
                    if (examDateStr.isNotBlank()) {
                        parsedDateMillis = sdf.parse(examDateStr.trim())?.time ?: parsedDateMillis
                    }
                } catch (e: Exception) {
                    // ignore
                }

                if (cleanName.contains("rrb") && (cleanName.contains("ntpc") || cleanName.contains("non technical"))) {
                    resolvedQuestName = "RRB NTPC Quest"
                    resolvedPlans = listOf(
                        WeekPlan(1, "Phase 1: Foundations", "Mathematics", "Number System & Decimals", "WiFiStudy - Number System basics", "Exampur - NTPC Math Practice"),
                        WeekPlan(2, "Phase 1: Foundations", "Mathematics", "Fractions, LCM & HCF", "WiFiStudy - LCM & HCF math", "Exampur - Fractions practice"),
                        WeekPlan(3, "Phase 1: Foundations", "General Intelligence", "Analogies & Venn Diagrams", "Adda247 - Reasoning Analogies", "WiFiStudy - Venn Diagram practice"),
                        WeekPlan(4, "Phase 1: Foundations", "General Intelligence", "Coding-Decoding & Syllogism", "Adda247 - Reasoning Coding basics", "WiFiStudy - Syllogism practice"),
                        WeekPlan(5, "Phase 2: Core Prep", "General Awareness", "Indian History & Culture", "WifiStudy - GK History lectures", "Exampur - History PYQs"),
                        WeekPlan(6, "Phase 2: Core Prep", "General Awareness", "Geography & Environment", "Adda247 - GK Geography prep", "WiFiStudy - Geography MCQ practice"),
                        WeekPlan(7, "Phase 2: Core Prep", "General Awareness", "Indian Polity & Economy", "Exampur - GK Polity lectures", "Adda247 - Economy MCQ practice"),
                        WeekPlan(8, "Phase 2: Core Prep", "General Science", "Physics & Chemistry basics", "Exampur - Physics GK", "WiFiStudy - Chemistry GK practice"),
                        WeekPlan(9, "Phase 3: Strengthening", "General Science", "Life Sciences & Biology", "WiFiStudy - Biology basics", "Exampur - Science PYQs"),
                        WeekPlan(10, "Phase 3: Strengthening", "Full Review", "Current Affairs (Last 6 Months)", "WiFiStudy - Monthly Current Affairs", "Adda247 - Current Affairs MCQs"),
                        WeekPlan(11, "Phase 4: Mocks", "Practice", "Previous Year Question Papers", "Exampur - NTPC Mock Tests", "WiFiStudy - Math & Reasoning PYQs"),
                        WeekPlan(12, "Phase 4: Mocks", "Practice", "Full Syllabus Mock Test & Review", "Adda247 - Full NTPC Mock Exam", "Self-Review - Weakness postmortem")
                    )
                } else if (cleanName.contains("rrb") && (cleanName.contains("alp") || cleanName.contains("assistant loco"))) {
                    resolvedQuestName = "RRB ALP Quest"
                    resolvedPlans = listOf(
                        WeekPlan(1, "Phase 1: Mathematics", "Maths", "Number System, BODMAS & Fractions", "WiFiStudy - ALP Maths Basic", "Exampur - Math Practice"),
                        WeekPlan(2, "Phase 1: Mathematics", "Maths", "Ratio, Proportion & Percentage", "WiFiStudy - Ratio & Proportions", "Exampur - Percents practice"),
                        WeekPlan(3, "Phase 2: General Science", "Science", "Basic Physics & Mechanics", "Exampur - Physics for ALP", "WiFiStudy - Physics PYQs"),
                        WeekPlan(4, "Phase 2: General Science", "Science", "Basic Chemistry & Biology", "Exampur - Chemistry for ALP", "Adda247 - Biology basics"),
                        WeekPlan(5, "Phase 3: Reasoning", "Reasoning", "Analogies, Venn Diagrams & Series", "Adda247 - Reasoning Analogies", "WiFiStudy - Venn Diagram practice"),
                        WeekPlan(6, "Phase 3: Reasoning", "Reasoning", "Syllogism, Coding-Decoding & Puzzle", "Adda247 - Syllogism basics", "WiFiStudy - Coding practice"),
                        WeekPlan(7, "Phase 4: Core Trade", "Basic Science & Eng", "Engineering Drawing & Units", "Exampur - Basic Science & Eng Drawing", "WiFiStudy - Drawing MCQs"),
                        WeekPlan(8, "Phase 4: Core Trade", "Basic Science & Eng", "Work, Power, Energy & Speed", "Exampur - Work Power Energy physics", "WiFiStudy - Speed MCQs"),
                        WeekPlan(9, "Phase 4: Core Trade", "Basic Science & Eng", "Heat, Temp & Basic Electricity", "Exampur - Heat Electricity physics", "WiFiStudy - Electricity PYQs"),
                        WeekPlan(10, "Phase 5: Revision", "Mocks", "Daily Mini Mock Tests & GK", "WiFiStudy - General Awareness", "Adda247 - ALP Mini Mock"),
                        WeekPlan(11, "Phase 5: Revision", "Mocks", "Full Length Mock test Series", "Exampur - Full Mock test review", "Adda247 - Mock test postmortem"),
                        WeekPlan(12, "Phase 5: Revision", "Practice", "Rest & Final Formulas review", "WiFiStudy - Formula marathon", "Self-Review - Calm down and rest")
                    )
                } else if (cleanName.contains("upsc")) {
                    resolvedQuestName = "UPSC CSE Quest"
                    resolvedPlans = listOf(
                        WeekPlan(1, "Phase 1: Polity & History", "Polity", "Constitutional Framework & Rights", "Mrunal Patel - Polity basics", "ClearIAS - MCQ Practice"),
                        WeekPlan(2, "Phase 1: Polity & History", "Polity", "Executive, Legislature & Judiciary", "Mrunal Patel - Government bodies", "ClearIAS - Polity PYQs"),
                        WeekPlan(3, "Phase 1: Polity & History", "History", "Ancient & Medieval India", "StudyIQ - Ancient History", "ClearIAS - History PYQs"),
                        WeekPlan(4, "Phase 1: Polity & History", "History", "Modern History & Freedom Struggle", "StudyIQ - Modern History", "ClearIAS - Modern History PYQs"),
                        WeekPlan(5, "Phase 2: Geography & Econ", "Geography", "Physical Geography & Climatology", "StudyIQ - Physical Geography", "InsightsonIndia - Geography MCQs"),
                        WeekPlan(6, "Phase 2: Geography & Econ", "Geography", "Indian & World Resource Geography", "StudyIQ - Indian Geography", "InsightsonIndia - Resource MCQs"),
                        WeekPlan(7, "Phase 2: Geography & Econ", "Economics", "National Income, Banking & Inflation", "Mrunal Patel - Economics basics", "ClearIAS - Economics PYQs"),
                        WeekPlan(8, "Phase 2: Geography & Econ", "Economics", "Fiscal Policy, Budget & Schemes", "Mrunal Patel - Budget & Schemes", "InsightsonIndia - Economy MCQs"),
                        WeekPlan(9, "Phase 3: Science & Env", "Environment", "Ecology, Biodiversity & Climate Change", "StudyIQ - Ecology & Biodiversity", "InsightsonIndia - Environment PYQs"),
                        WeekPlan(10, "Phase 3: Science & Env", "Science & Tech", "Space, Defence, Biotech & IT", "StudyIQ - Tech & Science current updates", "ClearIAS - Science PYQs"),
                        WeekPlan(11, "Phase 4: CSAT & Current", "CSAT", "Comprehension & Basic Numeracy", "StudyIQ - CSAT preparation", "ClearIAS - CSAT Mock Tests"),
                        WeekPlan(12, "Phase 4: CSAT & Current", "Current Affairs", "International Relations & Schemes review", "StudyIQ - International Relations", "InsightsonIndia - Current Affairs Mock")
                    )
                } else if (cleanName.contains("gate") && cleanName.contains("cs")) {
                    resolvedQuestName = "GATE CSE Quest"
                    resolvedPlans = listOf(
                        WeekPlan(1, "Phase 1: Math & DS", "Mathematics", "Linear Algebra & Calculus", "Gate Smashers - Linear Algebra", "gateoverflow.in - Math PYQs"),
                        WeekPlan(2, "Phase 1: Math & DS", "Mathematics", "Probability & Discrete Math basics", "Gate Smashers - Discrete Mathematics", "gateoverflow.in - Probability PYQs"),
                        WeekPlan(3, "Phase 1: Math & DS", "Data Structures", "Arrays, Stacks, Queues & Linked Lists", "Amit Khurana - Data Structures basics", "gateoverflow.in - DS PYQs"),
                        WeekPlan(4, "Phase 1: Math & DS", "Algorithms", "Searching, Sorting & Greedy Algorithms", "Amit Khurana - Algorithms sorting", "gateoverflow.in - Algo PYQs"),
                        WeekPlan(5, "Phase 2: Core Heavy", "Algorithms", "Dynamic Programming & Graph Traversal", "Amit Khurana - DP and Graph algos", "gateoverflow.in - Graph PYQs"),
                        WeekPlan(6, "Phase 2: Core Heavy", "Digital Logic", "Boolean Algebra & Combinational Circuits", "Gate Smashers - Digital Electronics", "gateoverflow.in - Digital Logic PYQs"),
                        WeekPlan(7, "Phase 2: Core Heavy", "COA", "Machine Instructions, Addressing Modes & CPU", "Gate Smashers - Computer Architecture", "gateoverflow.in - COA PYQs"),
                        WeekPlan(8, "Phase 2: Core Heavy", "Operating Systems", "Process Management & CPU Scheduling", "Gate Smashers - OS scheduling", "gateoverflow.in - OS Scheduling PYQs"),
                        WeekPlan(9, "Phase 3: TOC & Compiler", "Operating Systems", "Deadlocks & Memory Management", "Gate Smashers - Deadlocks & Memory", "gateoverflow.in - Memory PYQs"),
                        WeekPlan(10, "Phase 3: TOC & Compiler", "TOC", "Regular Languages & Finite Automata", "Amit Khurana - TOC Automata", "gateoverflow.in - TOC PYQs"),
                        WeekPlan(11, "Phase 3: TOC & Compiler", "Compiler Design", "Lexical Analysis & Parsing", "Gate Smashers - Compiler Design", "gateoverflow.in - Compiler PYQs"),
                        WeekPlan(12, "Phase 4: Networks & DB", "DBMS", "ER Model, Relational Algebra & Normalization", "Gate Smashers - DBMS Normalization", "gateoverflow.in - DBMS PYQs"),
                        WeekPlan(13, "Phase 4: Networks & DB", "Computer Networks", "IP Routing, TCP/UDP & DNS", "Gate Smashers - Computer Networks", "gateoverflow.in - Networks PYQs")
                    )
                }

                if (resolvedPlans == null && state.geminiApiKey.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        plannerStatusText = "Connecting to Gemini AI..."
                    }
                    val response = queryGeminiAPI(state.geminiApiKey, examName)
                    if (response != null) {
                        resolvedPlans = parseGeminiResponse(response)
                    }
                }

                withContext(Dispatchers.Main) {
                    if (resolvedPlans != null) {
                        viewModel.updateQuestConfig(resolvedQuestName, parsedDateMillis)
                        viewModel.updateCustomSyllabus(resolvedPlans)
                        plannerStatusText = "Generated and loaded successfully!"
                        targetExamText = ""
                    } else {
                        plannerStatusText = "Could not generate. Enter your Gemini API Key in the Settings tab (Alerts) to query custom exams, or choose a preset (e.g. RRB NTPC, UPSC)!"
                    }
                    isPlanning = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    plannerStatusText = "Error: ${e.message}"
                    isPlanning = false
                }
            }
        }
    }

    // 10 Core Study Techniques Reference Data
    val studyTechniques = remember {
        listOf(
            StudyTechnique("Active Recall", "Instead of passively reading, quiz yourself on concepts and formulate active questions after each block."),
            StudyTechnique("Spaced Repetition", "Re-visit formulas and hard concepts at expanding intervals (1 day, 3 days, 7 days, 30 days) to lock them in long-term memory."),
            StudyTechnique("Pomodoro Focus Blocks", "Use deep work chunks (e.g. 50 mins study, 10 mins break) to maintain peak focus during Blocks 1 and 2."),
            StudyTechnique("Error Logs", "Record every question you get wrong in mocks or practice portals. Document the concept, why you failed, and how to solve it next time."),
            StudyTechnique("Self-built Formula Sheets", "Create your own summary pages instead of downloading pre-made sheets. Writing helps synthesize relationships between variables."),
            StudyTechnique("Teach-It-Back Method", "Explain complex electrical machine equations or power systems networks to an imaginary peer. If you can't teach it, you don't fully understand it."),
            StudyTechnique("Interleaving Topics", "Mix up math problems with network theory practice. Alternating concepts trains your brain to select the right tool in the real GATE exam."),
            StudyTechnique("Mock Postmortems", "Spend twice as much time analyzing a mock test as you did taking it. Check your errors, guess accuracy, and time management efficiency."),
            StudyTechnique("Distraction Shield", "Enable the Scroll Shield accessibility filter during your morning and evening study blocks. Stop doomscrolling on YouTube Shorts."),
            StudyTechnique("Sleep Consistency", "Synchronize your biological clock. Sleeping at the same time ensures peak cognitive performance during early morning blocks.")
        )
    }

    val totalWeeks = if (state.customSyllabus.isNotEmpty()) state.customSyllabus else ScheduleData.weeks

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
                    text = "${totalWeeks.size}-WEEK STUDY ROADMAP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA78BFA),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = state.questName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // AI Quest Planner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🤖 AI Quest Planner & Roadmap Generator",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Enter any exam and target date to auto-generate a structured roadmap with recommended resources.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = targetExamText,
                        onValueChange = { targetExamText = it },
                        placeholder = { Text("Exam Name (e.g. RRB NTPC, UPSC, GATE CSE)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = targetDateText,
                        onValueChange = { targetDateText = it },
                        placeholder = { Text("Target Exam Date (Format: YYYY-MM-DD)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8B5CF6)
                        )
                    )

                    if (plannerStatusText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = plannerStatusText,
                            fontSize = 11.sp,
                            color = Color(0xFFA78BFA),
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { generateSchedule(targetExamText, targetDateText) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPlanning && targetExamText.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        if (isPlanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("AI Generate Syllabus & Resources", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Weeks Progress Grid Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Roadmap Grid (Tap to Select Current Focus)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Instead of a true grid (which is hard to layout inside LazyColumn), we render rows of weeks
                    val chunkedWeeks = totalWeeks.chunked(4)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunkedWeeks.forEach { rowWeeks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowWeeks.forEach { week ->
                                    val isCompleted = state.completedWeeks.contains(week.weekNumber)
                                    val isActive = state.currentWeek == week.weekNumber
                                    
                                    val phaseColor = when {
                                        week.weekNumber <= totalWeeks.size / 4 -> Color(0xFF2563EB)
                                        week.weekNumber <= totalWeeks.size / 2 -> Color(0xFFD97706)
                                        week.weekNumber <= (totalWeeks.size * 3) / 4 -> Color(0xFF059669)
                                        else -> Color(0xFF7C3AED)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isCompleted) Color(0xFF065F46) else phaseColor.copy(alpha = 0.15f)
                                            )
                                            .border(
                                                width = if (isActive) 2.dp else 1.dp,
                                                color = if (isActive) Color.White else phaseColor.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.selectWeek(week.weekNumber)
                                                onNavigateToToday()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "W${week.weekNumber}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isCompleted) Color.Green else Color.White
                                            )
                                            if (isCompleted) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Green,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = when {
                                                        week.weekNumber <= totalWeeks.size / 4 -> "FDN"
                                                        week.weekNumber <= totalWeeks.size / 2 -> "CORE"
                                                        week.weekNumber <= (totalWeeks.size * 3) / 4 -> "STR"
                                                        else -> "REV"
                                                    },
                                                    fontSize = 8.sp,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                // Pad trailing slots in incomplete row
                                if (rowWeeks.size < 4) {
                                    for (j in 0 until (4 - rowWeeks.size)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend description
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendBullet("Foundations (W1-${totalWeeks.size/4})", Color(0xFF2563EB))
                            LegendBullet("Core Heavy (W${totalWeeks.size/4 + 1}-${totalWeeks.size/2})", Color(0xFFD97706))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendBullet("Strengthening (W${totalWeeks.size/2 + 1}-${(totalWeeks.size*3)/4})", Color(0xFF059669))
                            LegendBullet("Revision + Mocks (W${(totalWeeks.size*3)/4 + 1}-${totalWeeks.size})", Color(0xFF7C3AED))
                        }
                    }
                }
            }
        }

        // Techniques header
        item {
            Text(
                text = "10 Core Study Techniques",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Techniques list
        items(studyTechniques) { technique ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = technique.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = technique.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LegendBullet(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

data class StudyTechnique(
    val title: String,
    val description: String
)
