package com.crystall.music.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track

class MusicDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "crystall_music.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var instance: MusicDatabaseHelper? = null

        fun getInstance(context: Context): MusicDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: MusicDatabaseHelper(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE tracks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                duration_ms INTEGER DEFAULT 0,
                cover_url TEXT,
                stream_url TEXT,
                local_path TEXT,
                source TEXT NOT NULL,
                is_downloaded INTEGER DEFAULT 0,
                waveform_url TEXT,
                added_at INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE playlists (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                author TEXT,
                source_url TEXT,
                cover_url TEXT,
                track_count INTEGER DEFAULT 0,
                is_imported INTEGER DEFAULT 0,
                created_at INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE playlist_tracks (
                playlist_id TEXT NOT NULL,
                track_id TEXT NOT NULL,
                position INTEGER DEFAULT 0,
                PRIMARY KEY (playlist_id, track_id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE favorites (
                track_id TEXT PRIMARY KEY,
                added_at INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE play_history (
                track_id TEXT PRIMARY KEY,
                played_at INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS play_history")
        db.execSQL("DROP TABLE IF EXISTS favorites")
        db.execSQL("DROP TABLE IF EXISTS playlist_tracks")
        db.execSQL("DROP TABLE IF EXISTS playlists")
        db.execSQL("DROP TABLE IF EXISTS tracks")
        onCreate(db)
    }

    fun insertOrUpdateTrack(track: Track) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", track.id)
            put("title", track.title)
            put("artist", track.artist)
            put("duration_ms", track.durationMs)
            put("cover_url", track.coverUrl)
            if (track.streamUrl != null) put("stream_url", track.streamUrl)
            if (track.localPath != null) put("local_path", track.localPath)
            put("source", track.source.name)
            put("is_downloaded", if (track.isDownloaded) 1 else 0)
            put("waveform_url", track.waveformUrl)
            put("added_at", track.addedAt)
        }
        db.insertWithOnConflict(
            "tracks",
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getTrack(trackId: String): Track? {
        val db = readableDatabase
        val cursor = db.query(
            "tracks",
            null,
            "id = ?",
            arrayOf(trackId),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToTrack(it) else null
        }
    }

    fun getAllTracks(): List<Track> {
        val db = readableDatabase
        val cursor = db.query("tracks", null, null, null, null, null, "added_at DESC")
        val list = mutableListOf<Track>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTrack(it))
            }
        }
        return list
    }

    fun getDownloadedTracks(): List<Track> {
        val db = readableDatabase
        val cursor = db.query(
            "tracks",
            null,
            "is_downloaded = 1",
            null,
            null,
            null,
            "added_at DESC"
        )
        val list = mutableListOf<Track>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTrack(it))
            }
        }
        return list
    }

    fun markTrackDownloaded(trackId: String, localPath: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("is_downloaded", 1)
            put("local_path", localPath)
        }
        db.update("tracks", cv, "id = ?", arrayOf(trackId))
    }

    fun deleteDownloadedTrack(trackId: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("is_downloaded", 0)
            putNull("local_path")
        }
        db.update("tracks", cv, "id = ?", arrayOf(trackId))
    }

    fun toggleFavorite(track: Track): Boolean {
        insertOrUpdateTrack(track)
        val db = writableDatabase
        val isFav = isFavorite(track.id)
        return if (isFav) {
            db.delete("favorites", "track_id = ?", arrayOf(track.id))
            false
        } else {
            val cv = ContentValues().apply {
                put("track_id", track.id)
                put("added_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("favorites", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            true
        }
    }

    fun isFavorite(trackId: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            "favorites",
            arrayOf("track_id"),
            "track_id = ?",
            arrayOf(trackId),
            null,
            null,
            null
        )
        return cursor.use { it.count > 0 }
    }

    fun getFavoriteTracks(): List<Track> {
        val db = readableDatabase
        val query = """
            SELECT t.* FROM tracks t
            INNER JOIN favorites f ON t.id = f.track_id
            ORDER BY f.added_at DESC
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        val list = mutableListOf<Track>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTrack(it))
            }
        }
        return list
    }

    fun addToHistory(track: Track) {
        insertOrUpdateTrack(track)
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("track_id", track.id)
            put("played_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("play_history", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getHistoryTracks(): List<Track> {
        val db = readableDatabase
        val query = """
            SELECT t.* FROM tracks t
            INNER JOIN play_history h ON t.id = h.track_id
            ORDER BY h.played_at DESC LIMIT 30
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        val list = mutableListOf<Track>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTrack(it))
            }
        }
        return list
    }

    fun savePlaylist(playlist: Playlist, tracks: List<Track>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("id", playlist.id)
                put("title", playlist.title)
                put("author", playlist.author)
                put("source_url", playlist.sourceUrl)
                put("cover_url", playlist.coverUrl ?: tracks.firstOrNull()?.coverUrl)
                put("track_count", tracks.size)
                put("is_imported", if (playlist.isImported) 1 else 0)
                put("created_at", playlist.createdAt)
            }
            db.insertWithOnConflict("playlists", null, cv, SQLiteDatabase.CONFLICT_REPLACE)

            // Save all tracks
            for ((idx, track) in tracks.withIndex()) {
                insertOrUpdateTrack(track)
                val ptCv = ContentValues().apply {
                    put("playlist_id", playlist.id)
                    put("track_id", track.id)
                    put("position", idx)
                }
                db.insertWithOnConflict("playlist_tracks", null, ptCv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllPlaylists(): List<Playlist> {
        val db = readableDatabase
        val cursor = db.query("playlists", null, null, null, null, null, "created_at DESC")
        val list = mutableListOf<Playlist>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Playlist(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        author = it.getString(it.getColumnIndexOrThrow("author")) ?: "",
                        sourceUrl = it.getString(it.getColumnIndexOrThrow("source_url")),
                        coverUrl = it.getString(it.getColumnIndexOrThrow("cover_url")),
                        trackCount = it.getInt(it.getColumnIndexOrThrow("track_count")),
                        isImported = it.getInt(it.getColumnIndexOrThrow("is_imported")) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        return list
    }

    fun getPlaylistTracks(playlistId: String): List<Track> {
        val db = readableDatabase
        val query = """
            SELECT t.* FROM tracks t
            INNER JOIN playlist_tracks pt ON t.id = pt.track_id
            WHERE pt.playlist_id = ?
            ORDER BY pt.position ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(playlistId))
        val list = mutableListOf<Track>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTrack(it))
            }
        }
        return list
    }

    fun deletePlaylist(playlistId: String) {
        val db = writableDatabase
        db.delete("playlist_tracks", "playlist_id = ?", arrayOf(playlistId))
        db.delete("playlists", "id = ?", arrayOf(playlistId))
    }

    private fun cursorToTrack(c: Cursor): Track {
        val sourceStr = c.getString(c.getColumnIndexOrThrow("source")) ?: "YOUTUBE"
        val source = try {
            AudioSource.valueOf(sourceStr)
        } catch (_: Exception) {
            AudioSource.YOUTUBE
        }
        return Track(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            title = c.getString(c.getColumnIndexOrThrow("title")),
            artist = c.getString(c.getColumnIndexOrThrow("artist")),
            durationMs = c.getLong(c.getColumnIndexOrThrow("duration_ms")),
            coverUrl = c.getString(c.getColumnIndexOrThrow("cover_url")) ?: "",
            streamUrl = c.getString(c.getColumnIndexOrThrow("stream_url")),
            localPath = c.getString(c.getColumnIndexOrThrow("local_path")),
            source = source,
            isDownloaded = c.getInt(c.getColumnIndexOrThrow("is_downloaded")) == 1,
            waveformUrl = c.getString(c.getColumnIndexOrThrow("waveform_url")),
            addedAt = c.getLong(c.getColumnIndexOrThrow("added_at"))
        )
    }

    fun getSetting(key: String, defaultValue: String = ""): String {
        return try {
            val db = readableDatabase
            db.execSQL("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)")
            val cursor = db.query("settings", arrayOf("value"), "key = ?", arrayOf(key), null, null, null)
            cursor.use {
                if (it.moveToFirst()) it.getString(0) ?: defaultValue else defaultValue
            }
        } catch (_: Exception) {
            defaultValue
        }
    }

    fun setSetting(key: String, value: String) {
        try {
            val db = writableDatabase
            db.execSQL("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)")
            val cv = ContentValues().apply {
                put("key", key)
                put("value", value)
            }
            db.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (_: Exception) {}
    }

    fun getFavoriteArtists(): List<String> {
        val raw = getSetting("favorite_artists", "")
        if (raw.isBlank()) return emptyList()
        return raw.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun saveFavoriteArtists(artists: List<String>) {
        setSetting("favorite_artists", artists.joinToString("|||"))
    }

    fun hasCompletedTasteOnboarding(): Boolean {
        return getSetting("taste_onboarding_done", "false") == "true"
    }

    fun setTasteOnboardingCompleted(completed: Boolean) {
        setSetting("taste_onboarding_done", if (completed) "true" else "false")
    }

    fun isEconomyMode(): Boolean {
        // Default to true for minimum internet usage
        return getSetting("economy_mode", "true") == "true"
    }

    fun setEconomyMode(enabled: Boolean) {
        setSetting("economy_mode", if (enabled) "true" else "false")
    }

    fun getDislikedTrackIds(): Set<String> {
        val raw = getSetting("disliked_tracks", "")
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun toggleDislike(trackId: String): Boolean {
        val current = getDislikedTrackIds().toMutableSet()
        val isDisliked = if (current.contains(trackId)) {
            current.remove(trackId)
            false
        } else {
            current.add(trackId)
            true
        }
        setSetting("disliked_tracks", current.joinToString(","))
        return isDisliked
    }
}
