package com.crystall.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.DownloadProgress
import com.crystall.music.ui.theme.*

/**
 * 1:1 YouTube Music Track Row
 */
@Composable
fun TrackItem(
    track: Track,
    isPlaying: Boolean = false,
    isCurrentTrack: Boolean = false,
    downloadProgress: DownloadProgress? = null,
    isFavorite: Boolean = false,
    onTrackClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrentTrack) Color(0x1AFFFFFF) else Color.Transparent)
            .clickable { onTrackClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // YouTube Music 48x48dp square artwork
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(YtSurface),
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
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        EqualizerVisualizer(
                            isPlaying = true,
                            barCount = 3,
                            barWidth = 2.5.dp,
                            maxHeight = 16.dp,
                            barSpacing = 1.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = if (isCurrentTrack) YtRed else YtTextPrimary,
                fontSize = 15.sp,
                fontWeight = if (isCurrentTrack) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.artist,
                    color = YtTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (track.durationMs > 0) {
                    Text(
                        text = " • ${track.durationFormatted}",
                        color = YtTextSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Thumbs Up / Favorite Action
        if (onFavoriteClick != null) {
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.White else YtTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Download status / button
        val isDownloading = downloadProgress?.isDownloading == true
        val isDownloaded = track.isDownloaded || downloadProgress?.isCompleted == true

        if (isDownloading) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { (downloadProgress?.progressPercent ?: 0) / 100f },
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        } else if (isDownloaded) {
            IconButton(
                onClick = { onDeleteClick?.invoke() ?: onDownloadClick() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = GreenSuccess,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Download",
                    tint = YtTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
