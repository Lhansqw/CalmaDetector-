package com.example.calma.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calma.detection.AlertLevel
import com.example.calma.ui.theme.CalmPastel
import com.example.calma.ui.theme.ImminentPastel
import com.example.calma.ui.theme.InkSecondary
import com.example.calma.ui.theme.MildPastel
import com.example.calma.ui.theme.PinkSoft
import kotlin.math.cos
import kotlin.math.sin

fun alertColor(level: AlertLevel): Color = when (level) {
    AlertLevel.NONE -> CalmPastel
    AlertLevel.LEVEL_1_MILD -> MildPastel
    AlertLevel.LEVEL_2_IMMINENT -> ImminentPastel
}

fun alertTitle(level: AlertLevel): String = when (level) {
    AlertLevel.NONE -> "Calma"
    AlertLevel.LEVEL_1_MILD -> "Estrés leve"
    AlertLevel.LEVEL_2_IMMINENT -> "Ansiedad inminente"
}

/**
 * Dial central: marcas alrededor (como el cronómetro de la referencia), un arco que muestra qué tan cerca
 * está el pulso del umbral de alerta y, en el centro, el BPM y el estado.
 * [thresholdFraction] va de 0 (reposo) a 1 (umbral de alerta inminente).
 */
@Composable
fun StatusDial(
    level: AlertLevel,
    bpm: Int,
    thresholdFraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 250.dp
) {
    val color by animateColorAsState(alertColor(level), label = "dialColor")
    val arc by animateFloatAsState(thresholdFraction.coerceIn(0f, 1f), tween(600), label = "dialArc")
    val pulse by rememberInfiniteTransition(label = "dialPulse").animateFloat(
        initialValue = 1f,
        targetValue = if (level == AlertLevel.NONE) 1f else 1.04f,
        animationSpec = infiniteRepeatable(
            tween(if (level == AlertLevel.LEVEL_2_IMMINENT) 500 else 1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dialPulseScale"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        NeuCircle(modifier = Modifier.size(size)) {
            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val radius = this.size.minDimension / 2
                val c = center
                // 60 marcas; una de cada 5 más larga, como un reloj.
                repeat(60) { i ->
                    val angle = Math.toRadians(i * 6.0 - 90)
                    val major = i % 5 == 0
                    val outer = radius
                    val inner = radius - if (major) 14.dp.toPx() else 8.dp.toPx()
                    val lit = i / 60f < arc
                    drawLine(
                        color = if (lit) color else PinkSoft,
                        start = Offset(c.x + inner * cos(angle).toFloat(), c.y + inner * sin(angle).toFloat()),
                        end = Offset(c.x + outer * cos(angle).toFloat(), c.y + outer * sin(angle).toFloat()),
                        strokeWidth = if (major) 4.dp.toPx() else 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                // Arco fino por dentro de las marcas
                val inset = 26.dp.toPx()
                drawArc(
                    color = color.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = 360f * arc,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            NeuCircle(modifier = Modifier.size(size * 0.56f).scale(pulse), pressed = true) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$bpm", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = color)
                    Text(text = "BPM", fontSize = 12.sp, color = InkSecondary)
                }
            }
        }
        Text(
            text = alertTitle(level),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp)
        )
    }
}
