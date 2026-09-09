package com.maxrave.simpmusic.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color tokens adapted from Samsung Music / One UI (SESL) player surfaces.
 *
 * Source references (decompiled Samsung Music Port):
 * - full_player_background #FCFCFC
 * - sesl_primary_color #0381FE
 * - basics_one_ui_window_background_dark #171717
 * - white/black opacity ramps used on seekbar and control icons
 *
 * These are visual adaptation tokens only — no Samsung code is copied.
 */
object SamsungColors {
    /** One UI primary blue (active shuffle/repeat, accents). */
    val Primary = Color(0xFF0381FE)

    /** Slightly brighter primary for pressed / dark surfaces. */
    val PrimaryBright = Color(0xFF3E91FF)

    /** Dark window / immersive player canvas. */
    val WindowDark = Color(0xFF171717)

    /** Light full-player page background (classic Samsung light mode). */
    val WindowLight = Color(0xFFFCFCFC)

    /** Main title text on dark immersive player. */
    val TitleOnDark = Color(0xFFFFFFFF)

    /** Subtitle / artist on dark (≈ white 70%). */
    val SubtitleOnDark = Color(0xB3FFFFFF)

    /** Secondary grey title on light surfaces. */
    val TitleOnLight = Color(0xFF252525)

    /** Subtitle on light (dark grey 60%). */
    val SubtitleOnLight = Color(0x99252525)

    /** Seekbar inactive track (white ~30%). */
    val SeekTrack = Color(0x4DFFFFFF)

    /** Seekbar secondary / buffered (white ~50%). */
    val SeekSecondary = Color(0x80FFFFFF)

    /** Control icons default on immersive player. */
    val ControlIcon = Color(0xFFFFFFFF)

    /** Subtle album-art stroke (black 10%). */
    val AlbumStroke = Color(0x1A000000)

    /** Mini-player / bottom bar soft surface. */
    val MiniSurface = Color(0xE6171717)
}
