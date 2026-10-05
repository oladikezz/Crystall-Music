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
import com.crystall.music.ui.i18n.LocalAppLanguage
import com.crystall.music.ui.i18n.LocalStrings
import com.crystall.music.ui.theme.GreenSuccess
import com.crystall.music.ui.theme.TextPrimary
import com.crystall.music.ui.theme.TextSecondary
import com.crystall.music.ui.theme.YtRed
import com.crystall.music.ui.theme.YtSurfaceElevated

@Composable
fun PlayerOptionsSheet(
    sleepTimerRemainingMs: Long?,
    isSleepTimerEndOfTrack: Boolean,
    isEndlessRadioEnabled: Boolean,
    playbackSpeed: Float,
    isEconomyMode: Boolean = true,
    onSetSleepTimer: (Int) -> Unit,
    onSetSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleEndlessRadio: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleEconomyMode: () -> Unit = {},
    onOpenLanguagePicker: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val currentLang = LocalAppLanguage.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    spotColor = Color.Black.copy(alpha = 0.85f)
                )
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(YtSurfaceElevated)
                .border(
                    width = 1.dp,
                    color = Color(0x22FFFFFF),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x4DFFFFFF))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.optionsTitle,
                        color = Color.White,
                        fontSize = 18.sp,
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

                Spacer(modifier = Modifier.height(14.dp))

                // ---------------------------------------------------------
                // SECTION 0: Language Setting Pill Button
                // ---------------------------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                        .clickable { onOpenLanguagePicker() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = strings.optionsLanguageTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${currentLang.flag} ${currentLang.nativeName} (${currentLang.displayName})",
                                    color = YtRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ---------------------------------------------------------
                // SECTION 1: Экономия интернет-трафика (Эконом / Норм)
                // ---------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DataSaverOn,
                            contentDescription = null,
                            tint = if (isEconomyMode) GreenSuccess else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.optionsDataSaverTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Эконом режим Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isEconomyMode) GreenSuccess.copy(alpha = 0.18f) else Color(0x10FFFFFF)
                                )
                                .border(
                                    width = if (isEconomyMode) 1.5.dp else 0.5.dp,
                                    color = if (isEconomyMode) GreenSuccess else Color(0x25FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (!isEconomyMode) onToggleEconomyMode()
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = strings.optionsEcoName,
                                        color = if (isEconomyMode) GreenSuccess else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isEconomyMode) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = GreenSuccess,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strings.optionsEcoSize,
                                    color = if (isEconomyMode) Color.White else Color(0x99FFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = strings.optionsEcoSub,
                                    color = Color(0x88FFFFFF),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        // Норм режим Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (!isEconomyMode) Color(0x28FFFFFF) else Color(0x10FFFFFF)
                                )
                                .border(
                                    width = if (!isEconomyMode) 1.5.dp else 0.5.dp,
                                    color = if (!isEconomyMode) Color.White else Color(0x25FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isEconomyMode) onToggleEconomyMode()
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = strings.optionsNormName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!isEconomyMode) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strings.optionsNormSize,
                                    color = if (!isEconomyMode) Color.White else Color(0x99FFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = strings.optionsNormSub,
                                    color = Color(0x88FFFFFF),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ---------------------------------------------------------
                // SECTION 2: Таймер сна (Sleep Timer)
                // ---------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(14.dp)
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
                                text = strings.optionsSleepTimerTitle,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (sleepTimerRemainingMs != null || isSleepTimerEndOfTrack) {
                            val activeText = if (isSleepTimerEndOfTrack) {
                                strings.optionsSleepTimerEndTrack
                            } else {
                                val minutesLeft = (sleepTimerRemainingMs ?: 0L) / 60000
                                "⏱ ${minutesLeft}м"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = activeText,
                                    color = GreenSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = onCancelSleepTimer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.optionsSleepTimerReset,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val timerPresets = listOf(
                        10 to "10м",
                        15 to "15м",
                        30 to "30м",
                        45 to "45м",
                        60 to "60м"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((minutes, label) in timerPresets) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x18FFFFFF))
                                    .border(0.5.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp))
                                    .clickable { onSetSleepTimer(minutes) }
                                    .padding(vertical = 8.dp),
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSleepTimerEndOfTrack) Color(0x38FFFFFF) else Color(0x12FFFFFF)
                            )
                            .border(0.5.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { onSetSleepTimerEndOfTrack() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.optionsSleepTimerEndTrack,
                            color = if (isSleepTimerEndOfTrack) Color.White else Color(0xCCFFFFFF),
                            fontSize = 12.sp,
                            fontWeight = if (isSleepTimerEndOfTrack) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ---------------------------------------------------------
                // SECTION 3: Скорость воспроизведения (0.8x - 1.5x)
                // ---------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(14.dp)
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
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.optionsSpeedTitle,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "${playbackSpeed}x",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val speeds = listOf(0.8f, 1.0f, 1.25f, 1.5f)
                        for (spd in speeds) {
                            val isSelected = (playbackSpeed == spd)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0x38FFFFFF) else Color(0x18FFFFFF)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) Color.White else Color(0x30FFFFFF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSetPlaybackSpeed(spd) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (spd == 1.0f) "1.0x" else "${spd}x",
                                    color = if (isSelected) Color.White else Color(0xCCFFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
