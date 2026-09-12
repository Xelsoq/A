package com.xelsoq.musicfy.data.network.lyrics

import android.util.Log
import com.xelsoq.musicfy.data.model.Lyrics
import com.xelsoq.musicfy.utils.LyricsUtils
import com.xelsoq.musicfy.utils.TtmlLyricsParser
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
 * Returns TTML word-by-word lyrics when available, converted to Musicfy [Lyrics].
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
    }

    suspend fun fetchWordByWordLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Int = -1
    ): Lyrics? = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return@withContext null

        for (path in listOf(PATH_TTML, PATH_KUGOU)) {
            fetchFromEndpoint(path, cleanTitle, cleanArtist, album?.trim().orEmpty(), durationSeconds)
                ?.let { return@withContext it }
        }
        null
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

            val request = Request.Builder().url(urlBuilder.build()).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "$path status ${response.code}")
                    return null
                }
                val body = response.body?.string()?.removePrefix("\uFEFF") ?: return null
                val ttml = extractTtml(body) ?: return null
                ttmlToLyrics(ttml)
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
            sequenceOf("ttml", "lyrics", "data", "result", "response")
                .mapNotNull { key ->
                    when {
                        obj.has(key) && obj.get(key) is String -> obj.optString(key).takeIf { it.isNotBlank() }
                        obj.has(key) && obj.get(key) is JSONObject -> {
                            val nested = obj.getJSONObject(key)
                            nested.optString("ttml").takeIf { it.isNotBlank() }
                                ?: nested.optString("lyrics").takeIf { it.isNotBlank() }
                        }
                        else -> null
                    }
                }
                .firstOrNull { TTML_ROOT.containsMatchIn(it.take(4096)) }
        } catch (_: Exception) {
            null
        }
    }

    private fun ttmlToLyrics(ttml: String): Lyrics? {
        val enhancedLrc = TtmlLyricsParser.parseToEnhancedLrc(ttml) ?: return null
        val parsed = LyricsUtils.parseLyrics(enhancedLrc)
        return if (parsed.synced != null || parsed.plain != null) {
            parsed.copy(areFromRemote = true)
        } else null
    }
}
