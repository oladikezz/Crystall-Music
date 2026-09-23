package com.crystall.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.crystall.music.data.model.Track
import com.crystall.music.ui.theme.*

/**
 * 1:1 YouTube Music MiniPlayer
 * Features:
 * - Signature YouTube Music Red (#FF0000) progress scrubber at the top
 * - Authentic YtSurface (#212121) matte dark finish
 * - Compact 44x44dp square album cover with subtle rounded corners (6dp)
 * - YouTube Music title/artist typography with basicMarquee
 * - Thumbs up, Play/Pause, and Skip Next transport controls
 * - Horizontal swipe gesture to skip tracks
 */
@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    isFavorite: Boolean,
    onPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCloseClick: () -> Unit = {}
) {
    val totalDuration = if (durationMs > 0) durationMs else track.durationMs
    val progress = if (totalDuration > 0) (positionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = LinearEasing),
        label = "mini_progress"
    )

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.8f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(YtSurface)
            .border(0.5.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset < -70f) {
                            onNextClick()
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
            .clickable { onPlayerClick() }
    ) {
        // YouTube Music Signature Top Red Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(YtRed)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square Artwork (YouTube Music 1:1)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF141414)),
                contentAlignment = Alignment.Center
            ) {
                if (track.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = com.crystall.music.engine.YouTubeEngine.upgradeThumbnailUrl(track.coverUrl),
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = YtTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track info: Title and Artist
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.title,
                        color = YtTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isPlaying) {
                        EqualizerVisualizer(
                            isPlaying = true,
                            barCount = 3,
                            barWidth = 2.dp,
                            maxHeight = 11.dp,
                            barSpacing = 1.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    color = YtTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Thumbs Up / Like Button (YouTube Music 1:1)
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Like",
                    tint = if (isFavorite) Color.White else YtTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Play / Pause Button (YouTube Music 1:1)
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(40.dp)
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = YtTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Next Button (YouTube Music 1:1)
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = YtTextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
