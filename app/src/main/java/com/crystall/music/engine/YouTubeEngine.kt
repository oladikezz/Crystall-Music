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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object YouTubeEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedVisitorData: String = "CgtUODJLS25FMmROSSiT2LDVBjIKCgJBWhIEGgAgOg=="
    @Volatile
    private var cachedSts: Int = 20710
    @Volatile
    private var lastFetchTime: Long = 0L

    suspend fun ensureVisitorAndSts(): Pair<String, Int> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedVisitorData.isNotBlank() && (now - lastFetchTime) < 1_800_000L) {
            return@withContext Pair(cachedVisitorData, cachedSts)
        }
        // Strategy 1: Fetch visitorData via clean JSON API from YouTube Music
        try {
            val browsePayload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240901.01.00","hl":"en"}},"browseId":"FEmusic_home"}"""
            val req = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Content-Type", "application/json")
                .post(browsePayload.toRequestBody(JSON_MEDIA))
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val root = JsonParser.parseString(body).asJsonObject
                val vis = root.getAsJsonObject("responseContext")?.get("visitorData")?.asString
                if (!vis.isNullOrBlank()) {
                    cachedVisitorData = java.net.URLDecoder.decode(vis, "UTF-8")
                    lastFetchTime = now
                }
            }
        } catch (_: Exception) {}

        // Strategy 2: Extract dynamic STS from youtube.com if needed
        try {
            val req = Request.Builder()
                .url("https://www.youtube.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val resp = httpClient.newCall(req).execute()
            val html = resp.body?.string() ?: ""
            val stsMatcher = Pattern.compile("\"STS\":\\s*(\\d+)").matcher(html)
            if (stsMatcher.find()) {
                cachedSts = stsMatcher.group(1)?.toIntOrNull() ?: 20710
            }
            if (cachedVisitorData.isBlank()) {
                val visMatcher = Pattern.compile("\"(?:visitorData|VISITOR_DATA)\":\\s*\"([^\"]+)\"").matcher(html)
                if (visMatcher.find()) {
                    cachedVisitorData = visMatcher.group(1) ?: cachedVisitorData
                }
            }
            lastFetchTime = now
        } catch (_: Exception) {}

        Pair(cachedVisitorData, cachedSts)
    }

    suspend fun getVisitorData(): String = ensureVisitorAndSts().first

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        try {
            val visitorId = getVisitorData()
            val jsonPayload = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB_REMIX",
                            "clientVersion": "1.20240901.01.00",
                            "hl": "ru",
                            "gl": "RU",
                            "visitorData": "$visitorId"
                        }
                    },
                    "query": ${escapeJson(query)}
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Content-Type", "application/json")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .post(jsonPayload.toRequestBody(JSON_MEDIA))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()

            val root = JsonParser.parseString(body).asJsonObject
            val newVis = root.getAsJsonObject("responseContext")?.get("visitorData")?.asString
            if (!newVis.isNullOrBlank()) {
                try {
                    cachedVisitorData = java.net.URLDecoder.decode(newVis, "UTF-8")
                } catch (_: Exception) {}
            }
            val items = mutableListOf<JsonObject>()
            findJsonObjects(root, "musicResponsiveListItemRenderer", items)

            for (item in items) {
                parseTrackItem(item)?.let { tracks.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    suspend fun getTrendingMusic(): List<Track> = withContext(Dispatchers.IO) {
        val results = search("Топ треки 2024")
        if (results.isNotEmpty()) results else search("Popular Music")
    }

    suspend fun resolvePlaylist(urlOrId: String): Pair<Playlist, List<Track>>? = withContext(Dispatchers.IO) {
        val playlistId = extractPlaylistId(urlOrId) ?: return@withContext null
        try {
            val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
            val jsonPayload = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB_REMIX",
                            "clientVersion": "1.20240901.01.00",
                            "hl": "ru",
                            "gl": "RU"
                        }
                    },
                    "browseId": "$browseId"
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Content-Type", "application/json")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .post(jsonPayload.toRequestBody(JSON_MEDIA))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null

            val root = JsonParser.parseString(body).asJsonObject

            var title = "YouTube Music Playlist"
            var coverUrl: String? = null
            var author = "YouTube Music"

            val mf = root.getAsJsonObject("microformat")?.getAsJsonObject("microformatDataRenderer")
            if (mf != null) {
                title = mf.get("title")?.asString ?: title
                val thumbs = mf.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")
                if (thumbs != null && thumbs.size() > 0) {
                    coverUrl = thumbs.get(thumbs.size() - 1).asJsonObject.get("url")?.asString
                }
            }

            val items = mutableListOf<JsonObject>()
            findJsonObjects(root, "musicResponsiveListItemRenderer", items)

            val tracks = mutableListOf<Track>()
            for (item in items) {
                parseTrackItem(item)?.let { tracks.add(it) }
            }

            if (tracks.isEmpty() && coverUrl == null) return@withContext null

            val playlist = Playlist(
                id = playlistId,
                title = title,
                author = author,
                sourceUrl = urlOrId,
                coverUrl = coverUrl ?: tracks.firstOrNull()?.coverUrl,
                trackCount = tracks.size,
                isImported = true
            )
            Pair(playlist, tracks)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val result = fetchAudioStreamUrl(videoId)
        if (result != null) return@withContext result
        // If first attempt failed, refresh visitor data and retry once
        lastFetchTime = 0L
        ensureVisitorAndSts()
        fetchAudioStreamUrl(videoId)
    }

    private suspend fun fetchAudioStreamUrl(videoId: String): String? {
        // First try the official ANDROID client which returns itag 18 (MP4 AAC stereo) with ratebypass=yes
        // This format plays flawlessly in ExoPlayer and downloads in full without 403 or throttling.
        val androidUrl = fetchAudioStreamUrlAndroid(videoId)
        if (!androidUrl.isNullOrBlank()) return androidUrl

        // Fallback to ANDROID_VR client
        return fetchAudioStreamUrlAndroidVr(videoId)
    }

    private suspend fun fetchAudioStreamUrlAndroid(videoId: String): String? {
        return try {
            val (visitorId, sts) = ensureVisitorAndSts()

            val jsonPayload = """
            {
                "context": {
                    "client": {
                        "clientName": "ANDROID",
                        "clientVersion": "21.02.35",
                        "androidSdkVersion": 30,
                        "userAgent": "com.google.android.youtube/21.02.35 (Linux; U; Android 11) gzip",
                        "osName": "Android",
                        "osVersion": "11",
                        "hl": "en",
                        "gl": "US",
                        "visitorData": "$visitorId"
                    }
                },
                "videoId": "$videoId",
                "playbackContext": {
                    "contentPlaybackContext": {
                        "html5Preference": "HTML5_PREF_WANTS",
                        "signatureTimestamp": $sts
                    }
                },
                "contentCheckOk": true,
                "racyCheckOk": true
            }
            """.trimIndent()

            val reqBuilder = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .header("X-YouTube-Client-Name", "3")
                .header("X-YouTube-Client-Version", "21.02.35")
                .header("Origin", "https://www.youtube.com")
                .header("User-Agent", "com.google.android.youtube/21.02.35 (Linux; U; Android 11) gzip")
                .header("Content-Type", "application/json")

            if (visitorId.isNotBlank()) {
                reqBuilder.header("X-Goog-Visitor-Id", visitorId)
            }

            val request = reqBuilder.post(jsonPayload.toRequestBody(JSON_MEDIA)).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null

            val root = JsonParser.parseString(body).asJsonObject
            val status = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            if (status != null && status != "OK") return null

            val streamingData = root.getAsJsonObject("streamingData") ?: return null

            // 1. Check standard combined formats (itag 18 is 360p MP4 with full AAC stereo audio and ratebypass=yes)
            val formats = streamingData.getAsJsonArray("formats")
            if (formats != null) {
                var itag18Url: String? = null
                var anyBypassUrl: String? = null
                for (elem in formats) {
                    val fmt = elem.asJsonObject
                    val url = fmt.get("url")?.asString
                    val itag = fmt.get("itag")?.asInt ?: 0
                    if (!url.isNullOrBlank()) {
                        if (itag == 18) {
                            itag18Url = url
                            break
                        }
                        if (url.contains("ratebypass=yes") && anyBypassUrl == null) {
                            anyBypassUrl = url
                        }
                    }
                }
                val best = itag18Url ?: anyBypassUrl
                if (!best.isNullOrBlank()) return best
            }

            // 2. Check adaptiveFormats
            val adaptive = streamingData.getAsJsonArray("adaptiveFormats")
            if (adaptive != null) {
                var bestAudio: String? = null
                var maxBitrate = 0
                for (elem in adaptive) {
                    val fmt = elem.asJsonObject
                    val mime = fmt.get("mimeType")?.asString ?: ""
                    val url = fmt.get("url")?.asString
                    val bitrate = fmt.get("bitrate")?.asInt ?: 0
                    if (mime.contains("audio") && !url.isNullOrBlank()) {
                        if (bitrate > maxBitrate) {
                            maxBitrate = bitrate
                            bestAudio = url
                        }
                    }
                }
                if (!bestAudio.isNullOrBlank()) return bestAudio
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchAudioStreamUrlAndroidVr(videoId: String): String? {
        return try {
            val (visitorId, sts) = ensureVisitorAndSts()

            val jsonPayload = """
            {
                "context": {
                    "client": {
                        "clientName": "ANDROID_VR",
                        "clientVersion": "1.65.10",
                        "deviceMake": "Oculus",
                        "deviceModel": "Quest 3",
                        "androidSdkVersion": 32,
                        "userAgent": "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
                        "osName": "Android",
                        "osVersion": "12L",
                        "hl": "en",
                        "timeZone": "UTC",
                        "utcOffsetMinutes": 0,
                        "visitorData": "$visitorId"
                    }
                },
                "videoId": "$videoId",
                "playbackContext": {
                    "contentPlaybackContext": {
                        "html5Preference": "HTML5_PREF_WANTS",
                        "signatureTimestamp": $sts
                    }
                },
                "contentCheckOk": true,
                "racyCheckOk": true
            }
            """.trimIndent()

            val reqBuilder = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .header("X-YouTube-Client-Name", "28")
                .header("X-YouTube-Client-Version", "1.65.10")
                .header("Origin", "https://www.youtube.com")
                .header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip")
                .header("Content-Type", "application/json")

            if (visitorId.isNotBlank()) {
                reqBuilder.header("X-Goog-Visitor-Id", visitorId)
            }

            val request = reqBuilder.post(jsonPayload.toRequestBody(JSON_MEDIA)).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null

            val root = JsonParser.parseString(body).asJsonObject
            val status = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            if (status != null && status != "OK") return null

            val streamingData = root.getAsJsonObject("streamingData") ?: return null
            val formats = streamingData.getAsJsonArray("adaptiveFormats") ?: return null

            var aacUrl: String? = null
            var bestAudioUrl: String? = null
            var highestBitrate = 0

            for (elem in formats) {
                val fmt = elem.asJsonObject
                val mime = fmt.get("mimeType")?.asString ?: ""
                if (mime.contains("audio")) {
                    val url = fmt.get("url")?.asString
                    val itag = fmt.get("itag")?.asInt ?: 0
                    val bitrate = fmt.get("bitrate")?.asInt ?: 0
                    if (!url.isNullOrBlank()) {
                        if (itag == 140) {
                            aacUrl = url
                        }
                        if (bitrate > highestBitrate) {
                            highestBitrate = bitrate
                            bestAudioUrl = url
                        }
                    }
                }
            }
            aacUrl ?: bestAudioUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun extractPlaylistId(urlOrId: String): String? {
        if (!urlOrId.contains("http") && (urlOrId.startsWith("PL") || urlOrId.startsWith("RD") || urlOrId.startsWith("VL"))) {
            return urlOrId
        }
        val matcher = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)").matcher(urlOrId)
        return if (matcher.find()) matcher.group(1) else null
    }

    fun extractVideoId(urlOrId: String): String? {
        if (!urlOrId.contains("http") && urlOrId.length == 11) {
            return urlOrId
        }
        val p1 = Pattern.compile("(?:v=|/v/|youtu\\.be/|/embed/|/watch\\?v=)([a-zA-Z0-9_-]{11})").matcher(urlOrId)
        if (p1.find()) return p1.group(1)
        val p2 = Pattern.compile("music\\.youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})").matcher(urlOrId)
        if (p2.find()) return p2.group(1)
        return null
    }

    private fun parseTrackItem(item: JsonObject): Track? {
        try {
            // 1. Extract videoId robustly
            var videoId = item.getAsJsonObject("playlistItemData")?.get("videoId")?.asString
            if (videoId == null) {
                val nav = item.getAsJsonObject("navigationEndpoint")
                videoId = nav?.getAsJsonObject("watchEndpoint")?.get("videoId")?.asString
            }
            if (videoId == null) {
                val overlay = item.getAsJsonObject("overlay")
                    ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                    ?.getAsJsonObject("content")
                    ?.getAsJsonObject("musicPlayButtonRenderer")
                    ?.getAsJsonObject("playNavigationEndpoint")
                    ?.getAsJsonObject("watchEndpoint")
                videoId = overlay?.get("videoId")?.asString
            }
            if (videoId == null) {
                val flexCols = item.getAsJsonArray("flexColumns")
                if (flexCols != null) {
                    for (fc in flexCols) {
                        val runs = fc.asJsonObject
                            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")
                        if (runs != null) {
                            for (r in runs) {
                                val rVid = r.asJsonObject
                                    .getAsJsonObject("navigationEndpoint")
                                    ?.getAsJsonObject("watchEndpoint")
                                    ?.get("videoId")?.asString
                                if (rVid != null) {
                                    videoId = rVid
                                    break
                                }
                            }
                        }
                        if (videoId != null) break
                    }
                }
            }

            // If still no videoId, skip (it is likely an album or artist card, not a playable track)
            if (videoId.isNullOrBlank()) return null

            val flexCols = item.getAsJsonArray("flexColumns") ?: return null
            if (flexCols.size() == 0) return null

            var title = "Unknown Title"
            var artist = "YouTube Music"
            var durationMs = 0L

            // Col 0: Title
            val col0 = flexCols.get(0).asJsonObject
                .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs")
            if (col0 != null && col0.size() > 0) {
                title = col0.get(0).asJsonObject.get("text")?.asString ?: title
            }

            // Col 1: Artist & Info
            if (flexCols.size() > 1) {
                val col1 = flexCols.get(1).asJsonObject
                    .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")
                    ?.getAsJsonArray("runs")
                if (col1 != null) {
                    val artistsList = mutableListOf<String>()
                    for (runElem in col1) {
                        val text = runElem.asJsonObject.get("text")?.asString ?: continue
                        if (text.matches(Regex("\\d+:\\d+"))) {
                            durationMs = parseDurationMs(text)
                        } else if (text != " • " && text != " & " && text.isNotBlank() && text != "Song" && text != "Video" && text != "Песня" && text != "Видео") {
                            artistsList.add(text)
                        }
                    }
                    if (artistsList.isNotEmpty()) {
                        artist = artistsList.first()
                    }
                }
            }

            // Fixed columns (duration)
            val fixedCols = item.getAsJsonArray("fixedColumns")
            if (fixedCols != null && fixedCols.size() > 0) {
                val runs = fixedCols.get(0).asJsonObject
                    .getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
                    ?.getAsJsonObject("text")
                    ?.getAsJsonArray("runs")
                if (runs != null && runs.size() > 0) {
                    val durText = runs.get(0).asJsonObject.get("text")?.asString
                    if (durText != null) {
                        durationMs = parseDurationMs(durText)
                    }
                }
            }

            // Thumbnail
            var coverUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val thumbs = item.getAsJsonObject("thumbnail")
                ?.getAsJsonObject("musicThumbnailRenderer")
                ?.getAsJsonObject("thumbnail")
                ?.getAsJsonArray("thumbnails")
            if (thumbs != null && thumbs.size() > 0) {
                coverUrl = thumbs.get(thumbs.size() - 1).asJsonObject.get("url")?.asString ?: coverUrl
            }

            return Track(
                id = videoId,
                title = title,
                artist = artist,
                durationMs = durationMs,
                coverUrl = coverUrl,
                source = AudioSource.YOUTUBE
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseDurationMs(text: String): Long {
        val parts = text.split(":")
        return when (parts.size) {
            2 -> {
                val m = parts[0].toLongOrNull() ?: 0L
                val s = parts[1].toLongOrNull() ?: 0L
                (m * 60 + s) * 1000L
            }
            3 -> {
                val h = parts[0].toLongOrNull() ?: 0L
                val m = parts[1].toLongOrNull() ?: 0L
                val s = parts[2].toLongOrNull() ?: 0L
                (h * 3600 + m * 60 + s) * 1000L
            }
            else -> 0L
        }
    }

    private fun findJsonObjects(element: JsonElement, keyName: String, results: MutableList<JsonObject>) {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has(keyName)) {
                results.add(obj.getAsJsonObject(keyName))
            }
            for (entry in obj.entrySet()) {
                findJsonObjects(entry.value, keyName, results)
            }
        } else if (element.isJsonArray) {
            for (item in element.asJsonArray) {
                findJsonObjects(item, keyName, results)
            }
        }
    }

    private fun escapeJson(str: String): String {
        return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
    }
}
