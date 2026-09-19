package com.crystall.music.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystall.music.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    var animationStarted by remember { mutableStateOf(false) }

    // Progress percentage
    val progress = remember { Animatable(0f) }

    // Shimmer sweep across crystal
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )

    // Pulse ripples
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_1"
    )
    val pulse1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_1_alpha"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_2"
    )
    val pulse2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_2_alpha"
    )

    // Main logo scale and alpha
    val logoScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(700, easing = EaseOutCubic),
        label = "logo_alpha"
    )

    // Dynamic loading status message
    val statusMessage by remember {
        derivedStateOf {
            when {
                progress.value < 0.35f -> "Инициализация звукового ядра..."
                progress.value < 0.75f -> "Синхронизация медиатеки..."
                progress.value < 0.95f -> "Калибровка Liquid Glass..."
                else -> "Погружение в музыку..."
            }
        }
    }

    LaunchedEffect(Unit) {
        animationStarted = true
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
        )
        delay(350)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080C))
            .clickable { onFinished() }, // Instant tap-to-skip
        contentAlignment = Alignment.Center
    ) {
        // Deep ambient atmospheric glow
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(logoScale * 1.15f)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFF6B00).copy(alpha = 0.28f),
                            Color(0xFF8A2BE2).copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Concentric Soundwave Pulse Ripples
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(pulse2)
                .alpha(pulse2Alpha)
                .clip(CircleShape)
                .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(pulse1)
                .alpha(pulse1Alpha)
                .clip(CircleShape)
                .border(1.5.dp, Color(0x88FFFFFF), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Central Crystal Logo Badge with Liquid Glass specularity
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x28FFFFFF),
                                Color(0x0AFFFFFF)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x99FFFFFF),
                                Color(0x22FFFFFF)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_crystall_logo),
                    contentDescription = "Crystall Music",
                    modifier = Modifier.fillMaxSize()
                )

                // Diagonal caustic light beam shimmer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                start = Offset(shimmerOffset, 0f),
                                end = Offset(shimmerOffset + 140f, 140f)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Typography: CRYSTALL MUSIC
            Text(
                text = "C R Y S T A L L",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp,
                modifier = Modifier.alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "M U S I C",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Liquid Glass Progress Capsule
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x18FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFF7A00),
                                    Color(0xFFFFFFFF)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic status text
            Text(
                text = statusMessage,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
