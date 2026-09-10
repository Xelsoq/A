package com.xelsoq.musicfy.data.remote.youtube

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * YouTube Liked Songs sync removed — local library syncing disabled.
 * This class is kept as a stub so all injection call sites still compile.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LikedSongsSyncHelperEntryPoint {
    fun musicfyDatabase(): com.xelsoq.musicfy.data.database.MusicfyDatabase
    fun musicDao(): com.xelsoq.musicfy.data.database.MusicDao
    fun favoritesDao(): com.xelsoq.musicfy.data.database.FavoritesDao
}

object LikedSongsSyncHelper {
    /** No-op: YouTube liked songs sync has been removed. */
    fun syncLikedSongsIfNeeded(context: Context, scope: CoroutineScope) {
        // Sync removed intentionally
    }
}
