package com.example.calma.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme as PhoneMaterialTheme
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme as WearColorScheme
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme

private val PhoneColorScheme = lightColorScheme(
    primary = Pink,
    secondary = PinkSoft,
    background = Blush,
    surface = Blush,
    surfaceVariant = Blush,
    onPrimary = NeuLight,
    onSecondary = InkPrimary,
    onBackground = InkPrimary,
    onSurface = InkPrimary,
    onSurfaceVariant = InkSecondary
)

private val WearColorScheme = WearColorScheme(
    primary = Pink,
    secondary = PinkSoft,
    background = DarkBackground,
    surfaceContainer = CardBackground,
    onPrimary = NeuLight,
    onSecondary = DarkBackground,
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
