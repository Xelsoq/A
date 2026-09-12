package com.xelsoq.musicfy.data.preferences

import kotlinx.serialization.Serializable

/**
 * Customizable full-player layout on a fixed 4-column × 7-row grid
 * (phone-widget style). Each item occupies a rectangular region.
 */
object PlayerLayoutGrid {
    const val COLUMNS = 4
    const val ROWS = 7
}

@Serializable
enum class PlayerLayoutItemId {
    ALBUM_ART,
    METADATA,
    PROGRESS,
    CONTROLS
}

@Serializable
data class PlayerLayoutSlot(
    val col: Int = 0,
    val row: Int = 0,
    val colSpan: Int = 1,
    val rowSpan: Int = 1
) {
    fun isValid(): Boolean {
        if (col < 0 || row < 0) return false
        if (colSpan < 1 || rowSpan < 1) return false
        if (col + colSpan > PlayerLayoutGrid.COLUMNS) return false
        if (row + rowSpan > PlayerLayoutGrid.ROWS) return false
        return true
    }

    fun occupies(c: Int, r: Int): Boolean =
        c in col until (col + colSpan) && r in row until (row + rowSpan)
}

@Serializable
data class PlayerLayoutConfig(
    /** When false, the stock portrait/landscape layout is used. */
    val enabled: Boolean = false,
    val slots: Map<PlayerLayoutItemId, PlayerLayoutSlot> = defaultSlots()
) {
    companion object {
        fun defaultSlots(): Map<PlayerLayoutItemId, PlayerLayoutSlot> = mapOf(
            PlayerLayoutItemId.ALBUM_ART to PlayerLayoutSlot(col = 0, row = 0, colSpan = 4, rowSpan = 4),
            PlayerLayoutItemId.METADATA to PlayerLayoutSlot(col = 0, row = 4, colSpan = 4, rowSpan = 1),
            PlayerLayoutItemId.PROGRESS to PlayerLayoutSlot(col = 0, row = 5, colSpan = 4, rowSpan = 1),
            PlayerLayoutItemId.CONTROLS to PlayerLayoutSlot(col = 0, row = 6, colSpan = 4, rowSpan = 1)
        )

        fun default(): PlayerLayoutConfig = PlayerLayoutConfig(
            enabled = false,
            slots = defaultSlots()
        )
    }

    fun withSlot(id: PlayerLayoutItemId, slot: PlayerLayoutSlot): PlayerLayoutConfig {
        if (!slot.isValid()) return this
        // Prevent overlapping other items (keep current item's area free).
        val others = slots.filterKeys { it != id }
        for ((_, other) in others) {
            for (r in slot.row until (slot.row + slot.rowSpan)) {
                for (c in slot.col until (slot.col + slot.colSpan)) {
                    if (other.occupies(c, r)) return this
                }
            }
        }
        return copy(slots = slots + (id to slot))
    }

    fun remove(id: PlayerLayoutItemId): PlayerLayoutConfig =
        copy(slots = slots - id)

    fun resetToDefault(): PlayerLayoutConfig =
        PlayerLayoutConfig(enabled = enabled, slots = defaultSlots())
}
