package com.crystall.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.DownloadProgress
import com.crystall.music.engine.YouTubeEngine
import com.crystall.music.ui.components.EqualizerVisualizer
import com.crystall.music.ui.components.TrackItem
import com.crystall.music.ui.i18n.LocalStrings
import com.crystall.music.ui.theme.*

private data class ExploreGenre(
    val name: String,
    val color: Color
)

/**
 * 1:1 YouTube Music Search Screen
 * Features:
 * - Pure obsidian black matte background (#030303)
 * - YouTube Music rounded pill search bar with YtSurface (#1B1B1F) container
 * - Mood and category exploration cards in 2-column grid when idle
 * - Category filter chips (All, Songs, Artists) when results are present
 * - YouTube Music Top Result hero card
 * - Signature YouTube Red accent on active indicators and progress
 */
@Composable
fun SearchScreen(
    currentTrack: Track?,
    isPlaying: Boolean,
    downloadStates: Map<String, DownloadProgress>,
    favoriteIds: Set<String>,
    searchResults: List<Track>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onDownloadClick: (Track) -> Unit,
    onFavoriteClick: (Track) -> Unit
) {
    val strings = LocalStrings.current
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var selectedFilter by remember { mutableStateOf("all") }

    val searchSuggestions = listOf(
        "Хиты", "Новинки", "Тренды", "Charts", "Workout",
        "Chill", "Focus", "Party", "Hip-hop", "Rock"
    )

    val exploreGenres = listOf(
        ExploreGenre("Chill", Color(0xFF1D5D9B)),
        ExploreGenre("Workout", Color(0xFFC2185B)),
        ExploreGenre("Focus", Color(0xFF2E7D32)),
        ExploreGenre("Hip-Hop", Color(0xFFE65100)),
        ExploreGenre("Rock", Color(0xFF455A64)),
        ExploreGenre("Pop", Color(0xFF7B1FA2)),
        ExploreGenre("Electronic", Color(0xFF00838F)),
        ExploreGenre("Party", Color(0xFFF57C00)),
        ExploreGenre("Acoustic", Color(0xFF6D4C41)),
        ExploreGenre("Romance", Color(0xFFAD1457)),
        ExploreGenre("Indie", Color(0xFF4E342E)),
        ExploreGenre("Gaming", Color(0xFF3F51B5))
    )

    val filterOptions = listOf(
        "all" to strings.searchFilterAll,
        "songs" to strings.searchFilterSongs,
        "artists" to strings.searchFilterArtists
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YtBackground)
            .statusBarsPadding()
            .padding(bottom = 90.dp)
    ) {
        // -------------------------------------------------------------
        // 1. YouTube Music Matte Pill Search Bar
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        strings.searchPlaceholder,
                        color = YtTextSecondary,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = YtTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = YtTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    if (searchQuery.isNotBlank()) {
                        onSearch(searchQuery)
                    }
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = YtSurface,
                    unfocusedContainerColor = YtSurface,
                    focusedTextColor = YtTextPrimary,
                    unfocusedTextColor = YtTextPrimary,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }

        // -------------------------------------------------------------
        // 2. Filter Chips (When Search Results Are Loaded)
        // -------------------------------------------------------------
        if (searchResults.isNotEmpty() && !isSearching) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { (key, label) ->
                    val isSelected = selectedFilter == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) Color.White else Color(0x18FFFFFF))
                            .clickable { selectedFilter = key }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else YtTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // -------------------------------------------------------------
        // 3. Main Content: Idle / Loading / Results / Empty States
        // -------------------------------------------------------------
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = YtRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = YtTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.searchNothingFound,
                        color = YtTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (searchResults.isEmpty()) {
            // Idle State: YouTube Music Exploration & Genre Cards
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Quick suggestions horizontal pills
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.searchPopularCategories,
                            color = YtTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            items(searchSuggestions) { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0x18FFFFFF))
                                        .clickable {
                                            searchQuery = tag
                                            focusManager.clearFocus()
                                            onSearch(tag)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        color = YtTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Moods & Genres Header
                item {
                    Text(
                        text = strings.searchExploreGenres,
                        color = YtTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // 2-Column Exploration Cards Grid
                val chunkedGenres = exploreGenres.chunked(2)
                items(chunkedGenres) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (genre in pair) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(YtSurface)
                                    .clickable {
                                        searchQuery = genre.name
                                        focusManager.clearFocus()
                                        onSearch(genre.name)
                                    }
                            ) {
                                // Subtle left vertical accent bar
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .fillMaxHeight()
                                        .background(genre.color)
                                        .align(Alignment.CenterStart)
                                )
                                Text(
                                    text = genre.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 16.dp)
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // Results List
            val displayedTracks = when (selectedFilter) {
                "artists" -> searchResults.distinctBy { it.artist }
                else -> searchResults
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // YouTube Music "Top Result" card (only in "all" filter mode)
                if (selectedFilter == "all" && searchResults.isNotEmpty()) {
                    val topTrack = searchResults.first()
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = strings.searchTopResult.uppercase(),
                                color = YtTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(YtSurface)
                                    .clickable { onTrackClick(topTrack, searchResults) }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Large Artwork
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (topTrack.coverUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = YouTubeEngine.upgradeThumbnailUrl(topTrack.coverUrl),
                                                contentDescription = topTrack.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = YtTextSecondary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        if (currentTrack?.id == topTrack.id && isPlaying) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                EqualizerVisualizer(
                                                    isPlaying = true,
                                                    barCount = 3,
                                                    barWidth = 2.dp,
                                                    maxHeight = 14.dp,
                                                    barSpacing = 1.5.dp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = topTrack.title,
                                            color = if (currentTrack?.id == topTrack.id) YtRed else Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${topTrack.artist} • YouTube Music",
                                            color = YtTextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Play icon
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (currentTrack?.id == topTrack.id && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Section header for songs
                    item {
                        Text(
                            text = strings.searchFilterSongs,
                            color = YtTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    val remainingTracks = searchResults.drop(1)
                    items(remainingTracks) { track ->
                        TrackItem(
                            track = track,
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            downloadProgress = downloadStates[track.id],
                            isFavorite = favoriteIds.contains(track.id),
                            onTrackClick = { onTrackClick(track, searchResults) },
                            onDownloadClick = { onDownloadClick(track) },
                            onFavoriteClick = { onFavoriteClick(track) }
                        )
                    }
                } else {
                    items(displayedTracks) { track ->
                        TrackItem(
                            track = track,
                            isCurrentTrack = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            downloadProgress = downloadStates[track.id],
                            isFavorite = favoriteIds.contains(track.id),
                            onTrackClick = { onTrackClick(track, searchResults) },
                            onDownloadClick = { onDownloadClick(track) },
                            onFavoriteClick = { onFavoriteClick(track) }
                        )
                    }
                }
            }
        }
    }
}
