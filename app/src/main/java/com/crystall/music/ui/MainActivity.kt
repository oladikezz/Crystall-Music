package com.crystall.music.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.crystall.music.ui.components.FullPlayerSheet
import com.crystall.music.ui.components.LanguagePickerSheet
import com.crystall.music.ui.components.MiniPlayer
import com.crystall.music.ui.components.PlayerOptionsSheet
import com.crystall.music.ui.components.TastePickerSheet
import com.crystall.music.ui.i18n.LocalAppLanguage
import com.crystall.music.ui.i18n.LocalStrings
import com.crystall.music.ui.i18n.getStringsForLanguage
import com.crystall.music.ui.screens.*
import com.crystall.music.ui.theme.*
import com.crystall.music.downloader.DownloadStorageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val strings = remember(currentLanguage) { getStringsForLanguage(currentLanguage) }

            CompositionLocalProvider(
                LocalStrings provides strings,
                LocalAppLanguage provides currentLanguage
            ) {
                CrystallMusicTheme {
                    MainAppScreen(viewModel = viewModel)
                }
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
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val trendingTracks by viewModel.trendingTracks.collectAsStateWithLifecycle()
    val recentTracks by viewModel.recentTracks.collectAsStateWithLifecycle()
    val downloadedTracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchFailed by viewModel.searchFailed.collectAsStateWithLifecycle()
    val hasSearched by viewModel.hasSearched.collectAsStateWithLifecycle()
    val blockedArtists by viewModel.blockedArtists.collectAsStateWithLifecycle()
    val isLoadingTrending by viewModel.isLoadingTrending.collectAsStateWithLifecycle()
    val isResolving by viewModel.isResolving.collectAsStateWithLifecycle()
    val resolveResult by viewModel.resolveResult.collectAsStateWithLifecycle()
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsStateWithLifecycle()
    val favoriteArtists by viewModel.favoriteArtists.collectAsStateWithLifecycle()
    val showTastePicker by viewModel.showTastePicker.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsStateWithLifecycle()
    val selectedMood by viewModel.selectedMood.collectAsStateWithLifecycle()
    val isEconomyMode by viewModel.isEconomyMode.collectAsStateWithLifecycle()
    val dislikedTrackIds by viewModel.dislikedTrackIds.collectAsStateWithLifecycle()
    val relatedTracks by viewModel.relatedTracks.collectAsStateWithLifecycle()
    val isSyncingPlaylist by viewModel.isSyncingPlaylist.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.syncMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val playerManager = viewModel.playerManager
    val currentTrack by playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by playerManager.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    val isSleepTimerEndOfTrack by playerManager.isSleepTimerEndOfTrack.collectAsStateWithLifecycle()
    val isEndlessRadioEnabled by playerManager.isEndlessRadioEnabled.collectAsStateWithLifecycle()
    val playbackSpeed by playerManager.playbackSpeed.collectAsStateWithLifecycle()
    var showPlayerOptions by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val downloadStates by viewModel.downloadManager.downloadStates.collectAsStateWithLifecycle()
    var wifiOnlyDownloads by remember { mutableStateOf(viewModel.downloadManager.isWifiOnlyDownloads()) }
    var offlineStorage by remember { mutableStateOf<DownloadStorageSummary?>(null) }
    var downloadQueueTracks by remember { mutableStateOf<List<com.crystall.music.data.model.Track>>(emptyList()) }
    LaunchedEffect(currentTab, downloadedTracks) {
        if (currentTab == 3) {
            val snapshot = withContext(Dispatchers.IO) {
                viewModel.downloadManager.getStorageSummary() to viewModel.downloadManager.getRecoverableDownloads()
            }
            offlineStorage = snapshot.first
            downloadQueueTracks = snapshot.second
        }
    }

    // Android System Back Button Handler
    BackHandler(enabled = isFullPlayerVisible) {
        viewModel.hideFullPlayer()
    }
    BackHandler(enabled = !isFullPlayerVisible && showSettings) {
        showSettings = false
    }
    BackHandler(enabled = !isFullPlayerVisible && !showSettings && showPlayerOptions) {
        showPlayerOptions = false
    }
    BackHandler(enabled = !isFullPlayerVisible && !showSettings && !showPlayerOptions && showLanguagePicker) {
        showLanguagePicker = false
    }
    BackHandler(enabled = !isFullPlayerVisible && !showSettings && !showPlayerOptions && !showLanguagePicker && selectedPlaylist != null) {
        viewModel.closePlaylistDetail()
    }
    BackHandler(enabled = !isFullPlayerVisible && !showSettings && !showPlayerOptions && !showLanguagePicker && selectedPlaylist == null && showTastePicker) {
        viewModel.dismissTastePicker(skipped = true)
    }
    BackHandler(enabled = !isFullPlayerVisible && !showSettings && !showPlayerOptions && !showLanguagePicker && selectedPlaylist == null && !showTastePicker && currentTab != 0) {
        viewModel.setTab(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
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
                        selectedMood = selectedMood,
                        isEconomyMode = isEconomyMode,
                        onSelectMood = { mood -> viewModel.selectMood(mood) },
                        onToggleEconomyMode = { viewModel.toggleEconomyMode() },
                        onTrackClick = { track, q ->
                            playerManager.playTrack(track, q)
                            viewModel.showFullPlayer()
                        },
                        onDownloadClick = { track -> viewModel.downloadTrack(track) },
                        onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                        onNavigateToSearch = { viewModel.setTab(1) },
                        onNavigateToImport = { viewModel.setTab(2) },
                        onOpenTastePicker = { viewModel.openTastePicker() },
                        onOpenLanguagePicker = { showLanguagePicker = true },
                        onOpenSettings = { showSettings = true },
                        onPlayMyWave = { viewModel.playMyWave() }
                    )
                    1 -> SearchScreen(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        downloadStates = downloadStates,
                        favoriteIds = favoriteIds,
                        searchQuery = searchQuery,
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
                        onDeletePlaylistClick = { pl -> viewModel.deletePlaylist(pl) },
                        downloadQueueTracks = downloadQueueTracks,
                        offlineStorageUsedBytes = offlineStorage?.totalBytes,
                        offlineStorageFreeBytes = offlineStorage?.availableBytes,
                        wifiOnlyDownloads = wifiOnlyDownloads,
                        onWifiOnlyDownloadsChange = { enabled ->
                            wifiOnlyDownloads = enabled
                            viewModel.downloadManager.setWifiOnlyDownloads(enabled)
                        },
                        onCancelDownload = { viewModel.downloadManager.cancelDownload(it) },
                        onRetryDownload = { track, complete ->
                            viewModel.downloadManager.retryDownload(track) { success ->
                                viewModel.refreshLibraryData()
                                complete(success)
                            }
                        }
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
            // Mini Player (Isolated from root recompositions)
            MiniPlayerSection(
                playerManager = playerManager,
                isFullPlayerVisible = isFullPlayerVisible,
                favoriteIds = favoriteIds,
                onPlayerClick = { viewModel.showFullPlayer() },
                onFavoriteClick = { viewModel.toggleFavorite(it) }
            )

            // Clean Pure White Docked Bottom Navigation Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                color = LightSurface,
                border = androidx.compose.foundation.BorderStroke(
                    width = 0.5.dp,
                    color = LightBorder
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val strings = LocalStrings.current
                    val items = listOf(
                        Triple(0, strings.navHome, if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home),
                        Triple(1, strings.navSearch, if (currentTab == 1) Icons.Filled.Search else Icons.Outlined.Search),
                        Triple(2, strings.navImport, if (currentTab == 2) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline),
                        Triple(3, strings.navLibrary, if (currentTab == 3) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic)
                    )

                    for ((tabIndex, label, icon) in items) {
                        val isSelected = currentTab == tabIndex
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { viewModel.setTab(tabIndex) },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) LightTextPrimary else LightTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = label,
                                color = if (isSelected) LightTextPrimary else LightTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Full Player Modal Screen (Isolated from root recompositions with smooth spring physics)
        FullPlayerModalSection(
            viewModel = viewModel,
            playerManager = playerManager,
            isFullPlayerVisible = isFullPlayerVisible,
            favoriteIds = favoriteIds,
            dislikedTrackIds = dislikedTrackIds,
            onOpenPlayerOptions = { showPlayerOptions = true },
            onDismiss = { viewModel.hideFullPlayer() }
        )

        // Playlist Detail Bottom Sheet
        if (selectedPlaylist != null) {
            PlaylistDetailSheet(
                playlist = selectedPlaylist!!,
                tracks = selectedPlaylistTracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                downloadStates = downloadStates,
                favoriteIds = favoriteIds,
                isSyncing = isSyncingPlaylist,
                onSyncPlaylist = { pl -> viewModel.syncPlaylist(pl) },
                onDismiss = { viewModel.closePlaylistDetail() },
                onTrackClick = { track, q ->
                    playerManager.playTrack(track, q)
                    viewModel.showFullPlayer()
                },
                onDownloadClick = { track -> viewModel.downloadTrack(track) },
                onFavoriteClick = { track -> viewModel.toggleFavorite(track) },
                onDownloadAll = { tracks -> viewModel.downloadAllTracks(tracks) },
                onRenamePlaylist = { title -> selectedPlaylist?.let { viewModel.renamePlaylist(it, title) } },
                onRemoveTrack = { track -> selectedPlaylist?.let { viewModel.removeTrackFromPlaylist(it, track) } }
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

        // Player Options Modal Sheet (Native smooth bottom sheet)
        if (showPlayerOptions) {
            PlayerOptionsSheet(
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                isSleepTimerEndOfTrack = isSleepTimerEndOfTrack,
                isEndlessRadioEnabled = isEndlessRadioEnabled,
                playbackSpeed = playbackSpeed,
                isEconomyMode = isEconomyMode,
                onSetSleepTimer = { minutes -> playerManager.setSleepTimer(minutes) },
                onSetSleepTimerEndOfTrack = { playerManager.setSleepTimerUntilEndOfTrack() },
                onCancelSleepTimer = { playerManager.cancelSleepTimer() },
                onToggleEndlessRadio = { playerManager.toggleEndlessRadio() },
                onSetPlaybackSpeed = { speed -> playerManager.setPlaybackSpeed(speed) },
                onToggleEconomyMode = { viewModel.toggleEconomyMode() },
                onOpenLanguagePicker = { showLanguagePicker = true },
                onDismiss = { showPlayerOptions = false },
                onHideArtist = currentTrack?.let { track ->
                    { viewModel.hideArtist(track.artist); showPlayerOptions = false }
                }
            )
        }

        // Language Picker Modal Sheet
        AnimatedVisibility(
            visible = showLanguagePicker,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            LanguagePickerSheet(
                onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                onDismiss = { showLanguagePicker = false }
            )
        }

        // Dedicated Settings Screen
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            SettingsScreen(
                isEconomyMode = isEconomyMode,
                isEndlessRadioEnabled = isEndlessRadioEnabled,
                playbackSpeed = playbackSpeed,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                isSleepTimerEndOfTrack = isSleepTimerEndOfTrack,
                downloadedTracksCount = downloadedTracks.size,
                favoriteArtistsCount = favoriteArtists.size,
                onBackClick = { showSettings = false },
                onToggleEconomyMode = { viewModel.toggleEconomyMode() },
                onToggleEndlessRadio = { playerManager.toggleEndlessRadio() },
                onSetPlaybackSpeed = { speed -> playerManager.setPlaybackSpeed(speed) },
                onSetSleepTimer = { minutes -> playerManager.setSleepTimer(minutes) },
                onSetSleepTimerEndOfTrack = { playerManager.setSleepTimerUntilEndOfTrack() },
                onCancelSleepTimer = { playerManager.cancelSleepTimer() },
                onOpenLanguagePicker = { showLanguagePicker = true },
                onOpenTastePicker = { viewModel.openTastePicker() },
                onClearCache = { playerManager.clearCache() },
                onLessThisArtistClick = { currentTrack?.let { viewModel.hideArtist(it.artist) } },
                canHideCurrentArtist = currentTrack != null,
                currentArtistName = currentTrack?.artist,
                onResetHiddenArtistsClick = if (blockedArtists.isEmpty()) null else {
                    { viewModel.resetHiddenArtists() }
                }
            )
        }


    }
}

@Composable
private fun MiniPlayerSection(
    playerManager: com.crystall.music.player.MusicPlayerManager,
    isFullPlayerVisible: Boolean,
    favoriteIds: Set<String>,
    onPlayerClick: () -> Unit,
    onFavoriteClick: (com.crystall.music.data.model.Track) -> Unit
) {
    val currentTrack by playerManager.currentTrack.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = currentTrack != null && !isFullPlayerVisible,
        enter = slideInVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = tween(150)),
        exit = slideOutVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(150))
    ) {
        currentTrack?.let { track ->
            val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
            val isBuffering by playerManager.isBuffering.collectAsStateWithLifecycle()
            val currentPosition by playerManager.currentPosition.collectAsStateWithLifecycle()
            val duration by playerManager.duration.collectAsStateWithLifecycle()
            val isFav = remember(favoriteIds, track.id) { favoriteIds.contains(track.id) }

            MiniPlayer(
                track = track,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                positionMs = currentPosition,
                durationMs = duration,
                isFavorite = isFav,
                onPlayerClick = onPlayerClick,
                onPlayPauseClick = { playerManager.togglePlayPause() },
                onNextClick = { playerManager.skipNext() },
                onFavoriteClick = { onFavoriteClick(track) },
                onCloseClick = { playerManager.stopAndClear() }
            )
        }
    }
}

@Composable
private fun FullPlayerModalSection(
    viewModel: MainViewModel,
    playerManager: com.crystall.music.player.MusicPlayerManager,
    isFullPlayerVisible: Boolean,
    favoriteIds: Set<String>,
    dislikedTrackIds: Set<String>,
    onOpenPlayerOptions: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentTrack by playerManager.currentTrack.collectAsStateWithLifecycle()
    val lastTrack = remember { mutableStateOf(currentTrack) }
    if (currentTrack != null) lastTrack.value = currentTrack

    AnimatedVisibility(
        visible = isFullPlayerVisible && currentTrack != null,
        enter = slideInVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = tween(150)),
        exit = slideOutVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(150))
    ) {
        val track = lastTrack.value ?: return@AnimatedVisibility
        val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
        val isBuffering by playerManager.isBuffering.collectAsStateWithLifecycle()
        val currentPosition by playerManager.currentPosition.collectAsStateWithLifecycle()
        val duration by playerManager.duration.collectAsStateWithLifecycle()
        val queue by playerManager.queue.collectAsStateWithLifecycle()
        val isShuffle by playerManager.isShuffle.collectAsStateWithLifecycle()
        val repeatMode by playerManager.repeatMode.collectAsStateWithLifecycle()
        val sleepTimerRemainingMs by playerManager.sleepTimerRemainingMs.collectAsStateWithLifecycle()
        val isSleepTimerEndOfTrack by playerManager.isSleepTimerEndOfTrack.collectAsStateWithLifecycle()
        val isEndlessRadioEnabled by playerManager.isEndlessRadioEnabled.collectAsStateWithLifecycle()
        val isEconomyMode by viewModel.isEconomyMode.collectAsStateWithLifecycle()
        val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
        val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsStateWithLifecycle()
        val relatedTracks by viewModel.relatedTracks.collectAsStateWithLifecycle()
        val downloadStates by viewModel.downloadManager.downloadStates.collectAsStateWithLifecycle()

        FullPlayerSheet(
            track = track,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            positionMs = currentPosition,
            durationMs = duration,
            isFavorite = favoriteIds.contains(track.id),
            isDisliked = dislikedTrackIds.contains(track.id),
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            queue = queue,
            downloadProgress = downloadStates[track.id],
            lyricsResult = currentLyrics,
            isLoadingLyrics = isLoadingLyrics,
            relatedTracks = relatedTracks,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            isSleepTimerEndOfTrack = isSleepTimerEndOfTrack,
            isEndlessRadioEnabled = isEndlessRadioEnabled,
            isEconomyMode = isEconomyMode,
            onToggleEconomyMode = { viewModel.toggleEconomyMode() },
            onToggleEndlessRadio = { playerManager.toggleEndlessRadio() },
            onOpenPlayerOptions = onOpenPlayerOptions,
            onDismiss = onDismiss,
            onPlayPauseClick = { playerManager.togglePlayPause() },
            onNextClick = { playerManager.skipNext() },
            onPreviousClick = { playerManager.skipPrevious() },
            onSeekTo = { pos -> playerManager.seekTo(pos) },
            onFavoriteClick = { viewModel.toggleFavorite(track) },
            onDislikeClick = { viewModel.toggleDislike(track) },
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
}
