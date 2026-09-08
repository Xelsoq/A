package com.xelsoq.musicfy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xelsoq.musicfy.data.preferences.UserPreferencesRepository
import com.xelsoq.musicfy.data.repository.MusicRepository
import com.xelsoq.musicfy.data.worker.SyncManager
import com.xelsoq.musicfy.data.remote.youtube.DatastoreRepository
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import com.xelsoq.musicfy.data.worker.SyncProgress
import com.xelsoq.musicfy.utils.LogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val youtubeDatastoreRepository: DatastoreRepository,
    musicRepository: MusicRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    init {
        // Keep InnerTube auth in sync with stored YouTube cookies / dataSyncId
        viewModelScope.launch {
            youtubeDatastoreRepository.cookies.collect { cookies ->
                val raw = cookies.toRawCookie()
                YouTube.cookie = raw
                Timber.tag("MainViewModel").d("Synced YouTube cookies (len=%d)", raw.length)
            }
        }
        viewModelScope.launch {
            youtubeDatastoreRepository.dataSyncId.collect { id ->
                YouTube.dataSyncId = id
                Timber.tag("MainViewModel").d("Synced YouTube dataSyncId")
            }
        }
        // Bootstrap visitorData so anonymous / logged-in requests work
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (YouTube.visitorData.isNullOrBlank()) {
                    YouTube.visitorData().getOrNull()?.let { YouTube.visitorData = it }
                }
            } catch (e: Exception) {
                Timber.tag("MainViewModel").w(e, "Failed to bootstrap visitorData")
            }
        }
    }

    val isSetupComplete: StateFlow<Boolean?> = userPreferencesRepository.initialSetupDoneFlow
        .map { it as Boolean? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val hasCompletedInitialSync: StateFlow<Boolean> = userPreferencesRepository.lastSyncTimestampFlow
        .map { it > 0L }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true // 乐观策略：默认已同步
        )

    /**
     * Un Flow que emite `true` si el SyncWorker está encolado o en ejecución.
     * Ideal para mostrar un indicador de carga.
     */
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    /**
     * Flow that exposes detailed sync progress including file count and phase.
     */
    val syncProgress: StateFlow<SyncProgress> = syncManager.syncProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncProgress()
        )

    /**
     * Un Flow que emite `true` si la base de datos de Room no tiene canciones.
     * Nos ayuda a saber si es la primera vez que se abre la app.
     */
    val isLibraryEmpty: StateFlow<Boolean> = musicRepository
        .getAudioFiles()
        .map { it.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Función para iniciar la sincronización de la biblioteca de música.
     * Se debe llamar después de que los permisos hayan sido concedidos.
     */
    fun startSync() {
        LogUtils.i(this, "startSync called")
        viewModelScope.launch {
            // For fresh installs after setup, SetupViewModel.setSetupComplete() triggers sync
            // For returning users (setup already complete), we trigger sync here
            if (isSetupComplete.value == true) {
                syncManager.sync()
            }
        }
    }
}
