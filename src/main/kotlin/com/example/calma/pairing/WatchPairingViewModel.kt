package com.example.calma.pairing

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Base64

sealed interface WatchPairingState {
    data object Loading : WatchPairingState

    data class WaitingForPhone(
        val code: String,
        val secondsLeft: Long,
        val totalSeconds: Long,
        val notice: String?
    ) : WatchPairingState

    data class Paired(val phone: PairedDevice) : WatchPairingState
}

/**
 * Lado del reloj: muestra un código, espera a que el teléfono lo envíe y lo valida.
 * Escucha también la desvinculación remota mientras está vinculado.
 */
class WatchPairingViewModel(app: Application) : AndroidViewModel(app),
    MessageClient.OnMessageReceivedListener {

    private val store = PairingStore(app)
    private val messageClient = Wearable.getMessageClient(app)
    private val codes = PairingCodeManager()
    private val random = SecureRandom()

    private val _state = MutableStateFlow<WatchPairingState>(WatchPairingState.Loading)
    val state: StateFlow<WatchPairingState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var notice: String? = null

    init {
        messageClient.addListener(this)
        val saved = store.load()
        if (saved != null) _state.value = WatchPairingState.Paired(saved) else startWaiting(null)
    }

    /** Genera un código nuevo y actualiza la cuenta atrás cada segundo; lo renueva al caducar. */
    fun startWaiting(message: String?) {
        notice = message
        codes.newCode()
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                if (codes.isExpired()) {
                    codes.newCode()
                    notice = "Código renovado"
                }
                val code = codes.active ?: codes.newCode()
                val left = ((code.expiresAtMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                _state.value = WatchPairingState.WaitingForPhone(
                    code = code.formatted,
                    secondsLeft = left,
                    totalSeconds = codes.ttlMs / 1000,
                    notice = notice
                )
                delay(1000)
            }
        }
    }

    fun unpair() {
        val phone = store.load()
        store.clear()
        if (phone != null) send(phone.nodeId, PairingProtocol.PATH_REVOKE, ByteArray(0))
        startWaiting(null)
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            PairingProtocol.PATH_REQUEST -> handleRequest(event)
            PairingProtocol.PATH_REVOKE -> {
                if (store.load()?.nodeId == event.sourceNodeId) {
                    store.clear()
                    startWaiting("El teléfono desvinculó este reloj")
                }
            }
        }
    }

    private fun handleRequest(event: MessageEvent) {
        val request = PairingProtocol.decodeRequest(event.data) ?: return

        if (_state.value is WatchPairingState.Paired) {
            reply(event.sourceNodeId, PairingProtocol.Result(false, error = "Este reloj ya está vinculado. Desvincúlalo desde el reloj"))
            return
        }

        when (val result = codes.verify(request.code)) {
            VerifyResult.Success -> {
                val device = PairedDevice(
                    nodeId = event.sourceNodeId,
                    name = request.phoneName,
                    token = newToken(),
                    pairedAtMs = System.currentTimeMillis()
                )
                store.save(device)
                tickerJob?.cancel()
                _state.value = WatchPairingState.Paired(device)
                reply(event.sourceNodeId, PairingProtocol.Result(true, token = device.token, watchName = Build.MODEL ?: "Reloj"))
            }
            VerifyResult.TooManyAttempts -> {
                reply(event.sourceNodeId, PairingProtocol.errorFor(result))
                startWaiting("Demasiados intentos. Código nuevo")
            }
            is VerifyResult.WrongCode -> {
                notice = "Código incorrecto (quedan ${result.attemptsLeft})"
                reply(event.sourceNodeId, PairingProtocol.errorFor(result))
            }
            VerifyResult.Expired, VerifyResult.NoActiveCode ->
                reply(event.sourceNodeId, PairingProtocol.errorFor(result))
        }
    }

    private fun reply(nodeId: String, result: PairingProtocol.Result) =
        send(nodeId, PairingProtocol.PATH_RESULT, PairingProtocol.encode(result))

    private fun send(nodeId: String, path: String, data: ByteArray) {
        viewModelScope.launch {
            runCatching { messageClient.sendMessage(nodeId, path, data).await() }
        }
    }

    private fun newToken(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    override fun onCleared() {
        messageClient.removeListener(this)
        super.onCleared()
    }
}
