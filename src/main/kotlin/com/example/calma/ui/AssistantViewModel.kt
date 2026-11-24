package com.example.calma.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calma.assistant.AssistantPrompts
import com.example.calma.assistant.AssistantResult
import com.example.calma.assistant.CalmaAssistant
import com.example.calma.assistant.ChatTurn
import com.example.calma.detection.Sample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    /** Lo que se ve en pantalla (sin el contexto de la alerta, que solo lee el modelo). */
    val messages: List<ChatTurn> = emptyList(),
    val isThinking: Boolean = false,
    val error: String? = null,
    val enabled: Boolean = true
)

/** Charla con el asistente que arranca cuando salta una alerta. No se guarda: se pierde al cerrar la app. */
class AssistantViewModel : ViewModel() {

    private val assistant = CalmaAssistant()

    /** Historial completo enviado a la API, incluido el contexto inicial de la alerta. */
    private val history = mutableListOf<ChatTurn>()

    /** true si la charla la abrió una alerta: el primer turno son los números y no se muestra. */
    private var startedByAlert = false

    private val _state = MutableStateFlow(
        AssistantUiState(
            enabled = assistant.isConfigured,
            error = if (assistant.isConfigured) null else CalmaAssistant.NO_KEY
        )
    )
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    /** Alerta que ya se atendió, para no reabrir la charla con la misma (p. ej. al rotar la pantalla). */
    private var lastAlertMs: Long? = null

    /** Una alerta nueva abre una charla, salvo que ya haya una en curso. */
    fun onAlert(event: AlertEvent, sample: Sample, baselineBpm: Double, simulated: Boolean) {
        if (event.timestampMs == lastAlertMs) return
        lastAlertMs = event.timestampMs
        if (!assistant.isConfigured || _state.value.isThinking) return
        // Si ya hay una charla con respuestas, se respeta. Si el primer pedido falló, se empieza de nuevo
        // (en el reloj no hay botón de reintentar).
        if (history.any { !it.fromUser }) return
        history.clear()
        startedByAlert = true
        history += ChatTurn(fromUser = true, text = AssistantPrompts.alertContext(event.level, sample, baselineBpm, simulated))
        requestReply()
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _state.value.isThinking || !assistant.isConfigured) return
        history += ChatTurn(fromUser = true, text = clean)
        requestReply()
    }

    /** Vuelve a pedir la respuesta al último mensaje (tras un error de red, por ejemplo). */
    fun retry() {
        if (history.lastOrNull()?.fromUser == true && !_state.value.isThinking) requestReply()
    }

    fun clear() {
        if (_state.value.isThinking) return
        history.clear()
        startedByAlert = false
        _state.update { it.copy(messages = emptyList(), error = null) }
    }

    private fun requestReply() {
        _state.update { it.copy(messages = visibleMessages(), isThinking = true, error = null) }
        viewModelScope.launch {
            when (val result = assistant.reply(history.toList())) {
                is AssistantResult.Reply -> {
                    history += ChatTurn(fromUser = false, text = result.text)
                    _state.update { it.copy(messages = visibleMessages(), isThinking = false) }
                }
                is AssistantResult.Failure -> _state.update { it.copy(isThinking = false, error = result.message) }
            }
        }
    }

    private fun visibleMessages(): List<ChatTurn> = if (startedByAlert) history.drop(1) else history.toList()
}
