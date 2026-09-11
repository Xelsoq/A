package com.xelsoq.musicfy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xelsoq.musicfy.ui.theme.MusicfyStatusBarStyle
import androidx.compose.ui.res.stringResource
import com.xelsoq.musicfy.R
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun CollapsibleCommonTopBar(
    title: String,
    collapseFraction: Float,
    headerHeight: Dp,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    collapsedTitleStartPadding: Dp = 68.dp,
    expandedTitleStartPadding: Dp = 20.dp,
    collapsedTitleEndPadding: Dp = 24.dp,
    expandedTitleEndPadding: Dp = 24.dp,
    containerHeightRange: Pair<Dp, Dp> = 88.dp to 56.dp,
    titleStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    titleScaleRange: Pair<Float, Float> = 1.2f to 0.8f,
    titleFontSizeRange: Pair<TextUnit, TextUnit>? = null,
    maxLines: Int = 1,
    collapsedSubtitleMaxLines: Int = 1,
    expandedSubtitleMaxLines: Int = 1,
    containerColor: Color? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fadeSubtitleOnCollapse: Boolean = true,
    enableCollapsedTitleWidthCompression: Boolean = true,
    enableExpandedTitleWidthCompression: Boolean = true,
    titleWidthCompressionThreshold: Dp? = null,
    titleMinWidthAxis: Float = 78f,
    syncStatusBarWithContainer: Boolean = true,
    /**
     * When provided, uses the Home-style transparent top bar with progressive blur fade
     * + dark tint. Caller must place [hazeSource] on the scrolling content with the same state.
     * When null, falls back to the previous solid alpha background based on collapseFraction.
     */
    hazeState: HazeState? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val useBlurFade = hazeState != null

    // solidAlpha only used when not in blur mode (legacy solid fill on collapse).
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)

    val backgroundColor = when {
        useBlurFade -> Color.Transparent
        containerColor != null -> containerColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = solidAlpha)
    }
    val statusBarFallbackColor = if (useBlurFade) {
        Color.Black.copy(alpha = 0.45f).compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        backgroundColor.compositeOver(MaterialTheme.colorScheme.surface)
    }

    if (syncStatusBarWithContainer) {
        MusicfyStatusBarStyle(
            color = statusBarFallbackColor,
            useDarkIcons = !useBlurFade && MaterialTheme.colorScheme.surface.luminance() > 0.5f
        )
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Same height as Home: status bar + ~half (or a bit more) of the top bar area.
    val blurFadeHeight = statusBarHeight + 56.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(backgroundColor)
            .zIndex(5f)
    ) {
        // Progressive blur + dark tint layer (Home style). Drawn under the top bar content
        // so icons/title stay sharp. Only active when hazeState is provided by the caller.
        if (hazeState != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(blurFadeHeight)
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = Color.Transparent,
                            tints = listOf(
                                HazeTint(Color.Black.copy(alpha = 0.45f))
                            ),
                            blurRadius = 28.dp,
                            noiseFactor = 0f
                        )
                    ) {
                        progressive = HazeProgressive.verticalGradient(
                            startIntensity = 1f,
                            endIntensity = 0f,
                            preferPerformance = false
                        )
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ExpressiveTopBarContent(
                title = title,
                collapseFraction = collapseFraction,
                modifier = Modifier.fillMaxSize(),
                subtitle = subtitle,
                collapsedTitleStartPadding = collapsedTitleStartPadding,
                expandedTitleStartPadding = expandedTitleStartPadding,
                collapsedTitleEndPadding = collapsedTitleEndPadding,
                expandedTitleEndPadding = expandedTitleEndPadding,
                containerHeightRange = containerHeightRange,
                titleStyle = titleStyle,
                titleScaleRange = titleScaleRange,
                titleFontSizeRange = titleFontSizeRange,
                maxLines = maxLines,
                collapsedSubtitleMaxLines = collapsedSubtitleMaxLines,
                expandedSubtitleMaxLines = expandedSubtitleMaxLines,
                contentColor = contentColor,
                subtitleColor = subtitleColor,
                fadeSubtitleOnCollapse = fadeSubtitleOnCollapse,
                enableCollapsedTitleWidthCompression = enableCollapsedTitleWidthCompression,
                enableExpandedTitleWidthCompression = enableExpandedTitleWidthCompression,
                titleWidthCompressionThreshold = titleWidthCompressionThreshold,
                titleMinWidthAxis = titleMinWidthAxis,
                supportingContent = supportingContent
            )

            FilledIconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 4.dp)
                    .zIndex(1f),
                onClick = onBackClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.common_back)
                )
            }

            // Actions (e.g. Equalizer toggle)
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp) // Align with back button
                    .zIndex(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
        }
    }
}
