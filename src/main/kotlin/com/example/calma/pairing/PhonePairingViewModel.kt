package com.example.calma.pairing

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class PhonePairingState(
    val paired: PairedDevice? = null,
    val codeInput: String = "",
    val isSending: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

/** Lado del teléfono: el usuario escribe el código que ve en el reloj y lo confirma. */
class PhonePairingViewModel(app: Application) : AndroidViewModel(app),
    MessageClient.OnMessageReceivedListener {

    private val store = PairingStore(app)
    private val messageClient = Wearable.getMessageClient(app)
    private val nodeClient = Wearable.getNodeClient(app)

    private val _state = MutableStateFlow(PhonePairingState(paired = store.load()))
    val state: StateFlow<PhonePairingState> = _state.asStateFlow()

    /** Respuestas de los relojes mientras hay una vinculación en curso. */
    private var responses: Channel<Pair<String, PairingProtocol.Result>>? = null

    init {
        messageClient.addListener(this)
    }

    fun onCodeChange(input: String) {
        val digits = PairingCodeManager.normalize(input).take(CODE_LENGTH)
        _state.update { it.copy(codeInput = digits, message = null, isError = false) }
    }

    fun submitCode() {
        val code = _state.value.codeInput
        if (code.length != CODE_LENGTH) {
            showError("El código tiene $CODE_LENGTH dígitos")
            return
        }
        if (_state.value.isSending) return

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, message = "Buscando reloj…", isError = false) }
            val channel = Channel<Pair<String, PairingProtocol.Result>>(Channel.UNLIMITED)
            responses = channel
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    showError("No hay ningún reloj conectado. Revisa el Bluetooth y la app Wear OS.")
                    return@launch
                }

                val payload = PairingProtocol.encode(PairingProtocol.Request(code, Build.MODEL ?: "Teléfono"))
                val delivered = nodes.count { node ->
                    runCatching {
                        messageClient.sendMessage(node.id, PairingProtocol.PATH_REQUEST, payload).await()
                    }.isSuccess
                }
                if (delivered == 0) {
                    showError("No se pudo enviar el código al reloj.")
                    return@launch
                }

                var lastError: PairingProtocol.Result? = null
                val success = withTimeoutOrNull(RESPONSE_TIMEOUT_MS) {
                    repeat(delivered) {
                        val (nodeId, result) = channel.receive()
                        if (result.ok) return@withTimeoutOrNull nodeId to result
                        lastError = result
                    }
                    null
                }

                when {
                    success != null -> {
                        val (nodeId, result) = success
                        val device = PairedDevice(
                            nodeId = nodeId,
                            name = result.watchName ?: "Reloj",
                            token = result.token.orEmpty(),
                            pairedAtMs = System.currentTimeMillis()
                        )
                        store.save(device)
                        _state.update {
                            it.copy(paired = device, codeInput = "", message = "Reloj vinculado ✅", isError = false)
                        }
                    }
                    lastError != null -> {
                        val e = lastError!!
                        val extra = e.attemptsLeft?.let { " (quedan $it intentos)" }.orEmpty()
                        showError((e.error ?: "No se pudo vincular") + extra)
                    }
                    else -> showError("El reloj no respondió. Abre CalmaDetector en el reloj e inténtalo de nuevo.")
                }
            } catch (e: ApiException) {
                showError("Wear OS no está disponible en este teléfono (${e.statusCode}).")
            } finally {
                responses = null
                channel.close()
                _state.update { it.copy(isSending = false) }
            }
        }
    }

    fun unpair() {
        val watch = store.load()
        store.clear()
        _state.update { PhonePairingState(message = "Reloj desvinculado") }
        if (watch != null) {
            viewModelScope.launch {
                runCatching { messageClient.sendMessage(watch.nodeId, PairingProtocol.PATH_REVOKE, ByteArray(0)).await() }
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            PairingProtocol.PATH_RESULT -> {
                val result = PairingProtocol.decodeResult(event.data) ?: return
                responses?.trySend(event.sourceNodeId to result)
            }
            PairingProtocol.PATH_REVOKE -> {
                if (store.load()?.nodeId == event.sourceNodeId) {
                    store.clear()
                    _state.update { PhonePairingState(message = "El reloj cerró la sesión", isError = true) }
                }
            }
        }
    }

    private fun showError(text: String) {
        _state.update { it.copy(message = text, isError = true) }
    }

    override fun onCleared() {
        messageClient.removeListener(this)
        super.onCleared()
    }

    private companion object {
        const val CODE_LENGTH = 6
        const val RESPONSE_TIMEOUT_MS = 10_000L
    }
}
