package com.example.calma.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calma.detection.AlertLevel
import com.example.calma.detection.Baseline
import com.example.calma.detection.DetectorConfig
import com.example.calma.detection.Sample
import com.example.calma.detection.StressDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class SimulationScenario(val displayName: String, val targetBpm: Double, val targetHrv: Double, val motion: Double) {
    REPOSO_NORMAL("1. Reposo Normal", 65.0, 50.0, 0.05),
    EJERCICIO("2. Ejercicio Físico", 120.0, 20.0, 3.0),
    ESTRES_ANSIEDAD("3. Estrés en Reposo", 102.0, 22.0, 0.05),
    RECUPERACION("4. Recuperación", 66.0, 48.0, 0.05)
}

data class AlertEvent(
    val timestampMs: Long,
    val level: AlertLevel,
    val bpm: Double,
    val scenarioName: String
)

data class CalmaUiState(
    val latestSample: Sample = Sample(System.currentTimeMillis(), 65.0, 50.0, 0.05),
    val currentAlert: AlertLevel = AlertLevel.NONE,
    val baselineReady: Boolean = false,
    val baselineProgress: Float = 0.0f,
    val baselineMeanBpm: Double = 0.0,
    val baselineMeanHrv: Double = 0.0,
    val bpmZScore: Double = 0.0,
    val sensitivity: Double = 1.0,
    val currentScenario: SimulationScenario = SimulationScenario.REPOSO_NORMAL,
    val sampleHistory: List<Sample> = emptyList(),
    val alertLog: List<AlertEvent> = emptyList(),
    val isSimulationRunning: Boolean = true
)

class CalmaViewModel : ViewModel() {

    private val baseline = Baseline(minSamples = 60) // 60 muestras para demo rápida
    private val detector = StressDetector(baseline, DetectorConfig(minSamplesInWindow = 5))

    private val _uiState = MutableStateFlow(CalmaUiState())
    val uiState: StateFlow<CalmaUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private var simulatedTimeMs = System.currentTimeMillis()
    private val random = Random(42)

    init {
        // Pre-entrenar parcialmente la línea base para una experiencia fluida
        pretrainBaseline()
        startSimulation()
    }

    private fun pretrainBaseline() {
        repeat(40) {
            simulatedTimeMs += 10_000L
            val sample = Sample(
                timestampMs = simulatedTimeMs,
                bpm = 65.0 + random.nextDouble(-2.0, 2.0),
                hrvMs = 50.0 + random.nextDouble(-3.0, 3.0),
                motion = 0.05 + random.nextDouble(0.0, 0.05)
            )
            detector.onSample(sample)
        }
        updateBaselineState()
    }

    fun startSimulation() {
        if (simulationJob?.isActive == true) return

        _uiState.update { it.copy(isSimulationRunning = true) }
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(1200L) // Genera una nueva muestra cada 1.2 segundos
                generateNextSample()
            }
        }
    }

    fun pauseSimulation() {
        simulationJob?.cancel()
        _uiState.update { it.copy(isSimulationRunning = false) }
    }

    fun selectScenario(scenario: SimulationScenario) {
        _uiState.update { it.copy(currentScenario = scenario) }
    }

    fun sendFeedback(wasAnxiety: Boolean) {
        detector.onUserFeedback(wasAnxiety)
        val newSensitivity = if (wasAnxiety) 0.95 else 1.10
        _uiState.update { it.copy(sensitivity = newSensitivity) }
    }

    fun resetDetector() {
        detector.resetState()
        _uiState.update {
            it.copy(
                currentAlert = AlertLevel.NONE,
                alertLog = emptyList(),
                sampleHistory = emptyList()
            )
        }
    }

    private fun generateNextSample() {
        simulatedTimeMs += 10_000L
        val scenario = _uiState.value.currentScenario

        val sample = Sample(
            timestampMs = simulatedTimeMs,
            bpm = scenario.targetBpm + random.nextDouble(-3.0, 3.0),
            hrvMs = scenario.targetHrv + random.nextDouble(-4.0, 4.0),
            motion = (scenario.motion + random.nextDouble(-0.02, 0.05)).coerceAtLeast(0.0)
        )

        val newAlert = detector.onSample(sample)

        // Actualizar lista de alertas si se detecta un nuevo nivel
        val currentAlertLog = _uiState.value.alertLog.toMutableList()
        if (newAlert != AlertLevel.NONE) {
            currentAlertLog.add(0, AlertEvent(simulatedTimeMs, newAlert, sample.bpm, scenario.displayName))
        }

        val updatedHistory = (_uiState.value.sampleHistory + sample).takeLast(20)
        val zScore = baseline.bpmZ(sample.bpm)

        _uiState.update { state ->
            state.copy(
                latestSample = sample,
                currentAlert = newAlert,
                baselineReady = baseline.isReady,
                baselineProgress = (baseline.bpm.n.toFloat() / 60f).coerceAtMost(1.0f),
                baselineMeanBpm = baseline.bpm.mean,
                baselineMeanHrv = baseline.hrv.mean,
                bpmZScore = zScore,
                sampleHistory = updatedHistory,
                alertLog = currentAlertLog
            )
        }
    }

    private fun updateBaselineState() {
        _uiState.update { state ->
            state.copy(
                baselineReady = baseline.isReady,
                baselineProgress = (baseline.bpm.n.toFloat() / 60f).coerceAtMost(1.0f),
                baselineMeanBpm = baseline.bpm.mean,
                baselineMeanHrv = baseline.hrv.mean
            )
        }
    }
}
