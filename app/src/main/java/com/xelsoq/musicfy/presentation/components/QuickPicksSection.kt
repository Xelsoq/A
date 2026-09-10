package com.xelsoq.musicfy.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.size.Size
import com.xelsoq.musicfy.data.model.Song
import com.xelsoq.musicfy.data.preferences.QuickPicksDisplayMode
import com.xelsoq.musicfy.presentation.components.snapping.LazyGridSnapLayoutInfoProvider

/** Matches ArchiveTune `ListItemHeight`. */
private val ListItemHeight = 64.dp
private const val QuickPicksLimit = 48

/**
 * Quick Picks — ported 1:1 from ArchiveTune's Home screen:
 * - CARD: the real Material3 `HorizontalCenteredHeroCarousel` (the same
 *   component ArchiveTune uses) with `maskClip`/`maskBorder`, so side cards
 *   morph and squeeze exactly like the upstream hero carousel — that's what
 *   gives the parallax feel, not a manual scale/alpha hack.
 * - LIST: 4-row LazyHorizontalGrid (unchanged, already matched ArchiveTune)
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
        // Header ported from ArchiveTune's HomeSectionHeader: the whole row
        // is the tap target, title uses the Expressive titleLarge style, and
        // the chevron is a plain icon (no filled pill button around it).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .then(
                    if (onSeeAllClick != null) {
                        Modifier.clickable(onClick = onSeeAllClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Quick Picks",
                style = MaterialTheme.typography.titleLargeEmphasized,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (onSeeAllClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "See all quick picks",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)
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
        val heroMaxWidth = (maxWidth - 48.dp)
            .coerceAtLeast(232.dp)
            .coerceAtMost(440.dp)
        val density = LocalDensity.current
        val requestWidthPx = with(density) { heroMaxWidth.roundToPx().coerceAtLeast(1) }
        val requestHeightPx = with(density) { heroHeight.roundToPx().coerceAtLeast(1) }
        val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)

        // The real Material3 hero carousel: it centers the focused item and
        // peeks the neighbours, morphing/squeezing their mask as you drag —
        // that built-in behaviour IS the parallax effect, so no manual
        // scale/alpha math is needed here (unlike a plain HorizontalPager).
        HorizontalCenteredHeroCarousel(
            state = rememberCarouselState { songs.size },
            maxItemWidth = heroMaxWidth,
            itemSpacing = 10.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
        ) { index ->
            val song = songs[index]
            val isActive = song.id == currentSongId
            val imageTargetSize = remember(requestWidthPx, requestHeightPx) {
                Size(requestWidthPx, requestHeightPx)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .maskBorder(
                        BorderStroke(1.dp, borderColor),
                        MaterialTheme.shapes.extraLarge
                    )
                    .focusable()
                    .combinedClickable(
                        onClick = { onSongClick(song) },
                        onLongClick = { onSongLongClick?.invoke(song) }
                    )
            ) {
                SmartImage(
                    model = song.albumArtUriString,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    shape = RectangleShape,
                    targetSize = imageTargetSize,
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
                        style = MaterialTheme.typography.titleLargeEmphasized,
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
