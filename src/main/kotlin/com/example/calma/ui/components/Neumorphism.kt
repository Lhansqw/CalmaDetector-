package com.example.calma.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.graphics.withClip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.calma.ui.theme.Blush
import com.example.calma.ui.theme.InkSecondary
import com.example.calma.ui.theme.NeuLight
import com.example.calma.ui.theme.NeuShadow
import com.example.calma.ui.theme.Pink

/**
 * Relieve neumórfico: una sombra blanca hacia arriba a la izquierda y una rosada hacia abajo a la derecha.
 * Con [pressed] dibuja la versión hundida (sombras por dentro), para lo seleccionado y los campos de texto.
 * En Android 8.x (API 26-27) las sombras no se dibujan con aceleración por hardware y queda plano.
 */
fun Modifier.neumorphic(
    cornerRadius: Dp,
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    color: Color = Blush
): Modifier = drawBehind {
    val r = cornerRadius.toPx()
    val e = elevation.toPx()
    val w = size.width
    val h = size.height
    val canvas = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }
    if (!pressed) {
        paint.setShadowLayer(e * 1.6f, e, e, NeuShadow.toArgb())
        canvas.drawRoundRect(0f, 0f, w, h, r, r, paint)
        paint.setShadowLayer(e * 1.6f, -e, -e, NeuLight.toArgb())
        canvas.drawRoundRect(0f, 0f, w, h, r, r, paint)
    } else {
        canvas.drawRoundRect(0f, 0f, w, h, r, r, paint)
        // Sombra interior: un borde grueso por fuera de la forma, recortado, cuya sombra cae hacia adentro.
        val clip = android.graphics.Path().apply {
            addRoundRect(0f, 0f, w, h, r, r, android.graphics.Path.Direction.CW)
        }
        canvas.withClip(clip) {
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = e * 2
            paint.color = NeuShadow.toArgb()
            paint.setShadowLayer(e, e / 1.5f, e / 1.5f, NeuShadow.toArgb())
            drawRoundRect(-e, -e, w + e, h + e, r + e, r + e, paint)
            paint.color = NeuLight.toArgb()
            paint.setShadowLayer(e, -e / 1.5f, -e / 1.5f, NeuLight.toArgb())
            drawRoundRect(-e, -e, w + e, h + e, r + e, r + e, paint)
        }
    }
}

/** Tarjeta con relieve, del mismo color que el fondo. */
@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .neumorphic(cornerRadius)
            .clip(shape)
            .background(Blush, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(18.dp),
        content = content
    )
}

/** Fila en forma de pastilla, como las de la referencia. [selected] la pinta hundida. */
@Composable
fun NeuPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .neumorphic(16.dp, elevation = 4.dp, pressed = selected)
            .clip(shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Botón redondo con relieve (como el engranaje de la referencia). */
@Composable
fun NeuIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = Pink,
    filled: Boolean = false,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .neumorphic(size / 2, elevation = 4.dp, color = if (filled) Pink else Blush)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = when {
                !enabled -> InkSecondary.copy(alpha = 0.5f)
                filled -> NeuLight
                else -> tint
            },
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

/** Contenedor redondo con relieve, para el dial. */
@Composable
fun NeuCircle(modifier: Modifier, pressed: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(
        // Un radio mayor que la mitad del tamaño se recorta solo: queda un círculo.
        modifier = modifier.neumorphic(cornerRadius = 1000.dp, elevation = 10.dp, pressed = pressed),
        contentAlignment = Alignment.Center,
        content = content
    )
}
