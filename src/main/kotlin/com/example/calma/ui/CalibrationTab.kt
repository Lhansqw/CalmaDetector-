package com.example.calma.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calma.ui.components.NeuCard
import com.example.calma.ui.components.NeuPill
import com.example.calma.ui.components.neumorphic
import com.example.calma.ui.theme.Blush
import com.example.calma.ui.theme.ImminentPastel
import com.example.calma.ui.theme.InkPrimary
import com.example.calma.ui.theme.InkSecondary
import com.example.calma.ui.theme.MildPastel
import com.example.calma.ui.theme.NeuLight
import com.example.calma.ui.theme.Pink

/** Calibración: simulador de escenarios, respuesta a la última alerta y línea base. */
@Composable
fun CalibrationTab(
    state: CalmaUiState,
    onSelectScenario: (SimulationScenario) -> Unit,
    onFeedback: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionLabel("Simular escenario")
        SimulationScenario.entries.forEach { scenario ->
            val selected = scenario == state.currentScenario
            NeuPill(selected = selected, onClick = { onSelectScenario(scenario) }) {
                Text(
                    text = scenario.displayName.substringAfter(". "),
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Pink else InkPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text("${scenario.targetBpm.toInt()} BPM", fontSize = 12.sp, color = InkSecondary)
                if (selected) {
                    Spacer(Modifier.padding(start = 10.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Pink))
                }
            }
        }

        SectionLabel("Calibración")
        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Text(alertThresholdLabel(state.sensitivity), fontSize = 14.sp, color = InkPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state.awaitingFeedback) "¿La última alerta fue ansiedad real?"
                else "Los botones se activan cuando salta una alerta.",
                fontSize = 13.sp,
                color = if (state.awaitingFeedback) MildPastel else InkSecondary
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                NeuButton("Sí, sentí ansiedad", Modifier.weight(1f), enabled = state.awaitingFeedback, primary = true) { onFeedback(true) }
                NeuButton("Fue falsa alarma", Modifier.weight(1f), enabled = state.awaitingFeedback) { onFeedback(false) }
            }
            Spacer(Modifier.height(14.dp))
            NeuButton("Restablecer el detector", Modifier.fillMaxWidth(), textColor = ImminentPastel, onClick = onReset)
        }

        SectionLabel("Línea base de reposo")
        NeuPill {
            Text("Pulso en reposo", fontSize = 13.sp, color = InkSecondary, modifier = Modifier.weight(1f))
            Text(if (state.baselineReady) "%.1f".format(state.baselineMeanBpm) else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Pink)
            Text(" BPM", fontSize = 11.sp, color = InkSecondary)
        }
        NeuPill {
            Text("HRV en reposo", fontSize = 13.sp, color = InkSecondary, modifier = Modifier.weight(1f))
            Text(if (state.baselineReady) "%.1f".format(state.baselineMeanHrv) else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Pink)
            Text(" ms", fontSize = 11.sp, color = InkSecondary)
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkSecondary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

/** Botón con relieve. [primary] lo rellena de rosa. */
@Composable
private fun NeuButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    textColor: Color = InkPrimary,
    onClick: () -> Unit
) {
    val filled = primary && enabled
    Box(
        modifier = modifier
            .height(46.dp)
            .neumorphic(16.dp, elevation = if (enabled) 4.dp else 1.dp, color = if (filled) Pink else Blush)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = when {
                !enabled -> InkSecondary.copy(alpha = 0.5f)
                filled -> NeuLight
                else -> textColor
            }
        )
    }
}
