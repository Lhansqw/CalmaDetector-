package com.example.calma.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme as PhoneMaterialTheme
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme as WearColorScheme
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme

private val PhoneColorScheme = darkColorScheme(
    primary = CyanPrimary,
    secondary = CyanSecondary,
    background = DarkBackground,
    surface = CardBackground,
    surfaceVariant = CardBackground,
    onPrimary = DarkBackground,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val WearColorScheme = WearColorScheme(
    primary = CyanPrimary,
    secondary = CyanSecondary,
    background = DarkBackground,
    surfaceContainer = CardBackground,
    onPrimary = DarkBackground,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun CalmaTheme(
    content: @Composable () -> Unit
) {
    PhoneMaterialTheme(
        colorScheme = PhoneColorScheme
    ) {
        WearMaterialTheme(
            colorScheme = WearColorScheme,
            content = content
        )
    }
}
