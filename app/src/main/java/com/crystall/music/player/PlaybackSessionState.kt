package com.crystall.music.player

import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Track
import com.google.gson.Gson

internal data class PlaybackSessionState(
    val queue: List<Track> = emptyList(),
    val currentTrack: Track? = null,
    val positionMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: Int = 0,
    val playbackSpeed: Float = 1f,
    val isEndlessRadioEnabled: Boolean = true
)

internal object PlaybackSessionStateCodec {
    private const val MAX_QUEUE_SIZE = 1000
    private const val MAX_POSITION_MS = 24L * 60 * 60 * 1000
    private val gson = Gson()

    fun encode(state: PlaybackSessionState): String = gson.toJson(
        state.copy(
            queue = state.queue.take(MAX_QUEUE_SIZE).mapNotNull(::safeTrack),
            currentTrack = safeTrack(state.currentTrack)
        )
    )

    fun decode(json: String?): PlaybackSessionState? {
        if (json.isNullOrBlank()) return null
        return try {
            val decoded = gson.fromJson(json, PlaybackSessionState::class.java) ?: return null
            PlaybackSessionState(
                queue = decoded.queue.orEmpty().take(MAX_QUEUE_SIZE).mapNotNull(::safeTrack),
                currentTrack = safeTrack(decoded.currentTrack),
                positionMs = decoded.positionMs.coerceIn(0L, MAX_POSITION_MS),
                isShuffle = decoded.isShuffle,
                repeatMode = decoded.repeatMode.coerceIn(0, 2),
                playbackSpeed = decoded.playbackSpeed.takeIf { it.isFinite() && it in 0.25f..4f } ?: 1f,
                isEndlessRadioEnabled = decoded.isEndlessRadioEnabled
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun safeTrack(track: Track?): Track? {
        if (track == null || track.id.isNullOrBlank() || track.title.isNullOrBlank() || track.artist.isNullOrBlank()) {
            return null
        }
        return Track(
            id = track.id,
            title = track.title,
            artist = track.artist,
            durationMs = track.durationMs.coerceAtLeast(0L),
            coverUrl = track.coverUrl.orEmpty(),
            streamUrl = null,
            localPath = track.localPath,
            source = track.source ?: AudioSource.YOUTUBE,
            isDownloaded = track.isDownloaded,
            waveformUrl = track.waveformUrl,
            addedAt = track.addedAt
        )
    }
}
