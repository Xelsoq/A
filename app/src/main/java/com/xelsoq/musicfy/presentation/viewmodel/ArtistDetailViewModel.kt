package com.xelsoq.musicfy.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xelsoq.musicfy.R
import com.xelsoq.musicfy.data.model.Artist
import com.xelsoq.musicfy.data.model.Song
import com.xelsoq.musicfy.data.remote.youtube.toNativeSong
import com.xelsoq.musicfy.data.repository.ArtistImageRepository
import com.xelsoq.musicfy.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import unshoo.ianshulyadav.pixelmusic.innertube.models.AlbumItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.ArtistItem
import unshoo.ianshulyadav.pixelmusic.innertube.models.SongItem
import javax.inject.Inject

data class ArtistDetailUiState(
    val artist: Artist? = null,
    val songs: List<Song> = emptyList(),
    val albumSections: List<ArtistAlbumSection> = emptyList(),
    val effectiveImageUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOnlineArtist: Boolean = false,
    val artistDescription: String? = null,
    val browseId: String? = null,
)

@Immutable
data class ArtistAlbumSection(
    val albumId: Long,
    val title: String,
    val year: Int?,
    val albumArtUriString: String?,
    val songs: List<Song>,
    val browseId: String? = null,
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val artistImageRepository: ArtistImageRepository,
    val themeStateHolder: ThemeStateHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    private val _artistColorScheme = MutableStateFlow<ColorSchemePair?>(null)
    val artistColorScheme: StateFlow<ColorSchemePair?> = _artistColorScheme.asStateFlow()

    init {
        savedStateHandle.getStateFlow<String?>("artistId", null)
            .onEach { idString ->
                if (idString != null) {
                    loadArtistData(idString)
                } else {
                    _uiState.update {
                        it.copy(
                            error = context.getString(R.string.artist_detail_id_not_found),
                            isLoading = false
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private var currentLoadJob: Job? = null

    private fun loadArtistData(artistIdStr: String) {
        currentLoadJob?.cancel()
        currentLoadJob = viewModelScope.launch {
            Log.d("ArtistDebug", "loadArtistData: idStr=$artistIdStr")
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val numericId = artistIdStr.toLongOrNull()
                var browseId: String? = null

                if (artistIdStr.startsWith("UC") || artistIdStr.startsWith("LA") ||
                    artistIdStr.startsWith("MP") || (numericId == null && artistIdStr.isNotBlank())
                ) {
                    browseId = artistIdStr
                } else if (numericId != null) {
                    SearchStateHolder.artistIdMap[numericId]?.let { browseId = it }
                    if (browseId == null) {
                        val localArtist = musicRepository.getArtistById(numericId).first()
                        if (localArtist != null) {
                            // Still try to enrich with YouTube when possible, but local
                            // library remains the source of truth for tracks.
                            val primaryArtistName = localArtist.name.split(
                                ", ", " & ", " feat.", " feat ", " Feat.", " Feat ",
                                " FT.", " FT ", " ft.", " ft "
                            ).firstOrNull()?.trim() ?: localArtist.name
                            // Prefer local path for local artists
                            loadLocalArtist(numericId)
                            return@launch
                        }
                    }
                }

                if (browseId != null && (
                        browseId.startsWith("UC") || browseId.startsWith("LA") ||
                            browseId.startsWith("MP") || numericId == null
                        )
                ) {
                    val artistPageResult = withContext(Dispatchers.IO) {
                        YouTube.artist(browseId!!)
                    }
                    artistPageResult.onSuccess { artistPage ->
                        val artistItem = artistPage.artist
                        val channelId = browseId!!

                        // Map channel id for later navigation / playArtist
                        val artistLongId = -(17_000_000_000_000L +
                            kotlin.math.abs(channelId.hashCode().toLong()))
                        SearchStateHolder.artistIdMap[artistLongId] = channelId

                        // Popular / Songs shelf
                        val ytSongsSection = artistPage.sections.find {
                            it.title.contains("Songs", ignoreCase = true) ||
                                it.title.contains("Popular", ignoreCase = true) ||
                                it.title.contains("Top", ignoreCase = true)
                        }
                        val popularSongs = ytSongsSection?.items
                            ?.mapNotNull { (it as? SongItem)?.toNativeSong() }
                            ?.take(25)
                            .orEmpty()

                        // Collect album-like items from Albums / Releases / Singles / EPs
                        fun sectionAlbumItems(predicate: (String) -> Boolean): List<AlbumItem> =
                            artistPage.sections
                                .filter { predicate(it.title) }
                                .flatMap { section -> section.items.filterIsInstance<AlbumItem>() }

                        val albumItems = (
                            sectionAlbumItems {
                                it.contains("Album", ignoreCase = true) ||
                                    it.contains("Release", ignoreCase = true)
                            } + sectionAlbumItems {
                                it.contains("Single", ignoreCase = true) ||
                                    it.contains("EP", ignoreCase = true)
                            }
                            )
                            .distinctBy { it.browseId }
                            .take(10)

                        // Register browseIds and hydrate a few albums with tracks (parallel)
                        val hydratedAlbumSections = coroutineScope {
                            albumItems.map { album ->
                                async(Dispatchers.IO) {
                                    val longId = ytAlbumLongId(album.browseId, album.title)
                                    SearchStateHolder.albumIdMap[longId] = album.browseId
                                    val tracks = YouTube.album(album.browseId)
                                        .getOrNull()
                                        ?.songs
                                        ?.map { it.toNativeSong() }
                                        .orEmpty()
                                    ArtistAlbumSection(
                                        albumId = longId,
                                        title = album.title,
                                        year = album.year,
                                        albumArtUriString = album.thumbnail,
                                        songs = tracks,
                                        browseId = album.browseId,
                                    )
                                }
                            }.awaitAll()
                        }

                        val albumSections = buildList {
                            if (popularSongs.isNotEmpty()) {
                                add(
                                    ArtistAlbumSection(
                                        albumId = -(18_000_000_000_000L +
                                            kotlin.math.abs(channelId.hashCode().toLong())),
                                        title = "Popular",
                                        year = null,
                                        albumArtUriString = artistItem.thumbnail,
                                        songs = popularSongs,
                                        browseId = null,
                                    )
                                )
                            }
                            addAll(hydratedAlbumSections)
                        }

                        val allSongs = buildList {
                            addAll(popularSongs)
                            hydratedAlbumSections.forEach { addAll(it.songs) }
                        }.distinctBy { it.id }

                        val artistModel = Artist(
                            id = artistLongId,
                            name = artistItem.title,
                            songCount = allSongs.size.coerceAtLeast(popularSongs.size),
                            imageUrl = artistItem.thumbnail
                        )

                        val effectiveUrl = artistItem.thumbnail
                        val newScheme = if (!effectiveUrl.isNullOrBlank()) {
                            try {
                                themeStateHolder.getOrGenerateColorScheme(effectiveUrl)
                            } catch (e: Exception) {
                                Log.w("ArtistDebug", "Failed to warm color scheme: ${e.message}")
                                null
                            }
                        } else null

                        _artistColorScheme.value = newScheme
                        _uiState.value = ArtistDetailUiState(
                            artist = artistModel,
                            songs = allSongs,
                            albumSections = albumSections,
                            effectiveImageUrl = effectiveUrl,
                            isLoading = false,
                            isOnlineArtist = true,
                            artistDescription = artistPage.description,
                            browseId = channelId,
                        )
                    }.onFailure { e ->
                        if (numericId != null) {
                            loadLocalArtist(numericId)
                        } else {
                            _uiState.update {
                                it.copy(
                                    error = context.getString(
                                        R.string.artist_error_loading_artist,
                                        e.localizedMessage ?: ""
                                    ),
                                    isLoading = false
                                )
                            }
                        }
                    }
                    return@launch
                }

                val id = numericId ?: run {
                    _uiState.update {
                        it.copy(
                            error = context.getString(R.string.artist_detail_invalid_id),
                            isLoading = false
                        )
                    }
                    return@launch
                }
                loadLocalArtist(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = context.getString(
                            R.string.artist_error_loading_artist,
                            e.localizedMessage ?: ""
                        ),
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun loadLocalArtist(id: Long) {
        val artistDetailsFlow = musicRepository.getArtistById(id)
        val artistSongsFlow = musicRepository.getSongsForArtist(id)

        combine(artistDetailsFlow, artistSongsFlow) { artist, songs ->
            Log.d("ArtistDebug", "loadLocalArtist: id=$id found=${artist != null} songs=${songs.size}")
            artist to songs
        }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        error = context.getString(
                            R.string.artist_error_loading_artist,
                            e.localizedMessage ?: ""
                        ),
                        isLoading = false
                    )
                }
            }
            .collect { (artist, songs) ->
                if (artist == null) {
                    _uiState.update {
                        it.copy(
                            error = context.getString(R.string.artist_detail_id_not_found),
                            isLoading = false
                        )
                    }
                    return@collect
                }

                val orderedSongs = songs.sortedWith(songDisplayComparator)
                val albumSections = buildAlbumSections(orderedSongs)

                val effectiveUrl = artist.effectiveImageUrl
                val newScheme = if (!effectiveUrl.isNullOrBlank()) {
                    try {
                        themeStateHolder.getOrGenerateColorScheme(effectiveUrl)
                    } catch (e: Exception) {
                        Log.w("ArtistDebug", "Failed to warm color scheme: ${e.message}")
                        null
                    }
                } else null

                _artistColorScheme.value = newScheme
                _uiState.value = ArtistDetailUiState(
                    artist = artist.copy(
                        imageUrl = if (artist.customImageUri.isNullOrBlank()) effectiveUrl else artist.imageUrl
                    ),
                    songs = orderedSongs,
                    albumSections = albumSections,
                    effectiveImageUrl = effectiveUrl,
                    isLoading = false
                )
            }
    }

    fun setCustomImage(sourceUri: Uri) {
        val artistId = _uiState.value.artist?.id ?: return
        viewModelScope.launch {
            try {
                val internalPath = artistImageRepository.setCustomArtistImage(context, artistId, sourceUri)
                if (!internalPath.isNullOrBlank()) {
                    val oldEffectiveUrl = _uiState.value.effectiveImageUrl

                    // Regenerate palette from the new image url — invalidate old and warm-up new
                    if (!oldEffectiveUrl.isNullOrBlank() && oldEffectiveUrl != internalPath) {
                        themeStateHolder.forceRegenerateColorScheme(oldEffectiveUrl)
                    }
                    val newScheme = try {
                        themeStateHolder.forceRegenerateColorScheme(internalPath)
                        themeStateHolder.getOrGenerateColorScheme(internalPath)
                    } catch (e: Exception) {
                        Log.w("ArtistDebug", "Failed to regenerate color scheme for custom image: ${e.message}")
                        null
                    }

                    _artistColorScheme.value = newScheme
                    _uiState.update { state ->
                        // Cache-busting: add timestamp to internalPath to force Coil to reload
                        val effectiveUrlWithBust = "$internalPath?t=${System.currentTimeMillis()}"
                        state.copy(
                            effectiveImageUrl = effectiveUrlWithBust,
                            artist = state.artist?.copy(customImageUri = internalPath)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ArtistDebug", "Failed to set custom image: ${e.message}")
            }
        }
    }

    /**
     * Called when the user wants to revert to the Deezer-sourced image.
     */
    fun clearCustomImage() {
        val artist = _uiState.value.artist ?: return
        viewModelScope.launch {
            try {
                val oldEffectiveUrl = _uiState.value.effectiveImageUrl
                artistImageRepository.clearCustomArtistImage(context, artist.id)

                // Fall back to Deezer URL
                val deezerUrl = artistImageRepository.getArtistImageUrl(artist.name, artist.id)
                val newEffectiveUrl = deezerUrl.takeIf { !it.isNullOrBlank() }

                // Invalidate old custom image palette
                if (!oldEffectiveUrl.isNullOrBlank()) {
                    themeStateHolder.forceRegenerateColorScheme(oldEffectiveUrl)
                }

                val newScheme = if (!newEffectiveUrl.isNullOrBlank()) {
                    try {
                        themeStateHolder.getOrGenerateColorScheme(newEffectiveUrl)
                    } catch (e: Exception) {
                        Log.w("ArtistDebug", "Failed to regenerate palette after clear: ${e.message}")
                        null
                    }
                } else null

                _artistColorScheme.value = newScheme
                _uiState.update { state ->
                    state.copy(
                        effectiveImageUrl = newEffectiveUrl,
                        artist = state.artist?.copy(customImageUri = null, imageUrl = deezerUrl)
                    )
                }

            } catch (e: Exception) {
                Log.e("ArtistDebug", "Failed to clear custom image: ${e.message}")
            }
        }
    }

    fun removeSongFromAlbumSection(songId: String) {
        _uiState.update { currentState ->
            val updatedAlbumSections = currentState.albumSections.map { section ->
                val updatedSongs = section.songs.filterNot { it.id == songId }
                section.copy(songs = updatedSongs)
            }.filter { it.songs.isNotEmpty() }

            currentState.copy(
                albumSections = updatedAlbumSections,
                songs = currentState.songs.filterNot { it.id == songId }
            )
        }
    }
}


private fun ytAlbumLongId(browseId: String, title: String = ""): Long {
    val seed = browseId.ifBlank { title }.lowercase()
    return -(16_000_000_000_000L + kotlin.math.abs(seed.hashCode().toLong()))
}

private val songDisplayComparator = compareBy<Song> { it.discNumber ?: 1 }
    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
    .thenBy { it.title.lowercase() }

private fun buildAlbumSections(songs: List<Song>): List<ArtistAlbumSection> {
    if (songs.isEmpty()) return emptyList()

    val sections = songs
        .groupBy { it.albumId to it.album }
        .map { (key, albumSongs) ->
            val sortedSongs = albumSongs.sortedWith(songDisplayComparator)
            val albumYear = albumSongs.mapNotNull { song -> song.year.takeIf { it > 0 } }.maxOrNull()
            val albumArtUri = albumSongs.firstNotNullOfOrNull { it.albumArtUriString }
            ArtistAlbumSection(
                albumId = key.first,
                title = (key.second.takeIf { it.isNotBlank() } ?: "Unknown Album"),
                year = albumYear,
                albumArtUriString = albumArtUri,
                songs = sortedSongs
            )
        }

    val (withYear, withoutYear) = sections.partition { it.year != null }
    val withYearSorted = withYear.sortedWith(
        compareByDescending<ArtistAlbumSection> { it.year ?: Int.MIN_VALUE }
            .thenBy { it.title.lowercase() }
    )
    val withoutYearSorted = withoutYear.sortedBy { it.title.lowercase() }

    return withYearSorted + withoutYearSorted
}
