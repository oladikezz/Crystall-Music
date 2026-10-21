package com.crystall.music.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.DownloadProgress
import com.crystall.music.engine.LyricsResult
import com.crystall.music.engine.YouTubeEngine
import com.crystall.music.ui.i18n.LocalStrings
import com.crystall.music.ui.theme.*

enum class PlayerBottomTab {
    UP_NEXT, LYRICS, RELATED
}

/**
 * 1:1 YouTube Music Clean Light Full Screen Player
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    track: Track,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    isFavorite: Boolean,
    isDisliked: Boolean = false,
    isShuffle: Boolean,
    repeatMode: Int,
    queue: List<Track>,
    downloadProgress: DownloadProgress?,
    lyricsResult: LyricsResult? = null,
    isLoadingLyrics: Boolean = false,
    relatedTracks: List<Track> = emptyList(),
    sleepTimerRemainingMs: Long? = null,
    isSleepTimerEndOfTrack: Boolean = false,
    isEndlessRadioEnabled: Boolean = true,
    isEconomyMode: Boolean = true,
    onToggleEconomyMode: () -> Unit = {},
    onToggleEndlessRadio: () -> Unit = {},
    onOpenPlayerOptions: () -> Unit = {},
    onDismiss: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onFavoriteClick: () -> Unit,
    onDislikeClick: () -> Unit = {},
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onQueueTrackClick: (Track) -> Unit,
    onStopPlayback: () -> Unit = {}
) {
    val strings = LocalStrings.current
    var selectedBottomTab by remember { mutableStateOf<PlayerBottomTab?>(null) }
    var isVideoMode by remember { mutableStateOf(false) }

    val upgradedCoverUrl = remember(track.coverUrl, isEconomyMode) {
        YouTubeEngine.upgradeThumbnailUrl(track.coverUrl, isEconomyMode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Down Arrow, Song/Video Toggle, Options Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Collapse Down Arrow
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = LightTextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Center: Song | Video Toggle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(LightSurfaceElevated)
                        .border(1.dp, LightBorder, RoundedCornerShape(20.dp))
                        .padding(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Song Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (!isVideoMode) Color.White else Color.Transparent)
                                .shadow(if (!isVideoMode) 2.dp else 0.dp, RoundedCornerShape(18.dp))
                                .clickable { isVideoMode = false }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.playerSongMode,
                                color = if (!isVideoMode) LightTextPrimary else LightTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        // Video Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isVideoMode) Color.White else Color.Transparent)
                                .shadow(if (isVideoMode) 2.dp else 0.dp, RoundedCornerShape(18.dp))
                                .clickable { isVideoMode = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.playerVideoMode,
                                color = if (isVideoMode) LightTextPrimary else LightTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Right: 3-Dots Menu
                IconButton(onClick = onOpenPlayerOptions) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = LightTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // 2. Square Artwork with rounded corners and swipe gestures
            var dragOffset by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 8.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset < -80f) {
                                    onNextClick()
                                } else if (dragOffset > 80f) {
                                    onPreviousClick()
                                }
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragOffset += dragAmount
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(16.dp, RoundedCornerShape(14.dp), spotColor = Color(0x33000000), ambientColor = Color(0x14000000))
                        .clip(RoundedCornerShape(14.dp))
                        .background(LightSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (upgradedCoverUrl.isNotBlank()) {
                        AsyncImage(
                            model = upgradedCoverUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = LightTextSecondary,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    if (isVideoMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = YtRed,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = strings.videoModeOverlay,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // 3. Track Info (Title, Artist) + Thumbs Up & Thumbs Down Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = LightTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        color = LightTextSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Thumbs Down & Thumbs Up
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDislikeClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                            contentDescription = "Dislike",
                            tint = if (isDisliked) YtRed else LightTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            tint = if (isFavorite) YtRed else LightTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Scrubber / Progress Bar (YouTube Music Red Indicator)
            val totalDuration: Long = if (durationMs > 0L) durationMs else track.durationMs
            var isDraggingSlider by remember { mutableStateOf(false) }
            var dragPositionFraction by remember { mutableFloatStateOf(0f) }

            val currentFraction = if (isDraggingSlider) {
                dragPositionFraction
            } else if (totalDuration > 0L) {
                (positionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            Slider(
                value = currentFraction,
                onValueChange = { newFrac ->
                    isDraggingSlider = true
                    dragPositionFraction = newFrac
                },
                onValueChangeFinished = {
                    if (totalDuration > 0L) {
                        onSeekTo((dragPositionFraction * totalDuration).toLong())
                    }
                    isDraggingSlider = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = YtRed,
                    activeTrackColor = YtRed,
                    inactiveTrackColor = Color(0xFFE5E7EB)
                )
            )

            // Time Row
            val displayedPositionMs = if (isDraggingSlider && totalDuration > 0L) {
                (dragPositionFraction * totalDuration).toLong()
            } else {
                positionMs
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(displayedPositionMs),
                    color = LightTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (totalDuration > 0L) formatMs(totalDuration) else "--:--",
                    color = LightTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Main Controls Row (Shuffle, Prev, Big Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Shuffle
                IconButton(onClick = onShuffleClick) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) YtRed else LightTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = LightTextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Center Circular Play/Pause (Deep Black button with pure white icon for striking contrast)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(LightTextPrimary)
                        .clickable { onPlayPauseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = LightTextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onRepeatClick) {
                    Icon(
                        imageVector = when (repeatMode) {
                            2 -> Icons.Default.RepeatOne
                            1 -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) YtRed else LightTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // 6. Action Dock (Download & Data Saver Quick Chip)
            val isDownloaded = track.isDownloaded || downloadProgress?.isCompleted == true
            val isDownloading = downloadProgress?.isDownloading == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Download Action
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onDownloadClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            progress = { (downloadProgress?.progressPercent ?: 0) / 100f },
                            modifier = Modifier.size(18.dp),
                            color = YtRed,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "${downloadProgress?.progressPercent ?: 0}%",
                            color = LightTextPrimary,
                            fontSize = 12.sp
                        )
                    } else if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GreenSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.playerDownloaded,
                            color = GreenSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "Download",
                            tint = LightTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.playerDownload,
                            color = LightTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Data Saver Toggle Action
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggleEconomyMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isEconomyMode) strings.dataSaverEco else strings.dataSaverNorm,
                        color = if (isEconomyMode) GreenSuccess else LightTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Sleep timer indicator if active
                if (sleepTimerRemainingMs != null || isSleepTimerEndOfTrack) {
                    val timerText = if (isSleepTimerEndOfTrack) "🌙 Конец" else {
                        val min = (sleepTimerRemainingMs ?: 0L) / 60000
                        "🌙 ${min}м"
                    }
                    Text(
                        text = timerText,
                        color = GreenSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onOpenPlayerOptions() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 7. Signature 3-Tab Bottom Pill (UP NEXT | LYRICS | RELATED)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(LightSurfaceElevated)
                    .border(1.dp, LightBorder, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // UP NEXT Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedBottomTab = PlayerBottomTab.UP_NEXT },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.playerTabUpNext,
                            color = LightTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(LightBorder)
                    )

                    // LYRICS Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedBottomTab = PlayerBottomTab.LYRICS },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.playerTabLyrics,
                            color = LightTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(LightBorder)
                    )

                    // RELATED Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedBottomTab = PlayerBottomTab.RELATED },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.playerTabRelated,
                            color = LightTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // YouTube Music Bottom Drawer Sheets for UP NEXT, LYRICS, RELATED
        // -------------------------------------------------------------
        if (selectedBottomTab != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedBottomTab = null },
                containerColor = LightSurface,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1D5DB))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.82f)
                        .padding(horizontal = 20.dp)
                ) {
                    // Header with 3 Tabs Switcher in Sheet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (tab in PlayerBottomTab.values()) {
                            val isTabSelected = selectedBottomTab == tab
                            val label = when (tab) {
                                PlayerBottomTab.UP_NEXT -> strings.playerTabUpNext
                                PlayerBottomTab.LYRICS -> strings.playerTabLyrics
                                PlayerBottomTab.RELATED -> strings.playerTabRelated
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedBottomTab = tab }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isTabSelected) LightTextPrimary else LightTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (isTabSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(YtRed)
                                    )
                                }
                            }
                        }
                    }

                    // Drawer Content per Tab
                    when (selectedBottomTab) {
                        PlayerBottomTab.UP_NEXT -> {
                            // Autoplay / Endless Radio Toggle Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.autoplayTitle,
                                        color = LightTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = strings.autoplaySubtitle,
                                        color = LightTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = isEndlessRadioEnabled,
                                    onCheckedChange = { onToggleEndlessRadio() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = YtRed
                                    )
                                )
                            }

                            HorizontalDivider(color = LightBorder, thickness = 1.dp)

                            // Upcoming Queue List
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(queue) { idx, qTrack ->
                                    val isCurrent = qTrack.id == track.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) LightSurfaceElevated else Color.Transparent)
                                            .clickable {
                                                onQueueTrackClick(qTrack)
                                                selectedBottomTab = null
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            color = if (isCurrent) YtRed else LightTextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.width(24.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = qTrack.title,
                                                color = if (isCurrent) YtRed else LightTextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = qTrack.artist,
                                                color = LightTextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isCurrent && isPlaying) {
                                            Icon(
                                                imageVector = Icons.Default.GraphicEq,
                                                contentDescription = null,
                                                tint = YtRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        PlayerBottomTab.LYRICS -> {
                            // Synced / Plain Lyrics View
                            if (isLoadingLyrics) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = YtRed,
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            } else if (lyricsResult != null && lyricsResult.syncedLyrics.isNotEmpty()) {
                                val listState = rememberLazyListState()
                                val synced = lyricsResult.syncedLyrics
                                val activeIndex = remember(positionMs, synced) {
                                    val idx = synced.indexOfLast { it.timeMs <= positionMs }
                                    if (idx >= 0) idx else 0
                                }
                                LaunchedEffect(activeIndex) {
                                    if (activeIndex in synced.indices) {
                                        listState.animateScrollToItem(
                                            index = (activeIndex - 1).coerceAtLeast(0)
                                        )
                                    }
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(vertical = 24.dp)
                                ) {
                                    itemsIndexed(synced) { idx, line ->
                                        val isCurrent = idx == activeIndex
                                        Text(
                                            text = line.text,
                                            color = if (isCurrent) LightTextPrimary else Color(0xFF9AA0A6),
                                            fontSize = if (isCurrent) 22.sp else 16.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSeekTo(line.timeMs) }
                                                .padding(vertical = 4.dp),
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            } else if (lyricsResult != null && !lyricsResult.plainLyrics.isNullOrBlank()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 16.dp)
                                ) {
                                    item {
                                        Text(
                                            text = lyricsResult.plainLyrics,
                                            color = LightTextPrimary,
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.lyricsUnavailable,
                                        color = LightTextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        PlayerBottomTab.RELATED -> {
                            // Related Tracks & Recommendations
                            if (relatedTracks.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.relatedEmpty,
                                        color = LightTextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = strings.relatedHeader,
                                            color = LightTextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                    items(relatedTracks) { rTrack ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onQueueTrackClick(rTrack)
                                                    selectedBottomTab = null
                                                }
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(LightSurfaceElevated),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (rTrack.coverUrl.isNotBlank()) {
                                                    AsyncImage(
                                                        model = YouTubeEngine.upgradeThumbnailUrl(rTrack.coverUrl, isEconomyMode),
                                                        contentDescription = rTrack.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rTrack.title,
                                                    color = LightTextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = rTrack.artist,
                                                    color = LightTextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = LightTextPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
