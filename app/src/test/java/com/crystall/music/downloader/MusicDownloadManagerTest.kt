package com.crystall.music.downloader

import android.content.Context
import android.os.Environment
import com.crystall.music.data.db.MusicDatabaseHelper
import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MusicDownloadManagerTest {
    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var manager: MusicDownloadManager

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        clearSingleton()
        context.deleteDatabase("crystall_music.db")
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.listFiles()?.forEach { it.delete() }
        context.getSharedPreferences("music_download_recovery", Context.MODE_PRIVATE).edit().clear().commit()
        server = MockWebServer().also { it.start() }
        manager = MusicDownloadManager(context)
    }

    @After fun tearDown() {
        server.shutdown()
        clearSingleton()
        context.deleteDatabase("crystall_music.db")
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.listFiles()?.forEach { it.delete() }
    }

    private fun clearSingleton() {
        val field = MusicDatabaseHelper::class.java.getDeclaredField("instance")
        field.isAccessible = true
        (field.get(null) as? MusicDatabaseHelper)?.close()
        field.set(null, null)
    }

    private fun track(id: String) = Track(
        id, "Test song", "Test artist", source = AudioSource.LOCAL,
        localPath = server.url("/$id.m4a").toString()
    )

    private fun awaitState(id: String, predicate: (DownloadProgress) -> Boolean): DownloadProgress = runBlocking {
        withTimeout(10_000) { manager.downloadStates.first { it[id]?.let(predicate) == true }.getValue(id) }
    }

    @Test fun individualAndPlaylistDownloadShareOneRequestAndFinalizeFile() {
        server.enqueue(MockResponse().setBody("test-audio-data").setBodyDelay(300, TimeUnit.MILLISECONDS))
        val track = track("deduplicated")
        manager.downloadTrack(track)
        manager.downloadPlaylist(listOf(track, track))
        val result = awaitState(track.id) { it.isCompleted }
        assertEquals(100, result.progressPercent)
        val saved = MusicDatabaseHelper.getInstance(context).getTrack(track.id)!!
        assertTrue(saved.isDownloaded)
        assertEquals("test-audio-data", File(saved.localPath!!).readText())
        assertFalse(File(saved.localPath + ".part").exists())
        assertEquals(1, server.requestCount)
    }

    @Test fun cancelCleansPartialFileAndRetryCompletes() {
        server.enqueue(MockResponse().setBody("x".repeat(8192)).throttleBody(512, 100, TimeUnit.MILLISECONDS))
        val track = track("cancel-retry")
        manager.downloadTrack(track)
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertTrue(manager.cancelDownload(track.id))
        awaitState(track.id) { it.isCanceled }
        server.enqueue(MockResponse().setBody("complete-audio"))
        manager.retryDownload(track)
        awaitState(track.id) { it.isCompleted }
        val saved = MusicDatabaseHelper.getInstance(context).getTrack(track.id)!!
        assertEquals("complete-audio", File(saved.localPath!!).readText())
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!
        assertFalse(directory.listFiles().orEmpty().any { it.name.startsWith(track.id) && it.name.endsWith(".part") })
    }

    @Test fun failedRequestIsNotMarkedDownloadedAndRetainsRetryMetadata() {
        server.enqueue(MockResponse().setResponseCode(503))
        val track = track("failed")
        manager.downloadTrack(track)
        awaitState(track.id) { !it.isDownloading && it.errorMessage != null }
        assertFalse(MusicDatabaseHelper.getInstance(context).getTrack(track.id)!!.isDownloaded)
        assertTrue(manager.getRecoverableDownloads().any { it.id == track.id })
    }
}
