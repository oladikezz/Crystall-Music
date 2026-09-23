package com.crystall.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.DownloadProgress
import com.crystall.music.ui.components.TrackItem
import com.crystall.music.ui.theme.*

/**
 * 1:1 YouTube Music Search Screen
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
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val searchSuggestions = listOf(
        "Хиты", "Новинки", "Хип-хоп", "Поп-музыка", "Рок", "Электроника",
        "Для отдыха", "В дорогу", "Фокус", "Тренировка"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YtBackground)
            .statusBarsPadding()
            .padding(bottom = 90.dp)
    ) {
        // YouTube Music Top Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Исполнители, треки или альбомы",
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
                    cursorColor = YtRed,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        // Search Suggestions / Exploration Chips
        if (searchQuery.isEmpty() && searchResults.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Популярные категории",
                    color = YtTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Chips Flow/Rows
                val chunkedSuggestions = searchSuggestions.chunked(2)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(searchSuggestions) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(YtSurface)
                                .clickable {
                                    searchQuery = tag
                                    focusManager.clearFocus()
                                    onSearch(tag)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = tag,
                                color = YtTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Loading or Results State
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
                        text = "Ничего не найдено",
                        color = YtTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(searchResults) { track ->
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
