package com.example.grindsphere

import android.graphics.RadialGradient
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0A041A) // Deep space-like purple
            ) {
                // Track which screen we are on
                var currentScreen by remember { mutableStateOf("splash") }

                when (currentScreen) {
                    "splash" -> {
                        GrindSphereSplashScreen {
                            currentScreen = "welcome"
                        }
                    }
                    "welcome" -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Welcome to GrindSphere!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6A1B9A)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { currentScreen = "login" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                            ) {
                                Text("Login", color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { currentScreen = "signup" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                            ) {
                                Text("Sign Up", color = Color.White)
                            }
                        }
                    }

                    "login" -> GrindSphereLogin(
                        isPreview = false,
                        onNavigateToSignup = { currentScreen = "signup" }
                    )

                    "signup" -> ExperimentSignupScreen(
                        navController = null,
                        isPreview = false
                    )
                }
            }
        }
    }
}

@Composable
fun GrindSphereSplashScreen(onAnimationComplete: () -> Unit) {
    var animationPlayed by remember { mutableStateOf(false) }
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1.2f) }
    val rotation = remember { Animatable(0f) }

    // Particle effects state
    val particles = remember { mutableStateListOf<Particle>() }

    LaunchedEffect(Unit) {
        if (!animationPlayed) {
            animationPlayed = true

            // Create particles
            repeat(30) {
                particles.add(
                    Particle(
                        x = (0..100).random() / 100f,
                        y = (0..100).random() / 100f,
                        size = (5..20).random().toFloat(),
                        speed = (1..5).random().toFloat()
                    )
                )
            }

            // Start animations
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
                )
            }

            launch {
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                )
            }

            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
            )

            // Wait a moment at full opacity
            delay(1500)

            // Fade out
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, easing = LinearEasing)
            )

            onAnimationComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A041A)), // Deep purple-black background
        contentAlignment = Alignment.Center
    ) {
        // Draw starry background
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw stars
            for (i in 0 until 100) {
                val x = (0..size.width.toInt()).random().toFloat()
                val y = (0..size.height.toInt()).random().toFloat()
                val radius = (1..3).random().toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    center = Offset(x, y),
                    radius = radius
                )
            }

            // Draw particles
            for (particle in particles) {
                drawCircle(
                    color = Color(0xFF9C27B0).copy(alpha = 0.6f),
                    center = Offset(
                        particle.x * size.width,
                        particle.y * size.height
                    ),
                    radius = particle.size
                )
            }
        }

        // Draw rotating sphere in background
        Canvas(modifier = Modifier.size(300.dp)) {
            rotate(rotation.value) {
                // Create a proper radial gradient
                val gradient = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6A1B9A).copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width / 2
                )

                drawCircle(
                    brush = gradient,
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width / 2
                )
            }
        }

        // App name with gradient
        Text(
            text = "GrindSphere",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value)
        )

        // Subtitle text
        Text(
            text = "Where Student Hustles Shine",
            fontSize = 16.sp,
            color = Color(0xFFBB86FC), // Light purple
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .alpha(alpha.value)
        )
    }
}


data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float
)

@Preview(showBackground = true, apiLevel = 35)
@Composable
fun SplashScreenPreview() {
    GrindSphereSplashScreen(onAnimationComplete = {})
}
