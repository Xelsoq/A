package com.xelsoq.musicfy.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xelsoq.musicfy.R
import com.xelsoq.musicfy.data.model.Album
import com.xelsoq.musicfy.data.model.Song
import com.xelsoq.musicfy.data.remote.youtube.toNativeSong
import com.xelsoq.musicfy.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube as InnerTubeYouTube
import javax.inject.Inject

data class AlbumDetailUiState(
    val album: Album? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        val albumIdString: String? = savedStateHandle.get("albumId")
        if (albumIdString != null) {
            val albumId = albumIdString.toLongOrNull()
            val mappedBrowseId = albumId?.let { SearchStateHolder.albumIdMap[it] }
            when {
                mappedBrowseId != null -> loadOnlineAlbumData(mappedBrowseId)
                // Non-numeric id: treat as YouTube browseId directly
                albumId == null -> loadOnlineAlbumData(albumIdString)
                // Local library album
                else -> loadAlbumData(albumId)
            }
        } else {
            _uiState.update {
                it.copy(
                    error = context.getString(R.string.album_detail_id_not_found),
                    isLoading = false
                )
            }
        }
    }

    private fun loadAlbumData(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val albumDetailsFlow = musicRepository.getAlbumById(id)
                val albumSongsFlow = musicRepository.getSongsForAlbum(id)

                combine(albumDetailsFlow, albumSongsFlow) { album, songs ->
                    if (album != null) {
                        AlbumDetailUiState(
                            album = album,
                            songs = songs.sortedWith(
                                compareBy<Song> { it.discNumber ?: 1 }
                                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                                    .thenBy { it.title.lowercase() }
                            ),
                            isLoading = false
                        )
                    } else {
                        // Local miss — try YouTube map as fallback
                        val browseId = SearchStateHolder.albumIdMap[id]
                        if (browseId != null) {
                            loadOnlineAlbumData(browseId)
                            return@combine _uiState.value.copy(isLoading = true)
                        }
                        AlbumDetailUiState(
                            error = context.getString(R.string.album_detail_not_found),
                            isLoading = false
                        )
                    }
                }
                    .catch { e ->
                        emit(
                            AlbumDetailUiState(
                                error = context.getString(
                                    R.string.album_detail_error_loading_album,
                                    e.localizedMessage ?: ""
                                ),
                                isLoading = false
                            )
                        )
                    }
                    .collect { newState ->
                        _uiState.value = newState
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = context.getString(
                            R.string.album_detail_error_loading_album,
                            e.localizedMessage ?: ""
                        ),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun update(songs: List<Song>) {
        _uiState.update {
            it.copy(
                isLoading = false,
                songs = songs
            )
        }
    }

    private fun loadOnlineAlbumData(browseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    InnerTubeYouTube.album(browseId)
                }
                result.onSuccess { albumPage ->
                    val albumItem = albumPage.album
                    val longId = -(16_000_000_000_000L +
                        kotlin.math.abs(albumItem.browseId.hashCode().toLong()))
                    SearchStateHolder.albumIdMap[longId] = albumItem.browseId

                    val albumModel = Album(
                        id = longId,
                        title = albumItem.title,
                        artist = albumItem.artists?.joinToString { it.name }.orEmpty(),
                        year = albumItem.year ?: 0,
                        dateAdded = System.currentTimeMillis(),
                        albumArtUriString = albumItem.thumbnail,
                        songCount = albumPage.songs.size,
                        albumArtist = albumItem.artists?.joinToString { it.name }
                    )
                    val songsModels = albumPage.songs.map { it.toNativeSong() }

                    _uiState.value = AlbumDetailUiState(
                        album = albumModel,
                        songs = songsModels,
                        isLoading = false,
                        error = null
                    )
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = e.localizedMessage ?: "Failed to load online album",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.localizedMessage, isLoading = false)
                }
            }
        }
    }
}
