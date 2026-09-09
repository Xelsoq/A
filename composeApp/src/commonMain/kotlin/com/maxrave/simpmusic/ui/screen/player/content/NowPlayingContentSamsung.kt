package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.PlayerControlLayout
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.SamsungColors
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlin.math.roundToLong

/**
 * Samsung Music / One UI–inspired Now Playing content layer.
 *
 * Keeps SimpMusic YouTube Music playback + [NowPlayingContentState] / [NowPlayingContentActions]
 * contract. Visual language adapted from Samsung Music Port resources (full_player, mini_player,
 * SESL primary #0381FE, One UI spacing) — not a binary/layout copy.
 *
 * Layout (portrait-first, One UI player feel):
 * - Adaptive gradient backdrop from palette colors
 * - Large centered album art with soft rounded corners + light stroke
 * - Title / artist (marquee)
 * - Thin seekbar + time labels
 * - Transport: shuffle · prev · play/pause · next · repeat (Samsung blue when active)
 * - Bottom actions: queue, lyrics, like, more
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingContentSamsung(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val density = LocalDensity.current
    val screenInfo = getScreenSizeInfo()
    val isRepeatOne = state.controllerState.repeatState is RepeatState.One

    val controlsAlpha by animateFloatAsState(
        targetValue = if (state.showControlLayout) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = if (state.showControlLayout) 180 else 500,
                easing = LinearEasing,
            ),
        label = "samsungControlsAlpha",
    )

    // Palette-driven backdrop (same sources the shell already animates for other styles).
    val topColor = state.startColor.value
    val bottomColor = state.endColor.value
    val backdrop =
        Brush.verticalGradient(
            colors =
                listOf(
                    topColor.copy(alpha = 0.92f),
                    bottomColor.copy(alpha = 0.98f),
                    SamsungColors.WindowDark,
                ),
        )

    val statusTop =
        with(density) {
            WindowInsets.statusBars.getTop(density).toDp()
        }
    val systemBottom =
        with(density) {
            WindowInsets.systemBars.getBottom(density).toDp()
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(SamsungColors.WindowDark)
                .background(backdrop),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = statusTop)
                    .alpha(controlsAlpha.coerceAtLeast(0.35f)),
        ) {
            // Top bar: dismiss + more
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = actions.onDismiss) {
                    Icon(
                        imageVector = state.dismissIcon,
                        contentDescription = null,
                        tint = SamsungColors.ControlIcon,
                    )
                }
                IconButton(onClick = actions.onShowMoreSheet) {
                    Icon(
                        imageVector = SimpIcons.MoreVert,
                        contentDescription = null,
                        tint = SamsungColors.ControlIcon,
                    )
                }
            }

            // Artwork zone — large, centered, One UI rounded square
            val artMax = (screenInfo.hDP * 0.38f).coerceIn(220f, 360f).dp
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalPager(
                    state = state.artworkPagerState,
                    modifier =
                        Modifier
                            .size(artMax)
                            .clip(RoundedCornerShape(20.dp)),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isRepeatOne && state.artworkQueue.isNotEmpty(),
                ) { page ->
                    val track = state.artworkQueue.getOrNull(page)
                    val url =
                        track?.thumbnails?.maxByOrNull { it.width * it.height }?.url
                            ?: state.screenData.thumbnailURL
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        placeholder = rememberHolderPainter(),
                        error = rememberHolderPainter(),
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    width = 1.dp,
                                    color = SamsungColors.AlbumStroke,
                                    shape = RoundedCornerShape(20.dp),
                                ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Title + artist
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.screenData.nowPlayingTitle,
                    color = SamsungColors.TitleOnDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.screenData.isExplicit) {
                        ExplicitBadge(modifier = Modifier.size(18.dp).padding(end = 4.dp))
                    }
                    Text(
                        text = state.screenData.artistName,
                        color = SamsungColors.SubtitleOnDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { actions.onNavigateToArtist() },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Seekbar — thin One UI style
            val progressColor =
                if (state.timelineState.isCrossfading) {
                    state.sliderTrackColor
                } else {
                    SamsungColors.TitleOnDark
                }
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Slider(
                    value = state.sliderValue.coerceIn(0f, 100f),
                    onValueChange = actions.onSliderChange,
                    onValueChangeFinished = actions.onSliderChangeFinished,
                    valueRange = 0f..100f,
                    colors =
                        SliderDefaults.colors(
                            thumbColor = progressColor,
                            activeTrackColor = progressColor,
                            inactiveTrackColor = SamsungColors.SeekTrack,
                        ),
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text =
                            formatDuration(
                                (state.timelineState.total * (state.sliderValue / 100f)).roundToLong(),
                            ),
                        color = SamsungColors.SubtitleOnDark,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = formatDuration(state.timelineState.total),
                        color = SamsungColors.SubtitleOnDark,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Transport — reuse shared layout with Samsung active blue
            PlayerControlLayout(
                controllerState = state.controllerState,
                isSmallSize = false,
                plainPlayPause = false,
                horizontalPadding = 12.dp,
                activeColor = SamsungColors.Primary,
                contentColor = SamsungColors.ControlIcon,
                onUIEvent = actions.onUIEvent,
            )

            Spacer(Modifier.height(8.dp))

            // Bottom action row (queue / lyrics / like / playlist)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = systemBottom + 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions.onShowQueue) {
                    Icon(
                        imageVector = SimpIcons.QueueMusic,
                        contentDescription = null,
                        tint = SamsungColors.ControlIcon,
                    )
                }
                IconButton(onClick = actions.onShowFullscreenLyrics) {
                    Icon(
                        imageVector = SimpIcons.Lyrics,
                        contentDescription = null,
                        tint = SamsungColors.ControlIcon,
                    )
                }
                IconButton(onClick = { actions.onUIEvent(UIEvent.ToggleLike) }) {
                    Crossfade(targetState = state.likeStatus, label = "samsungLike") { liked ->
                        Icon(
                            imageVector = if (liked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                            contentDescription = null,
                            tint = if (liked) SamsungColors.Primary else SamsungColors.ControlIcon,
                        )
                    }
                }
                IconButton(onClick = actions.onShowAddToPlaylist) {
                    Icon(
                        imageVector = SimpIcons.PlaylistAdd,
                        contentDescription = null,
                        tint = SamsungColors.ControlIcon,
                    )
                }
            }
        }

        // When controls are hidden (e.g. canvas focus patterns), a tap restores them.
        if (!state.showControlLayout) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = actions.onToggleControls,
                        ),
            )
        }

        // Current lyric peek (One UI often shows a line above controls)
        AnimatedVisibility(
            visible = state.currentLyricLineIndex > -1 && state.showControlLayout,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = systemBottom + 100.dp)
                    .padding(horizontal = 32.dp),
        ) {
            val line =
                state.screenData.lyricsData
                    ?.lyrics
                    ?.lines
                    ?.getOrNull(state.currentLyricLineIndex)
                    ?.words
                    ?.stripRichSyncTimestamps()
                    .orEmpty()
            if (line.isNotBlank()) {
                Text(
                    text = line,
                    color = SamsungColors.SubtitleOnDark,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
