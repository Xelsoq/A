package com.xelsoq.musicfy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import eightbitlab.com.blurview.BlurView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import com.xelsoq.musicfy.ui.theme.MusicfyStatusBarStyle
import androidx.compose.ui.res.stringResource
import com.xelsoq.musicfy.R


@Composable
private fun TopBarBackdropBlur(
    modifier: Modifier = Modifier,
    fadeHeight: Dp = 104.dp,
    blurRadius: Float = 22f
) {
    val localView = LocalView.current
    val rootView = localView.rootView as? ViewGroup

    if (rootView != null) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(fadeHeight)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to androidx.compose.ui.graphics.Color.Transparent,
                            0.62f to androidx.compose.ui.graphics.Color.Transparent,
                            1f to androidx.compose.ui.graphics.Color.Black
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                },
            factory = { context ->
                BlurView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)

                    // Android may invoke AndroidView creation/measurement from inside
                    // Compose's own measure/layout pass. BlurView snapshots its target
                    // hierarchy when it is configured, so doing that synchronously here
                    // can trigger: "performMeasureAndLayout called during measure layout".
                    // Defer setup until the current layout pass has completely finished.
                    post {
                        if (!isAttachedToWindow) return@post

                        setupWith(rootView)
                            .setBlurRadius(blurRadius)
                            .setBlurAutoUpdate(true)
                    }
                }
            }
        )
    }
}

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
    supportingContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    // Logic from GenreDetailScreen:
    // solidAlpha goes from 0 to 1 as collapseFraction goes from 0 to 0.5 (approx).
    // Actually GenreDetailScreen uses: (collapseFraction * 2f).coerceIn(0f, 1f)
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    
    val backgroundColor = containerColor ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = solidAlpha * 0.45f)
    val statusBarFallbackColor = backgroundColor.compositeOver(MaterialTheme.colorScheme.surface)

    if (syncStatusBarWithContainer) {
        MusicfyStatusBarStyle(color = statusBarFallbackColor)
    }
    // We can also fade the content color if we want, but usually onSurface is fine.
    // GenreDetail interpolates content color, but for standard screens onSurface is usually correct for both states 
    // (transparent surface vs surfaceContainer).
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(backgroundColor)
            .zIndex(5f)
    ) {
        TopBarBackdropBlur(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(-1f)
        )

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
