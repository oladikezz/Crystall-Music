package com.crystall.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crystall.music.data.db.MusicDatabaseHelper
import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.MusicDownloadManager
import com.crystall.music.engine.LyricsEngine
import com.crystall.music.engine.LyricsResult
import com.crystall.music.engine.ResolveResult
import com.crystall.music.engine.SoundCloudEngine
import com.crystall.music.engine.UniversalMusicResolver
import com.crystall.music.engine.YouTubeEngine
import com.crystall.music.player.MusicPlayerManager
import com.crystall.music.ui.i18n.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val playerManager = MusicPlayerManager.getInstance(application)
    val downloadManager = MusicDownloadManager.getInstance(application)
    private val dbHelper = MusicDatabaseHelper.getInstance(application)

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _trendingTracks = MutableStateFlow<List<Track>>(emptyList())
    val trendingTracks: StateFlow<List<Track>> = _trendingTracks.asStateFlow()

    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<Track>>(emptyList())
    val downloadedTracks: StateFlow<List<Track>> = _downloadedTracks.asStateFlow()

    private val _favoriteTracks = MutableStateFlow<List<Track>>(emptyList())
    val favoriteTracks: StateFlow<List<Track>> = _favoriteTracks.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val trackSearch = TrackSearch(viewModelScope, YouTubeEngine::searchOrThrow)
    val searchQuery = trackSearch.query
    val searchResults = trackSearch.results
    val isSearching = trackSearch.isSearching
    val searchFailed = trackSearch.failed
    val hasSearched = trackSearch.hasSearched

    private val _isLoadingTrending = MutableStateFlow(false)
    val isLoadingTrending: StateFlow<Boolean> = _isLoadingTrending.asStateFlow()

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _resolveResult = MutableStateFlow<ResolveResult?>(null)
    val resolveResult: StateFlow<ResolveResult?> = _resolveResult.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    private val _favoriteArtists = MutableStateFlow<List<String>>(emptyList())
    val favoriteArtists: StateFlow<List<String>> = _favoriteArtists.asStateFlow()

    private val _showTastePicker = MutableStateFlow(false)
    val showTastePicker: StateFlow<Boolean> = _showTastePicker.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LyricsResult?>(null)
    val currentLyrics: StateFlow<LyricsResult?> = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylistTracks.asStateFlow()

    private val _isSyncingPlaylist = MutableStateFlow(false)
    val isSyncingPlaylist: StateFlow<Boolean> = _isSyncingPlaylist.asStateFlow()

    private val _syncMessage = MutableSharedFlow<String>()
    val syncMessage: SharedFlow<String> = _syncMessage.asSharedFlow()

    private val _selectedMood = MutableStateFlow("Все")
    val selectedMood: StateFlow<String> = _selectedMood.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.fromCode(dbHelper.getAppLanguage()))
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _relatedTracks = MutableStateFlow<List<Track>>(emptyList())
    val relatedTracks: StateFlow<List<Track>> = _relatedTracks.asStateFlow()

    val isEconomyMode = playerManager.isEconomyMode
    val dislikedTrackIds = playerManager.dislikedTrackIds

    private var lyricsJob: Job? = null
    private var relatedJob: Job? = null
    private var recommendationsJob: Job? = null
    private val _blockedArtists = MutableStateFlow(dbHelper.getBlockedArtists())
    val blockedArtists = _blockedArtists.asStateFlow()

    private fun filterRecommendations(tracks: List<Track>): List<Track> = tracks.filter { track ->
        track.id !in dislikedTrackIds.value &&
            _blockedArtists.value.none { it.equals(track.artist, ignoreCase = true) }
    }

    fun hideArtist(artist: String) {
        if (artist.isBlank()) return
        _blockedArtists.value = _blockedArtists.value + artist
        _trendingTracks.value = filterRecommendations(_trendingTracks.value)
        _relatedTracks.value = filterRecommendations(_relatedTracks.value)
        viewModelScope.launch(Dispatchers.IO) { dbHelper.setBlockedArtists(_blockedArtists.value) }
    }

    fun resetHiddenArtists() {
        _blockedArtists.value = emptySet()
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.setBlockedArtists(emptySet())
            loadTrendingTracks()
        }
    }

    init {
        refreshLibraryData()
        loadFavoriteArtists()
        loadTrendingTracks()
        observeCurrentTrackForLyrics()
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.setAppLanguage(language.code)
        }
    }

    fun playMyWave() {
        viewModelScope.launch(Dispatchers.IO) {
            val favs = _favoriteArtists.value
            val tracks = if (favs.isNotEmpty()) {
                YouTubeEngine.getPersonalizedRecommendations(favs)
            } else {
                YouTubeEngine.getTrendingMusic()
            }
            val recommendations = filterRecommendations(tracks)
            if (recommendations.isNotEmpty()) {
                playerManager.playTrack(recommendations.first(), recommendations)
                playerManager.setEndlessRadio(true)
                showFullPlayer()
            }
        }
    }

    fun toggleEconomyMode() = playerManager.toggleEconomyMode()
    fun toggleDislike(track: Track) {
        playerManager.toggleDislike(track.id)
        _trendingTracks.value = filterRecommendations(_trendingTracks.value)
        _relatedTracks.value = filterRecommendations(_relatedTracks.value)
    }

    fun selectMood(mood: String) {
        _selectedMood.value = mood
        loadTrendingTracks()
    }

    private fun loadFavoriteArtists() {
        val artists = dbHelper.getFavoriteArtists()
        _favoriteArtists.value = artists
        if (!dbHelper.hasCompletedTasteOnboarding()) {
            _showTastePicker.value = true
        }
    }

    fun openTastePicker() {
        _showTastePicker.value = true
    }

    fun dismissTastePicker(skipped: Boolean = false) {
        _showTastePicker.value = false
        if (skipped) {
            dbHelper.setTasteOnboardingCompleted(true)
        }
    }

    fun saveFavoriteArtists(artists: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.saveFavoriteArtists(artists)
            dbHelper.setTasteOnboardingCompleted(true)
            _favoriteArtists.value = artists
            _showTastePicker.value = false
            loadTrendingTracks()
        }
    }

    private fun observeCurrentTrackForLyrics() {
        viewModelScope.launch {
            playerManager.currentTrack.collect { track ->
                lyricsJob?.cancel()
                relatedJob?.cancel()
                if (track == null) {
                    _currentLyrics.value = null
                    _isLoadingLyrics.value = false
                    _relatedTracks.value = emptyList()
                    return@collect
                }
                _isLoadingLyrics.value = true
                _currentLyrics.value = null
                lyricsJob = launch(Dispatchers.IO) {
                    val result = LyricsEngine.getLyrics(track.title, track.artist, track.id)
                    _currentLyrics.value = result
                    _isLoadingLyrics.value = false
                }
                relatedJob = launch(Dispatchers.IO) {
                    val results = YouTubeEngine.search("${track.artist} radio", songsOnly = true)
                    _relatedTracks.value = results.filter { it.id != track.id }.take(12)
                }
            }
        }
    }

    fun setTab(tab: Int) {
        _currentTab.value = tab
    }

    fun showFullPlayer() {
        _isFullPlayerVisible.value = true
    }

    fun hideFullPlayer() {
        _isFullPlayerVisible.value = false
    }

    fun refreshLibraryData() {
        viewModelScope.launch(Dispatchers.IO) {
            val dl = dbHelper.getDownloadedTracks()
            val favs = dbHelper.getFavoriteTracks()
            val pls = dbHelper.getAllPlaylists()
            val recents = dbHelper.getHistoryTracks()

            _downloadedTracks.value = dl
            _favoriteTracks.value = favs
            _favoriteIds.value = favs.map { it.id }.toSet()
            _playlists.value = pls
            _recentTracks.value = recents
        }
    }

    fun loadTrendingTracks() {
        recommendationsJob?.cancel()
        recommendationsJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoadingTrending.value = true
            try {
                val mood = _selectedMood.value
                val favs = _favoriteArtists.value
                val tracks = when {
                    mood != "Все" && !mood.equals("all", ignoreCase = true) -> YouTubeEngine.getMoodTracks(mood)
                    favs.isNotEmpty() -> YouTubeEngine.getPersonalizedRecommendations(favs)
                    else -> YouTubeEngine.getTrendingMusic()
                }
                _trendingTracks.value = filterRecommendations(tracks)
            } finally {
                _isLoadingTrending.value = false
            }
        }
    }

    fun search(query: String) = trackSearch.submit(query)
    fun retrySearch() = trackSearch.retry()

    fun resolveUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isResolving.value = true
            _resolveResult.value = null  // Clear previous result while loading
            val result = UniversalMusicResolver.resolve(url)
            _resolveResult.value = result
            _isResolving.value = false
        }
    }

    fun savePlaylist(playlist: Playlist, tracks: List<Track>) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.savePlaylist(playlist, tracks)
            refreshLibraryData()
        }
    }

    fun downloadTrack(track: Track) {
        downloadManager.downloadTrack(track) {
            refreshLibraryData()
        }
    }

    fun downloadAllTracks(tracks: List<Track>) {
        downloadManager.downloadPlaylist(tracks) {
            refreshLibraryData()
        }
    }

    fun deleteDownload(track: Track) {
        downloadManager.deleteDownload(track) { refreshLibraryData() }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNowFav = dbHelper.toggleFavorite(track)
            refreshLibraryData()
        }
    }

    fun openPlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            val tracks = dbHelper.getPlaylistTracks(playlist.id)
            _selectedPlaylist.value = playlist
            _selectedPlaylistTracks.value = tracks
        }
    }

    fun closePlaylistDetail() {
        _selectedPlaylist.value = null
        _selectedPlaylistTracks.value = emptyList()
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deletePlaylist(playlist.id)
            refreshLibraryData()
            closePlaylistDetail()
        }
    }

    fun renamePlaylist(playlist: Playlist, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.renamePlaylist(playlist.id, title.trim())
            _selectedPlaylist.value = playlist.copy(title = title.trim())
            refreshLibraryData()
        }
    }

    fun removeTrackFromPlaylist(playlist: Playlist, track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.removeTrackFromPlaylist(playlist.id, track.id)
            val tracks = dbHelper.getPlaylistTracks(playlist.id)
            _selectedPlaylistTracks.value = tracks
            _selectedPlaylist.value = playlist.copy(trackCount = tracks.size)
            refreshLibraryData()
        }
    }

    fun handleSharedUrl(url: String) {
        setTab(2) // Switch to Import tab
        resolveUrl(url)
    }

    fun syncPlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingPlaylist.value = true
            try {
                val syncTarget = playlist.sourceUrl
                    ?: if (playlist.id.startsWith("PL") || playlist.id.startsWith("VL") || playlist.id.startsWith("OLAK5uy_")) {
                        "https://music.youtube.com/playlist?list=${playlist.id}"
                    } else null

                if (syncTarget.isNullOrBlank()) {
                    _syncMessage.emit("Не найден источник плейлиста для синхронизации")
                    return@launch
                }

                val resolveResult = UniversalMusicResolver.resolve(syncTarget)
                if (resolveResult is ResolveResult.PlaylistResult) {
                    val (newAdded, total) = dbHelper.syncPlaylist(playlist.id, resolveResult.tracks)
                    val updatedTracks = dbHelper.getPlaylistTracks(playlist.id)
                    val updatedPlaylists = dbHelper.getAllPlaylists()
                    val currentSelected = updatedPlaylists.firstOrNull { it.id == playlist.id } ?: playlist

                    _selectedPlaylist.value = currentSelected
                    _selectedPlaylistTracks.value = updatedTracks
                    refreshLibraryData()

                    val msg = if (newAdded > 0) {
                        "Синхронизировано: добавлено новых треков: $newAdded (всего: $total)"
                    } else {
                        "Плейлист актуален (всего треков: $total)"
                    }
                    _syncMessage.emit(msg)
                } else if (resolveResult is ResolveResult.Error) {
                    _syncMessage.emit("Ошибка: ${resolveResult.message}")
                } else {
                    _syncMessage.emit("Не удалось получить треки из YouTube Music")
                }
            } catch (e: Exception) {
                _syncMessage.emit("Ошибка сети при синхронизации")
            } finally {
                _isSyncingPlaylist.value = false
            }
        }
    }
}
