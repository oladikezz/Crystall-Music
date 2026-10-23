package com.crystall.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.DownloadProgress
import com.crystall.music.ui.components.TrackItem
import com.crystall.music.ui.i18n.LocalAppLanguage
import com.crystall.music.ui.i18n.LocalStrings
import com.crystall.music.ui.theme.*

/**
 * 1:1 YouTube Music Clean Light Playlist Detail Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailSheet(
    playlist: Playlist,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    downloadStates: Map<String, DownloadProgress>,
    favoriteIds: Set<String>,
    onDismiss: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onDownloadClick: (Track) -> Unit,
    onFavoriteClick: (Track) -> Unit,
    onDownloadAll: (List<Track>) -> Unit,
    onRenamePlaylist: (String) -> Unit = {},
    onRemoveTrack: (Track) -> Unit = {}
) {
    val strings = LocalStrings.current
    val labels = uiEnhancementStrings(LocalAppLanguage.current)
    var showRenameDialog by remember { mutableStateOf(false) }
    var playlistName by remember(playlist.id, playlist.title) { mutableStateOf(playlist.title) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LightSurface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFD1D5DB),
                shape = RoundedCornerShape(999.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (!playlist.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = com.crystall.music.engine.YouTubeEngine.upgradeThumbnailUrl(playlist.coverUrl),
                            contentDescription = playlist.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = LightTextSecondary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        color = LightTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${playlist.author} • ${tracks.size} ${strings.playerTabUpNext.lowercase()}",
                        color = LightTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = {
                        playlistName = playlist.title
                        showRenameDialog = true
                    },
                    modifier = Modifier.semantics { contentDescription = "${labels.renamePlaylist}: ${playlist.title}" }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = LightTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Play All & Download All (Clean Light Pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (tracks.isNotEmpty()) {
                            onTrackClick(tracks.first(), tracks)
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightTextPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.playerPlay, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onDownloadAll(tracks) },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightSurfaceElevated,
                        contentColor = LightTextPrimary
                    )
                ) {
                    Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp), tint = LightTextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.importDownloadAll, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(tracks) { track ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
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
                        IconButton(
                            onClick = { onRemoveTrack(track) },
                            modifier = Modifier.semantics { contentDescription = "${labels.removeTrack}: ${track.title}" }
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = LightTextSecondary)
                        }
                    }
                }
            }
        }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text(labels.renamePlaylist) },
                text = {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text(labels.playlistName) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRenamePlaylist(playlistName.trim())
                            showRenameDialog = false
                        },
                        enabled = playlistName.isNotBlank()
                    ) { Text(labels.save) }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text(labels.cancel) }
                }
            )
        }
    }
}
