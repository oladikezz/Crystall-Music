package com.crystall.music.downloader

import android.content.Context
import android.os.Environment
import com.crystall.music.data.db.MusicDatabaseHelper
import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Track
import com.crystall.music.engine.SoundCloudEngine
import com.crystall.music.engine.UniversalMusicResolver
import com.crystall.music.engine.YouTubeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val trackId: String,
    val progressPercent: Int = 0,
    val isDownloading: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

class MusicDownloadManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: MusicDownloadManager? = null

        fun getInstance(context: Context): MusicDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: MusicDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dbHelper = MusicDatabaseHelper.getInstance(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadProgress>> = _downloadStates.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Boolean>()

    private val musicDir: File by lazy {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "music")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    fun downloadTrack(track: Track, onComplete: ((Boolean) -> Unit)? = null) {
        if (activeJobs[track.id] == true) return
        // If already downloaded and the file still exists on disk, skip.
        if (track.isDownloaded && !track.localPath.isNullOrBlank() && File(track.localPath).exists()) {
            onComplete?.invoke(true)
            return
        }
        activeJobs[track.id] = true

        updateProgress(track.id, DownloadProgress(trackId = track.id, isDownloading = true, progressPercent = 0))

        scope.launch {
            val success = performDownload(track)
            activeJobs.remove(track.id)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(success)
            }
        }
    }

    fun downloadPlaylist(tracks: List<Track>, onAllFinished: (() -> Unit)? = null) {
        scope.launch {
            for (track in tracks) {
                if (!track.isDownloaded && activeJobs[track.id] != true) {
                    performDownload(track)
                }
            }
            withContext(Dispatchers.Main) {
                onAllFinished?.invoke()
            }
        }
    }

    private suspend fun performDownload(track: Track): Boolean = withContext(Dispatchers.IO) {
        try {
            dbHelper.insertOrUpdateTrack(track)

            // For SoundCloud use the progressive-only download URL.
            // For YouTube use the standard resolver (already returns a direct URL).
            val streamUrl = when (track.source) {
                AudioSource.SOUNDCLOUD -> SoundCloudEngine.getDownloadUrl(track)
                else -> {
                    if (!track.localPath.isNullOrBlank()) track.localPath
                    else YouTubeEngine.getAudioStreamUrl(track.id)
                }
            }

            if (streamUrl == null) {
                updateProgress(track.id, DownloadProgress(trackId = track.id, errorMessage = "Не удалось получить аудиопоток"))
                return@withContext false
            }

            val sanitizedId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val extension = when {
                track.source == AudioSource.SOUNDCLOUD -> "mp3"
                streamUrl.contains(".mp3") -> "mp3"
                else -> "m4a"
            }
            val targetFile = File(musicDir, "${sanitizedId}.$extension")

            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                updateProgress(track.id, DownloadProgress(trackId = track.id, errorMessage = "Ошибка сети: ${response.code}"))
                return@withContext false
            }

            val body = response.body ?: run {
                updateProgress(track.id, DownloadProgress(trackId = track.id, errorMessage = "Пустой ответ сервера"))
                return@withContext false
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var lastPercent = -1

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                updateProgress(
                                    track.id,
                                    DownloadProgress(
                                        trackId = track.id,
                                        isDownloading = true,
                                        progressPercent = percent
                                    )
                                )
                            }
                        }
                    }
                    output.flush()
                }
            }

            // Save to database as downloaded
            dbHelper.markTrackDownloaded(track.id, targetFile.absolutePath)
            updateProgress(
                track.id,
                DownloadProgress(
                    trackId = track.id,
                    isDownloading = false,
                    isCompleted = true,
                    progressPercent = 100
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            updateProgress(
                track.id,
                DownloadProgress(
                    trackId = track.id,
                    isDownloading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка скачивания"
                )
            )
            false
        }
    }

    fun deleteDownload(track: Track) {
        scope.launch {
            track.localPath?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
            dbHelper.deleteDownloadedTrack(track.id)
            val current = _downloadStates.value.toMutableMap()
            current.remove(track.id)
            _downloadStates.value = current
        }
    }

    private fun updateProgress(trackId: String, progress: DownloadProgress) {
        val current = _downloadStates.value.toMutableMap()
        current[trackId] = progress
        _downloadStates.value = current
    }
}
