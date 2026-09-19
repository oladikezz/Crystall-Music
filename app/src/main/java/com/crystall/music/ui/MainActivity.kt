package com.crystall.music.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.crystall.music.ui.components.FullPlayerSheet
import com.crystall.music.ui.components.MiniPlayer
import com.crystall.music.ui.components.PlayerOptionsSheet
import com.crystall.music.ui.components.TastePickerSheet
import com.crystall.music.ui.screens.*
import com.crystall.music.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleIncomingIntent(intent)

        setContent {
            CrystallMusicTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.handleSharedUrl(sharedText)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val trendingTracks by viewModel.trendingTracks.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isLoadingTrending by viewModel.isLoadingTrending.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()
    val resolveResult by viewModel.resolveResult.collectAsState()
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsState()
    val favoriteArtists by viewModel.favoriteArtists.collectAsState()
    val showTastePicker by viewModel.showTastePicker.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()

    val playerManager = viewModel.playerManager
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val currentPosition by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()
    val queue by playerManager.queue.collectAsState()
    val isShuffle by playerManager.isShuffle.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val sleepTimerRemainingMs by playerManager.sleepTimerRemainingMs.collectAsState()
    val isSleepTimerEndOfTrack by playerManager.isSleepTimerEndOfTrack.collectAsState()
    val isEndlessRadioEnabled by playerManager.isEndlessRadioEnabled.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()

    var isSplashVisible by remember { mutableStateOf(true) }
    var showPlayerOptions by remember { mutableStateOf(false) }

    val downloadStates by viewModel.downloadManager.downloadStates.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Main Screen Content with smooth crossfade
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) togetherWith
                            fadeOut(animationSpec = androidx.compose.animation.core.tween(220))
                },
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        trendingTracks = trendingTracks,
                        recentTracks = recentTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        downloadStates = downloadStates,
                        favoriteIds = favoriteIds,
                        isLoading = isLoadingTrending,
                        hasCustomTastes = favoriteArtists.isNotEmpty(),
                        onTrackClick = { track, q ->
                            playerManager.playTrack(track, q)
                            viewModel.showFullPlayer()
                        },
                        onDownloadClick = { track -> viewModel.downloadTrack(track) },
                        onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                        onNavigateToSearch = { viewModel.setTab(1) },
                        onNavigateToImport = { viewModel.setTab(2) },
                        onOpenTastePicker = { viewModel.openTastePicker() }
                    )
                    1 -> SearchScreen(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        downloadStates = downloadStates,
                        favoriteIds = favoriteIds,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        onSearch = { q -> viewModel.search(q) },
                        onTrackClick = { track, q ->
                            playerManager.playTrack(track, q)
                            viewModel.showFullPlayer()
                        },
                        onDownloadClick = { track -> viewModel.downloadTrack(track) },
                        onFavoriteClick = { track -> viewModel.toggleFavorite(track) }
                    )
                    2 -> ImportScreen(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        downloadStates = downloadStates,
                        favoriteIds = favoriteIds,
                        isResolving = isResolving,
                        resolveResult = resolveResult,
                        onResolveUrl = { url -> viewModel.resolveUrl(url) },
                        onSavePlaylist = { pl, tracks -> viewModel.savePlaylist(pl, tracks) },
                        onDownloadAllTracks = { tracks -> viewModel.downloadAllTracks(tracks) },
                        onTrackClick = { track, q ->
                            playerManager.playTrack(track, q)
                            viewModel.showFullPlayer()
                        },
                        onDownloadClick = { track -> viewModel.downloadTrack(track) },
                        onFavoriteClick = { track -> viewModel.toggleFavorite(track) }
                    )
                    3 -> LibraryScreen(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        downloadedTracks = downloadedTracks,
                        favoriteTracks = favoriteTracks,
                        playlists = playlists,
                        downloadStates = downloadStates,
                        favoriteIds = favoriteIds,
                        onTrackClick = { track, q ->
                            playerManager.playTrack(track, q)
                            viewModel.showFullPlayer()
                        },
                        onDownloadClick = { track -> viewModel.downloadTrack(track) },
                        onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                        onDeleteDownloadClick = { track -> viewModel.deleteDownload(track) },
                        onPlaylistClick = { pl -> viewModel.openPlaylist(pl) },
                        onDeletePlaylistClick = { pl -> viewModel.deletePlaylist(pl) }
                    )
                }
            }
        }

        // Bottom Overlay (MiniPlayer + Navigation Bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Mini Player if a track is active and full player is not open
            AnimatedVisibility(
                visible = currentTrack != null && !isFullPlayerVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                currentTrack?.let { track ->
                    MiniPlayer(
                        track = track,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        positionMs = currentPosition,
                        durationMs = duration,
                        isFavorite = favoriteIds.contains(track.id),
                        onPlayerClick = { viewModel.showFullPlayer() },
                        onPlayPauseClick = { playerManager.togglePlayPause() },
                        onNextClick = { playerManager.skipNext() },
                        onFavoriteClick = { viewModel.toggleFavorite(track) },
                        onCloseClick = { playerManager.stopAndClear() }
                    )
                }
            }

            // iOS 27 Floating Dynamic Island Tab Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color.Black.copy(alpha = 0.7f)
                        ),
                    shape = RoundedCornerShape(32.dp),
                    color = IosDynamicIsland,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x4DFFFFFF),
                                Color(0x1AFFFFFF)
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val items = listOf(
                            Triple(0, "Слушать", if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home),
                            Triple(1, "Поиск", Icons.Default.Search),
                            Triple(2, "Импорт", if (currentTab == 2) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline),
                            Triple(3, "Медиатека", if (currentTab == 3) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic)
                        )

                        for ((tabIndex, label, icon) in items) {
                            val isSelected = currentTab == tabIndex
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.06f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "tab_scale_$tabIndex"
                            )

                            Box(
                                modifier = Modifier
                                    .scale(scale)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        if (isSelected) Color(0x33FFFFFF) else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 0.5.dp else 0.dp,
                                        color = if (isSelected) Color(0x40FFFFFF) else Color.Transparent,
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .clickable { viewModel.setTab(tabIndex) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) Color.White else IosTextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Player Modal Screen
        // We keep a remembered snapshot of the last non-null track so the exit
        // animation can complete without crashing on currentTrack!!.
        val lastTrack = remember { mutableStateOf(currentTrack) }
        if (currentTrack != null) lastTrack.value = currentTrack

        AnimatedVisibility(
            visible = isFullPlayerVisible && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val track = lastTrack.value ?: return@AnimatedVisibility
            FullPlayerSheet(
                track = track,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                positionMs = currentPosition,
                durationMs = duration,
                isFavorite = favoriteIds.contains(track.id),
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                queue = queue,
                downloadProgress = downloadStates[track.id],
                lyricsResult = currentLyrics,
                isLoadingLyrics = isLoadingLyrics,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                isSleepTimerEndOfTrack = isSleepTimerEndOfTrack,
                onOpenPlayerOptions = { showPlayerOptions = true },
                onDismiss = { viewModel.hideFullPlayer() },
                onPlayPauseClick = { playerManager.togglePlayPause() },
                onNextClick = { playerManager.skipNext() },
                onPreviousClick = { playerManager.skipPrevious() },
                onSeekTo = { pos -> playerManager.seekTo(pos) },
                onFavoriteClick = { viewModel.toggleFavorite(track) },
                onShuffleClick = { playerManager.toggleShuffle() },
                onRepeatClick = { playerManager.cycleRepeatMode() },
                onDownloadClick = { viewModel.downloadTrack(track) },
                onQueueTrackClick = { qTrack -> playerManager.playTrack(qTrack) },
                onStopPlayback = {
                    playerManager.stopAndClear()
                    viewModel.hideFullPlayer()
                }
            )
        }

        // Playlist Detail Bottom Sheet
        if (selectedPlaylist != null) {
            PlaylistDetailSheet(
                playlist = selectedPlaylist!!,
                tracks = selectedPlaylistTracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                downloadStates = downloadStates,
                favoriteIds = favoriteIds,
                onDismiss = { viewModel.closePlaylistDetail() },
                onTrackClick = { track, q ->
                    playerManager.playTrack(track, q)
                    viewModel.showFullPlayer()
                },
                onDownloadClick = { track -> viewModel.downloadTrack(track) },
                onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                onDownloadAll = { tracks -> viewModel.downloadAllTracks(tracks) }
            )
        }

        // Taste Picker Modal Sheet
        AnimatedVisibility(
            visible = showTastePicker,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            TastePickerSheet(
                initialSelectedArtists = favoriteArtists,
                onDismiss = { viewModel.dismissTastePicker(skipped = false) },
                onSkip = { viewModel.dismissTastePicker(skipped = true) },
                onSave = { artists -> viewModel.saveFavoriteArtists(artists) }
            )
        }

        // Player Options Modal Sheet (Sleep Timer, Playback Speed, Endless Radio)
        AnimatedVisibility(
            visible = showPlayerOptions,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
        ) {
            PlayerOptionsSheet(
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                isSleepTimerEndOfTrack = isSleepTimerEndOfTrack,
                isEndlessRadioEnabled = isEndlessRadioEnabled,
                playbackSpeed = playbackSpeed,
                onSetSleepTimer = { minutes -> playerManager.setSleepTimer(minutes) },
                onSetSleepTimerEndOfTrack = { playerManager.setSleepTimerUntilEndOfTrack() },
                onCancelSleepTimer = { playerManager.cancelSleepTimer() },
                onToggleEndlessRadio = { playerManager.toggleEndlessRadio() },
                onSetPlaybackSpeed = { speed -> playerManager.setPlaybackSpeed(speed) },
                onDismiss = { showPlayerOptions = false }
            )
        }

        // Liquid Glass Animated Launch / Splash Screen
        AnimatedVisibility(
            visible = isSplashVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
        ) {
            SplashScreen(
                onFinished = { isSplashVisible = false }
            )
        }
    }
}
