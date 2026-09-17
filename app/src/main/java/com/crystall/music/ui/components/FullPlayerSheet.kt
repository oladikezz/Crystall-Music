package com.crystall.music.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.DownloadProgress
import com.crystall.music.engine.LyricsResult
import com.crystall.music.engine.YouTubeEngine
import com.crystall.music.ui.theme.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    track: Track,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    isFavorite: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    queue: List<Track>,
    downloadProgress: DownloadProgress?,
    lyricsResult: LyricsResult? = null,
    isLoadingLyrics: Boolean = false,
    onDismiss: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onFavoriteClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onQueueTrackClick: (Track) -> Unit,
    onStopPlayback: () -> Unit = {}
) {
    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val upgradedCoverUrl = remember(track.coverUrl) {
        YouTubeEngine.upgradeThumbnailUrl(track.coverUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF161622),
                        Color(0xFF0C0C12),
                        Color(0xFF060608)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // iOS Dynamic Island Grabber Pill
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x55FFFFFF))
                    .clickable { onDismiss() }
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Minimal Liquid Glass Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Minimal glass pill
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x28FFFFFF))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Lyrics toggle
                    IconButton(
                        onClick = { showLyrics = !showLyrics },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (showLyrics) Color(0x33FFFFFF) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) Color.White else Color(0x88FFFFFF),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Queue button
                    IconButton(onClick = { showQueueSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Interactive Center Area: Artwork OR Synced Lyrics
            Crossfade(
                targetState = showLyrics,
                animationSpec = tween(300),
                label = "cover_lyrics_crossfade"
            ) { lyricsActive ->
                if (lyricsActive) {
                    // Real-time Synced Karaoke Lyrics View
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0x18FFFFFF))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(Color(0x4DFFFFFF), Color(0x12FFFFFF))
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingLyrics) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
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
                                contentPadding = PaddingValues(vertical = 36.dp)
                            ) {
                                itemsIndexed(synced) { idx, line ->
                                    val isCurrent = idx == activeIndex
                                    Text(
                                        text = line.text,
                                        color = if (isCurrent) Color.White else Color(0x55FFFFFF),
                                        fontSize = if (isCurrent) 20.sp else 15.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSeekTo(line.timeMs)
                                            }
                                            .padding(vertical = 4.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        } else if (lyricsResult != null && !lyricsResult.plainLyrics.isNullOrBlank()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                item {
                                    Text(
                                        text = lyricsResult.plainLyrics,
                                        color = Color.White.copy(alpha = 0.88f),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Слова песни недоступны",
                                color = Color(0x77FFFFFF),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // Ultra HD Artwork with Liquid Glass Glow
                    Box(
                        modifier = Modifier.size(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (upgradedCoverUrl.isNotBlank()) {
                            AsyncImage(
                                model = upgradedCoverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(280.dp)
                                    .blur(48.dp)
                                    .alpha(0.4f),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(290.dp)
                                .shadow(32.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.75f))
                                .clip(RoundedCornerShape(32.dp))
                                .background(SurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0x55FFFFFF), Color(0x18FFFFFF))
                                    ),
                                    shape = RoundedCornerShape(32.dp)
                                ),
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
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(90.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Title & Artist with Equalizer and Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = track.title,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee()
                        )
                        if (isPlaying) {
                            EqualizerVisualizer(
                                isPlaying = true,
                                barCount = 4,
                                barWidth = 3.dp,
                                maxHeight = 16.dp,
                                barSpacing = 2.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        color = TextSecondary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Pure Liquid Glass Waveform Scrubber
            val totalDuration: Long = if (durationMs > 0L) durationMs else track.durationMs
            val progress: Float = if (totalDuration > 0L) (positionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f

            WaveformScrubber(
                progress = progress,
                onSeek = { newFraction ->
                    if (totalDuration > 0L) {
                        onSeekTo((newFraction * totalDuration).toLong())
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(positionMs),
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (totalDuration > 0L) formatMs(totalDuration) else "--:--",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Controls Row (Shuffle, Prev, Big Play/Pause, Next, Repeat)
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
                        tint = if (isShuffle) Color.White else TextMuted,
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
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Main Pure White Apple Music Play/Pause Button
                var isPlayPressed by remember { mutableStateOf(false) }
                val playButtonScale by animateFloatAsState(
                    targetValue = if (isPlayPressed) 0.86f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "playButtonScale"
                )

                Box(
                    modifier = Modifier
                        .scale(playButtonScale)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(20.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            isPlayPressed = true
                            onPlayPauseClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    LaunchedEffect(isPlayPressed) {
                        if (isPlayPressed) {
                            kotlinx.coroutines.delay(120)
                            isPlayPressed = false
                        }
                    }
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF101014),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF101014),
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
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
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
                        tint = if (repeatMode > 0) Color.White else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Liquid Glass Action Dock (Lyrics, Download, Queue)
            val isDownloaded = track.isDownloaded || downloadProgress?.isCompleted == true
            val isDownloading = downloadProgress?.isDownloading == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0x18FFFFFF))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0x38FFFFFF), Color(0x10FFFFFF))
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Lyrics toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (showLyrics) Color(0x38FFFFFF) else Color.Transparent)
                        .clickable { showLyrics = !showLyrics }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) Color.White else Color(0x99FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                        if (showLyrics) {
                            Text(
                                text = "Текст",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Download icon action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                isDownloaded -> GreenSuccess.copy(alpha = 0.22f)
                                isDownloading -> Color(0x28FFFFFF)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onDownloadClick() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { (downloadProgress?.progressPercent ?: 0) / 100f },
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "${downloadProgress?.progressPercent ?: 0}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (isDownloaded) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GreenSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Офлайн",
                                color = GreenSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = "Download",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Queue button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Queue Sheet
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                containerColor = SurfaceElevated
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Очередь воспроизведения (${queue.size})",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                onStopPlayback()
                                showQueueSheet = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                tint = Color(0xFFFF453A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Выключить", color = Color(0xFFFF453A), fontSize = 13.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        itemsIndexed(queue) { _, qTrack ->
                            TrackItem(
                                track = qTrack,
                                isCurrentTrack = qTrack.id == track.id,
                                isPlaying = isPlaying && qTrack.id == track.id,
                                onTrackClick = {
                                    onQueueTrackClick(qTrack)
                                    showQueueSheet = false
                                },
                                onDownloadClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaveformScrubber(
    progress: Float,
    onSeek: (Float) -> Unit
) {
    // Generate 36 distinct bar heights resembling a clean audio waveform
    val barCount = 36
    val barHeights = remember {
        List(barCount) { idx ->
            val v = (sin(idx * 0.45) * 0.4 + 0.6).toFloat()
            val noise = if (idx % 3 == 0) 0.2f else if (idx % 2 == 0) -0.15f else 0.05f
            (v + noise).coerceIn(0.2f, 1.0f)
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val effectiveProgress = if (isDragging) dragProgress else progress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragProgress = fraction
                        onSeek(fraction)
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragProgress = fraction
                        onSeek(fraction)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            barHeights.forEachIndexed { i, hRatio ->
                val barFraction = (i.toFloat() + 0.5f) / barCount.toFloat()
                val isPlayed = barFraction <= effectiveProgress
                val isCurrent = (barFraction - effectiveProgress).let { it >= -0.025f && it <= 0.025f }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp)
                        .fillMaxHeight(hRatio)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                isCurrent -> Color.White
                                isPlayed -> Color(0xEEFFFFFF)
                                else -> Color(0x30FFFFFF)
                            }
                        )
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}
