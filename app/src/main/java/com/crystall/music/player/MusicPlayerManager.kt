package com.crystall.music.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.crystall.music.data.db.MusicDatabaseHelper
import com.crystall.music.data.model.Track
import com.crystall.music.engine.UniversalMusicResolver
import com.crystall.music.engine.YouTubeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class MusicPlayerManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dbHelper = MusicDatabaseHelper.getInstance(context)

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("com.google.android.youtube/21.02.35 (Linux; U; Android 11) gzip")
        .setConnectTimeoutMs(20000)
        .setReadTimeoutMs(30000)
        .setAllowCrossProtocolRedirects(true)

    private val upstreamDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(upstreamDataSourceFactory)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true // handle audio focus automatically
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    var mediaSession: MediaSession? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    // 0 = Repeat OFF, 1 = Repeat ALL, 2 = Repeat ONE
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    private val _isSleepTimerEndOfTrack = MutableStateFlow(false)
    val isSleepTimerEndOfTrack: StateFlow<Boolean> = _isSleepTimerEndOfTrack.asStateFlow()

    private val _isEndlessRadioEnabled = MutableStateFlow(true)
    val isEndlessRadioEnabled: StateFlow<Boolean> = _isEndlessRadioEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var progressTrackerJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        try {
            mediaSession = MediaSession.Builder(context, player).build()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                    startPlaybackService()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    onTrackEnded()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                error.printStackTrace()
                _isBuffering.value = false
            }
        })
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            context.startService(intent)
        } catch (_: Exception) {}
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        if (newQueue != null) {
            _queue.value = newQueue
        } else if (_queue.value.none { it.id == track.id }) {
            _queue.value = _queue.value + track
        }

        _currentTrack.value = track
        _isBuffering.value = true
        _currentPosition.value = 0L
        // Don't preset duration from track metadata — it may be 0 for YouTube tracks.
        // The real duration will be set once the player reports STATE_READY.
        _duration.value = 0L

        scope.launch {
            // Resolve the playable URI before touching the player so the current
            // track never gets interrupted by a stop() before the new one is ready.
            var playableUri: String? = track.localPath
            if (playableUri.isNullOrBlank()) {
                // Check if downloaded in DB
                val dbTrack = dbHelper.getTrack(track.id)
                if (dbTrack?.isDownloaded == true && !dbTrack.localPath.isNullOrBlank()) {
                    playableUri = dbTrack.localPath
                } else {
                    playableUri = UniversalMusicResolver.getStreamUrlForTrack(track)
                }
            }

            // Guard: if the user already switched to another track, abort.
            if (_currentTrack.value?.id != track.id) return@launch

            // Only record history once we know the track will actually play.
            dbHelper.addToHistory(track)

            if (playableUri.isNullOrBlank()) {
                _isBuffering.value = false
                return@launch
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setArtworkUri(if (track.coverUrl.isNotBlank()) Uri.parse(track.coverUrl) else null)
                .build()

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(playableUri)
                .setMediaMetadata(metadata)

            if (playableUri.startsWith("http")) {
                if (playableUri.contains("itag=251") || playableUri.contains("webm")) {
                    mediaItemBuilder.setMimeType(MimeTypes.AUDIO_WEBM)
                } else if (playableUri.contains("itag=18") || playableUri.contains("itag=140") || playableUri.contains("mime=audio%2Fmp4") || playableUri.contains("mime=video%2Fmp4")) {
                    mediaItemBuilder.setMimeType(MimeTypes.AUDIO_MP4)
                }
            }
            val mediaItem = mediaItemBuilder.build()

            withContext(Dispatchers.Main) {
                // Final guard on the main thread before we touch the player.
                if (_currentTrack.value?.id != track.id) return@withContext
                // Now it is safe to stop the previous playback and start the new one.
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
                player.play()
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            when (player.playbackState) {
                Player.STATE_ENDED -> {
                    player.seekTo(0)
                    player.play()
                }
                Player.STATE_IDLE -> {
                    // Player was stopped (e.g. after queue ended). Re-prepare if we have a track.
                    _currentTrack.value?.let { playTrack(it) }
                }
                else -> player.play()
            }
        }
    }

    fun stopAndClear() {
        try {
            player.stop()
            player.clearMediaItems()
        } catch (_: Exception) {}
        _currentTrack.value = null
        _queue.value = emptyList()
        _isPlaying.value = false
        _isBuffering.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        stopProgressTracker()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val current = _currentTrack.value ?: return

        val currentIndex = q.indexOfFirst { it.id == current.id }
        if (_isShuffle.value && q.size > 1) {
            val randomIdx = q.indices.filter { it != currentIndex }.random()
            playTrack(q[randomIdx])
        } else if (currentIndex != -1 && currentIndex + 1 < q.size) {
            playTrack(q[currentIndex + 1])
        } else if (_repeatMode.value == 1 && q.isNotEmpty()) {
            playTrack(q.first())
        }
    }

    fun skipPrevious() {
        if (player.currentPosition > 3000) {
            seekTo(0)
            return
        }
        val q = _queue.value
        if (q.isEmpty()) return
        val current = _currentTrack.value ?: return

        val currentIndex = q.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playTrack(q[currentIndex - 1])
        } else {
            seekTo(0)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _isSleepTimerEndOfTrack.value = false
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        val totalMs = minutes * 60 * 1000L
        sleepTimerJob = scope.launch(Dispatchers.Main) {
            var remaining = totalMs
            while (isActive && remaining > 0) {
                _sleepTimerRemainingMs.value = remaining
                // Fade out in last 10 seconds
                if (remaining <= 10000L) {
                    val vol = (remaining.toFloat() / 10000f).coerceIn(0f, 1f)
                    player.volume = vol
                } else {
                    player.volume = 1.0f
                }
                delay(1000)
                remaining -= 1000L
            }
            _sleepTimerRemainingMs.value = null
            player.pause()
            player.volume = 1.0f
        }
    }

    fun setSleepTimerUntilEndOfTrack() {
        sleepTimerJob?.cancel()
        _isSleepTimerEndOfTrack.value = true
        _sleepTimerRemainingMs.value = null
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = null
        _isSleepTimerEndOfTrack.value = false
        player.volume = 1.0f
    }

    fun toggleEndlessRadio() {
        _isEndlessRadioEnabled.value = !_isEndlessRadioEnabled.value
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.playbackParameters = PlaybackParameters(speed)
    }

    private fun onTrackEnded() {
        if (_isSleepTimerEndOfTrack.value) {
            _isSleepTimerEndOfTrack.value = false
            player.pause()
            player.seekTo(0)
            return
        }

        when (_repeatMode.value) {
            2 -> { // Repeat ONE
                player.seekTo(0)
                player.play()
            }
            else -> {
                val q = _queue.value
                val current = _currentTrack.value
                val currentIndex = if (current != null) q.indexOfFirst { it.id == current.id } else -1
                val hasNext = _isShuffle.value && q.size > 1
                    || currentIndex != -1 && currentIndex + 1 < q.size
                    || _repeatMode.value == 1 && q.isNotEmpty()

                if (hasNext) {
                    skipNext()
                } else if (_isEndlessRadioEnabled.value && current != null) {
                    // Smart Endless Radio: auto-discover similar tracks from artist/mood!
                    scope.launch {
                        try {
                            val recommendations = YouTubeEngine.search(current.artist, songsOnly = true)
                            val nextTracks = recommendations.filter { rec ->
                                rec.id != current.id && q.none { it.id == rec.id }
                            }
                            if (nextTracks.isNotEmpty()) {
                                val nextTrack = nextTracks.first()
                                withContext(Dispatchers.Main) {
                                    _queue.value = _queue.value + nextTracks.take(5)
                                    playTrack(nextTrack)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    _currentPosition.value = 0L
                                    player.seekTo(0)
                                    player.pause()
                                }
                            }
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                _currentPosition.value = 0L
                                player.seekTo(0)
                                player.pause()
                            }
                        }
                    }
                } else {
                    // End of queue — reset position so the UI shows 0:00 and
                    // the Play button can restart the track via togglePlayPause.
                    _currentPosition.value = 0L
                    player.seekTo(0)
                    player.pause()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                if (player.duration > 0) {
                    _duration.value = player.duration
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun release() {
        stopProgressTracker()
        cancelSleepTimer()
        mediaSession?.release()
        player.release()
    }
}
