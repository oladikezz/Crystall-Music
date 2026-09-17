package com.crystall.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crystall.music.data.db.MusicDatabaseHelper
import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track
import com.crystall.music.downloader.MusicDownloadManager
import com.crystall.music.engine.ResolveResult
import com.crystall.music.engine.SoundCloudEngine
import com.crystall.music.engine.UniversalMusicResolver
import com.crystall.music.engine.YouTubeEngine
import com.crystall.music.player.MusicPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isLoadingTrending = MutableStateFlow(false)
    val isLoadingTrending: StateFlow<Boolean> = _isLoadingTrending.asStateFlow()

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _resolveResult = MutableStateFlow<ResolveResult?>(null)
    val resolveResult: StateFlow<ResolveResult?> = _resolveResult.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylistTracks.asStateFlow()

    init {
        refreshLibraryData()
        loadTrendingTracks()
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

    private fun loadTrendingTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingTrending.value = true
            val ytTrending = YouTubeEngine.getTrendingMusic()
            _trendingTracks.value = ytTrending
            _isLoadingTrending.value = false
        }
    }

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            val list = YouTubeEngine.search(query)
            _searchResults.value = list
            _isSearching.value = false
        }
    }

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
        downloadManager.deleteDownload(track)
        refreshLibraryData()
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

    fun handleSharedUrl(url: String) {
        setTab(2) // Switch to Import tab
        resolveUrl(url)
    }
}
