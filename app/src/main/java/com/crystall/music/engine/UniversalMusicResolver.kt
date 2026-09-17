package com.crystall.music.engine

import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ResolveResult {
    data class PlaylistResult(val playlist: Playlist, val tracks: List<Track>) : ResolveResult()
    data class SingleTrackResult(val track: Track) : ResolveResult()
    data class Error(val message: String) : ResolveResult()
}

object UniversalMusicResolver {

    private fun isSoundCloudUrl(url: String): Boolean =
        url.contains("soundcloud.com", ignoreCase = true)

    suspend fun resolve(input: String): ResolveResult = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return@withContext ResolveResult.Error("Введите ссылку на трек или плейлист")
        }

        // 0. SoundCloud URL
        if (isSoundCloudUrl(trimmed)) {
            val scResult = SoundCloudEngine.resolveUrl(trimmed)
            if (scResult != null) {
                val (playlist, tracks) = scResult
                return@withContext if (playlist != null) {
                    ResolveResult.PlaylistResult(playlist, tracks)
                } else if (tracks.isNotEmpty()) {
                    ResolveResult.SingleTrackResult(tracks.first())
                } else {
                    ResolveResult.Error("Не удалось загрузить трек с SoundCloud")
                }
            }
            return@withContext ResolveResult.Error("Не удалось загрузить с SoundCloud. Проверьте ссылку.")
        }

        // 1. Check if YouTube Playlist URL
        val ytPlaylistId = YouTubeEngine.extractPlaylistId(trimmed)
        if (ytPlaylistId != null) {
            val ytResult = YouTubeEngine.resolvePlaylist(trimmed)
            if (ytResult != null) {
                return@withContext ResolveResult.PlaylistResult(ytResult.first, ytResult.second)
            }
        }

        // 2. Check if YouTube Video URL
        val ytVideoId = YouTubeEngine.extractVideoId(trimmed)
        if (ytVideoId != null) {
            // Fetch real metadata if possible, or create track
            val searchResults = YouTubeEngine.search(ytVideoId)
            val track = searchResults.firstOrNull() ?: Track(
                id = ytVideoId,
                title = "YouTube Music Track",
                artist = "YouTube Music",
                coverUrl = "https://i.ytimg.com/vi/$ytVideoId/hqdefault.jpg",
                source = AudioSource.YOUTUBE
            )
            return@withContext ResolveResult.SingleTrackResult(track)
        }

        // 3. Fallback: Search on YouTube Music
        val ytSearchResults = YouTubeEngine.search(trimmed)
        if (ytSearchResults.isNotEmpty()) {
            return@withContext ResolveResult.SingleTrackResult(ytSearchResults.first())
        }

        ResolveResult.Error("По этой ссылке ничего не найдено на YouTube Music. Проверьте правильность ссылки.")
    }

    suspend fun getStreamUrlForTrack(track: Track): String? {
        if (!track.localPath.isNullOrBlank()) {
            return track.localPath
        }
        if (!track.streamUrl.isNullOrBlank()) {
            return track.streamUrl
        }
        return when (track.source) {
            com.crystall.music.data.model.AudioSource.SOUNDCLOUD ->
                SoundCloudEngine.getStreamUrl(track)
            else ->
                YouTubeEngine.getAudioStreamUrl(track.id)
        }
    }
}
