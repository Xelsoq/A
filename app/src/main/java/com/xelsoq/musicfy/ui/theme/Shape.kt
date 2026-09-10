package com.xelsoq.musicfy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// extraSmall/extraLarge added to match ArchiveTune's expressive shape scale.
// Missing `extraLarge` (used by HorizontalCenteredHeroCarousel's maskClip/maskBorder
// on the Quick Picks CARD carousel) was falling back to the Material3 default,
// which — combined with the plain (non-expressive) MaterialTheme — made the
// carousel's mask/parallax clip render as a plain square instead of a rounded shape.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)