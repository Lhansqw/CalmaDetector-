package com.example.calma.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calma.detection.AlertLevel
import com.example.calma.ui.theme.CardBackground
import com.example.calma.ui.theme.CardBorder
import com.example.calma.ui.theme.CyanPrimary
import com.example.calma.ui.theme.DarkBackground
import com.example.calma.ui.theme.StatusCalm
import com.example.calma.ui.theme.StatusImminent
import com.example.calma.ui.theme.StatusMild
import com.example.calma.ui.theme.TextPrimary
import com.example.calma.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalmaDashboardScreen(
    viewModel: CalmaViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            DashboardHeader(
                isSimulating = state.isSimulationRunning,
                onToggleSimulation = {
                    if (state.isSimulationRunning) viewModel.pauseSimulation() else viewModel.startSimulation()
                }
            )

            // Tarjeta principal de Estado con pulso animado
            StatusPulseCard(alertLevel = state.currentAlert, bpmZScore = state.bpmZScore)

            // Fila de Métricas en tiempo real (BPM, HRV, Movimiento)
            SensorMetricsRow(
                bpm = state.latestSample.bpm,
                hrv = state.latestSample.hrvMs,
                motion = state.latestSample.motion
            )

            // Barra de progreso de la Línea Base
            BaselineProgressCard(
                progress = state.baselineProgress,
                isReady = state.baselineReady,
                meanBpm = state.baselineMeanBpm,
                meanHrv = state.baselineMeanHrv
            )

            // Panel Simulador de Escenarios
            ScenarioSimulatorCard(
                selectedScenario = state.currentScenario,
                onSelectScenario = { viewModel.selectScenario(it) }
            )

            // Panel de Retroalimentación del Usuario y Controles
            FeedbackAndControlCard(
                sensitivity = state.sensitivity,
                onFeedback = { viewModel.sendFeedback(it) },
                onReset = { viewModel.resetDetector() }
            )

            // Historial de Alertas Registradas
            AlertLogCard(alertLog = state.alertLog)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardHeader(
    isSimulating: Boolean,
    onToggleSimulation: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "CalmaDetector",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Monitoreo Anti-Ansiedad en Tiempo Real",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        OutlinedButton(
            onClick = onToggleSimulation,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isSimulating) StatusCalm else StatusMild
            )
        ) {
            Text(if (isSimulating) "⏸ Pausar" else "▶ Reanudar")
        }
    }
}

@Composable
private fun StatusPulseCard(
    alertLevel: AlertLevel,
    bpmZScore: Double
) {
    val (statusColor, statusTitle, statusSubtitle) = when (alertLevel) {
        AlertLevel.NONE -> Triple(
            StatusCalm,
            "Estado Normal / Calma",
            "Frecuencia cardíaca y variabilidad estables."
        )
        AlertLevel.LEVEL_1_MILD -> Triple(
            StatusMild,
            "Alerta: Estrés Leve",
            "Se detectó elevación inusual de BPM en reposo."
        )
        AlertLevel.LEVEL_2_IMMINENT -> Triple(
            StatusImminent,
            "ALERTA: Estrés / Ansiedad Inminente",
            "⚠️ Elevación crítica de BPM con caída de HRV en reposo. Se recomienda técnica de respiración 4-7-8."
        )
    }

    val animatedColor by animateColorAsState(targetValue = statusColor, label = "ColorAnimation")

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (alertLevel == AlertLevel.LEVEL_2_IMMINENT) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (alertLevel == AlertLevel.LEVEL_2_IMMINENT) 500 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, animatedColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Anillo pulsante
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(animatedColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(animatedColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (alertLevel) {
                            AlertLevel.NONE -> "🧘"
                            AlertLevel.LEVEL_1_MILD -> "⚡"
                            AlertLevel.LEVEL_2_IMMINENT -> "🚨"
                        },
                        fontSize = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = statusTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = animatedColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusSubtitle,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (alertLevel != AlertLevel.NONE) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "BPM Z-Score: ${"%.2f".format(bpmZScore)} σ sobre reposo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = animatedColor
                )
            }
        }
    }
}

@Composable
private fun SensorMetricsRow(
    bpm: Double,
    hrv: Double?,
    motion: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricItemCard(
            modifier = Modifier.weight(1f),
            title = "Ritmo Cardíaco",
            value = "${bpm.toInt()}",
            unit = "BPM",
            icon = "❤️",
            color = if (bpm > 95) StatusMild else StatusCalm
        )

        MetricItemCard(
            modifier = Modifier.weight(1f),
            title = "Variabilidad",
            value = hrv?.let { "${it.toInt()}" } ?: "--",
            unit = "ms HRV",
            icon = "🌊",
            color = hrv?.let { if (it < 30) StatusMild else StatusCalm } ?: TextSecondary
        )

        MetricItemCard(
            modifier = Modifier.weight(1f),
            title = "Movimiento",
            value = "%.2f".format(motion),
            unit = if (motion > 1.0) "Activo" else "Quieto",
            icon = "🏃",
            color = if (motion > 1.0) CyanPrimary else TextSecondary
        )
    }
}

@Composable
private fun MetricItemCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    icon: String,
    color: Color
) {
    Card(
        modifier = modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = icon, fontSize = 16.sp)
                Text(text = unit, fontSize = 10.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun BaselineProgressCard(
    progress: Float,
    isReady: Boolean,
    meanBpm: Double,
    meanHrv: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Línea Base de Reposo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = if (isReady) "✅ Lista" else "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isReady) StatusCalm else StatusMild
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isReady) StatusCalm else CyanPrimary,
                trackColor = CardBorder
            )

            if (isReady) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "BPM Promedio Reposo: ${"%.1f".format(meanBpm)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "HRV Promedio: ${"%.1f".format(meanHrv)} ms",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenarioSimulatorCard(
    selectedScenario: SimulationScenario,
    onSelectScenario: (SimulationScenario) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🧪 Simulador de Escenarios",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Prueba cómo reacciona el detector cambiando el estado del usuario:",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SimulationScenario.entries.forEach { scenario ->
                    FilterChip(
                        selected = scenario == selectedScenario,
                        onClick = { onSelectScenario(scenario) },
                        label = {
                            Text(
                                text = scenario.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (scenario == selectedScenario) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = CyanPrimary,
                            containerColor = DarkBackground,
                            labelColor = TextPrimary
                        )
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CalmaDashboardPreview() {
    com.example.calma.ui.theme.CalmaTheme {
        CalmaDashboardScreen(viewModel = androidx.lifecycle.viewmodel.compose.viewModel())
    }
}

@Composable
private fun FeedbackAndControlCard(
    sensitivity: Double,
    onFeedback: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "💬 Calibración y Retroalimentación",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Factor de Sensibilidad Actual: ${"%.2f".format(sensitivity)}x",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onFeedback(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusMild)
                ) {
                    Text("Sí, sentí ansiedad", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onFeedback(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("Fue falsa alarma", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusImminent)
            ) {
                Text("Restablecer Estado del Detector", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AlertLogCard(
    alertLog: List<AlertEvent>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📋 Registro de Alertas Disparadas (${alertLog.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (alertLog.isEmpty()) {
                Text(
                    text = "No se han disparado alertas en esta sesión.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            } else {
                val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    alertLog.take(5).forEach { alert ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = alert.level.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alert.level == AlertLevel.LEVEL_2_IMMINENT) StatusImminent else StatusMild
                                )
                                Text(
                                    text = "Escenario: ${alert.scenarioName}",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "${alert.bpm.toInt()} BPM @ ${dateFormat.format(Date(alert.timestampMs))}",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
