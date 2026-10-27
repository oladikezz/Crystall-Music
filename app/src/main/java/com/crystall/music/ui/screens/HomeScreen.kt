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

/**
 * 1:1 YouTube Music Clean Light Home Feed
 */
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
    onOpenSettings: () -> Unit = {},
    onPlayMyWave: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val currentLang = LocalAppLanguage.current

    // Localized mood list
    val moodList = listOf(
        "all" to strings.moodAll,
        "relax" to strings.moodRelax,
        "energy" to strings.moodEnergy,
        "workout" to strings.moodWorkout,
        "focus" to strings.moodFocus,
        "road" to strings.moodRoad
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding()
            .padding(bottom = 96.dp)
    ) {
        // -------------------------------------------------------------
        // 1. TOP BAR: Clean Light Brand Header
        // -------------------------------------------------------------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Logo & Brand
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_crystall_logo),
                        contentDescription = "Crystall Music",
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = "Crystall",
                        color = LightTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Right: Action Buttons (Search & Settings)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LightSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = LightTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LightSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = LightTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. MOOD & ACTIVITY FILTER CHIPS (YouTube Music 1:1 Light)
        // -------------------------------------------------------------
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(moodList, key = { it.first }) { (key, displayName) ->
                    val isSelected = (key == selectedMood || (selectedMood == "Все" && key == "all"))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) LightChipSelectedBackground else LightChipBackground)
                            .clickable { onSelectMood(key) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName,
                            color = if (isSelected) LightChipSelectedText else LightTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // -------------------------------------------------------------
        // 3. PERSONALIZED FLOW HERO CARD (My Wave / Personal Stream)
        // -------------------------------------------------------------
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightSurfaceElevated)
                    .border(1.dp, LightBorder, RoundedCornerShape(16.dp))
                    .clickable { onPlayMyWave() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(LightSurface)
                                .shadow(2.dp, CircleShape, spotColor = Color(0x18000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPlaying) {
                                EqualizerVisualizer(
                                    isPlaying = true,
                                    barCount = 3,
                                    barWidth = 2.dp,
                                    maxHeight = 14.dp,
                                    barSpacing = 1.5.dp,
                                    color = YtRed
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = YtRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = strings.waveTitle,
                                color = LightTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.waveSubtitle,
                                color = LightTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LightTextPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = strings.waveButtonPlay,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
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
                        color = LightTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = strings.quickPicksTitle,
                        color = LightTextPrimary,
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
                    items(columns, key = { col -> col.firstOrNull()?.id ?: col.hashCode() }) { columnTracks ->
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
        // 5. ALBUM CAROUSEL ("LISTEN AGAIN")
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
                        color = LightTextPrimary,
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
                    items(recentTracks.take(10), key = { it.id }) { track ->
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
        // 6. IMPORT PLAYLIST BANNER
        // -------------------------------------------------------------
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightSurfaceElevated)
                    .border(1.dp, LightBorder, RoundedCornerShape(16.dp))
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
                                .background(LightSurface)
                                .shadow(2.dp, CircleShape, spotColor = Color(0x18000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLink,
                                contentDescription = null,
                                tint = LightTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.importBannerTitle,
                                color = LightTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.importBannerSubtitle,
                                color = LightTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = LightTextSecondary,
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
                    color = LightTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = YtRed,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        // List of trending / recommendation tracks
        items(trendingTracks, key = { it.id }) { track ->
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
// YouTube Music Quick Pick Track Item (Clean Light)
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
            .background(if (isCurrentTrack) LightSurfaceElevated else LightSurface)
            .border(
                width = 0.5.dp,
                color = if (isCurrentTrack) YtRed.copy(alpha = 0.35f) else LightBorder,
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
                .background(LightSurfaceElevated),
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
                    tint = LightTextSecondary,
                    modifier = Modifier.size(24.dp)
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
                            barCount = 3,
                            barWidth = 2.dp,
                            maxHeight = 14.dp,
                            minHeight = 3.dp,
                            barSpacing = 1.5.dp,
                            color = Color.White
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
                color = if (isCurrentTrack) YtRed else LightTextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LightTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3-dots indicator
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            tint = LightTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// -------------------------------------------------------------
// YouTube Music Square Album Card (Clean Light for carousels)
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
                .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color(0x20000000))
                .clip(RoundedCornerShape(12.dp))
                .background(LightSurfaceElevated)
                .border(0.5.dp, LightBorder, RoundedCornerShape(12.dp)),
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
                    tint = LightTextSecondary,
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
                            barSpacing = 2.dp,
                            color = Color.White
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
            color = if (isCurrentTrack) YtRed else LightTextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = LightTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
