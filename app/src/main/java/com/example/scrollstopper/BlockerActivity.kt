package com.example.scrollstopper

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrollstopper.data.PreferenceManager
import com.example.scrollstopper.data.QuotesData
import com.example.scrollstopper.theme.ScrollStopperTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class BlockerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefManager = PreferenceManager(applicationContext)

        setContent {
            ScrollStopperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BlockerScreen(
                        prefManager = prefManager,
                        onClose = {
                            // Redirect to home screen
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BlockerScreen(
    prefManager: PreferenceManager,
    onClose: () -> Unit
) {
    // Intercept back button to block dismissal
    BackHandler(enabled = true) {
        // Do nothing (block back action)
    }

    var timeRemaining by remember {
        val remaining = (prefManager.blockUntilTimestamp - System.currentTimeMillis()).coerceAtLeast(0L)
        mutableLongStateOf(remaining)
    }

    val totalDuration = remember {
        prefManager.pauseDurationSeconds * 1000L
    }

    // Select a random quote on open
    val randomQuote = remember {
        QuotesData.quotes[Random.nextInt(QuotesData.quotes.size)]
    }

    LaunchedEffect(key1 = true) {
        while (timeRemaining > 0) {
            delay(200)
            val remaining = (prefManager.blockUntilTimestamp - System.currentTimeMillis()).coerceAtLeast(0L)
            timeRemaining = remaining
        }
    }

    val progress = if (totalDuration > 0) {
        (timeRemaining.toFloat() / totalDuration).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F0C1B), // Dark slate
            Color(0xFF201635)  // Deep indigo purple
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "MINDFUL PAUSE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA78BFA), // Pastel purple
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Doomscrolling Paused",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Timer & Progress Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White.copy(alpha = 0.08f),
                    strokeWidth = 12.dp
                )

                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF8B5CF6), // Soothing violet
                    strokeWidth = 12.dp
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val secondsLeft = (timeRemaining + 999) / 1000
                    Text(
                        text = String.format("%02d", secondsLeft),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "seconds left",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Quotes Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${randomQuote.text}\"",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "- ${randomQuote.author}",
                        fontSize = 13.sp,
                        color = Color(0xFFA78BFA),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Bottom CTA
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timeRemaining <= 0) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.15f),
                    contentColor = if (timeRemaining <= 0) Color.White else Color.White.copy(alpha = 0.5f)
                ),
                enabled = true // Allow exit but it redirects to home screen
            ) {
                Text(
                    text = if (timeRemaining <= 0) "Back to Focus" else "Exit YouTube",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
