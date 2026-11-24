package com.example.calma.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calma.R
import com.example.calma.ui.components.BottomTabBar
import com.example.calma.ui.components.DashboardTab
import com.example.calma.ui.components.NeuIconButton
import com.example.calma.ui.theme.Blush
import com.example.calma.ui.theme.InkSecondary
import com.example.calma.ui.theme.Pink

@Composable
fun CalmaDashboardScreen(viewModel: CalmaViewModel, assistantViewModel: AssistantViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val assistant by assistantViewModel.state.collectAsStateWithLifecycle()
    // Va acá y no en la pestaña: el asistente tiene que reaccionar aunque esté abierta otra.
    AssistantAlertEffect(state, assistantViewModel)

    var tab by rememberSaveable { mutableStateOf(DashboardTab.HOME) }
    // Mensajes del asistente ya vistos, para marcar la pestaña cuando llega uno nuevo.
    var seenReplies by rememberSaveable { mutableIntStateOf(0) }
    val replies = assistant.messages.count { !it.fromUser }
    LaunchedEffect(tab, replies) {
        if (tab == DashboardTab.ASSISTANT || replies < seenReplies) seenReplies = replies
    }

    Scaffold(
        containerColor = Blush,
        bottomBar = {
            BottomTabBar(
                selected = tab,
                onSelect = { tab = it },
                badgeOn = if (replies > seenReplies) setOf(DashboardTab.ASSISTANT) else emptySet()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            Header(
                title = tab.title,
                isSimulating = state.isSimulationRunning,
                onToggleSimulation = {
                    if (state.isSimulationRunning) viewModel.pauseSimulation() else viewModel.startSimulation()
                }
            )
            if (tab == DashboardTab.ASSISTANT) {
                AssistantTab(
                    state = assistant,
                    onSend = assistantViewModel::send,
                    onRetry = assistantViewModel::retry,
                    onClear = assistantViewModel::clear,
                    modifier = Modifier.weight(1f).padding(bottom = 4.dp)
                )
            } else {
                // Margen para que las sombras del relieve no se corten en los bordes del scroll.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    when (tab) {
                        DashboardTab.HOME -> HomeTab(state, assistant, onOpenAssistant = { tab = DashboardTab.ASSISTANT })
                        DashboardTab.CALIBRATION -> CalibrationTab(
                            state = state,
                            onSelectScenario = viewModel::selectScenario,
                            onFeedback = viewModel::sendFeedback,
                            onReset = viewModel::resetDetector
                        )
                        DashboardTab.HISTORY -> HistoryTab(state.alertLog)
                        DashboardTab.ASSISTANT -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, isSimulating: Boolean, onToggleSimulation: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("CalmaDetector", fontSize = 12.sp, color = InkSecondary)
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Pink)
        }
        NeuIconButton(
            iconRes = if (isSimulating) R.drawable.ic_pause else R.drawable.ic_play,
            contentDescription = if (isSimulating) "Pausar simulación" else "Reanudar simulación",
            onClick = onToggleSimulation
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CalmaDashboardPreview() {
    com.example.calma.ui.theme.CalmaTheme {
        CalmaDashboardScreen(
            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
            assistantViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        )
    }
}
