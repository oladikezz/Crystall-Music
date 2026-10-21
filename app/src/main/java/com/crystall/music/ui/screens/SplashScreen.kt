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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystall.music.R
import com.crystall.music.ui.theme.LightBackground
import com.crystall.music.ui.theme.LightBorder
import com.crystall.music.ui.theme.LightSurfaceElevated
import com.crystall.music.ui.theme.LightTextPrimary
import com.crystall.music.ui.theme.LightTextSecondary
import com.crystall.music.ui.theme.YouTubeRed
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
        initialValue = 0.25f,
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
        initialValue = 0.2f,
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
                progress.value < 0.35f -> "Инициализация аудиосистемы..."
                progress.value < 0.75f -> "Синхронизация медиатеки..."
                progress.value < 0.95f -> "Загрузка рекомендаций..."
                else -> "Готово к воспроизведению..."
            }
        }
    }

    LaunchedEffect(Unit) {
        animationStarted = true
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
        )
        delay(250)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .clickable { onFinished() }, // Instant tap-to-skip
        contentAlignment = Alignment.Center
    ) {
        // Soft subtle ambient glow
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(logoScale * 1.15f)
                .background(
                    Brush.radialGradient(
                        listOf(
                            YouTubeRed.copy(alpha = 0.08f),
                            Color(0x06000000),
                            Color.Transparent
                        )
                    )
                )
        )

        // Concentric Soundwave Pulse Ripples (light modern rings)
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(pulse2)
                .alpha(pulse2Alpha)
                .clip(CircleShape)
                .border(1.5.dp, Color(0x1F000000), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(pulse1)
                .alpha(pulse1Alpha)
                .clip(CircleShape)
                .border(1.5.dp, YouTubeRed.copy(alpha = 0.25f), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Central Crystal Logo Card with clean light surface & subtle shadow
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(120.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0x14000000))
                    .clip(RoundedCornerShape(32.dp))
                    .background(LightSurfaceElevated)
                    .border(
                        width = 1.dp,
                        color = LightBorder,
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

                // Diagonal light shimmer beam
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.4f),
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
                color = LightTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp,
                modifier = Modifier.alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "M U S I C",
                color = LightTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LightBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    YouTubeRed,
                                    Color(0xFFFF5252)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic status text
            Text(
                text = statusMessage,
                color = LightTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
