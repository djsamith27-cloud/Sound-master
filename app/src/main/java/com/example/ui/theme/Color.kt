package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val StudioBg = Color(0xFF0B0F17)
val StudioSurface = Color(0xFF111722)
val StudioSurfaceElevated = Color(0xFF161E2E)
val StudioBorder = Color(0xFF1E293B)
val StudioBorderMuted = Color(0xFF334155)

val StudioAmber = Color(0xFFF59E0B)
val StudioAmberLight = Color(0xFFFBBF24)
val StudioAmberSoft = Color(0xFFFDE68A)
val StudioAmberGlow = Color(0x33F59E0B)

val StudioEmerald = Color(0xFF10B981)
val StudioEmeraldDark = Color(0xFF059669)

val StudioTextPrimary = Color(0xFFF1F5F9)
val StudioTextSecondary = Color(0xFFCBD5E1)
val StudioTextMuted = Color(0xFF94A3B8)
val StudioTextDim = Color(0xFF64748B)

val SwaraDarkColorScheme = darkColorScheme(
    primary = StudioAmber,
    onPrimary = StudioBg,
    primaryContainer = StudioAmberGlow,
    onPrimaryContainer = StudioAmberSoft,
    secondary = StudioEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    background = StudioBg,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StudioTextMuted,
    outline = StudioBorder,
    outlineVariant = StudioBorderMuted
)
