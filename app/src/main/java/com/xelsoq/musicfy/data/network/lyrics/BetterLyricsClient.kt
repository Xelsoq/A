package com.xelsoq.musicfy.data.network.lyrics

import android.util.Log
import com.xelsoq.musicfy.data.model.Lyrics
import com.xelsoq.musicfy.utils.LyricsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for BetterLyrics API (lyrics-api.boidu.dev) used by ArchiveTune.
 * Fetches Apple Music-style TTML with word-level timings and converts to Musicfy [Lyrics].
 */
@Singleton
class BetterLyricsClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "BetterLyricsClient"
        private const val BASE = "https://lyrics-api.boidu.dev/"
        private const val PATH_TTML = "getLyrics"
        private const val PATH_KUGOU = "kugou/getLyrics"
        private val TTML_ROOT = Regex("""<(?:[A-Za-z_][\w.-]*:)?tt(?:\s|>)""", RegexOption.IGNORE_CASE)
        private val TITLE_NOISE = Regex(
            """\s*[\(\[\{]?\s*(official\s+video|official\s+audio|lyrics?|lyric\s+video|audio|video|mv|hd|hq|remaster(?:ed)?|live|radio\s+edit|explicit|clean)\s*[\)\]\}]?""",
            RegexOption.IGNORE_CASE
        )
    }

    suspend fun fetchWordByWordLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Int = -1
    ): Lyrics? = withContext(Dispatchers.IO) {
        val cleanTitle = sanitizeTitle(title)
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return@withContext null

        // Try primary artist first, then first segment before comma (multi-artist fallback)
        val artistVariants = linkedSetOf(
            cleanArtist,
            cleanArtist.substringBefore(",").trim(),
            cleanArtist.substringBefore("&").trim(),
            cleanArtist.substringBefore(" feat", ignoreCase = true).trim(),
            cleanArtist.substringBefore(" ft.", ignoreCase = true).trim(),
        ).filter { it.isNotBlank() }

        var bestLineOnly: Lyrics? = null
        for (artistVariant in artistVariants) {
            for (path in listOf(PATH_TTML, PATH_KUGOU)) {
                val candidate = fetchFromEndpoint(
                    path = path,
                    title = cleanTitle,
                    artist = artistVariant,
                    album = album?.trim().orEmpty(),
                    durationSeconds = durationSeconds
                ) ?: continue

                val hasWords = candidate.synced?.any { !it.words.isNullOrEmpty() } == true
                val wordCount = candidate.synced?.sumOf { it.words?.size ?: 0 } ?: 0
                Log.d(
                    TAG,
                    "$path artist='$artistVariant' lines=${candidate.synced?.size} wordByWord=$hasWords words=$wordCount"
                )
                if (hasWords && wordCount >= 4) {
                    return@withContext candidate
                }
                if (bestLineOnly == null && !candidate.synced.isNullOrEmpty()) {
                    bestLineOnly = candidate
                }
            }
        }
        // Prefer returning nothing over line-only so callers fall back / keep searching
        null
    }

    private fun sanitizeTitle(title: String): String {
        var t = title.trim()
        // Strip common noise once
        repeat(3) {
            val next = TITLE_NOISE.replace(t, "").trim()
            if (next == t) return@repeat
            t = next
        }
        return t.ifBlank { title.trim() }
    }

    private fun fetchFromEndpoint(
        path: String,
        title: String,
        artist: String,
        album: String,
        durationSeconds: Int
    ): Lyrics? {
        return try {
            val urlBuilder = (BASE + path).toHttpUrl().newBuilder()
                .addQueryParameter("s", title)
                .addQueryParameter("a", artist)
            if (album.isNotBlank()) urlBuilder.addQueryParameter("al", album)
            if (durationSeconds > 0) urlBuilder.addQueryParameter("d", durationSeconds.toString())

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .header("User-Agent", "Musicfy/1.0 (Android; Music Player)")
                .header("Accept", "application/json, text/plain, */*")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "$path status ${response.code} for '$title' / '$artist'")
                    return null
                }
                val body = response.body?.string()?.removePrefix("\uFEFF") ?: return null
                Log.d(TAG, "$path bodyLen=${body.length}")
                val ttml = extractTtml(body) ?: run {
                    Log.d(TAG, "$path: no TTML in response")
                    return null
                }
                // LyricsUtils detects TTML and converts spans → SyncedWord
                val parsed = LyricsUtils.parseLyrics(ttml).copy(areFromRemote = true)
                if (parsed.synced.isNullOrEmpty() && parsed.plain.isNullOrEmpty()) {
                    Log.d(TAG, "$path: empty parse")
                    return null
                }
                val hasWords = parsed.synced?.any { !it.words.isNullOrEmpty() } == true
                if (!hasWords) {
                    Log.d(TAG, "$path: TTML parsed but no word timings (line-level only)")
                }
                parsed
            }
        } catch (e: Exception) {
            Log.w(TAG, "$path error: ${e.message}")
            null
        }
    }

    private fun extractTtml(raw: String): String? {
        if (TTML_ROOT.containsMatchIn(raw.take(4096))) return raw
        return try {
            val obj = JSONObject(raw)
            obj.optString("ttml").takeIf { it.isNotBlank() && TTML_ROOT.containsMatchIn(it.take(4096)) }
                ?: sequenceOf("lyrics", "data", "result", "response")
                    .mapNotNull { key ->
                        when {
                            !obj.has(key) -> null
                            obj.get(key) is String -> obj.optString(key).takeIf { it.isNotBlank() }
                            obj.get(key) is JSONObject -> {
                                val nested = obj.getJSONObject(key)
                                nested.optString("ttml").takeIf { it.isNotBlank() }
                                    ?: nested.optString("lyrics").takeIf { it.isNotBlank() }
                            }
                            else -> null
                        }
                    }
                    .firstOrNull { TTML_ROOT.containsMatchIn(it.take(4096)) }
        } catch (e: Exception) {
            Log.d(TAG, "extractTtml failed: ${e.message}")
            null
        }
    }
}
