package com.crystall.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import com.crystall.music.ui.components.TrackItem
import com.crystall.music.ui.theme.*

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
    selectedMood: String = "Все",
    isEconomyMode: Boolean = true,
    onSelectMood: (String) -> Unit = {},
    onToggleEconomyMode: () -> Unit = {},
    onTrackClick: (Track, List<Track>) -> Unit,
    onDownloadClick: (Track) -> Unit,
    onFavoriteClick: (Track) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToImport: () -> Unit,
    onOpenTastePicker: () -> Unit = {}
) {
    val moods = listOf("Все", "Для отдыха", "Энергия", "Тренировка", "Фокус", "В дорогу")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(YtBackground)
            .statusBarsPadding()
            .padding(bottom = 96.dp)
    ) {
        // 1. YouTube Music Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Logo + Crystall Music Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_crystall_logo),
                        contentDescription = "Crystall Music",
                        modifier = Modifier.size(30.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Crystall",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Music",
                            color = YtTextSecondary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                // Right: Data Saver Capsule, Search & Taste Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Data Saver Mode Toggle Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isEconomyMode) Color(0x2E30D158) else Color(0x2EFFFFFF)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isEconomyMode) GreenSuccess.copy(alpha = 0.6f) else Color(0x44FFFFFF),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onToggleEconomyMode() }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isEconomyMode) "⚡ Эконом" else "💎 Норм",
                                color = if (isEconomyMode) GreenSuccess else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Search Icon
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Taste Star Button
                    IconButton(
                        onClick = onOpenTastePicker,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (hasCustomTastes) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Taste",
                            tint = if (hasCustomTastes) Color(0xFFFFD700) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 2. YouTube Music Mood / Category Filter Chips (Horizontal Row)
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(moods) { mood ->
                    val isSelected = (mood == selectedMood)
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) YtChipSelectedBackground else YtChipBackground,
                        label = "chip_bg"
                    )
                    val textCol by animateColorAsState(
                        targetValue = if (isSelected) YtChipSelectedText else YtTextPrimary,
                        label = "chip_text"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else YtChipBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectMood(mood) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mood,
                            color = textCol,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. YouTube Music Signature Section: "Быстрый выбор" (Quick Picks)
        // Horizontal scroll with columns of 4 tracks each
        val quickPicks = if (trendingTracks.isNotEmpty()) trendingTracks.take(16) else emptyList()
        if (quickPicks.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Включить радио по треку",
                        color = YtTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = "Быстрый выбор",
                        color = YtTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                // Chunk into columns of 4 items each
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
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 4. "Снова послушайте" (Listen Again / Recently Played)
        if (recentTracks.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Снова послушайте",
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
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 5. Import Playlist Banner (YouTube Music style card)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(YtSurface)
                    .border(1.dp, YtSurfaceBorder, RoundedCornerShape(12.dp))
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
                                text = "Импорт плейлиста",
                                color = YtTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "YouTube Music, SoundCloud, ссылки",
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

        // 6. Section Header: "Рекомендации и чарты" / "Хиты"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedMood != "Все") "Музыка: $selectedMood" else if (hasCustomTastes) "Рекомендации для вас" else "Топ чарты и хиты",
                    color = YtTextPrimary,
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
// YouTube Music Quick Pick Track Item (used in 4-item columns)
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrentTrack) Color(0x22FFFFFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
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
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrentTrack) YtRed else YtTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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
// YouTube Music Square Album Card (used in carousels)
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
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title,
            color = if (isCurrentTrack) YtRed else YtTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
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
