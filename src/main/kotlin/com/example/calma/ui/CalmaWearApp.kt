package com.example.calma.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    viewModel: CalmaViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val columnState = rememberTransformingLazyColumnState()

    AppScaffold {
        ScreenScaffold(
            scrollState = columnState,
            timeText = { TimeText { time() } },
            content = {
                TransformingLazyColumn(
                    state = columnState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 28.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Header Título
                    item {
                        ListHeader(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CalmaDetector",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 2. Tarjeta Principal de Estado de Estrés
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
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = icon, fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = statusTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    textAlign = TextAlign.Center
                                )
                                if (state.currentAlert == AlertLevel.LEVEL_2_IMMINENT) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🧘 Inicia respiración 4-7-8",
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // 3. Tarjeta de Métricas Rápidas (BPM, HRV, Movimiento)
                    item {
                        Card(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "❤️ ${state.latestSample.bpm.toInt()}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(text = "BPM", fontSize = 10.sp, color = TextSecondary)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🌊 ${state.latestSample.hrvMs?.toInt() ?: "--"}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(text = "HRV (ms)", fontSize = 10.sp, color = TextSecondary)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (state.latestSample.motion > 1.0) "🏃" else "🧘",
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = if (state.latestSample.motion > 1.0) "Activo" else "Quieto",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 4. Progreso de Línea Base
                    item {
                        Card(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🎯 Línea Base", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        text = if (state.baselineReady) "Lista (${state.baselineMeanBpm.toInt()} BPM)" else "${(state.baselineProgress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.baselineReady) StatusCalm else StatusMild
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

                    // 5. Header de Escenarios
                    item {
                        ListHeader(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Simular Escenario",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // 6. Botones de Selección de Escenario
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
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 7. Header Retroalimentación
                    item {
                        ListHeader(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Calibración",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = { viewModel.sendFeedback(true) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusMild)
                        ) {
                            Text("Sí, sentí ansiedad", fontSize = 11.sp)
                        }
                    }

                    item {
                        Button(
                            onClick = { viewModel.sendFeedback(false) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text("Fue falsa alarma", fontSize = 11.sp)
                        }
                    }

                    item {
                        CompactButton(
                            onClick = { viewModel.resetDetector() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusImminent),
                            label = { Text("Restablecer Estado", fontSize = 11.sp) }
                        )
                    }
                }
            }
        )
    }
}

@WearPreviewLargeRound
@Composable
fun CalmaWearAppPreview() {
    CalmaTheme {
        CalmaWearApp(viewModel = androidx.lifecycle.viewmodel.compose.viewModel())
    }
}
