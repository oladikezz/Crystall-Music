package com.crystall.music.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MusicDatabaseHelperTest {
    private lateinit var context: Context
    private lateinit var database: MusicDatabaseHelper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        clearSingleton()
        context.deleteDatabase(DATABASE_NAME)
        database = MusicDatabaseHelper.getInstance(context)
    }

    @After
    fun tearDown() {
        clearSingleton()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun versionOneUpgradePreservesDataAndSettingsTable() {
        val firstTrack = track("first")
        val secondTrack = track("second")
        database.savePlaylist(playlist("saved"), listOf(firstTrack, secondTrack))
        database.toggleFavorite(firstTrack)
        database.addToHistory(secondTrack)
        database.setSetting("migration_marker", "kept")

        database.writableDatabase.apply {
            execSQL("DROP INDEX IF EXISTS idx_playlist_tracks_playlist_position")
            execSQL("DROP INDEX IF EXISTS idx_favorites_added_at")
            execSQL("DROP INDEX IF EXISTS idx_play_history_played_at")
            execSQL("PRAGMA user_version = 1")
        }
        clearSingleton()

        database = MusicDatabaseHelper.getInstance(context)

        assertNotNull(database.getTrack("first"))
        assertEquals(listOf("first", "second"), database.getPlaylistTracks("saved").map { it.id })
        assertTrue(database.isFavorite("first"))
        assertEquals(listOf("second"), database.getHistoryTracks().map { it.id })
        assertEquals("kept", database.getSetting("migration_marker"))
        assertEquals(1, scalar(database.readableDatabase, "SELECT COUNT(*) FROM settings"))
        assertTrue(indexExists("idx_playlist_tracks_playlist_position"))
        assertTrue(indexExists("idx_favorites_added_at"))
        assertTrue(indexExists("idx_play_history_played_at"))
    }

    @Test
    fun savePlaylistReplacesLinksAndKeepsDownloadedMetadataOnRefresh() {
        val downloaded = track("downloaded", isDownloaded = true, localPath = "/music/downloaded.mp3")
        database.savePlaylist(playlist("mix"), listOf(downloaded, track("removed")))

        database.savePlaylist(
            playlist("mix", title = "Refreshed mix"),
            listOf(track("downloaded", title = "Updated title"), track("added"))
        )

        assertEquals(listOf("downloaded", "added"), database.getPlaylistTracks("mix").map { it.id })
        assertEquals(2, database.getAllPlaylists().single().trackCount)
        val updatedTrack = database.getTrack("downloaded")!!
        assertEquals("Updated title", updatedTrack.title)
        assertTrue(updatedTrack.isDownloaded)
        assertEquals("/music/downloaded.mp3", updatedTrack.localPath)
    }

    @Test
    fun playlistRenameAndRemovalKeepPositionsAndCountConsistent() {
        database.savePlaylist(playlist("mix"), listOf(track("a"), track("b"), track("c")))

        assertTrue(database.renamePlaylist("mix", "Renamed"))
        assertTrue(database.removeTrackFromPlaylist("mix", "b"))

        assertEquals("Renamed", database.getAllPlaylists().single().title)
        assertEquals(2, database.getAllPlaylists().single().trackCount)
        assertEquals(listOf("a", "c"), database.getPlaylistTracks("mix").map { it.id })
        assertEquals(1, playlistPosition("mix", "c"))
        assertFalse(database.removeTrackFromPlaylist("mix", "missing"))
        assertFalse(database.renamePlaylist("missing", "No playlist"))
    }

    @Test
    fun blockedArtistsAndRecommendationFeedbackRoundTrip() {
        database.setBlockedArtists(setOf("Artist A", "Artist B"))
        database.setRecommendationFeedback("track-1", "dislike")
        database.setRecommendationFeedback("track-2", "like")

        assertEquals(setOf("Artist A", "Artist B"), database.getBlockedArtists())
        assertEquals(mapOf("track-1" to "dislike", "track-2" to "like"), database.getRecommendationFeedback())

        database.setRecommendationFeedback("track-1", null)
        assertNull(database.getRecommendationFeedback()["track-1"])
    }

    private fun playlistPosition(playlistId: String, trackId: String): Int {
        return database.readableDatabase.rawQuery(
            "SELECT position FROM playlist_tracks WHERE playlist_id = ? AND track_id = ?",
            arrayOf(playlistId, trackId)
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getInt(0)
        }
    }

    private fun indexExists(name: String): Boolean {
        return database.readableDatabase.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = ?",
            arrayOf(name)
        ).use { it.moveToFirst() }
    }

    private fun scalar(db: SQLiteDatabase, sql: String): Int {
        return db.rawQuery(sql, null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getInt(0)
        }
    }

    private fun track(
        id: String,
        title: String = id,
        isDownloaded: Boolean = false,
        localPath: String? = null
    ) = Track(
        id = id,
        title = title,
        artist = "artist",
        isDownloaded = isDownloaded,
        localPath = localPath
    )

    private fun playlist(id: String, title: String = id) = Playlist(id = id, title = title)

    private fun clearSingleton() {
        val field = MusicDatabaseHelper::class.java.getDeclaredField("instance")
        field.isAccessible = true
        (field.get(null) as? MusicDatabaseHelper)?.close()
        field.set(null, null)
    }

    companion object {
        private const val DATABASE_NAME = "crystall_music.db"
    }
}
