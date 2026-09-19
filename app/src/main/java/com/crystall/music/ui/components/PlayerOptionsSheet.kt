package com.crystall.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystall.music.ui.theme.GreenSuccess
import com.crystall.music.ui.theme.TextPrimary
import com.crystall.music.ui.theme.TextSecondary

@Composable
fun PlayerOptionsSheet(
    sleepTimerRemainingMs: Long?,
    isSleepTimerEndOfTrack: Boolean,
    isEndlessRadioEnabled: Boolean,
    playbackSpeed: Float,
    onSetSleepTimer: (Int) -> Unit,
    onSetSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleEndlessRadio: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    spotColor = Color.Black.copy(alpha = 0.8f)
                )
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xF51A1B29),
                            Color(0xF811121C),
                            Color(0xFF090A10)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0x4DFFFFFF), Color(0x10FFFFFF))
                    ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0x4DFFFFFF))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Опции воспроизведения",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: Sleep Timer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bedtime,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Таймер сна",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (sleepTimerRemainingMs != null || isSleepTimerEndOfTrack) {
                            val statusText = if (isSleepTimerEndOfTrack) {
                                "До конца трека"
                            } else {
                                val totalSec = (sleepTimerRemainingMs ?: 0L) / 1000
                                val min = totalSec / 60
                                val sec = totalSec % 60
                                String.format("%d:%02d", min, sec)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    color = GreenSuccess,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x28FFFFFF))
                                        .clickable { onCancelSleepTimer() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Сброс",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Timer Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timerPresets = listOf(
                            Pair(15, "15 мин"),
                            Pair(30, "30 мин"),
                            Pair(45, "45 мин"),
                            Pair(60, "60 мин")
                        )

                        for ((minutes, label) in timerPresets) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x18FFFFFF))
                                    .border(0.5.dp, Color(0x30FFFFFF), RoundedCornerShape(14.dp))
                                    .clickable { onSetSleepTimer(minutes) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSleepTimerEndOfTrack) Color(0x38FFFFFF) else Color(0x12FFFFFF)
                            )
                            .border(0.5.dp, Color(0x28FFFFFF), RoundedCornerShape(14.dp))
                            .clickable { onSetSleepTimerEndOfTrack() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Остановить в конце текущего трека",
                            color = if (isSleepTimerEndOfTrack) Color.White else Color(0xCCFFFFFF),
                            fontSize = 13.sp,
                            fontWeight = if (isSleepTimerEndOfTrack) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Playback Speed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Скорость воспроизведения",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val speeds = listOf(
                            Pair(0.8f, "0.8x"),
                            Pair(1.0f, "1.0x"),
                            Pair(1.25f, "1.25x"),
                            Pair(1.5f, "1.5x")
                        )

                        for ((spd, spdLabel) in speeds) {
                            val isSelected = kotlin.math.abs(playbackSpeed - spd) < 0.05f
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) Color.White else Color(0x14FFFFFF),
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "speed_bg_$spd"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bg)
                                    .border(
                                        width = if (isSelected) 0.dp else 0.5.dp,
                                        color = Color(0x30FFFFFF),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSetPlaybackSpeed(spd) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = spdLabel,
                                    color = if (isSelected) Color(0xFF101014) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Endless Radio / Smart Autoplay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(20.dp))
                        .clickable { onToggleEndlessRadio() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isEndlessRadioEnabled) Color(0x30FFFFFF) else Color(0x14FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = null,
                                tint = if (isEndlessRadioEnabled) Color.White else Color(0x88FFFFFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Бесконечный поток",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Автоподбор музыки по окончании очереди",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isEndlessRadioEnabled,
                        onCheckedChange = { onToggleEndlessRadio() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0x60FFFFFF),
                            uncheckedThumbColor = Color(0x88FFFFFF),
                            uncheckedTrackColor = Color(0x20FFFFFF),
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
