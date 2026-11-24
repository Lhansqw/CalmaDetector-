package com.example.calma.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calma.detection.AlertLevel
import com.example.calma.ui.components.NeuCard
import com.example.calma.ui.components.NeuPill
import com.example.calma.ui.components.StatusDial
import com.example.calma.ui.components.alertColor
import com.example.calma.ui.theme.InkPrimary
import com.example.calma.ui.theme.InkSecondary
import com.example.calma.ui.theme.MildPastel
import com.example.calma.ui.theme.Pink
import com.example.calma.ui.theme.PinkSoft

/** Inicio: el dial de estado, las métricas y el último mensaje del asistente. */
@Composable
fun HomeTab(state: CalmaUiState, assistant: AssistantUiState, onOpenAssistant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Fracción del camino al umbral de alerta inminente (2.5 σ, ajustado por la calibración).
        val threshold = 2.5 * state.sensitivity
        StatusDial(
            level = state.currentAlert,
            bpm = state.latestSample.bpm.toInt(),
            thresholdFraction = (state.bpmZScore / threshold).toFloat(),
            modifier = Modifier.padding(top = 8.dp)
        )
        if (state.currentAlert == AlertLevel.LEVEL_2_IMMINENT) {
            Text(
                "Prueba la respiración 4-7-8: inhala 4 segundos, sostén 7 y exhala 8.",
                fontSize = 13.sp,
                color = InkSecondary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        MetricPill(
            label = "Variabilidad (HRV)",
            value = state.latestSample.hrvMs?.let { "${it.toInt()}" } ?: "--",
            unit = "ms",
            valueColor = state.latestSample.hrvMs?.let { if (it < 30) MildPastel else Pink } ?: InkSecondary
        )
        MetricPill(
            label = if (state.latestSample.motion > 1.0) "Movimiento · activo" else "Movimiento · quieto",
            value = "%.2f".format(state.latestSample.motion),
            unit = "m/s²",
            valueColor = Pink
        )

        BaselineSummary(state)

        val lastReply = assistant.messages.lastOrNull { !it.fromUser }?.text
        if (lastReply != null) {
            NeuCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenAssistant) {
                Text("Asistente", fontSize = 12.sp, color = Pink, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(lastReply, fontSize = 13.sp, color = InkPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, unit: String, valueColor: Color) {
    NeuPill {
        Text(label, fontSize = 13.sp, color = InkSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Text(" $unit", fontSize = 11.sp, color = InkSecondary)
    }
}

@Composable
private fun BaselineSummary(state: CalmaUiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Línea base de reposo", fontSize = 12.sp, color = InkSecondary, modifier = Modifier.weight(1f))
            Text(
                if (state.baselineReady) "Lista · ${state.baselineMeanBpm.toInt()} BPM" else "${(state.baselineProgress * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (state.baselineReady) alertColor(AlertLevel.NONE) else Pink
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { state.baselineProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (state.baselineReady) alertColor(AlertLevel.NONE) else Pink,
            trackColor = PinkSoft.copy(alpha = 0.5f),
            drawStopIndicator = {}
        )
    }
}
