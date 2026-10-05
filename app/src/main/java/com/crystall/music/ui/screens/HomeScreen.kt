package com.crystall.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.crystall.music.R
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.DownloadProgress
import com.crystall.music.engine.YouTubeEngine
import com.crystall.music.ui.components.EqualizerVisualizer
import com.crystall.music.ui.components.TrackItem
import com.crystall.music.ui.i18n.LocalAppLanguage
import com.crystall.music.ui.i18n.LocalStrings
import com.crystall.music.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(
    trendingTracks: List<Track>,
    recentTracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    downloadStates: Map<String, DownloadProgress>,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    hasCustomTastes: Boolean = false,
    selectedMood: String = "all",
    isEconomyMode: Boolean = true,
    onSelectMood: (String) -> Unit = {},
    onToggleEconomyMode: () -> Unit = {},
    onTrackClick: (Track, List<Track>) -> Unit,
    onDownloadClick: (Track) -> Unit,
    onFavoriteClick: (Track) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToImport: () -> Unit,
    onOpenTastePicker: () -> Unit = {},
    onOpenLanguagePicker: () -> Unit = {},
    onPlayMyWave: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val currentLang = LocalAppLanguage.current

    // Localized mood list: Pair of key to localized display name
    val moodList = listOf(
        "all" to strings.moodAll,
        "relax" to strings.moodRelax,
        "energy" to strings.moodEnergy,
        "workout" to strings.moodWorkout,
        "focus" to strings.moodFocus,
        "road" to strings.moodRoad
    )

    // Dynamic greeting based on hour of day
    val greeting = remember(strings) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> strings.greetingMorning
            in 12..16 -> strings.greetingAfternoon
            in 17..22 -> strings.greetingEvening
            else -> strings.greetingNight
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(YtBackground)
            .statusBarsPadding()
            .padding(bottom = 96.dp)
    ) {
        // -------------------------------------------------------------
        // 1. TOP BAR: Brand, Greeting, Language Pill, Data Saver Pill, Actions
        // -------------------------------------------------------------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Logo & Brand
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_crystall_logo),
                            contentDescription = "Crystall Music",
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Crystall",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Music",
                                    color = YtTextSecondary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                            Text(
                                text = greeting,
                                color = YtTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Right: Language Pill, Data Saver Pill, Search & Taste Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Language Pill Switcher
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x24FFFFFF))
                                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(16.dp))
                                .clickable { onOpenLanguagePicker() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentLang.flag,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = currentLang.code.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Data Saver Mode Toggle Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isEconomyMode) Color(0x2E30D158) else Color(0x22FFFFFF)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isEconomyMode) GreenSuccess.copy(alpha = 0.6f) else Color(0x35FFFFFF),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onToggleEconomyMode() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEconomyMode) strings.dataSaverEco else strings.dataSaverNorm,
                                color = if (isEconomyMode) GreenSuccess else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Taste Star Button
                        IconButton(
                            onClick = onOpenTastePicker,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (hasCustomTastes) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Taste",
                                tint = if (hasCustomTastes) Color(0xFFFFD700) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Search Icon
                        IconButton(
                            onClick = onNavigateToSearch,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // -------------------------------------------------------------
        // 2. YANDEX MUSIC "МОЯ ВОЛНА" / MY WAVE HERO FLOW CARD (Liquid Glass Aurora)
        // -------------------------------------------------------------
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color(0xFF6366F1).copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E1B4B), // Deep indigo
                                Color(0xFF0F172A), // Obsidian
                                Color(0xFF092C3E), // Deep cyan glow
                                Color(0xFF1E1B4B)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0x66818CF8),
                                Color(0x33FFFFFF),
                                Color(0x2238BDF8)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onPlayMyWave() }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33818CF8))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFA5B4FC),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "FLOW • АЛГОРИТМ",
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Live sound wave equalizer animation
                            EqualizerVisualizer(
                                isPlaying = isPlaying,
                                barCount = 4,
                                barWidth = 2.5.dp,
                                maxHeight = 12.dp,
                                minHeight = 3.dp,
                                barSpacing = 1.5.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = strings.waveTitle,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = strings.waveSubtitle,
                            color = Color(0xCCFFFFFF),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Circular Glowing Glass Play Button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(12.dp, CircleShape, spotColor = Color(0xFF6366F1))
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF818CF8),
                                        Color(0xFF4F46E5)
                                    )
                                )
                            )
                            .border(1.dp, Color(0x66FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = strings.waveButtonPlay,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // -------------------------------------------------------------
        // 3. MOOD / CATEGORY FILTER CHIPS (Liquid Glass Horizontal Row)
        // -------------------------------------------------------------
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(moodList) { (key, displayName) ->
                    val isSelected = (key == selectedMood || (selectedMood == "Все" && key == "all"))
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color(0x1AFFFFFF),
                        label = "chip_bg"
                    )
                    val textCol by animateColorAsState(
                        targetValue = if (isSelected) Color.Black else YtTextPrimary,
                        label = "chip_text"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else Color(0x28FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectMood(key) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName,
                            color = textCol,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 4. QUICK PICKS (YouTube Music 4-Item Columns Grid)
        // -------------------------------------------------------------
        val quickPicks = if (trendingTracks.isNotEmpty()) trendingTracks.take(16) else emptyList()
        if (quickPicks.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = strings.quickPicksSubtitle,
                        color = YtTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = strings.quickPicksTitle,
                        color = YtTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                val columns = quickPicks.chunked(4)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(columns) { columnTracks ->
                        Column(
                            modifier = Modifier.width(310.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (track in columnTracks) {
                                QuickPickTrackItem(
                                    track = track,
                                    isCurrentTrack = currentTrack?.id == track.id,
                                    isPlaying = isPlaying && currentTrack?.id == track.id,
                                    isEconomyMode = isEconomyMode,
                                    onClick = { onTrackClick(track, quickPicks) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // -------------------------------------------------------------
        // 5. SPOTIFY "LISTEN AGAIN" (Horizontal Album Carousel)
        // -------------------------------------------------------------
        if (recentTracks.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = strings.listenAgainTitle,
                        color = YtTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentTracks.take(10)) { track ->
                        YtAlbumCard(
                            track = track,
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isEconomyMode = isEconomyMode,
                            onClick = { onTrackClick(track, recentTracks) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // -------------------------------------------------------------
        // 6. IMPORT PLAYLIST BANNER (Frosted Glass Card)
        // -------------------------------------------------------------
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(16.dp))
                    .clickable { onNavigateToImport() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLink,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.importBannerTitle,
                                color = YtTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.importBannerSubtitle,
                                color = YtTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = YtTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // -------------------------------------------------------------
        // 7. RECOMMENDATIONS & TOP CHARTS
        // -------------------------------------------------------------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val headerTitle = if (selectedMood != "all" && selectedMood != "Все") {
                    val matchingMood = moodList.firstOrNull { it.first == selectedMood }?.second ?: selectedMood
                    "Музыка: $matchingMood"
                } else if (hasCustomTastes) {
                    strings.recommendationsTitle
                } else {
                    strings.topChartsTitle
                }

                Text(
                    text = headerTitle,
                    color = YtTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        // List of trending / recommendation tracks
        items(trendingTracks) { track ->
            TrackItem(
                track = track,
                isCurrentTrack = currentTrack?.id == track.id,
                isPlaying = isPlaying && currentTrack?.id == track.id,
                downloadProgress = downloadStates[track.id],
                isFavorite = favoriteIds.contains(track.id),
                onTrackClick = { onTrackClick(track, trendingTracks) },
                onDownloadClick = { onDownloadClick(track) },
                onFavoriteClick = { onFavoriteClick(track) }
            )
        }
    }
}

// -------------------------------------------------------------
// YouTube Music / SoundCloud Quick Pick Track Item
// -------------------------------------------------------------
@Composable
fun QuickPickTrackItem(
    track: Track,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    isEconomyMode: Boolean,
    onClick: () -> Unit
) {
    val thumb = remember(track.coverUrl, isEconomyMode) {
        YouTubeEngine.upgradeThumbnailUrl(track.coverUrl, isEconomyMode)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentTrack) Color(0x22FFFFFF) else Color(0x0EFFFFFF))
            .border(
                width = 0.5.dp,
                color = if (isCurrentTrack) Color(0x44FFFFFF) else Color(0x18FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(YtSurface),
            contentAlignment = Alignment.Center
        ) {
            if (thumb.isNotBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = YtTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        EqualizerVisualizer(
                            isPlaying = true,
                            barCount = 3,
                            barWidth = 2.dp,
                            maxHeight = 14.dp,
                            minHeight = 3.dp,
                            barSpacing = 1.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrentTrack) Color.White else YtTextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = YtTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3-dots indicator
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            tint = YtTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// -------------------------------------------------------------
// Spotify / YouTube Music Square Album Card (for carousels)
// -------------------------------------------------------------
@Composable
fun YtAlbumCard(
    track: Track,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    isEconomyMode: Boolean,
    onClick: () -> Unit
) {
    val thumb = remember(track.coverUrl, isEconomyMode) {
        YouTubeEngine.upgradeThumbnailUrl(track.coverUrl, isEconomyMode)
    }

    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(135.dp)
                .shadow(12.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.6f))
                .clip(RoundedCornerShape(14.dp))
                .background(YtSurface)
                .border(0.5.dp, Color(0x25FFFFFF), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumb.isNotBlank()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = YtTextSecondary,
                    modifier = Modifier.size(48.dp)
                )
            }

            if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        EqualizerVisualizer(
                            isPlaying = true,
                            barCount = 4,
                            barWidth = 3.dp,
                            maxHeight = 22.dp,
                            minHeight = 4.dp,
                            barSpacing = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title,
            color = if (isCurrentTrack) Color.White else YtTextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = YtTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
