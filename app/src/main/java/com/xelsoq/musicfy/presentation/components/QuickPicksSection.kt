package com.xelsoq.musicfy.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xelsoq.musicfy.data.model.Song
import com.xelsoq.musicfy.data.preferences.QuickPicksDisplayMode

private val ListItemHeight = 64.dp
private val ListRows = 4
private const val QuickPicksLimit = 48

/**
 * Quick Picks section — layout inspired by ArchiveTune:
 * - LIST: LazyHorizontalGrid with 4 rows of song rows (YT Music style)
 * - CARD: horizontal hero cards with art + gradient + title overlay
 */
@Composable
fun QuickPicksSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    currentSongId: String? = null,
    displayMode: QuickPicksDisplayMode = QuickPicksDisplayMode.LIST,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    val distinctSongs = remember(songs) { songs.distinctBy { it.id }.take(QuickPicksLimit) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Picks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (onSeeAllClick != null) {
                FilledIconButton(
                    modifier = Modifier
                        .height(40.dp)
                        .width(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    onClick = onSeeAllClick
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "See all quick picks",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        when (displayMode) {
            QuickPicksDisplayMode.CARD -> QuickPicksCardMode(
                songs = distinctSongs,
                currentSongId = currentSongId,
                onSongClick = onSongClick
            )
            QuickPicksDisplayMode.LIST -> QuickPicksListMode(
                songs = distinctSongs,
                currentSongId = currentSongId,
                onSongClick = onSongClick
            )
        }
    }
}

@Composable
private fun QuickPicksListMode(
    songs: List<Song>,
    currentSongId: String?,
    onSongClick: (Song) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // ArchiveTune-style: ~half width on large screens, nearly full on phones
        val widthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val itemWidth = maxWidth * widthFactor
        val gridState = rememberLazyGridState()

        LazyHorizontalGrid(
            state = gridState,
            rows = GridCells.Fixed(ListRows),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(ListItemHeight * ListRows)
        ) {
            items(
                items = songs,
                key = { it.id },
                contentType = { "quick_pick_song" }
            ) { song ->
                QuickPickListItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = { onSongClick(song) },
                    modifier = Modifier
                        .width(itemWidth)
                        .height(ListItemHeight)
                )
            }
        }
    }
}

@Composable
private fun QuickPickListItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBg = if (isPlaying) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        Color.Transparent
    }
    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 200),
        label = "QuickPickListBg"
    )

    Card(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmartImage(
                model = song.albumArtUriString,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(48.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickPicksCardMode(
    songs: List<Song>,
    currentSongId: String?,
    onSongClick: (Song) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val heroHeight = when {
            maxWidth >= 840.dp -> 340.dp
            maxWidth >= 600.dp -> 300.dp
            else -> 260.dp
        }
        val cardWidth = (maxWidth - 48.dp).coerceIn(220.dp, 360.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            songs.take(20).forEach { song ->
                QuickPickHeroCard(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = { onSongClick(song) },
                    modifier = Modifier
                        .width(cardWidth)
                        .height(heroHeight)
                )
            }
        }
    }
}

@Composable
private fun QuickPickHeroCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.extraLarge
    Card(
        onClick = onClick,
        modifier = modifier
            .clip(shape)
            .then(
                if (isPlaying) {
                    Modifier.border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        shape
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SmartImage(
                model = song.albumArtUriString,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                shape = shape,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.12f),
                            1f to Color.Black.copy(alpha = 0.82f)
                        )
                    )
            )
            if (isPlaying) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
