package com.crystall.music.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// PURE WHITE & LIGHT CLEAN THEME PALETTE
// ==========================================

// Base Backgrounds & Surfaces
val LightBackground = Color(0xFFFFFFFF)        // Pure White screen background
val LightSurface = Color(0xFFFFFFFF)           // Pure White card / sheet surface
val LightSurfaceElevated = Color(0xFFF7F7F9)   // Subtle light grey for secondary cards / chips / inputs
val LightSurfacePressed = Color(0xFFEBECEF)    // Ripple / pressed state
val LightBorder = Color(0xFFE5E7EB)            // Crisp hairline border (0.5dp or 1dp)
val LightBorderSubtle = Color(0xFFF0F1F3)

// Text Colors (High Contrast & Crisp)
val LightTextPrimary = Color(0xFF0F0F12)       // Ultra deep charcoal / black for headlines and titles
val LightTextSecondary = Color(0xFF5F6368)     // Neutral dark grey for artist names, descriptions
val LightTextMuted = Color(0xFF9AA0A6)         // Subtle grey for timestamps, badges, placeholders

// Chips & Controls
val LightChipBackground = Color(0xFFF1F3F4)    // Unselected chip light grey
val LightChipBorder = Color(0xFFE5E7EB)        // Subtle hairline
val LightChipSelectedBackground = Color(0xFF0F0F12) // Deep black pill when active
val LightChipSelectedText = Color(0xFFFFFFFF)       // Pure white text inside active pill

// Brand & Accents
val YtRed = Color(0xFFFF0000)                  // YouTube signature red (scrubber, active playing track, badge)
val YtRedAccent = Color(0xFFFF0033)
val GreenSuccess = Color(0xFF34C759)           // Download completed / success indicator

// Aliases mapped directly to Pure White Palette
val YtBackground = LightBackground
val YtSurface = LightSurface
val YtSurfaceElevated = LightSurfaceElevated
val YtSurfaceBorder = LightBorder
val YtChipBackground = LightChipBackground
val YtChipBorder = LightChipBorder
val YtChipSelectedBackground = LightChipSelectedBackground
val YtChipSelectedText = LightChipSelectedText
val YtTextPrimary = LightTextPrimary
val YtTextSecondary = LightTextSecondary
val YtTextMuted = LightTextMuted

// Legacy aliases redirected to light tokens
val BackgroundDark = LightBackground
val SurfaceDark = LightSurface
val SurfaceElevated = LightSurfaceElevated
val SurfaceBorder = LightBorder
val TextPrimary = LightTextPrimary
val TextSecondary = LightTextSecondary
val TextMuted = LightTextMuted
val CrystalPrimary = LightTextPrimary
val CrystalCyan = YtRed
val SoundCloudOrange = Color(0xFFFF5500)
val YouTubeRed = YtRed

// Pure Glass Translucency for Light Mode
val GlassPrimary = LightTextPrimary
val GlassSecondary = LightTextSecondary
val GlassSurface = Color(0x0D000000)
val GlassSurfaceElevated = Color(0x14000000)
val GlassSurfacePressed = Color(0x1F000000)
val GlassBorder = Color(0x1A000000)
val GlassBorderSubtle = Color(0x0F000000)

val IosBackground = LightBackground
val IosGlassSurface = Color(0x0A000000)
val IosGlassSurfaceElevated = Color(0x14000000)
val IosGlassBorder = Color(0x1A000000)
val IosGlassBorderSubtle = Color(0x0F000000)
val IosDynamicIsland = Color(0xFFFFFFFF)
val IosAccentWhite = Color(0xFF0F0F12)
val IosTextSecondary = LightTextSecondary
val IosTextTertiary = LightTextMuted
val IosPillBackground = Color(0xFFF1F3F4)
val IosPillGradient = listOf(Color(0xFF0F0F12), Color(0xFF2A2A2E))
val CrystalViolet = Color(0xFF7B1FA2)
val CrystalGradient = listOf(Color(0xFF0F0F12), Color(0xFF3A3A3C))
val IosAccentCyan = Color(0xFF007AFF)
val IosAccentPurple = Color(0xFF7B1FA2)
val IosAccentPink = Color(0xFFFF2D55)
val IosAccentBlue = Color(0xFF007AFF)
