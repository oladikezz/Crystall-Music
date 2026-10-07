package com.crystall.music.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystall.music.ui.i18n.LocalAppLanguage
import com.crystall.music.ui.i18n.LocalStrings
import com.crystall.music.ui.theme.*

/**
 * Authentic YouTube Music & Spotify style dedicated Settings Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isEconomyMode: Boolean,
    isEndlessRadioEnabled: Boolean,
    playbackSpeed: Float,
    sleepTimerRemainingMs: Long?,
    isSleepTimerEndOfTrack: Boolean,
    downloadedTracksCount: Int,
    favoriteArtistsCount: Int,
    onBackClick: () -> Unit,
    onToggleEconomyMode: () -> Unit,
    onToggleEndlessRadio: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onSetSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onOpenTastePicker: () -> Unit,
    onClearCache: () -> Unit
) {
    val strings = LocalStrings.current
    val currentLang = LocalAppLanguage.current
    val context = LocalContext.current
    var isCacheClearedMessageVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YtBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.settingsTitle,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // -------------------------------------------------------------
            // 1. LANGUAGE & DISPLAY
            // -------------------------------------------------------------
            item {
                SettingsSectionTitle(title = strings.settingsSectionGeneral)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.Translate,
                        title = strings.settingsAppLanguage,
                        subtitle = "${currentLang.flag} ${currentLang.nativeName} (${currentLang.code.uppercase()})",
                        onClick = onOpenLanguagePicker,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = YtTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            // -------------------------------------------------------------
            // 2. AUDIO & DATA SAVING
            // -------------------------------------------------------------
            item {
                SettingsSectionTitle(title = strings.settingsSectionAudio)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsCard {
                    Column {
                        SettingsRow(
                            icon = Icons.Outlined.Speed,
                            title = strings.settingsDataSaver,
                            subtitle = strings.settingsDataSaverDesc,
                            trailingContent = {
                                Switch(
                                    checked = isEconomyMode,
                                    onCheckedChange = { onToggleEconomyMode() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF30D158),
                                        uncheckedThumbColor = YtTextSecondary,
                                        uncheckedTrackColor = Color(0x33FFFFFF)
                                    )
                                )
                            }
                        )

                        HorizontalDivider(
                            color = Color(0x14FFFFFF),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEconomyMode) "Качество звука: Эконом (128 kbps)" else "Качество звука: Высокое (320 kbps)",
                                color = YtTextSecondary,
                                fontSize = 12.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isEconomyMode) Color(0x2830D158) else Color(0x28FFFFFF))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isEconomyMode) "ECO" else "HQ",
                                    color = if (isEconomyMode) Color(0xFF30D158) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. PLAYBACK
            // -------------------------------------------------------------
            item {
                SettingsSectionTitle(title = strings.settingsSectionPlayback)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsCard {
                    Column {
                        // Endless Radio / Autoplay
                        SettingsRow(
                            icon = Icons.Outlined.AllInclusive,
                            title = strings.settingsEndlessRadio,
                            subtitle = strings.settingsEndlessRadioDesc,
                            trailingContent = {
                                Switch(
                                    checked = isEndlessRadioEnabled,
                                    onCheckedChange = { onToggleEndlessRadio() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF30D158),
                                        uncheckedThumbColor = YtTextSecondary,
                                        uncheckedTrackColor = Color(0x33FFFFFF)
                                    )
                                )
                            }
                        )

                        HorizontalDivider(
                            color = Color(0x14FFFFFF),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Playback Speed
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.settingsSpeed,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = YtTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val speeds = listOf(0.8f, 1.0f, 1.25f, 1.5f)
                                speeds.forEach { speed ->
                                    val isSelected = playbackSpeed == speed
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color.White else Color(0x18FFFFFF))
                                            .clickable { onSetPlaybackSpeed(speed) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${speed}x",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color(0x14FFFFFF),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Sleep Timer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.settingsSleepTimer,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                val timerStatus = when {
                                    isSleepTimerEndOfTrack -> strings.optionsSleepTimerEndTrack
                                    sleepTimerRemainingMs != null -> {
                                        val totalSec = sleepTimerRemainingMs / 1000
                                        String.format("%02d:%02d", totalSec / 60, totalSec % 60)
                                    }
                                    else -> strings.settingsSleepTimerOff
                                }
                                Text(
                                    text = timerStatus,
                                    color = if (sleepTimerRemainingMs != null || isSleepTimerEndOfTrack) Color(0xFF30D158) else YtTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val options = listOf(
                                    0 to strings.settingsSleepTimerOff,
                                    15 to "15м",
                                    30 to "30м",
                                    60 to "60м"
                                )
                                options.forEach { (minutes, label) ->
                                    val isSelected = if (minutes == 0) (sleepTimerRemainingMs == null && !isSleepTimerEndOfTrack) else false
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color.White else Color(0x18FFFFFF))
                                            .clickable {
                                                if (minutes == 0) onCancelSleepTimer() else onSetSleepTimer(minutes)
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. MUSIC PREFERENCES (TASTE)
            // -------------------------------------------------------------
            item {
                SettingsSectionTitle(title = strings.settingsSectionTaste)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = strings.settingsTasteArtists,
                        subtitle = if (favoriteArtistsCount > 0) "Выбрано артистов: $favoriteArtistsCount" else strings.settingsTasteArtistsDesc,
                        onClick = onOpenTastePicker,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = YtTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            // -------------------------------------------------------------
            // 5. STORAGE & CACHE
            // -------------------------------------------------------------
            item {
                SettingsSectionTitle(title = strings.settingsSectionStorage)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsCard {
                    Column {
                        SettingsRow(
                            icon = Icons.Outlined.FileDownload,
                            title = strings.settingsDownloadedTracks,
                            subtitle = "$downloadedTracksCount треков в памяти устройства"
                        )

                        HorizontalDivider(
                            color = Color(0x14FFFFFF),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        SettingsRow(
                            icon = Icons.Outlined.CleaningServices,
                            title = strings.settingsClearCache,
                            subtitle = strings.settingsClearCacheDesc,
                            onClick = {
                                onClearCache()
                                isCacheClearedMessageVisible = true
                                Toast.makeText(context, strings.settingsCacheCleared, Toast.LENGTH_SHORT).show()
                            },
                            trailingContent = {
                                TextButton(
                                    onClick = {
                                        onClearCache()
                                        isCacheClearedMessageVisible = true
                                        Toast.makeText(context, strings.settingsCacheCleared, Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = "Очистить",
                                        color = Color(0xFF30D158),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        )

                        AnimatedVisibility(visible = isCacheClearedMessageVisible) {
                            Text(
                                text = strings.settingsCacheCleared,
                                color = Color(0xFF30D158),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 6. ABOUT
            // -------------------------------------------------------------
            item {
                SettingsSectionTitle(title = strings.settingsSectionAbout)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Crystall Music",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${strings.settingsVersion} • YouTube Music & SoundCloud Engine",
                            color = YtTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.settingsAboutDesc,
                            color = YtTextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = YtTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = YtSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x18FFFFFF))
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x18FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = YtTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}
