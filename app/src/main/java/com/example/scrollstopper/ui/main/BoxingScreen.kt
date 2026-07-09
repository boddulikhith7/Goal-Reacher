package com.example.scrollstopper.ui.main

import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random

enum class TimerState {
    IDLE, PREPARATION, WORK, REST, FINISHED
}

@Composable
fun BoxingScreen(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // TextToSpeech initialization
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        ttsEngine.language = Locale.US
        tts = ttsEngine

        onDispose {
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
    }

    // Workout parameters
    var totalRounds by remember { mutableIntStateOf(3) }
    var roundMinutes by remember { mutableIntStateOf(3) }
    var restSeconds by remember { mutableIntStateOf(60) }

    // Run-time states
    var timerState by remember { mutableStateOf(TimerState.IDLE) }
    var currentRound by remember { mutableIntStateOf(1) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var totalDurationForProgress by remember { mutableIntStateOf(1) }

    // List of combinations
    val combinations = remember {
        listOf(
            "1, 2!",
            "1, 2, 3!",
            "1, 2, slip, 2!",
            "Double jab, cross!",
            "Jab, body cross!",
            "Cross, hook, cross!",
            "1, 2, roll, hook!",
            "Hands up, move your head!",
            "Double jab, move, 2!",
            "Power hook, cross, hook!",
            "Hook, cross, hook!",
            "Jab, slip, jab, cross!",
            "1, 2, 5, 2!",
            "Uppercut, hook, cross!"
        )
    }

    // Interval Timer Loop
    LaunchedEffect(timerState, currentRound) {
        if (timerState == TimerState.IDLE || timerState == TimerState.FINISHED) return@LaunchedEffect

        // Setup duration
        val duration = when (timerState) {
            TimerState.PREPARATION -> 10 // 10 seconds prep
            TimerState.WORK -> roundMinutes * 60
            TimerState.REST -> restSeconds
            else -> 0
        }
        secondsLeft = duration
        totalDurationForProgress = duration

        // Initial speech announcements
        if (ttsReady) {
            when (timerState) {
                TimerState.PREPARATION -> tts?.speak("Get ready to fight. Round 1 starts in 10 seconds.", TextToSpeech.QUEUE_FLUSH, null, null)
                TimerState.WORK -> tts?.speak("Round $currentRound. Fight!", TextToSpeech.QUEUE_FLUSH, null, null)
                TimerState.REST -> {
                    if (currentRound < totalRounds) {
                        tts?.speak("Round $currentRound complete. Rest.", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
                else -> {}
            }
        }

        var voiceCallCooldown = 4 // Speak a combo every 5-6 seconds in WORK state
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--

            // Cues during WORK round
            if (timerState == TimerState.WORK && secondsLeft > 0) {
                voiceCallCooldown--
                if (voiceCallCooldown <= 0) {
                    voiceCallCooldown = Random.nextInt(4, 7) // Speak every 4 to 6 seconds
                    if (ttsReady) {
                        val combo = combinations[Random.nextInt(combinations.size)]
                        tts?.speak(combo, TextToSpeech.QUEUE_ADD, null, null)
                    }
                }
            }

            // Warning bell at 10 seconds left
            if (secondsLeft == 10 && ttsReady) {
                if (timerState == TimerState.WORK) {
                    tts?.speak("Ten seconds left. Finish strong!", TextToSpeech.QUEUE_ADD, null, null)
                } else if (timerState == TimerState.REST) {
                    tts?.speak("Round starts in 10 seconds.", TextToSpeech.QUEUE_ADD, null, null)
                }
            }
        }

        // State transitions
        when (timerState) {
            TimerState.PREPARATION -> {
                timerState = TimerState.WORK
            }
            TimerState.WORK -> {
                if (currentRound < totalRounds) {
                    timerState = TimerState.REST
                } else {
                    timerState = TimerState.FINISHED
                    // Reward XP
                    viewModel.addXp(50)
                    if (ttsReady) {
                        tts?.speak("Workout complete. Outstanding job. You earned 50 experience points.", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }
            TimerState.REST -> {
                currentRound++
                timerState = TimerState.WORK
            }
            else -> {}
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = when (timerState) {
            TimerState.PREPARATION -> Color(0xFF8B5CF6) // Soothing violet
            TimerState.WORK -> Color(0xFF10B981) // Emerald green
            TimerState.REST -> Color(0xFFF59E0B) // Amber rest
            TimerState.FINISHED -> Color(0xFF3B82F6) // Success blue
            else -> Color(0xFF8B5CF6)
        },
        label = "color"
    )

    val progress = if (totalDurationForProgress > 0) {
        (secondsLeft.toFloat() / totalDurationForProgress).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HEAVY BAG ARENA",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFA78BFA),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Boxing Workouts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "Train physically to earn XP and protect your study goals",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (timerState == TimerState.IDLE) {
            // Setup Screen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Customize Your Fight",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    // Round select
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Rounds: $totalRounds", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = totalRounds.toFloat(),
                            onValueChange = { totalRounds = it.toInt() },
                            valueRange = 1f..12f,
                            steps = 10,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF8B5CF6), activeTrackColor = Color(0xFF8B5CF6))
                        )
                    }

                    // Round minutes
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Round Length: $roundMinutes mins", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = roundMinutes.toFloat(),
                            onValueChange = { roundMinutes = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF8B5CF6), activeTrackColor = Color(0xFF8B5CF6))
                        )
                    }

                    // Rest select
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Rest Duration: $restSeconds secs", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = restSeconds.toFloat(),
                            onValueChange = { restSeconds = it.toInt() },
                            valueRange = 30f..120f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF8B5CF6), activeTrackColor = Color(0xFF8B5CF6))
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Start Button
                    Button(
                        onClick = {
                            currentRound = 1
                            timerState = TimerState.PREPARATION
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text(text = "START WORKOUT (+50 XP)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        } else {
            // Running Timer Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (timerState) {
                            TimerState.PREPARATION -> "PREPARATION"
                            TimerState.WORK -> "ROUND $currentRound"
                            TimerState.REST -> "REST INTERVAL"
                            TimerState.FINISHED -> "WORKOUT COMPLETE!"
                            else -> ""
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = animatedColor,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(240.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White.copy(alpha = 0.05f),
                            strokeWidth = 14.dp
                        )

                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = animatedColor,
                            strokeWidth = 14.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (timerState == TimerState.FINISHED) {
                                Text(text = "🎉", fontSize = 48.sp)
                                Text(
                                    text = "+50 XP Earned",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            } else {
                                val mins = secondsLeft / 60
                                val secs = secondsLeft % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = when (timerState) {
                                        TimerState.WORK -> "FIGHT!"
                                        TimerState.REST -> "BREATHE"
                                        TimerState.PREPARATION -> "GET READY"
                                        else -> ""
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = animatedColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (timerState != TimerState.FINISHED) {
                        Text(
                            text = "Total Rounds: $currentRound / $totalRounds",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Button(
                onClick = {
                    timerState = TimerState.IDLE
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (timerState == TimerState.FINISHED) "Back to Setup" else "STOP WORKOUT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
