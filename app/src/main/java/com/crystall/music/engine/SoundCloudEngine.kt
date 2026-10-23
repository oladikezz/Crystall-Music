package com.crystall.music.engine

import com.crystall.music.data.model.AudioSource
import com.crystall.music.data.model.Playlist
import com.crystall.music.data.model.Track
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object SoundCloudEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val FALLBACK_CLIENT_ID = "Pb72ranhoyt6gw7hM7TkzUItXlMWSNSo"
    private const val FALLBACK_CACHE_MS = 10 * 60 * 1000L

    @Volatile
    private var cachedClientId: String? = null
    @Volatile
    private var cachedClientIdAtMs: Long = 0L
    @Volatile
    private var cachedClientIdIsFallback = false

    suspend fun getClientId(): String = withContext(Dispatchers.IO) {
        cachedClientId?.let {
            if (!cachedClientIdIsFallback || System.currentTimeMillis() - cachedClientIdAtMs < FALLBACK_CACHE_MS) {
                return@withContext it
            }
        }
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
                        cachedClientIdAtMs = System.currentTimeMillis()
                        cachedClientIdIsFallback = false
                        return@withContext cid
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedClientId = FALLBACK_CLIENT_ID
        cachedClientIdAtMs = System.currentTimeMillis()
        cachedClientIdIsFallback = true
        FALLBACK_CLIENT_ID
    }

    @Synchronized
    private fun invalidateClientId(failedClientId: String) {
        if (cachedClientId == failedClientId) {
            cachedClientId = null
            cachedClientIdAtMs = 0L
            cachedClientIdIsFallback = false
        }
    }

    private data class AuthenticatedResponse(val clientId: String, val response: Response)

    private suspend fun executeWithClientId(requestFactory: (String) -> Request): AuthenticatedResponse? {
        val firstClientId = getClientId()
        var response = httpClient.newCall(requestFactory(firstClientId)).execute()
        if (response.code != 401 && response.code != 403) {
            return AuthenticatedResponse(firstClientId, response)
        }
        response.close()
        invalidateClientId(firstClientId)
        val refreshedClientId = getClientId()
        if (refreshedClientId == firstClientId) return null
        response = httpClient.newCall(requestFactory(refreshedClientId)).execute()
        if (response.code == 401 || response.code == 403) {
            response.close()
            invalidateClientId(refreshedClientId)
            return null
        }
        return AuthenticatedResponse(refreshedClientId, response)
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val authenticatedResponse = executeWithClientId { clientId ->
                Request.Builder()
                    .url("https://api-v2.soundcloud.com/search/tracks?q=$encodedQuery&client_id=$clientId&limit=25")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
            } ?: return@withContext emptyList()
            val json = authenticatedResponse.response.use { response ->
                if (response.isSuccessful) response.body?.string() else null
            } ?: return@withContext emptyList()

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
            val encodedUrl = URLEncoder.encode(url, "UTF-8")
            val authenticatedResponse = executeWithClientId { clientId ->
                Request.Builder()
                    .url("https://api-v2.soundcloud.com/resolve?url=$encodedUrl&client_id=$clientId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
            } ?: return@withContext null
            val json = authenticatedResponse.response.use { response ->
                if (response.isSuccessful) response.body?.string() else null
            } ?: return@withContext null

            val root = JsonParser.parseString(json).asJsonObject
            val kind = root.get("kind")?.asString ?: ""

            if (kind == "playlist" || kind == "system-playlist") {
                val title = root.get("title")?.asString ?: "SoundCloud Playlist"
                val author = root.getAsJsonObject("user")?.get("username")?.asString ?: "SoundCloud"
                val artwork = root.get("artwork_url")?.asString
                val id = root.get("id")?.asString ?: System.currentTimeMillis().toString()

                val rawTracks = mutableListOf<JsonObject>()
                val tracksElement = root.get("tracks")
                var nextHref = root.get("next_href")?.asString
                when {
                    tracksElement?.isJsonArray == true -> {
                        tracksElement.asJsonArray.forEach { if (it.isJsonObject) rawTracks.add(it.asJsonObject) }
                    }
                    tracksElement?.isJsonObject == true -> {
                        val tracksObject = tracksElement.asJsonObject
                        tracksObject.getAsJsonArray("collection")?.forEach {
                            if (it.isJsonObject) rawTracks.add(it.asJsonObject)
                        }
                        nextHref = tracksObject.get("next_href")?.asString ?: nextHref
                    }
                }

                val seenPageUrls = mutableSetOf<String>()
                var pageCount = 0
                while (!nextHref.isNullOrBlank() && pageCount < 20 && seenPageUrls.add(nextHref)) {
                    val pageUrl = nextHref.toHttpUrlOrNull()
                        ?.takeIf { it.isHttps && it.host == "api-v2.soundcloud.com" }
                        ?: break
                    val pageResponse = executeWithClientId { clientId ->
                        Request.Builder()
                            .url(pageUrl.newBuilder().setQueryParameter("client_id", clientId).build())
                            .build()
                    } ?: break
                    val pageJson = pageResponse.response.use { response ->
                        if (response.isSuccessful) response.body?.string() else null
                    } ?: break
                    val page = JsonParser.parseString(pageJson)
                    val collection = when {
                        page.isJsonArray -> page.asJsonArray
                        page.isJsonObject -> page.asJsonObject.getAsJsonArray("collection") ?: JsonArray()
                        else -> JsonArray()
                    }
                    collection.forEach { if (it.isJsonObject) rawTracks.add(it.asJsonObject) }
                    nextHref = page.takeIf { it.isJsonObject }
                        ?.asJsonObject?.get("next_href")?.asString
                    pageCount++
                }

                val tracks = completePlaylistTracks(rawTracks)

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

    private suspend fun completePlaylistTracks(rawTracks: List<JsonObject>): List<Track> {
        val knownTracks = mutableMapOf<String, Track>()
        val missingIds = linkedSetOf<String>()
        for (rawTrack in rawTracks) {
            val id = rawTrack.get("id")?.asString?.takeIf { it.matches(Regex("\\d+")) } ?: continue
            val track = parseSoundCloudTrack(rawTrack)
            if (track != null) knownTracks[id] = track else missingIds.add(id)
        }

        for (batch in missingIds.toList().chunked(50)) {
            val ids = batch.joinToString(",")
            val authenticatedResponse = executeWithClientId { clientId ->
                Request.Builder()
                    .url("https://api-v2.soundcloud.com/tracks?ids=$ids&client_id=$clientId")
                    .build()
            } ?: continue
            val json = authenticatedResponse.response.use { response ->
                if (response.isSuccessful) response.body?.string() else null
            } ?: continue
            val parsed = runCatching { JsonParser.parseString(json) }.getOrNull() ?: continue
            val items = when {
                parsed.isJsonArray -> parsed.asJsonArray
                parsed.isJsonObject -> parsed.asJsonObject.getAsJsonArray("collection") ?: JsonArray()
                else -> JsonArray()
            }
            for (item in items) {
                if (!item.isJsonObject) continue
                val track = parseSoundCloudTrack(item.asJsonObject) ?: continue
                knownTracks[track.id.removePrefix("sc_")] = track
            }
        }

        return rawTracks.mapNotNull { rawTrack ->
            val id = rawTrack.get("id")?.asString?.takeIf { it.matches(Regex("\\d+")) } ?: return@mapNotNull null
            knownTracks[id] ?: Track(
                id = "unavailable_sc_$id",
                title = rawTrack.get("title")?.asString?.takeIf { it.isNotBlank() } ?: "Unavailable track",
                artist = rawTrack.getAsJsonObject("user")?.get("username")?.asString ?: "SoundCloud",
                source = AudioSource.SOUNDCLOUD
            )
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
            val trackId = track.id.removePrefix("sc_")
            if (!trackId.matches(Regex("\\d+"))) return null
            val trackResponse = executeWithClientId { clientId ->
                Request.Builder()
                    .url("https://api-v2.soundcloud.com/tracks/$trackId?client_id=$clientId")
                    .build()
            } ?: return null
            val json = trackResponse.response.use { response ->
                if (response.isSuccessful) response.body?.string() else null
            } ?: return null

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

            val streamBaseUrl = targetTranscoding.toHttpUrlOrNull()
                ?.takeIf { it.isHttps && it.host == "api-v2.soundcloud.com" }
                ?: return null
            val streamResponse = executeWithClientId { clientId ->
                Request.Builder()
                    .url(streamBaseUrl.newBuilder().setQueryParameter("client_id", clientId).build())
                    .build()
            } ?: return null
            val sJson = streamResponse.response.use { response ->
                if (response.isSuccessful) response.body?.string() else null
            } ?: return null

            JsonParser.parseString(sJson).asJsonObject.get("url")?.asString
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSoundCloudTrack(tObj: JsonObject): Track? {
        try {
            val id = tObj.get("id")?.asString ?: return null
            val title = tObj.get("title")?.asString?.takeIf { it.isNotBlank() } ?: return null
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
