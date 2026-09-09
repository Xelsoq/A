package com.xelsoq.musicfy.data

import com.xelsoq.musicfy.shared.WearLibraryItem

data class WearLocalQueueState(
    val items: List<WearLibraryItem> = emptyList(),
    val currentIndex: Int = -1,
)
