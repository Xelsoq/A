package com.xelsoq.musicfy.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xelsoq.musicfy.data.model.Song
import com.xelsoq.musicfy.data.preferences.CarouselStyle
import com.xelsoq.musicfy.data.preferences.QuickPicksDisplayMode
import com.xelsoq.musicfy.presentation.components.snapping.LazyGridSnapLayoutInfoProvider

/** Matches ArchiveTune `ListItemHeight`. */
private val ListItemHeight = 64.dp
private const val QuickPicksLimit = 48

/** Material3 extraLarge-equivalent — used for every hero card, matching ArchiveTune's QuickPicksSection. */
private val HeroCorner = RoundedCornerShape(28.dp)

/**
 * Quick Picks, styled after ArchiveTune's `QuickPicksSection`:
 * - CARD: a real parallax hero carousel — [RoundedHorizontalMultiBrowseCarousel] (this app's
 *   own faithful reimplementation of Material3's experimental Carousel, already used by the
 *   player's album art carousel) in its centered "two peek" style: one large focused card with
 *   a small peek of the previous/next card on either side, true continuous mask-clip + resize
 *   as you drag — the same engine ArchiveTune's `HorizontalCenteredHeroCarousel` uses, not a
 *   fake scale/alpha approximation.
 * - LIST: 4-row LazyHorizontalGrid, unchanged.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPicksSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    currentSongId: String? = null,
    isPlaying: Boolean = true,
    displayMode: QuickPicksDisplayMode = QuickPicksDisplayMode.LIST,
    onSongLongClick: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    val distinctSongs = remember(songs) { songs.distinctBy { it.id }.take(QuickPicksLimit) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
            QuickPicksDisplayMode.CARD -> QuickPicksHeroCarousel(
                songs = distinctSongs,
                currentSongId = currentSongId,
                isPlaying = isPlaying,
                onSongClick = onSongClick,
                onSongLongClick = onSongLongClick
            )
            QuickPicksDisplayMode.LIST -> QuickPicksHorizontalList(
                songs = distinctSongs,
                currentSongId = currentSongId,
                isPlaying = isPlaying,
                onSongClick = onSongClick,
                onSongLongClick = onSongLongClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun QuickPicksHeroCarousel(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onSongLongClick: ((Song) -> Unit)?
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val heroHeight = when {
            maxWidth >= 840.dp -> 380.dp
            maxWidth >= 600.dp -> 356.dp
            else -> 332.dp
        }
        val carouselState = rememberCarouselState(itemCount = { songs.size })
        val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
        val haptic = LocalHapticFeedback.current

        RoundedHorizontalMultiBrowseCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
            itemSpacing = 10.dp,
            itemCornerRadius = 28.dp,
            // Large focused card centered, small peek of the previous/next card on either
            // side — this is ArchiveTune's "centered hero" look.
            carouselStyle = CarouselStyle.TWO_PEEK,
            carouselWidth = maxWidth,
            itemKey = { index -> songs[index].id }
        ) { index ->
            val song = songs[index]
            val isActive = song.id == currentSongId

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(HeroCorner)
                    .maskBorder(BorderStroke(1.dp, borderColor), HeroCorner)
                    .focusable()
                    .combinedClickable(
                        onClick = { onSongClick(song) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSongLongClick?.invoke(song)
                        }
                    )
            ) {
                SmartImage(
                    model = song.albumArtUriString,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.48f to Color.Black.copy(alpha = 0.08f),
                                1f to Color.Black.copy(alpha = 0.84f)
                            )
                        )
                )

                if (isActive && isPlaying) {
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
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
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
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPicksHorizontalList(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onSongLongClick: ((Song) -> Unit)?
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val widthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val itemWidth = maxWidth * widthFactor
        val gridState = rememberLazyGridState()
        // Matches ArchiveTune: items snap to a centered position instead of
        // free-scrolling, using the same positionInLayout formula.
        val snapLayoutInfoProvider = remember(gridState, widthFactor) {
            LazyGridSnapLayoutInfoProvider(
                lazyGridState = gridState,
                positionInLayout = { layoutSize, itemSize ->
                    layoutSize * widthFactor / 2f - itemSize / 2f
                }
            )
        }

        LazyHorizontalGrid(
            state = gridState,
            rows = GridCells.Fixed(4),
            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(ListItemHeight * 4)
        ) {
            items(
                items = songs,
                key = { it.id },
                contentType = { "quick_pick_song" }
            ) { song ->
                val isActive = song.id == currentSongId
                ArchiveStyleSongRow(
                    song = song,
                    isActive = isActive,
                    isPlaying = isActive && isPlaying,
                    onClick = { onSongClick(song) },
                    onLongClick = { onSongLongClick?.invoke(song) },
                    modifier = Modifier
                        .width(itemWidth)
                        .height(ListItemHeight)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArchiveStyleSongRow(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmartImage(
            model = song.albumArtUriString,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.size(48.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
