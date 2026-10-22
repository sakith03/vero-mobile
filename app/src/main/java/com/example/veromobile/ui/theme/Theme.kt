package com.example.veromobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Scheme = darkColorScheme(
    primary = Accent, onPrimary = OnDark,
    background = Bg, onBackground = OnDark,
    surface = Surface1, onSurface = OnDark,
    surfaceVariant = Surface2, onSurfaceVariant = Muted,
    error = Accent
)


@Composable
fun VeroMobileTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, content = content)