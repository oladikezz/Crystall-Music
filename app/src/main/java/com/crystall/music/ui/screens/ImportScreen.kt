package com.crystall.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.DownloadProgress
import com.crystall.music.engine.ResolveResult
import com.crystall.music.ui.components.TrackItem
import com.crystall.music.ui.theme.*

/**
 * 1:1 YouTube Music Import Screen
 */
@Composable
fun ImportScreen(
    currentTrack: Track?,
    isPlaying: Boolean,
    downloadStates: Map<String, DownloadProgress>,
    favoriteIds: Set<String>,
    isResolving: Boolean,
    resolveResult: ResolveResult?,
    onResolveUrl: (String) -> Unit,
    onSavePlaylist: (Playlist, List<Track>) -> Unit,
    onDownloadAllTracks: (List<Track>) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onDownloadClick: (Track) -> Unit,
    onFavoriteClick: (Track) -> Unit
) {
    var inputUrl by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var isSavedToLibrary by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(YtBackground)
            .statusBarsPadding()
            .padding(bottom = 90.dp)
    ) {
        // YouTube Music Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Импорт",
                    color = YtTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Вставьте ссылку на плейлист или трек",
                    color = YtTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Input Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = YtSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    TextField(
                        value = inputUrl,
                        onValueChange = {
                            inputUrl = it
                            isSavedToLibrary = false
                        },
                        placeholder = {
                            Text(
                                "https://music.youtube.com/playlist?list=...",
                                color = YtTextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161616),
                            unfocusedContainerColor = Color(0xFF161616),
                            focusedTextColor = YtTextPrimary,
                            unfocusedTextColor = YtTextPrimary,
                            cursorColor = YtRed,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    inputUrl = clip.trim()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = YtTextPrimary)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Вставить", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (inputUrl.isNotBlank()) {
                                    isSavedToLibrary = false
                                    onResolveUrl(inputUrl)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            enabled = !isResolving && inputUrl.isNotBlank()
                        ) {
                            if (isResolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Загрузка...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Импорт", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Result display
        when (resolveResult) {
            is ResolveResult.Error -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = resolveResult.message,
                            color = Color(0xFFFF8A80),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            is ResolveResult.PlaylistResult -> {
                val playlist = resolveResult.playlist
                val tracks = resolveResult.tracks

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Playlist Info Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(YtSurface)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF141414)),
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
                                        imageVector = Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        tint = YtTextSecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.title,
                                    color = YtTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${playlist.author} • ${tracks.size} треков",
                                    color = YtTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions: Save to Library & Download All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSavePlaylist(playlist, tracks)
                                    isSavedToLibrary = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSavedToLibrary) GreenSuccess.copy(alpha = 0.25f) else YtSurface,
                                    contentColor = if (isSavedToLibrary) GreenSuccess else YtTextPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isSavedToLibrary) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSavedToLibrary) "Сохранено" else "В медиатеку", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onSavePlaylist(playlist, tracks)
                                    onDownloadAllTracks(tracks)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Скачать всё (${tracks.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Треки в плейлисте:",
                            color = YtTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                items(tracks) { track ->
                    TrackItem(
                        track = track,
                        isCurrentTrack = currentTrack?.id == track.id,
                        isPlaying = isPlaying && currentTrack?.id == track.id,
                        downloadProgress = downloadStates[track.id],
                        isFavorite = favoriteIds.contains(track.id),
                        onTrackClick = { onTrackClick(track, tracks) },
                        onDownloadClick = { onDownloadClick(track) },
                        onFavoriteClick = { onFavoriteClick(track) }
                    )
                }
            }

            is ResolveResult.SingleTrackResult -> {
                val track = resolveResult.track
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Найден трек:",
                            color = YtTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                item {
                    TrackItem(
                        track = track,
                        isCurrentTrack = currentTrack?.id == track.id,
                        isPlaying = isPlaying && currentTrack?.id == track.id,
                        downloadProgress = downloadStates[track.id],
                        isFavorite = favoriteIds.contains(track.id),
                        onTrackClick = { onTrackClick(track, listOf(track)) },
                        onDownloadClick = { onDownloadClick(track) },
                        onFavoriteClick = { onFavoriteClick(track) }
                    )
                }
            }

            null -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = YtTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Импортируйте любимые плейлисты для прослушивания и загрузки в память устройства.",
                            color = YtTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
