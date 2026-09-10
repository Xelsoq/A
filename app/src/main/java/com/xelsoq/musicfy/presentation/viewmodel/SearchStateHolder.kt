package com.xelsoq.musicfy.presentation.viewmodel

import android.util.LruCache
import com.xelsoq.musicfy.data.model.Album
import com.xelsoq.musicfy.data.model.Artist
import com.xelsoq.musicfy.data.model.Playlist
import com.xelsoq.musicfy.data.model.SearchFilterType
import com.xelsoq.musicfy.data.model.SearchHistoryItem
import com.xelsoq.musicfy.data.model.SearchResultItem
import com.xelsoq.musicfy.data.preferences.SearchSource
import com.xelsoq.musicfy.data.preferences.UserPreferencesRepository
import com.xelsoq.musicfy.data.remote.youtube.toNativeSong
import com.xelsoq.musicfy.data.repository.MusicRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import unshoo.ianshulyadav.pixelmusic.innertube.models.AlbumItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.ArtistItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.PlaylistItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.SongItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.filterVideo
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages search state for both local library and YouTube Music online search.
 */
@Singleton
class SearchStateHolder @Inject constructor(
    private val musicRepository: MusicRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val SEARCH_CACHE_SIZE = 100
        /** Maps synthetic album Long ids to YouTube browseIds for later album playback. */
        val albumIdMap = java.util.concurrent.ConcurrentHashMap<Long, String>()
    }

    private val searchResultCache = LruCache<String, ImmutableList<SearchResultItem>>(SEARCH_CACHE_SIZE)

    private data class SearchRequest(
        val query: String,
        val requestId: Long,
    )

    private val _searchResults = MutableStateFlow<ImmutableList<SearchResultItem>>(persistentListOf())
    val searchResults = _searchResults.asStateFlow()

    private val _selectedSearchFilter = MutableStateFlow(SearchFilterType.ALL)
    val selectedSearchFilter = _selectedSearchFilter.asStateFlow()

    private val _searchHistory = MutableStateFlow<ImmutableList<SearchHistoryItem>>(persistentListOf())
    val searchHistory = _searchHistory.asStateFlow()

    private val searchRequests = MutableSharedFlow<SearchRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val latestSearchRequestId = AtomicLong(0L)

    private var scope: CoroutineScope? = null
    private var searchJob: Job? = null

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
        observeSearchRequests()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchRequests() {
        searchJob?.cancel()
        searchJob = scope?.launch {
            searchRequests
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { request ->
                    val normalizedQuery = request.query

                    if (normalizedQuery.isBlank()) {
                        if (_searchResults.value.isNotEmpty()) {
                            _searchResults.value = persistentListOf()
                        }
                        return@collectLatest
                    }

                    try {
                        val source = userPreferencesRepository.searchSourceFlow.first()
                        if (source == SearchSource.LOCAL) {
                            performLocalSearch(normalizedQuery, request.requestId)
                        } else {
                            performOnlineSearch(normalizedQuery, request.requestId)
                        }
                    } catch (_: CancellationException) {
                        // Superseded by a newer query; ignore.
                    } catch (e: Exception) {
                        if (request.requestId == latestSearchRequestId.get()) {
                            Timber.e(e, "Error performing search for query: $normalizedQuery")
                            _searchResults.value = persistentListOf()
                        }
                    }
                }
        }
    }

    private suspend fun performLocalSearch(normalizedQuery: String, requestId: Long) {
        val currentFilter = _selectedSearchFilter.value
        musicRepository.searchAll(normalizedQuery, currentFilter).collect { resultsList ->
            val sortedResults = resultsList.sortedWith(
                compareBy { result ->
                    when (result) {
                        is SearchResultItem.SongItem -> 0
                        is SearchResultItem.AlbumItem -> 1
                        is SearchResultItem.ArtistItem -> 2
                        is SearchResultItem.PlaylistItem -> 3
                    }
                }
            )
            if (requestId != latestSearchRequestId.get()) return@collect
            val immutableResults = sortedResults.toImmutableList()
            if (_searchResults.value != immutableResults) {
                _searchResults.value = immutableResults
            }
        }
    }

    private suspend fun performOnlineSearch(normalizedQuery: String, requestId: Long) {
        val cacheKey = "${normalizedQuery.lowercase()}|${_selectedSearchFilter.value.name}"
        searchResultCache.get(cacheKey)?.let { cached ->
            if (requestId == latestSearchRequestId.get()) {
                _searchResults.value = cached
            }
            return
        }

        val results = withContext(Dispatchers.IO) {
            searchYouTube(normalizedQuery, _selectedSearchFilter.value)
        }

        if (requestId != latestSearchRequestId.get()) return

        val immutable = results.toImmutableList()
        searchResultCache.put(cacheKey, immutable)
        _searchResults.value = immutable
    }

    private suspend fun searchYouTube(
        query: String,
        filter: SearchFilterType
    ): List<SearchResultItem> {
        val pureYtMusicOnly = userPreferencesRepository.pureYtMusicOnlyFlow.first()
        val items = mutableListOf<SearchResultItem>()

        when (filter) {
            SearchFilterType.ALL -> {
                coroutineScope {
                    val songsDeferred = async {
                        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                    }
                    val artistsDeferred = async {
                        YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                    }
                    val albumsDeferred = async {
                        YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
                    }
                    val playlistsDeferred = async {
                        YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()
                    }

                    val songsResult = songsDeferred.await()
                    val artistsResult = artistsDeferred.await()
                    val albumsResult = albumsDeferred.await()
                    val playlistsResult = playlistsDeferred.await()

                    val songsList = songsResult?.items
                        ?.filterIsInstance<SongItem>()
                        ?.filterVideo(pureYtMusicOnly)
                        .orEmpty()

                    // Top / popular songs first when available
                    songsResult?.items
                        ?.filterIsInstance<SongItem>()
                        ?.filterVideo(pureYtMusicOnly)
                        ?.take(3)
                        ?.forEach { items.add(SearchResultItem.SongItem(it.toNativeSong())) }

                    songsList.drop(3).forEach { song ->
                        items.add(SearchResultItem.SongItem(song.toNativeSong()))
                    }

                    artistsResult?.items?.filterIsInstance<ArtistItem>()?.forEach { a ->
                        items.add(
                            SearchResultItem.ArtistItem(
                                Artist(
                                    id = ytArtistId(a.title),
                                    name = a.title,
                                    songCount = 0,
                                    imageUrl = a.thumbnail
                                )
                            )
                        )
                    }

                    albumsResult?.items?.filterIsInstance<AlbumItem>()?.forEach { a ->
                        val longId = ytAlbumId(a.title)
                        albumIdMap[longId] = a.browseId
                        items.add(
                            SearchResultItem.AlbumItem(
                                Album(
                                    id = longId,
                                    title = a.title,
                                    artist = a.artists?.joinToString { it.name }.orEmpty(),
                                    year = a.year ?: 0,
                                    dateAdded = System.currentTimeMillis(),
                                    albumArtUriString = a.thumbnail,
                                    songCount = 0
                                )
                            )
                        )
                    }

                    playlistsResult?.items?.filterIsInstance<PlaylistItem>()?.forEach { p ->
                        items.add(
                            SearchResultItem.PlaylistItem(
                                Playlist(
                                    id = p.id,
                                    name = p.title,
                                    songIds = emptyList(),
                                    coverImageUri = p.thumbnail,
                                    source = "YOUTUBE"
                                )
                            )
                        )
                    }
                }
            }
            SearchFilterType.SONGS -> {
                val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                result?.items
                    ?.filterIsInstance<SongItem>()
                    ?.filterVideo(pureYtMusicOnly)
                    ?.forEach { items.add(SearchResultItem.SongItem(it.toNativeSong())) }
            }
            SearchFilterType.ARTISTS -> {
                val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                result?.items?.filterIsInstance<ArtistItem>()?.forEach { a ->
                    items.add(
                        SearchResultItem.ArtistItem(
                            Artist(
                                id = ytArtistId(a.title),
                                name = a.title,
                                songCount = 0,
                                imageUrl = a.thumbnail
                            )
                        )
                    )
                }
            }
            SearchFilterType.ALBUMS -> {
                val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
                result?.items?.filterIsInstance<AlbumItem>()?.forEach { a ->
                    val longId = ytAlbumId(a.title)
                    albumIdMap[longId] = a.browseId
                    items.add(
                        SearchResultItem.AlbumItem(
                            Album(
                                id = longId,
                                title = a.title,
                                artist = a.artists?.joinToString { it.name }.orEmpty(),
                                year = a.year ?: 0,
                                dateAdded = System.currentTimeMillis(),
                                albumArtUriString = a.thumbnail,
                                songCount = 0
                            )
                        )
                    )
                }
            }
            SearchFilterType.PLAYLISTS -> {
                val result = YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()
                result?.items?.filterIsInstance<PlaylistItem>()?.forEach { p ->
                    items.add(
                        SearchResultItem.PlaylistItem(
                            Playlist(
                                id = p.id,
                                name = p.title,
                                songIds = emptyList(),
                                coverImageUri = p.thumbnail,
                                source = "YOUTUBE"
                            )
                        )
                    )
                }
            }
        }
        return items
    }

    fun updateSearchFilter(filterType: SearchFilterType) {
        _selectedSearchFilter.value = filterType
    }

    fun performSearch(query: String) {
        val normalizedQuery = query.trim()
        val requestId = latestSearchRequestId.incrementAndGet()
        if (normalizedQuery.isBlank()) {
            if (_searchResults.value.isNotEmpty()) {
                _searchResults.value = persistentListOf()
            }
        }
        searchRequests.tryEmit(SearchRequest(normalizedQuery, requestId))
    }

    fun loadSearchHistory(limit: Int = 15) {
        scope?.launch {
            try {
                val history = withContext(Dispatchers.IO) {
                    musicRepository.getRecentSearchHistory(limit)
                }
                _searchHistory.value = history.toImmutableList()
            } catch (e: Exception) {
                Timber.e(e, "Error loading search history")
            }
        }
    }

    fun onSearchQuerySubmitted(query: String) {
        scope?.launch {
            if (query.isNotBlank()) {
                try {
                    withContext(Dispatchers.IO) {
                        musicRepository.addSearchHistoryItem(query)
                    }
                    loadSearchHistory()
                } catch (e: Exception) {
                    Timber.e(e, "Error adding search history item")
                }
            }
        }
    }

    fun deleteSearchHistoryItem(query: String) {
        scope?.launch {
            try {
                withContext(Dispatchers.IO) {
                    musicRepository.deleteSearchHistoryItemByQuery(query)
                }
                loadSearchHistory()
            } catch (e: Exception) {
                Timber.e(e, "Error deleting search history item")
            }
        }
    }

    fun clearSearchHistory() {
        scope?.launch {
            try {
                withContext(Dispatchers.IO) {
                    musicRepository.clearSearchHistory()
                }
                _searchHistory.value = persistentListOf()
            } catch (e: Exception) {
                Timber.e(e, "Error clearing search history")
            }
        }
    }

    fun onCleared() {
        searchJob?.cancel()
        scope = null
    }

    private fun ytArtistId(name: String): Long =
        -(17_000_000_000_000L + kotlin.math.abs(name.lowercase().hashCode().toLong()))

    private fun ytAlbumId(name: String): Long =
        -(16_000_000_000_000L + kotlin.math.abs(name.lowercase().hashCode().toLong()))
}
