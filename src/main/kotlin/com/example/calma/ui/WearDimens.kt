package com.example.calma.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Medidas del reloj calculadas a partir del tamaño real de la pantalla, para que la
 * interfaz se vea igual en relojes pequeños (~192 dp) y grandes (~240 dp o más).
 *
 * Sigue las pautas de Wear OS: márgenes en % del diámetro y más espacio arriba/abajo
 * en pantallas redondas, donde los bordes se recortan.
 */
@Immutable
data class WearDimens(
    val screenDp: Int,
    val isRound: Boolean,
    /** 1.0 = reloj de referencia (225 dp). */
    val scale: Float,
    /** Padding de la lista: deja libre la zona de la hora arriba y la curva de abajo. */
    val listPadding: PaddingValues,
    /** Margen extra para textos sueltos, que en pantallas redondas se cortarían en los lados. */
    val textInset: Dp,
    val itemSpacing: Dp
) {
    /** Tamaño de letra escalado a la pantalla. */
    fun sp(base: Float): TextUnit = (base * scale).sp

    fun dp(base: Float): Dp = (base * scale).dp

    val isSmall: Boolean get() = screenDp < 210
}

private const val REFERENCE_DP = 225f

@Composable
fun rememberWearDimens(): WearDimens {
    val config = LocalConfiguration.current
    val size = min(config.screenWidthDp, config.screenHeightDp)
    val round = config.isScreenRound
    return remember(size, round) {
        val s = size.toFloat()
        WearDimens(
            screenDp = size,
            isRound = round,
            scale = (s / REFERENCE_DP).coerceIn(0.85f, 1.2f),
            listPadding = PaddingValues(
                start = (s * 0.052f).dp,
                end = (s * 0.052f).dp,
                top = (s * if (round) 0.13f else 0.09f).dp,
                bottom = (s * if (round) 0.22f else 0.12f).dp
            ),
            textInset = (s * if (round) 0.06f else 0.02f).dp,
            itemSpacing = (s * 0.025f).coerceIn(4f, 8f).dp
        )
    }
}
