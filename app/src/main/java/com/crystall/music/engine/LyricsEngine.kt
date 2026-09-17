package com.crystall.music.engine

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class LyricsResult(
    val syncedLyrics: List<LyricLine> = emptyList(),
    val plainLyrics: String? = null,
    val isSynced: Boolean = syncedLyrics.isNotEmpty()
)

object LyricsEngine {

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getLyrics(title: String, artist: String, videoId: String? = null): LyricsResult? = withContext(Dispatchers.IO) {
        // 1. Try LRCLIB API first (synced lyrics karaoke support)
        val lrcResult = fetchFromLrcLib(title, artist)
        if (lrcResult != null && (lrcResult.syncedLyrics.isNotEmpty() || !lrcResult.plainLyrics.isNullOrBlank())) {
            return@withContext lrcResult
        }

        // 2. Fallback to YouTube Music native lyrics if videoId is available
        if (!videoId.isNullOrBlank()) {
            val ytLyrics = fetchFromYouTubeMusic(videoId)
            if (!ytLyrics.isNullOrBlank()) {
                return@withContext LyricsResult(plainLyrics = ytLyrics)
            }
        }

        null
    }

    private fun fetchFromLrcLib(title: String, artist: String): LyricsResult? {
        try {
            val cleanTitle = cleanSearchTerm(title)
            val cleanArtist = cleanSearchTerm(artist)

            // Direct match
            val q = "track_name=" + URLEncoder.encode(cleanTitle, "UTF-8") + "&artist_name=" + URLEncoder.encode(cleanArtist, "UTF-8")
            val req = Request.Builder()
                .url("https://lrclib.net/api/get?$q")
                .header("User-Agent", "CrystallMusic/1.0 (Android)")
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return null
                val root = JsonParser.parseString(body).asJsonObject
                val synced = root.get("syncedLyrics")?.let { if (it.isJsonNull) null else it.asString }
                val plain = root.get("plainLyrics")?.let { if (it.isJsonNull) null else it.asString }

                val lines = if (!synced.isNullOrBlank()) parseLrc(synced) else emptyList()
                if (lines.isNotEmpty() || !plain.isNullOrBlank()) {
                    return LyricsResult(syncedLyrics = lines, plainLyrics = plain)
                }
            }

            // Search query fallback
            val searchParam = URLEncoder.encode("$cleanTitle $cleanArtist", "UTF-8")
            val sReq = Request.Builder()
                .url("https://lrclib.net/api/search?q=$searchParam")
                .header("User-Agent", "CrystallMusic/1.0 (Android)")
                .build()
            val sResp = httpClient.newCall(sReq).execute()
            if (sResp.isSuccessful) {
                val sBody = sResp.body?.string() ?: return null
                val array = JsonParser.parseString(sBody).asJsonArray
                if (array.size() > 0) {
                    val first = array.get(0).asJsonObject
                    val synced = first.get("syncedLyrics")?.let { if (it.isJsonNull) null else it.asString }
                    val plain = first.get("plainLyrics")?.let { if (it.isJsonNull) null else it.asString }
                    val lines = if (!synced.isNullOrBlank()) parseLrc(synced) else emptyList()
                    return LyricsResult(syncedLyrics = lines, plainLyrics = plain)
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun fetchFromYouTubeMusic(videoId: String): String? {
        try {
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
                    "videoId": "$videoId"
                }
            """.trimIndent()

            val req = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/next")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .post(jsonPayload.toRequestBody(JSON_MEDIA))
                .build()

            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null

            val root = JsonParser.parseString(body).asJsonObject
            val tabs = root.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnMusicWatchNextResultsRenderer")
                ?.getAsJsonObject("tabbedRenderer")
                ?.getAsJsonObject("watchNextTabbedResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return null

            var lyricsBrowseId: String? = null
            for (t in tabs) {
                val tr = t.asJsonObject.getAsJsonObject("tabRenderer") ?: continue
                val title = tr.get("title")?.asString?.lowercase() ?: ""
                if (title.contains("текст") || title.contains("lyrics")) {
                    lyricsBrowseId = tr.getAsJsonObject("endpoint")?.getAsJsonObject("browseEndpoint")?.get("browseId")?.asString
                    break
                }
            }

            if (lyricsBrowseId.isNullOrBlank()) return null

            val bPayload = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB_REMIX",
                            "clientVersion": "1.20240901.01.00",
                            "hl": "ru",
                            "gl": "RU"
                        }
                    },
                    "browseId": "$lyricsBrowseId"
                }
            """.trimIndent()

            val bReq = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .post(bPayload.toRequestBody(JSON_MEDIA))
                .build()

            val bResp = httpClient.newCall(bReq).execute()
            if (!bResp.isSuccessful) return null
            val bBody = bResp.body?.string() ?: return null

            val bRoot = JsonParser.parseString(bBody).asJsonObject
            val sections = bRoot.getAsJsonObject("contents")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return null

            for (sec in sections) {
                val shelf = sec.asJsonObject.getAsJsonObject("musicDescriptionShelfRenderer") ?: continue
                val runs = shelf.getAsJsonObject("description")?.getAsJsonArray("runs") ?: continue
                val sb = StringBuilder()
                for (r in runs) {
                    sb.append(r.asJsonObject.get("text")?.asString ?: "")
                }
                val resText = sb.toString().trim()
                if (resText.isNotBlank()) return resText
            }
        } catch (_: Exception) {}
        return null
    }

    fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:[.:](\\d{2,3}))?\\](.*)")

        for (rawLine in lrcContent.lines()) {
            val matcher = pattern.matcher(rawLine.trim())
            if (matcher.find()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msPart = matcher.group(3)
                val ms = when {
                    msPart == null -> 0L
                    msPart.length == 2 -> (msPart.toLongOrNull() ?: 0L) * 10
                    else -> msPart.toLongOrNull() ?: 0L
                }
                val timeMs = min * 60000 + sec * 1000 + ms
                val text = matcher.group(4)?.trim() ?: ""
                if (text.isNotBlank()) {
                    lines.add(LyricLine(timeMs, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun cleanSearchTerm(term: String): String {
        return term.replace(Regex("(?i)\\(feat\\.[^)]+\\)"), "")
            .replace(Regex("(?i)\\(speed up\\)"), "")
            .replace(Regex("(?i)\\(slowed\\)"), "")
            .replace(Regex("(?i)\\[[^\\]]+\\]"), "")
            .trim()
    }
}
