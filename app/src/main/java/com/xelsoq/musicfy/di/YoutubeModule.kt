package com.xelsoq.musicfy.di

import android.content.Context
import com.xelsoq.musicfy.data.remote.youtube.DatastoreRepository
import com.xelsoq.musicfy.data.remote.youtube.DownloadRepository
import com.xelsoq.musicfy.data.remote.youtube.ExoCache
import com.xelsoq.musicfy.data.remote.youtube.SongRepository
import com.xelsoq.musicfy.data.remote.youtube.YoutubePlaylistDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YoutubeModule {

    @Provides
    @Singleton
    fun provideExoCache(@ApplicationContext context: Context): ExoCache {
        return ExoCache(context)
    }

    @Provides
    @Singleton
    fun provideDatastoreRepository(@ApplicationContext context: Context): DatastoreRepository {
        return DatastoreRepository(context)
    }

    @Provides
    @Singleton
    fun provideDownloadRepository(@ApplicationContext context: Context): DownloadRepository {
        return DownloadRepository(context)
    }

    @Provides
    @Singleton
    fun provideSongRepository(): SongRepository {
        return SongRepository()
    }

    @Provides
    @Singleton
    fun provideYoutubePlaylistDataSource(): YoutubePlaylistDataSource {
        return YoutubePlaylistDataSource()
    }
}
