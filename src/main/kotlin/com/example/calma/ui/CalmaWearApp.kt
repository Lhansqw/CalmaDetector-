package com.example.calma.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.example.calma.detection.AlertLevel
import com.example.calma.ui.theme.CalmaTheme
import com.example.calma.ui.theme.CyanPrimary
import com.example.calma.ui.theme.DarkBackground
import com.example.calma.ui.theme.StatusCalm
import com.example.calma.ui.theme.StatusImminent
import com.example.calma.ui.theme.StatusMild
import com.example.calma.ui.theme.TextPrimary
import com.example.calma.ui.theme.TextSecondary

@Composable
fun CalmaWearApp(
    viewModel: CalmaViewModel,
    pairedPhoneName: String? = null,
    onUnpair: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val columnState = rememberTransformingLazyColumnState()
    val d = rememberWearDimens()

    AppScaffold {
        ScreenScaffold(
            scrollState = columnState,
            timeText = { TimeText { time() } },
            content = {
                TransformingLazyColumn(
                    state = columnState,
                    contentPadding = d.listPadding,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(d.itemSpacing)
                ) {
                    // 1. Título
                    item {
                        ListHeader(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "CalmaDetector",
                                fontSize = d.sp(15f),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 2. Estado de estrés
                    item {
                        val (statusColor, statusTitle, icon) = when (state.currentAlert) {
                            AlertLevel.NONE -> Triple(StatusCalm, "Estado Normal", "🧘")
                            AlertLevel.LEVEL_1_MILD -> Triple(StatusMild, "Estrés Leve", "⚡")
                            AlertLevel.LEVEL_2_IMMINENT -> Triple(StatusImminent, "Ansiedad Inminente", "🚨")
                        }

                        Card(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = icon, fontSize = d.sp(if (d.isSmall) 26f else 30f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = statusTitle,
                                    fontSize = d.sp(15f),
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    textAlign = TextAlign.Center
                                )
                                if (state.currentAlert == AlertLevel.LEVEL_2_IMMINENT) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🧘 Inicia respiración 4-7-8",
                                        fontSize = d.sp(11f),
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // 3. Métricas rápidas (BPM, HRV, movimiento)
                    item {
                        Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WearMetric(d, value = "${state.latestSample.bpm.toInt()}", label = "BPM", color = TextPrimary)
                                WearMetric(d, value = "${state.latestSample.hrvMs?.toInt() ?: "--"}", label = "HRV ms", color = TextPrimary)
                                val active = state.latestSample.motion > 1.0
                                WearMetric(d, value = if (active) "🏃" else "🧘", label = if (active) "Activo" else "Quieto", color = TextPrimary)
                            }
                        }
                    }

                    // 4. Línea base
                    item {
                        Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Línea base", fontSize = d.sp(11f), color = TextSecondary, maxLines = 1)
                                    Text(
                                        text = if (state.baselineReady) "${state.baselineMeanBpm.toInt()} BPM" else "${(state.baselineProgress * 100).toInt()}%",
                                        fontSize = d.sp(11f),
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.baselineReady) StatusCalm else StatusMild,
                                        maxLines = 1
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { state.baselineProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = if (state.baselineReady) StatusCalm else CyanPrimary,
                                    trackColor = DarkBackground
                                )
                            }
                        }
                    }

                    // 5. Escenarios
                    item { WearSectionHeader(d, "Simular escenario") }

                    SimulationScenario.entries.forEach { scenario ->
                        item {
                            Button(
                                onClick = { viewModel.selectScenario(scenario) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (scenario == state.currentScenario) {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                } else {
                                    ButtonDefaults.filledTonalButtonColors()
                                }
                            ) {
                                Text(
                                    text = scenario.displayName,
                                    fontSize = d.sp(12f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 6. Calibración
                    item { WearSectionHeader(d, "Calibración") }

                    item {
                        Button(
                            onClick = { viewModel.sendFeedback(true) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusMild)
                        ) {
                            Text("Sí, sentí ansiedad", fontSize = d.sp(12f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    item {
                        Button(
                            onClick = { viewModel.sendFeedback(false) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text("Fue falsa alarma", fontSize = d.sp(12f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    item {
                        CompactButton(
                            onClick = { viewModel.resetDetector() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusImminent),
                            label = {
                                Text(
                                    "Restablecer estado",
                                    fontSize = d.sp(11f),
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }

                    // 7. Sesión: teléfono vinculado
                    if (pairedPhoneName != null) {
                        item { WearSectionHeader(d, "Sesión") }
                        item {
                            Text(
                                text = "📱 Vinculado con $pairedPhoneName",
                                fontSize = d.sp(11f),
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = d.textInset)
                            )
                        }
                        item { WearUnpairButton(d, onUnpair = onUnpair) }
                    }
                }
            }
        )
    }
}

@Composable
private fun WearMetric(d: WearDimens, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = d.sp(16f), fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        Text(text = label, fontSize = d.sp(10f), color = TextSecondary, maxLines = 1)
    }
}

@Composable
internal fun WearSectionHeader(d: WearDimens, title: String) {
    ListHeader(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontSize = d.sp(12f), color = TextSecondary, maxLines = 1)
    }
}

@WearPreviewLargeRound
@Composable
fun CalmaWearAppPreview() {
    CalmaTheme {
        CalmaWearApp(viewModel = androidx.lifecycle.viewmodel.compose.viewModel(), pairedPhoneName = "Pixel 8")
    }
}

@WearPreviewSmallRound
@Composable
fun CalmaWearAppSmallPreview() {
    CalmaTheme {
        CalmaWearApp(viewModel = androidx.lifecycle.viewmodel.compose.viewModel(), pairedPhoneName = "Pixel 8")
    }
}
