package com.xelsoq.musicfy.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.xelsoq.musicfy.R
import com.xelsoq.musicfy.data.preferences.PlayerLayoutConfig
import com.xelsoq.musicfy.data.preferences.PlayerLayoutGrid
import com.xelsoq.musicfy.data.preferences.PlayerLayoutItemId
import com.xelsoq.musicfy.data.preferences.PlayerLayoutSlot
import com.xelsoq.musicfy.presentation.components.MiniPlayerHeight
import com.xelsoq.musicfy.presentation.viewmodel.PlayerViewModel
import com.xelsoq.musicfy.presentation.viewmodel.SettingsViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerLayoutSettingsScreen(
    playerViewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val saved = uiState.playerLayoutConfig
    var draft by remember(saved) { mutableStateOf(saved) }
    var selectedItem by remember { mutableStateOf<PlayerLayoutItemId?>(PlayerLayoutItemId.ALBUM_ART) }
    val haptics = LocalHapticFeedback.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    fun commit(newConfig: PlayerLayoutConfig) {
        draft = newConfig
        settingsViewModel.setPlayerLayoutConfig(newConfig)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_player_layout_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            commit(draft.resetToDefault())
                        }
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = stringResource(R.string.settings_player_layout_reset)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = MiniPlayerHeight + navBarPadding + 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_player_layout_enable_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.settings_player_layout_enable_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = draft.enabled,
                    onCheckedChange = { enabled ->
                        commit(draft.copy(enabled = enabled))
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_player_layout_grid_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            PlayerLayoutGridEditor(
                config = draft,
                selectedItem = selectedItem,
                onSelectItem = { selectedItem = it },
                onPlaceAt = { id, col, row ->
                    val current = draft.slots[id]
                    val spanC = current?.colSpan ?: defaultSpan(id).first
                    val spanR = current?.rowSpan ?: defaultSpan(id).second
                    val slot = PlayerLayoutSlot(
                        col = col.coerceIn(0, PlayerLayoutGrid.COLUMNS - 1),
                        row = row.coerceIn(0, PlayerLayoutGrid.ROWS - 1),
                        colSpan = spanC.coerceAtMost(PlayerLayoutGrid.COLUMNS - col),
                        rowSpan = spanR.coerceAtMost(PlayerLayoutGrid.ROWS - row)
                    )
                    val next = draft.withSlot(id, slot)
                    if (next != draft) commit(next)
                }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_player_layout_items),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayerLayoutItemId.entries.forEach { id ->
                    FilterChip(
                        selected = selectedItem == id,
                        onClick = { selectedItem = id },
                        label = {
                            Text(
                                text = itemLabel(id),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = if (draft.slots.containsKey(id)) {
                            { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            val selected = selectedItem
            if (selected != null) {
                Spacer(Modifier.height(12.dp))
                val slot = draft.slots[selected]
                Text(
                    text = stringResource(
                        R.string.settings_player_layout_selected,
                        itemLabel(selected)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))

                if (slot != null) {
                    SizeControls(
                        label = stringResource(R.string.settings_player_layout_width),
                        value = slot.colSpan,
                        min = 1,
                        max = PlayerLayoutGrid.COLUMNS - slot.col,
                        onChange = { newSpan ->
                            commit(
                                draft.withSlot(
                                    selected,
                                    slot.copy(colSpan = newSpan)
                                )
                            )
                        }
                    )
                    SizeControls(
                        label = stringResource(R.string.settings_player_layout_height),
                        value = slot.rowSpan,
                        min = 1,
                        max = PlayerLayoutGrid.ROWS - slot.row,
                        onChange = { newSpan ->
                            commit(
                                draft.withSlot(
                                    selected,
                                    slot.copy(rowSpan = newSpan)
                                )
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = {
                            commit(draft.remove(selected))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_player_layout_remove_item))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settings_player_layout_tap_to_place),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = {
                            val (c, r) = defaultSpan(selected)
                            // Place at first free top-left cell that fits default span.
                            outer@ for (row in 0 until PlayerLayoutGrid.ROWS) {
                                for (col in 0 until PlayerLayoutGrid.COLUMNS) {
                                    val candidate = PlayerLayoutSlot(col, row, c, r)
                                    if (!candidate.isValid()) continue
                                    val next = draft.withSlot(selected, candidate)
                                    if (next.slots[selected] == candidate) {
                                        commit(next)
                                        break@outer
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_player_layout_add_item))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_player_layout_footer_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SizeControls(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        IconButton(
            onClick = { if (value > min) onChange(value - 1) },
            enabled = value > min
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = null)
        }
        Text(
            text = value.toString(),
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(
            onClick = { if (value < max) onChange(value + 1) },
            enabled = value < max
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
        }
    }
}

@Composable
private fun PlayerLayoutGridEditor(
    config: PlayerLayoutConfig,
    selectedItem: PlayerLayoutItemId?,
    onSelectItem: (PlayerLayoutItemId) -> Unit,
    onPlaceAt: (PlayerLayoutItemId, Int, Int) -> Unit
) {
    val cellColors = mapOf(
        PlayerLayoutItemId.ALBUM_ART to MaterialTheme.colorScheme.primaryContainer,
        PlayerLayoutItemId.METADATA to MaterialTheme.colorScheme.secondaryContainer,
        PlayerLayoutItemId.PROGRESS to MaterialTheme.colorScheme.tertiaryContainer,
        PlayerLayoutItemId.CONTROLS to MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    )
    val onColors = mapOf(
        PlayerLayoutItemId.ALBUM_ART to MaterialTheme.colorScheme.onPrimaryContainer,
        PlayerLayoutItemId.METADATA to MaterialTheme.colorScheme.onSecondaryContainer,
        PlayerLayoutItemId.PROGRESS to MaterialTheme.colorScheme.onTertiaryContainer,
        PlayerLayoutItemId.CONTROLS to MaterialTheme.colorScheme.onPrimary
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        val gap = 4.dp
        val cellW = (maxWidth - gap * (PlayerLayoutGrid.COLUMNS - 1)) / PlayerLayoutGrid.COLUMNS
        val cellH = cellW * 0.85f
        val gridHeight = cellH * PlayerLayoutGrid.ROWS + gap * (PlayerLayoutGrid.ROWS - 1)

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight)) {
            // Empty cells
            for (r in 0 until PlayerLayoutGrid.ROWS) {
                for (c in 0 until PlayerLayoutGrid.COLUMNS) {
                    val occupiedBy = config.slots.entries.firstOrNull { it.value.occupies(c, r) }?.key
                    Box(
                        modifier = Modifier
                            .padding(
                                start = (cellW + gap) * c,
                                top = (cellH + gap) * r
                            )
                            .size(cellW, cellH)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (occupiedBy == null) {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                                } else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val id = selectedItem ?: return@clickable
                                onPlaceAt(id, c, r)
                            }
                    )
                }
            }

            // Placed items (drawn on top)
            config.slots.forEach { (id, slot) ->
                val isSelected = selectedItem == id
                Box(
                    modifier = Modifier
                        .padding(
                            start = (cellW + gap) * slot.col,
                            top = (cellH + gap) * slot.row
                        )
                        .size(
                            width = cellW * slot.colSpan + gap * (slot.colSpan - 1),
                            height = cellH * slot.rowSpan + gap * (slot.rowSpan - 1)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(cellColors[id] ?: MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectItem(id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(id),
                        color = onColors[id] ?: MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun itemLabel(id: PlayerLayoutItemId): String = when (id) {
    PlayerLayoutItemId.ALBUM_ART -> stringResource(R.string.settings_player_layout_item_album)
    PlayerLayoutItemId.METADATA -> stringResource(R.string.settings_player_layout_item_metadata)
    PlayerLayoutItemId.PROGRESS -> stringResource(R.string.settings_player_layout_item_progress)
    PlayerLayoutItemId.CONTROLS -> stringResource(R.string.settings_player_layout_item_controls)
}

private fun defaultSpan(id: PlayerLayoutItemId): Pair<Int, Int> = when (id) {
    PlayerLayoutItemId.ALBUM_ART -> 4 to 3
    PlayerLayoutItemId.METADATA -> 4 to 1
    PlayerLayoutItemId.PROGRESS -> 4 to 1
    PlayerLayoutItemId.CONTROLS -> 4 to 1
}
