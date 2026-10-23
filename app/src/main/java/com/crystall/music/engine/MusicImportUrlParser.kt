package com.crystall.music.engine

import java.net.URI
import java.net.URLDecoder
import java.util.Locale

internal enum class ImportProvider { YOUTUBE, SOUNDCLOUD }

internal data class ParsedImportUrl(
    val provider: ImportProvider,
    val url: String,
    val videoId: String? = null,
    val playlistId: String? = null
)

internal object MusicImportUrlParser {
    private val candidatePattern = Regex("(?i)https?://[^\\s<>\\\"']+")
    private val videoIdPattern = Regex("[A-Za-z0-9_-]{11}")
    private val playlistIdPattern = Regex("[A-Za-z0-9_-]{5,}")
    private val soundCloudHosts = setOf("soundcloud.com", "www.soundcloud.com", "m.soundcloud.com", "on.soundcloud.com")
    private val youtubeHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")

    fun parse(text: String): ParsedImportUrl? {
        for (match in candidatePattern.findAll(text)) {
            val candidate = match.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
            val uri = runCatching { URI(candidate) }.getOrNull() ?: continue
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: continue
            if (scheme != "https" || uri.rawUserInfo != null) continue
            val host = uri.host?.lowercase(Locale.ROOT) ?: continue
            if (uri.port != -1 && uri.port != 443) continue

            if (host in soundCloudHosts) {
                if (uri.path.isNullOrBlank() || uri.path == "/") continue
                return ParsedImportUrl(ImportProvider.SOUNDCLOUD, uri.toASCIIString())
            }
            if (host !in youtubeHosts) continue

            val params = queryParameters(uri.rawQuery)
            val playlistId = params["list"]?.takeIf { playlistIdPattern.matches(it) }
            val videoId = when {
                host == "youtu.be" -> uri.path.orEmpty().trim('/').substringBefore('/').takeIf(videoIdPattern::matches)
                uri.path == "/watch" -> params["v"]?.takeIf(videoIdPattern::matches)
                else -> uri.path.orEmpty().split('/').let { parts ->
                    if (parts.size >= 3 && parts[1] in setOf("embed", "shorts", "live", "v")) {
                        parts[2].takeIf(videoIdPattern::matches)
                    } else null
                }
            }
            if (playlistId != null || videoId != null) {
                return ParsedImportUrl(ImportProvider.YOUTUBE, uri.toASCIIString(), videoId, playlistId)
            }
        }
        return null
    }

    private fun queryParameters(rawQuery: String?): Map<String, String> = rawQuery.orEmpty()
        .split('&')
        .mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator < 0) return@mapNotNull null
            val key = runCatching { URLDecoder.decode(part.substring(0, separator), "UTF-8") }.getOrNull()
            val value = runCatching { URLDecoder.decode(part.substring(separator + 1), "UTF-8") }.getOrNull()
            if (key == null || value == null) null else key to value
        }
        .toMap()
}
