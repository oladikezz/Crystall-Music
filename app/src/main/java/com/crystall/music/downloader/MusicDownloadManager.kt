package com.crystall.music.downloader

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.system.Os
import com.crystall.music.data.db.MusicDatabaseHelper
import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Track
import com.crystall.music.engine.SoundCloudEngine
import com.crystall.music.engine.YouTubeEngine
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val trackId: String,
    val progressPercent: Int = 0,
    val isDownloading: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null,
    val isCanceled: Boolean = false,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long? = null
)

data class DownloadStorageSummary(
    val fileCount: Int,
    val totalBytes: Long,
    val availableBytes: Long
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

        private const val RECOVERY_TRACKS_KEY = "pending_tracks"
        private const val KEY_WIFI_ONLY = "wifi_only_downloads"
        private const val UNKNOWN_LENGTH_UPDATE_BYTES = 256 * 1024L
        private val AUDIO_EXTENSIONS = setOf("mp3", "webm", "m4a")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dbHelper = MusicDatabaseHelper.getInstance(context)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val stateLock = Any()
    private val activeJobs = mutableMapOf<String, Job>()
    private val activeCalls = mutableMapOf<String, Call>()
    private val deletingTrackIds = mutableSetOf<String>()
    private val recoveryLock = Any()
    private val recoveryPreferences: SharedPreferences =
        context.getSharedPreferences("music_download_recovery", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val recoverableTracks = loadRecoveryMetadata().associateByTo(mutableMapOf()) { it.id }

    private val _downloadStates = MutableStateFlow(
        recoverableTracks.mapValues { (trackId, _) ->
            DownloadProgress(
                trackId = trackId,
                errorMessage = "Загрузка прервана. Можно повторить",
                isCanceled = true
            )
        }
    )
    val downloadStates: StateFlow<Map<String, DownloadProgress>> = _downloadStates.asStateFlow()

    private val musicDir: File by lazy {
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "music")
    }

    fun downloadTrack(track: Track, onComplete: ((Boolean) -> Unit)? = null) {
        startDownload(track, onComplete, force = false)
    }

    fun downloadPlaylist(tracks: List<Track>, onAllFinished: (() -> Unit)? = null) {
        scope.launch {
            for (track in tracks.distinctBy { it.id }) {
                if (!currentCoroutineContext().isActive) break
                startDownload(track, onComplete = null, force = false)?.join()
            }
            withContext(Dispatchers.Main) {
                onAllFinished?.invoke()
            }
        }
    }

    fun cancelDownload(trackId: String): Boolean {
        synchronized(stateLock) {
            val active = activeJobs[trackId] ?: return false
            active.cancel(CancellationException("Download cancelled"))
            activeCalls[trackId]?.cancel()
        }
        val oldState = _downloadStates.value[trackId]
        updateProgress(
            trackId,
            (oldState ?: DownloadProgress(trackId)).copy(
                isDownloading = false,
                isCompleted = false,
                isCanceled = true,
                errorMessage = "Загрузка отменена"
            )
        )
        return true
    }

    fun retryDownload(track: Track, onComplete: ((Boolean) -> Unit)? = null) {
        val active = synchronized(stateLock) { activeJobs[track.id] }
        if (active == null) {
            startDownload(track, onComplete, force = true)
            return
        }

        cancelDownload(track.id)
        scope.launch {
            active.join()
            val retry = startDownload(track, onComplete, force = true)
            retry?.join()
        }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        recoveryPreferences.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply()
    }

    fun isWifiOnlyDownloads(): Boolean = recoveryPreferences.getBoolean(KEY_WIFI_ONLY, false)

    fun getRecoverableDownloads(): List<Track> = synchronized(recoveryLock) {
        recoverableTracks.values.toList()
    }

    fun getStorageSummary(): DownloadStorageSummary {
        val audioFiles = musicDir.listFiles()
            ?.filter { file ->
                file.isFile && AUDIO_EXTENSIONS.any { file.name.endsWith(".$it", ignoreCase = true) }
            }
            .orEmpty()
        return DownloadStorageSummary(
            fileCount = audioFiles.size,
            totalBytes = audioFiles.sumOf { it.length() },
            availableBytes = musicDir.usableSpace
        )
    }

    fun deleteDownload(track: Track) {
        val active = synchronized(stateLock) {
            if (!deletingTrackIds.add(track.id)) return
            val running = activeJobs[track.id]
            running?.cancel(CancellationException("Download deleted"))
            activeCalls[track.id]?.cancel()
            running
        }

        scope.launch {
            try {
                active?.join()
                withContext(Dispatchers.IO) {
                    track.localPath?.let { File(it).delete() }
                    deleteTrackFiles(track.id)
                    dbHelper.deleteDownloadedTrack(track.id)
                    removeRecoveryMetadata(track.id)
                }
                _downloadStates.update { it - track.id }
            } finally {
                synchronized(stateLock) { deletingTrackIds.remove(track.id) }
            }
        }
    }

    private fun startDownload(
        track: Track,
        onComplete: ((Boolean) -> Unit)?,
        force: Boolean
    ): Job? {
        if (!force && track.isDownloaded && !track.localPath.isNullOrBlank() && File(track.localPath).isFile) {
            onComplete?.invoke(true)
            return null
        }

        val job = synchronized(stateLock) {
            if (track.id in deletingTrackIds) return null
            activeJobs[track.id]?.let { return it }

            scope.launch(start = CoroutineStart.LAZY) {
                var success = false
                try {
                    persistRecoveryMetadata(track)
                    success = performDownload(track)
                } catch (cancelled: CancellationException) {
                    val current = _downloadStates.value[track.id] ?: DownloadProgress(track.id)
                    updateProgress(
                        track.id,
                        current.copy(
                            isDownloading = false,
                            isCompleted = false,
                            isCanceled = true,
                            errorMessage = "Загрузка отменена"
                        )
                    )
                    throw cancelled
                } catch (error: Exception) {
                    val current = _downloadStates.value[track.id] ?: DownloadProgress(track.id)
                    updateProgress(
                        track.id,
                        current.copy(
                            isDownloading = false,
                            isCompleted = false,
                            isCanceled = false,
                            errorMessage = error.localizedMessage ?: "Ошибка скачивания"
                        )
                    )
                } finally {
                    synchronized(stateLock) {
                        if (activeJobs[track.id] === currentCoroutineContext()[Job]) {
                            activeJobs.remove(track.id)
                        }
                        activeCalls.remove(track.id)
                    }
                    if (onComplete != null) {
                        withContext(NonCancellable + Dispatchers.Main) {
                            runCatching { onComplete(success) }
                        }
                    }
                }
            }.also { activeJobs[track.id] = it }
        }

        updateProgress(track.id, DownloadProgress(trackId = track.id, isDownloading = true))
        job.start()
        return job
    }

    private suspend fun performDownload(track: Track): Boolean {
        var partFile: File? = null
        var completed = false
        try {
            dbHelper.insertOrUpdateTrack(track)
            val streamUrl = when (track.source) {
                AudioSource.SOUNDCLOUD -> SoundCloudEngine.getDownloadUrl(track)
                else -> if (!track.localPath.isNullOrBlank()) {
                    track.localPath
                } else {
                    YouTubeEngine.getAudioStreamUrl(track.id, dbHelper.isEconomyMode())
                }
            } ?: throw IOException("Не удалось получить аудиопоток")

            checkWifiPolicy()
            val targetFile = DownloadFilePolicy.targetFile(musicDir, track.id, track.source, streamUrl)
            val partialFile = DownloadFilePolicy.partialFile(targetFile)
            partFile = partialFile
            if (!musicDir.exists() && !musicDir.mkdirs() && !musicDir.isDirectory) {
                throw IOException("Не удалось создать папку для загрузок")
            }
            if (partialFile.exists() && !partialFile.delete()) {
                throw IOException("Не удалось очистить незавершённую загрузку")
            }

            val userAgent = if (track.source == AudioSource.SOUNDCLOUD) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            } else {
                "com.google.android.youtube/21.02.35 (Linux; U; Android 11) gzip"
            }
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", userAgent)
                .build()
            val call = httpClient.newCall(request)
            if (!registerCall(track.id, call)) throw CancellationException("Download cancelled")

            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Ошибка сети: ${response.code}")
                    val body = response.body
                    val totalBytes = body.contentLength()
                    var downloadedBytes = 0L
                    var lastPercent = -1
                    var lastUnknownLengthUpdate = 0L
                    val coroutineContext = currentCoroutineContext()

                    body.byteStream().use { input ->
                        FileOutputStream(partialFile, false).use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                coroutineContext.ensureActive()
                                checkWifiPolicy()
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                downloadedBytes += read

                                val percent = if (totalBytes > 0) {
                                    ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                                } else {
                                    null
                                }
                                if ((percent != null && percent != lastPercent) ||
                                    (percent == null && downloadedBytes - lastUnknownLengthUpdate >= UNKNOWN_LENGTH_UPDATE_BYTES)
                                ) {
                                    lastPercent = percent ?: lastPercent
                                    lastUnknownLengthUpdate = downloadedBytes
                                    updateProgress(
                                        track.id,
                                        DownloadProgress(
                                            trackId = track.id,
                                            progressPercent = percent ?: 0,
                                            isDownloading = true,
                                            bytesDownloaded = downloadedBytes,
                                            totalBytes = totalBytes.takeIf { it > 0 }
                                        )
                                    )
                                }
                            }
                            output.fd.sync()
                        }
                    }

                    coroutineContext.ensureActive()
                    if (!DownloadFilePolicy.hasValidSize(downloadedBytes, totalBytes)) {
                        throw IOException("Загрузка завершилась с неполным размером файла")
                    }
                    Os.rename(partialFile.absolutePath, targetFile.absolutePath)
                    dbHelper.markTrackDownloaded(track.id, targetFile.absolutePath)
                    removeRecoveryMetadata(track.id)
                    updateProgress(
                        track.id,
                        DownloadProgress(
                            trackId = track.id,
                            progressPercent = 100,
                            isCompleted = true,
                            bytesDownloaded = downloadedBytes,
                            totalBytes = downloadedBytes
                        )
                    )
                    completed = true
                }
            } finally {
                unregisterCall(track.id, call)
            }
            return completed
        } finally {
            if (!completed) partFile?.delete()
        }
    }

    private fun checkWifiPolicy() {
        if (!isWifiOnlyDownloads()) return
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = activeNetwork?.let(connectivityManager::getNetworkCapabilities)
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
            throw IOException("Загрузка разрешена только по Wi-Fi")
        }
    }

    private fun registerCall(trackId: String, call: Call): Boolean = synchronized(stateLock) {
        if (activeJobs[trackId]?.isActive != true || trackId in deletingTrackIds) {
            call.cancel()
            false
        } else {
            activeCalls[trackId] = call
            true
        }
    }

    private fun unregisterCall(trackId: String, call: Call) {
        synchronized(stateLock) {
            if (activeCalls[trackId] === call) activeCalls.remove(trackId)
        }
    }

    private fun deleteTrackFiles(trackId: String) {
        val safeId = DownloadFilePolicy.sanitizeTrackId(trackId)
        for (extension in AUDIO_EXTENSIONS) {
            File(musicDir, "$safeId.$extension").delete()
            File(musicDir, "$safeId.$extension.part").delete()
        }
    }

    private fun persistRecoveryMetadata(track: Track) {
        synchronized(recoveryLock) {
            recoverableTracks[track.id] = track
            recoveryPreferences.edit().putString(RECOVERY_TRACKS_KEY, gson.toJson(recoverableTracks.values)).commit()
        }
    }

    private fun removeRecoveryMetadata(trackId: String) {
        synchronized(recoveryLock) {
            recoverableTracks.remove(trackId)
            val editor = recoveryPreferences.edit()
            if (recoverableTracks.isEmpty()) {
                editor.remove(RECOVERY_TRACKS_KEY)
            } else {
                editor.putString(RECOVERY_TRACKS_KEY, gson.toJson(recoverableTracks.values))
            }
            editor.commit()
        }
    }

    private fun loadRecoveryMetadata(): List<Track> {
        val saved = recoveryPreferences.getString(RECOVERY_TRACKS_KEY, null) ?: return emptyList()
        return try {
            JsonParser.parseString(saved).asJsonArray.mapNotNull { element ->
                runCatching { gson.fromJson(element, Track::class.java) }.getOrNull()
                    ?.takeIf { it.id.isNotBlank() }
            }
        } catch (_: Exception) {
            recoveryPreferences.edit().remove(RECOVERY_TRACKS_KEY).apply()
            emptyList()
        }
    }

    private fun updateProgress(trackId: String, progress: DownloadProgress) {
        _downloadStates.update { current -> current + (trackId to progress) }
    }

}
