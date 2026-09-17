package com.crystall.music.data.model

enum class AudioSource {
    YOUTUBE,
    SOUNDCLOUD,
    LOCAL
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long = 0L,
    val coverUrl: String = "",
    val streamUrl: String? = null,
    val localPath: String? = null,
    val source: AudioSource = AudioSource.YOUTUBE,
    val isDownloaded: Boolean = false,
    val waveformUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    val durationFormatted: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val playableUri: String
        get() = localPath ?: streamUrl ?: ""
}

data class Playlist(
    val id: String,
    val title: String,
    val author: String = "",
    val sourceUrl: String? = null,
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val isImported: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
