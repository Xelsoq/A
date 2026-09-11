package com.xelsoq.musicfy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xelsoq.musicfy.data.model.youtube.Cookies
import com.xelsoq.musicfy.data.remote.youtube.DatastoreRepository
import com.xelsoq.musicfy.data.repository.MusicRepository
import com.xelsoq.musicfy.data.telegram.TelegramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import unshoo.ianshulyadav.pixelmusic.innertube.YouTube

enum class ExternalServiceAccount {
    TELEGRAM,
    YOUTUBE
}

data class ExternalAccountUiModel(
    val service: ExternalServiceAccount,
    val title: String,
    val accountLabel: String,
    val syncedContentLabel: String,
    val isLoggingOut: Boolean
)

data class AccountsUiState(
    val connectedAccounts: List<ExternalAccountUiModel> = emptyList(),
    val disconnectedServices: List<ExternalServiceAccount> = emptyList()
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val telegramRepository: TelegramRepository,
    private val musicRepository: MusicRepository,
    private val youtubeDatastoreRepository: DatastoreRepository
) : ViewModel() {

    private val loggingOutServices = MutableStateFlow<Set<ExternalServiceAccount>>(emptySet())

    private val telegramStateFlow = combine(
        telegramRepository.authorizationState
            .map { it is TdApi.AuthorizationStateReady }
            .distinctUntilChanged(),
        musicRepository.getAllTelegramChannels().map { it.size }
    ) { connected, channelCount ->
        connected to channelCount
    }

    private val youtubeStateFlow = youtubeDatastoreRepository.cookies.map { cookies ->
        val raw = cookies.toRawCookie()
        val connected = raw.isNotBlank() &&
            (raw.contains("SAPISID") || raw.contains("__Secure-3PAPISID"))
        connected to 0
    }

    val uiState: StateFlow<AccountsUiState> = combine(
        telegramStateFlow,
        youtubeStateFlow,
        loggingOutServices
    ) { telegram, youtube, activeLogouts ->
        val (telegramConnected, telegramChannelCount) = telegram
        val (youtubeConnected, _) = youtube

        val connectedAccounts = buildList {
            if (telegramConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.TELEGRAM,
                        title = "Telegram",
                        accountLabel = "Active Telegram session",
                        syncedContentLabel = formatCount(
                            count = telegramChannelCount,
                            singular = "synced channel",
                            plural = "synced channels"
                        ),
                        isLoggingOut = ExternalServiceAccount.TELEGRAM in activeLogouts
                    )
                )
            }
            if (youtubeConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.YOUTUBE,
                        title = "YouTube Music",
                        accountLabel = "Signed in to YouTube Music",
                        syncedContentLabel = "Library & streaming",
                        isLoggingOut = ExternalServiceAccount.YOUTUBE in activeLogouts
                    )
                )
            }
        }

        val disconnectedServices = buildList {
            if (!telegramConnected) add(ExternalServiceAccount.TELEGRAM)
            if (!youtubeConnected) add(ExternalServiceAccount.YOUTUBE)
        }

        AccountsUiState(
            connectedAccounts = connectedAccounts,
            disconnectedServices = disconnectedServices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun logout(service: ExternalServiceAccount) {
        if (service in loggingOutServices.value) return

        viewModelScope.launch {
            loggingOutServices.update { it + service }
            try {
                runCatching {
                    when (service) {
                        ExternalServiceAccount.TELEGRAM -> {
                            telegramRepository.logout()
                            telegramRepository.clearMemoryCache()
                            musicRepository.clearTelegramData()
                        }
                        ExternalServiceAccount.YOUTUBE -> {
                            youtubeDatastoreRepository.saveCookies(Cookies(""))
                            youtubeDatastoreRepository.saveDataSyncId("")
                            YouTube.cookie = null
                            YouTube.dataSyncId = null
                        }
                    }
                }
            } finally {
                loggingOutServices.update { it - service }
            }
        }
    }

    private fun formatCount(count: Int, singular: String, plural: String): String {
        return if (count == 1) {
            "1 $singular"
        } else {
            "$count $plural"
        }
    }
}
