package com.crystall.music.engine

import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object SoundCloudEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedClientId: String? = "Pb72ranhoyt6gw7hM7TkzUItXlMWSNSo"

    suspend fun getClientId(): String = withContext(Dispatchers.IO) {
        cachedClientId?.let { return@withContext it }
        try {
            val req = Request.Builder()
                .url("https://soundcloud.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val resp = httpClient.newCall(req).execute()
            val html = resp.body?.string() ?: ""
            val scriptPattern = Pattern.compile("https://a-v2\\.sndcdn\\.com/assets/[^\"]+\\.js")
            val matcher = scriptPattern.matcher(html)
            val scripts = mutableListOf<String>()
            while (matcher.find()) {
                scripts.add(matcher.group())
            }

            for (scriptUrl in scripts.takeLast(6).reversed()) {
                try {
                    val sReq = Request.Builder().url(scriptUrl).build()
                    val js = httpClient.newCall(sReq).execute().body?.string() ?: ""
                    val cidMatcher = Pattern.compile("client_id[:=][\"']([a-zA-Z0-9]{32})[\"']").matcher(js)
                    if (cidMatcher.find()) {
                        val cid = cidMatcher.group(1)
                        cachedClientId = cid
                        return@withContext cid
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val fallback = "Pb72ranhoyt6gw7hM7TkzUItXlMWSNSo"
        cachedClientId = fallback
        fallback
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        try {
            val clientId = getClientId()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api-v2.soundcloud.com/search/tracks?q=$encodedQuery&client_id=$clientId&limit=25"

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val json = resp.body?.string() ?: return@withContext emptyList()

            val root = JsonParser.parseString(json).asJsonObject
            val collection = root.getAsJsonArray("collection") ?: return@withContext emptyList()

            for (elem in collection) {
                val tObj = elem.asJsonObject
                parseSoundCloudTrack(tObj)?.let { tracks.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    suspend fun resolveUrl(url: String): Pair<Playlist?, List<Track>>? = withContext(Dispatchers.IO) {
        try {
            val clientId = getClientId()
            val encodedUrl = URLEncoder.encode(url, "UTF-8")
            val resolveApiUrl = "https://api-v2.soundcloud.com/resolve?url=$encodedUrl&client_id=$clientId"

            val req = Request.Builder()
                .url(resolveApiUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = resp.body?.string() ?: return@withContext null

            val root = JsonParser.parseString(json).asJsonObject
            val kind = root.get("kind")?.asString ?: ""

            if (kind == "playlist" || kind == "system-playlist") {
                val title = root.get("title")?.asString ?: "SoundCloud Playlist"
                val author = root.getAsJsonObject("user")?.get("username")?.asString ?: "SoundCloud"
                val artwork = root.get("artwork_url")?.asString
                val id = root.get("id")?.asString ?: System.currentTimeMillis().toString()

                val rawTracks = root.getAsJsonArray("tracks") ?: JsonArray()
                val tracks = mutableListOf<Track>()

                for (elem in rawTracks) {
                    val tObj = elem.asJsonObject
                    if (tObj.has("title")) {
                        parseSoundCloudTrack(tObj)?.let { tracks.add(it) }
                    }
                }

                val playlist = Playlist(
                    id = "sc_$id",
                    title = title,
                    author = author,
                    sourceUrl = url,
                    coverUrl = artwork ?: tracks.firstOrNull()?.coverUrl,
                    trackCount = tracks.size,
                    isImported = true
                )
                Pair(playlist, tracks)
            } else if (kind == "track") {
                val track = parseSoundCloudTrack(root) ?: return@withContext null
                Pair(null, listOf(track))
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Returns a stream URL suitable for playback (progressive preferred, HLS as fallback).
     * ExoPlayer handles HLS natively, so both protocols work for playback.
     */
    suspend fun getStreamUrl(track: Track): String? = withContext(Dispatchers.IO) {
        getTranscodingUrl(track, progressiveOnly = false)
    }

    /**
     * Returns a stream URL suitable for downloading (progressive only).
     * HLS streams cannot be downloaded as a single file.
     */
    suspend fun getDownloadUrl(track: Track): String? = withContext(Dispatchers.IO) {
        getTranscodingUrl(track, progressiveOnly = true)
    }

    private suspend fun getTranscodingUrl(track: Track, progressiveOnly: Boolean): String? {
        return try {
            val clientId = getClientId()
            val trackId = track.id.removePrefix("sc_")
            val trackApiUrl = "https://api-v2.soundcloud.com/tracks/$trackId?client_id=$clientId"

            val req = Request.Builder().url(trackApiUrl).build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val json = resp.body?.string() ?: return null

            val root = JsonParser.parseString(json).asJsonObject
            val media = root.getAsJsonObject("media") ?: return null
            val transcodings = media.getAsJsonArray("transcodings") ?: return null

            var progressiveUrl: String? = null
            var hlsUrl: String? = null

            for (elem in transcodings) {
                val tc = elem.asJsonObject
                val format = tc.getAsJsonObject("format")
                val protocol = format?.get("protocol")?.asString ?: ""
                val tcUrl = tc.get("url")?.asString ?: continue

                when (protocol) {
                    "progressive" -> if (progressiveUrl == null) progressiveUrl = tcUrl
                    "hls" -> if (hlsUrl == null) hlsUrl = tcUrl
                }
            }

            val targetTranscoding = progressiveUrl
                ?: if (!progressiveOnly) hlsUrl else null
                ?: return null

            val streamReqUrl = "$targetTranscoding?client_id=$clientId"
            val sReq = Request.Builder().url(streamReqUrl).build()
            val sResp = httpClient.newCall(sReq).execute()
            if (!sResp.isSuccessful) return null
            val sJson = sResp.body?.string() ?: return null

            JsonParser.parseString(sJson).asJsonObject.get("url")?.asString
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSoundCloudTrack(tObj: JsonObject): Track? {
        try {
            val id = tObj.get("id")?.asString ?: return null
            val title = tObj.get("title")?.asString ?: "Unknown Title"
            val artist = tObj.getAsJsonObject("user")?.get("username")?.asString ?: "Unknown Artist"
            val durationMs = tObj.get("duration")?.asLong ?: 0L
            var artwork = tObj.get("artwork_url")?.asString ?: ""
            if (artwork.contains("-large.jpg")) {
                artwork = artwork.replace("-large.jpg", "-t500x500.jpg")
            }
            val waveformUrl = tObj.get("waveform_url")?.asString

            return Track(
                id = "sc_$id",
                title = title,
                artist = artist,
                durationMs = durationMs,
                coverUrl = artwork,
                source = AudioSource.SOUNDCLOUD,
                waveformUrl = waveformUrl
            )
        } catch (e: Exception) {
            return null
        }
    }
}
