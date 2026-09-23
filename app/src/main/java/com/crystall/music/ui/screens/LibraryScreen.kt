package com.crystall.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.DownloadProgress
import com.crystall.music.ui.components.TrackItem
import com.crystall.music.ui.theme.*

/**
 * 1:1 YouTube Music Library Screen
 */
@Composable
fun LibraryScreen(
    currentTrack: Track?,
    isPlaying: Boolean,
    downloadedTracks: List<Track>,
    favoriteTracks: List<Track>,
    playlists: List<Playlist>,
    downloadStates: Map<String, DownloadProgress>,
    favoriteIds: Set<String>,
    onTrackClick: (Track, List<Track>) -> Unit,
    onDownloadClick: (Track) -> Unit,
    onFavoriteClick: (Track) -> Unit,
    onDeleteDownloadClick: (Track) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Downloads, 1 = Playlists, 2 = Favorites

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YtBackground)
            .statusBarsPadding()
            .padding(bottom = 90.dp)
    ) {
        // YouTube Music Library Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Фонотека",
                color = YtTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }

        // YouTube Music Category Filter Chips
        val tabs = listOf(
            "Скачанные (${downloadedTracks.size})",
            "Плейлисты (${playlists.size})",
            "Понравившиеся (${favoriteTracks.size})"
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.indices.toList()) { index ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else YtSurface)
                        .clickable { selectedTab = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabs[index],
                        color = if (isSelected) Color.Black else YtTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Sorting Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = "Sort",
                tint = YtTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Недавние действия",
                color = YtTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Content
        when (selectedTab) {
            0 -> {
                // Downloads
                if (downloadedTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FileDownloadOff,
                                contentDescription = null,
                                tint = YtTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Нет скачанных треков",
                                color = YtTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Скачивайте треки и слушайте их без подключения к сети",
                                color = YtTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Доступно офлайн: ${downloadedTracks.size}",
                                    color = GreenSuccess,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Button(
                                    onClick = { onTrackClick(downloadedTracks.first(), downloadedTracks) },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = YtSurface,
                                        contentColor = YtTextPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Перемешать всё", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        items(downloadedTracks) { track ->
                            TrackItem(
                                track = track,
                                isCurrentTrack = currentTrack?.id == track.id,
                                isPlaying = isPlaying && currentTrack?.id == track.id,
                                downloadProgress = downloadStates[track.id],
                                isFavorite = favoriteIds.contains(track.id),
                                onTrackClick = { onTrackClick(track, downloadedTracks) },
                                onDownloadClick = { onDownloadClick(track) },
                                onFavoriteClick = { onFavoriteClick(track) },
                                onDeleteClick = { onDeleteDownloadClick(track) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Playlists
                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = YtTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Плейлистов пока нет",
                                color = YtTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Импортируйте плейлисты из YouTube Music во вкладке «Импорт»",
                                color = YtTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlaylistClick(playlist) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(YtSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!playlist.coverUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = playlist.coverUrl,
                                            contentDescription = playlist.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = YtTextSecondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.title,
                                        color = YtTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Плейлист • ${playlist.trackCount} треков",
                                        color = YtTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(onClick = { onDeletePlaylistClick(playlist) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = YtTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Favorites
                if (favoriteTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ThumbUpOffAlt,
                                contentDescription = null,
                                tint = YtTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Понравившихся треков пока нет",
                                color = YtTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Нажмите «Нравится» в плеере, чтобы сохранить треки здесь",
                                color = YtTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(favoriteTracks) { track ->
                            TrackItem(
                                track = track,
                                isCurrentTrack = currentTrack?.id == track.id,
                                isPlaying = isPlaying && currentTrack?.id == track.id,
                                downloadProgress = downloadStates[track.id],
                                isFavorite = true,
                                onTrackClick = { onTrackClick(track, favoriteTracks) },
                                onDownloadClick = { onDownloadClick(track) },
                                onFavoriteClick = { onFavoriteClick(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}
