package com.crystall.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Locale

private enum class LibrarySort { RECENT, TITLE, ARTIST }

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
    onDeletePlaylistClick: (Playlist) -> Unit,
    downloadQueueTracks: List<Track> = emptyList(),
    offlineStorageUsedBytes: Long? = null,
    offlineStorageFreeBytes: Long? = null,
    wifiOnlyDownloads: Boolean = false,
    onWifiOnlyDownloadsChange: (Boolean) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onRetryDownload: (Track, (Boolean) -> Unit) -> Unit = { _, _ -> }
) {
    val strings = LocalStrings.current
    val labels = uiEnhancementStrings(LocalAppLanguage.current)
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(LibrarySort.RECENT) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val query = searchQuery.trim()

    val visibleDownloads = sortTracks(
        downloadedTracks.filter { it.matchesLibraryQuery(query) },
        sort
    )
    val visibleFavorites = sortTracks(
        favoriteTracks.filter { it.matchesLibraryQuery(query) },
        sort
    )
    val visiblePlaylists = sortPlaylists(
        playlists.filter {
            query.isEmpty() || it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true)
        },
        sort
    )
    val sortLabel = when (sort) {
        LibrarySort.RECENT -> strings.libRecentActivity
        LibrarySort.TITLE -> labels.sortTitle
        LibrarySort.ARTIST -> labels.sortArtist
    }
    val tabs = listOf(
        "${strings.libDownloads} (${downloadedTracks.size})",
        "${strings.libPlaylists} (${playlists.size})",
        "${strings.libFavorites} (${favoriteTracks.size})"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding()
            .padding(bottom = 90.dp)
    ) {
        Text(
            text = strings.libTitle,
            color = LightTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.size) { index ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) LightChipSelectedBackground else LightChipBackground)
                        .semantics { role = Role.Tab; selected = isSelected }
                        .clickable { selectedTab = index }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabs[index],
                        color = if (isSelected) LightChipSelectedText else LightTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(labels.searchLibrary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.semantics { contentDescription = labels.clearSearch }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TextButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.semantics { contentDescription = "${labels.sortBy}: $sortLabel" }
                ) {
                    Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sortLabel, fontSize = 13.sp)
                }
                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                    listOf(
                        LibrarySort.RECENT to strings.libRecentActivity,
                        LibrarySort.TITLE to labels.sortTitle,
                        LibrarySort.ARTIST to labels.sortArtist
                    ).forEach { (option, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                sort = option
                                sortMenuExpanded = false
                            },
                            leadingIcon = if (sort == option) ({ Icon(Icons.Default.Check, contentDescription = null) }) else null
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                item {
                    OfflineDownloadCenter(
                        downloadedCount = downloadedTracks.size,
                        usedBytes = offlineStorageUsedBytes,
                        freeBytes = offlineStorageFreeBytes,
                        wifiOnly = wifiOnlyDownloads,
                        onWifiOnlyChange = onWifiOnlyDownloadsChange,
                        tracks = (downloadQueueTracks + downloadedTracks + favoriteTracks).distinctBy { it.id },
                        downloadStates = downloadStates,
                        onCancelDownload = onCancelDownload,
                        onRetryDownload = onRetryDownload,
                        labels = labels
                    )
                }
                if (visibleDownloads.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            title = if (query.isNotEmpty()) labels.noMatches else strings.libNoDownloadsTitle,
                            subtitle = if (query.isNotEmpty()) "" else strings.libNoDownloadsSub,
                            icon = Icons.Default.FileDownloadOff
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.libAvailableOffline}: ${visibleDownloads.size}",
                                color = GreenSuccess,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = { onTrackClick(visibleDownloads.first(), visibleDownloads) },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LightSurfaceElevated,
                                    contentColor = LightTextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.libShuffleAll, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    items(visibleDownloads, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            downloadProgress = downloadStates[track.id],
                            isFavorite = track.id in favoriteIds,
                            onTrackClick = { onTrackClick(track, visibleDownloads) },
                            onDownloadClick = { onDownloadClick(track) },
                            onFavoriteClick = { onFavoriteClick(track) },
                            onDeleteClick = { onDeleteDownloadClick(track) }
                        )
                    }
                }
            }

            1 -> LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (visiblePlaylists.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            title = if (query.isNotEmpty()) labels.noMatches else strings.libNoPlaylistsTitle,
                            subtitle = if (query.isNotEmpty()) "" else strings.libNoPlaylistsSub,
                            icon = Icons.AutoMirrored.Filled.QueueMusic
                        )
                    }
                } else {
                    items(visiblePlaylists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { onPlaylistClick(playlist) }, role = Role.Button)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(LightSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!playlist.coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = playlist.coverUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = LightTextSecondary)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(playlist.title, color = LightTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    String.format(strings.libPlaylistTracksCount, playlist.trackCount),
                                    color = LightTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = { onDeletePlaylistClick(playlist) },
                                modifier = Modifier.semantics { contentDescription = "${strings.libDelete}: ${playlist.title}" }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = LightTextSecondary)
                            }
                        }
                    }
                }
            }

            else -> LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (visibleFavorites.isEmpty()) {
                    item {
                        EmptyLibraryState(
                            title = if (query.isNotEmpty()) labels.noMatches else strings.libNoFavoritesTitle,
                            subtitle = if (query.isNotEmpty()) "" else strings.libNoFavoritesSub,
                            icon = Icons.Default.ThumbUpOffAlt
                        )
                    }
                } else {
                    items(visibleFavorites, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            downloadProgress = downloadStates[track.id],
                            isFavorite = true,
                            onTrackClick = { onTrackClick(track, visibleFavorites) },
                            onDownloadClick = { onDownloadClick(track) },
                            onFavoriteClick = { onFavoriteClick(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineDownloadCenter(
    downloadedCount: Int,
    usedBytes: Long?,
    freeBytes: Long?,
    wifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    tracks: List<Track>,
    downloadStates: Map<String, DownloadProgress>,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (Track, (Boolean) -> Unit) -> Unit,
    labels: UiEnhancementStrings
) {
    val statusTracks = tracks.mapNotNull { track ->
        val progress = downloadStates[track.id] ?: return@mapNotNull null
        if (progress.isDownloading || !progress.errorMessage.isNullOrBlank()) track to progress else null
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, LightBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(labels.offlineCenter, color = LightTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${LocalStrings.current.libAvailableOffline}: $downloadedCount", color = LightTextSecondary, fontSize = 13.sp)
            if (usedBytes != null && freeBytes != null) {
                val safeUsed = usedBytes.coerceAtLeast(0L)
                val safeFree = freeBytes.coerceAtLeast(0L)
                val total = safeUsed.toDouble() + safeFree.toDouble()
                val fraction = if (total > 0.0) (safeUsed.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${labels.storageUsed}: ${formatByteCount(safeUsed)}", fontSize = 12.sp, color = LightTextSecondary)
                    Text("${labels.storageFree}: ${formatByteCount(safeFree)}", fontSize = 12.sp, color = LightTextSecondary)
                }
            } else {
                Text(labels.storageUnavailable, color = LightTextSecondary, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(labels.wifiOnly, modifier = Modifier.weight(1f), color = LightTextPrimary, fontSize = 14.sp)
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = onWifiOnlyChange,
                    modifier = Modifier.semantics { contentDescription = labels.wifiOnly }
                )
            }
            statusTracks.forEach { (track, progress) ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = LightTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (progress.isDownloading) {
                                Text("${labels.downloading} · ${progress.progressPercent.coerceIn(0, 100)}%", color = LightTextSecondary, fontSize = 12.sp)
                            } else {
                                Text(
                                    "${labels.failed}: ${progress.errorMessage ?: labels.failed}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                if (progress.isDownloading) onCancelDownload(track.id)
                                else onRetryDownload(track) { }
                            }
                        ) {
                            Text(if (progress.isDownloading) labels.cancelDownload else labels.retryDownload)
                        }
                    }
                    if (progress.isDownloading) {
                        LinearProgressIndicator(
                            progress = { progress.progressPercent.coerceIn(0, 100) / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = LightTextMuted, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, color = LightTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = LightTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

private fun Track.matchesLibraryQuery(query: String): Boolean =
    query.isEmpty() || title.contains(query, ignoreCase = true) || artist.contains(query, ignoreCase = true)

private fun sortTracks(tracks: List<Track>, sort: LibrarySort): List<Track> = when (sort) {
    LibrarySort.RECENT -> tracks.sortedByDescending { it.addedAt }
    LibrarySort.TITLE -> tracks.sortedBy { it.title.lowercase() }
    LibrarySort.ARTIST -> tracks.sortedBy { it.artist.lowercase() }
}

private fun sortPlaylists(playlists: List<Playlist>, sort: LibrarySort): List<Playlist> = when (sort) {
    LibrarySort.RECENT -> playlists.sortedByDescending { it.createdAt }
    LibrarySort.TITLE -> playlists.sortedBy { it.title.lowercase() }
    LibrarySort.ARTIST -> playlists.sortedBy { it.author.lowercase() }
}

private fun formatByteCount(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes ${units[unit]}" else String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}
