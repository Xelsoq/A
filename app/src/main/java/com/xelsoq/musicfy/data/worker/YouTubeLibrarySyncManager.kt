package com.xelsoq.musicfy.data.worker

import com.xelsoq.musicfy.data.database.MusicDao
import com.xelsoq.musicfy.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube library sync — simplified for Musicfy.
 * Full liked-songs / subscriptions sync requires deeper schema (channel_id, insertYoutubeSongs).
 * Catalog/home (Quick Picks) works via InnerTube without this sync.
 */
@Singleton
class YouTubeLibrarySyncManager @Inject constructor(
    private val musicDao: MusicDao,
    private val musicRepository: MusicRepository,
) {
    suspend fun syncSubscribedArtists() = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("syncSubscribedArtists: skipped (schema not fully wired)")
    }

    suspend fun syncLikedSongs() = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("syncLikedSongs: skipped (schema not fully wired)")
    }

    suspend fun syncAll() {
        syncSubscribedArtists()
        syncLikedSongs()
    }

    companion object {
        private const val TAG = "YouTubeLibrarySync"
    }
}
