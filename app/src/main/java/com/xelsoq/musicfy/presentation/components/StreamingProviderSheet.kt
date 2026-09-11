package com.xelsoq.musicfy.presentation.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xelsoq.musicfy.R
import com.xelsoq.musicfy.presentation.telegram.auth.TelegramLoginActivity
import com.xelsoq.musicfy.ui.theme.GoogleSansRounded

/**
 * Bottom sheet for streaming providers.
 * Only YouTube Music and Telegram are offered; other cloud providers were removed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingProviderSheet(
    onDismissRequest: () -> Unit,
    isYoutubeLoggedIn: Boolean = false,
    onNavigateToYoutubeAuth: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
) {
    val context = LocalContext.current
    val providerSegmentContainerShape = RoundedCornerShape(20.dp)
    val providerSegmentItemShape = RoundedCornerShape(8.dp)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.topbar_cloud_streaming_title),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.topbar_cloud_streaming_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = GoogleSansRounded,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = providerSegmentContainerShape,
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clip(providerSegmentContainerShape),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProviderRow(
                        iconPainter = painterResource(R.drawable.ic_youtube),
                        iconTint = Color(0xFFFF0000),
                        title = "YouTube Music",
                        subtitle = if (isYoutubeLoggedIn) {
                            "Connected · Library & streaming"
                        } else {
                            "Sign in to stream & sync library"
                        },
                        shape = providerSegmentItemShape,
                        isConnected = isYoutubeLoggedIn,
                        onClick = {
                            onNavigateToYoutubeAuth()
                            onDismissRequest()
                        }
                    )

                    ProviderRow(
                        iconPainter = painterResource(R.drawable.telegram),
                        iconTint = Color(0xFF2AABEE),
                        title = "Telegram",
                        subtitle = "Stream from channels & chats",
                        shape = providerSegmentItemShape,
                        onClick = {
                            context.startActivity(
                                Intent(context, TelegramLoginActivity::class.java)
                            )
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(
    iconPainter: Painter,
    iconTint: Color,
    title: String,
    subtitle: String,
    shape: RoundedCornerShape,
    enabled: Boolean = true,
    isConnected: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = when {
        isConnected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    }
    val titleColor = MaterialTheme.colorScheme.onSurface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val arrowContainerColor = when {
        isConnected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val arrowTint = when {
        isConnected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    }
    val iconTileShape = RoundedCornerShape(14.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.62f)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = containerColor
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = GoogleSansRounded,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(iconTileShape)
                        .background(iconTint.copy(alpha = if (enabled) 0.14f else 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = iconTint
                    )
                }
            },
            trailingContent = {
                Surface(
                    shape = CircleShape,
                    color = arrowContainerColor
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .size(26.dp),
                        tint = arrowTint
                    )
                }
            }
        )
    }
}
