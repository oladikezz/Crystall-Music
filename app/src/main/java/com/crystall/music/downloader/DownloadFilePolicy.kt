package com.crystall.music.downloader

import com.crystall.music.data.model.AudioSource
import java.io.File
import java.util.Locale

internal object DownloadFilePolicy {
    fun sanitizeTrackId(trackId: String): String = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    fun extension(source: AudioSource, streamUrl: String): String {
        val normalizedUrl = streamUrl.lowercase(Locale.ROOT)
        return when {
            source == AudioSource.SOUNDCLOUD -> "mp3"
            normalizedUrl.contains(".mp3") -> "mp3"
            normalizedUrl.contains("webm") || normalizedUrl.contains("itag=251") ||
                normalizedUrl.contains("itag=249") || normalizedUrl.contains("itag=250") -> "webm"
            else -> "m4a"
        }
    }

    fun targetFile(directory: File, trackId: String, source: AudioSource, streamUrl: String): File =
        File(directory, "${sanitizeTrackId(trackId)}.${extension(source, streamUrl)}")

    fun partialFile(targetFile: File): File = File(targetFile.parentFile, "${targetFile.name}.part")

    fun hasValidSize(actualBytes: Long, expectedBytes: Long): Boolean =
        actualBytes > 0 && (expectedBytes < 0 || actualBytes == expectedBytes)
}
