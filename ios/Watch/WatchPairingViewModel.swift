import Foundation
import Combine
import WatchKit
import CalmaCore

/// Lado del reloj: muestra un código, espera a que el iPhone lo envíe y lo valida.
/// Escucha también la desvinculación remota mientras está vinculado.
@MainActor
final class WatchPairingViewModel: ObservableObject {
    enum State: Equatable {
        case loading
        case waiting(code: String, secondsLeft: Int, totalSeconds: Int, notice: String?)
        case paired(PairedDevice)
    }

    @Published private(set) var state: State = .loading

    private let store = PairingStore()
    private let codes = PairingCodeManager()
    private let connectivity = ConnectivityService.shared
    private var ticker: Task<Void, Never>?
    private var notice: String?

    init() {
        connectivity.onMessage = { [weak self] path, payload, reply in
            self?.handle(path: path, payload: payload, reply: reply)
        }
        connectivity.activate()
        if let saved = store.load() {
            state = .paired(saved)
        } else {
            startWaiting(nil)
        }
    }

    /// Genera un código nuevo y actualiza la cuenta atrás cada segundo; lo renueva al caducar.
    func startWaiting(_ message: String?) {
        notice = message
        codes.newCode()
        ticker?.cancel()
        ticker = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { return }
                if self.codes.isExpired() {
                    self.codes.newCode()
                    self.notice = "Código renovado"
                }
                let code = self.codes.active ?? self.codes.newCode()
                let left = max(0, Int((code.expiresAtMs - currentTimeMs()) / 1000))
                self.state = .waiting(
                    code: code.formatted,
                    secondsLeft: left,
                    totalSeconds: Int(self.codes.ttlMs / 1000),
                    notice: self.notice
                )
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }

    func unpair() {
        if let phone = store.load() {
            connectivity.send(path: PairingProtocol.pathRevoke, payload: PairingProtocol.encode(PairingProtocol.Revoke(token: phone.token)))
        }
        store.clear()
        startWaiting(nil)
    }

    private func handle(path: String, payload: Data, reply: ((Data) -> Void)?) {
        switch path {
        case PairingProtocol.pathRequest:
            let result = handleRequest(payload)
            reply?(PairingProtocol.encode(result))
        case PairingProtocol.pathRevoke:
            reply?(Data())
            if let revoke = PairingProtocol.decode(PairingProtocol.Revoke.self, from: payload),
               let current = store.load(), current.token == revoke.token {
                store.clear()
                startWaiting("El iPhone desvinculó este reloj")
            }
        default:
            reply?(Data())
        }
    }

    private func handleRequest(_ payload: Data) -> PairingProtocol.Result {
        guard let request = PairingProtocol.decode(PairingProtocol.Request.self, from: payload) else {
            return PairingProtocol.Result(ok: false, error: "Mensaje no válido")
        }
        if case .paired = state {
            return PairingProtocol.Result(ok: false, error: "Este reloj ya está vinculado. Desvincúlalo desde el reloj")
        }

        let result = codes.verify(request.code)
        switch result {
        case .success:
            let device = PairedDevice(name: request.phoneName, token: PairingCodeManager.newToken(), pairedAtMs: currentTimeMs())
            store.save(device)
            ticker?.cancel()
            state = .paired(device)
            WKInterfaceDevice.current().play(.success)
            return PairingProtocol.Result(ok: true, token: device.token, watchName: WKInterfaceDevice.current().name)
        case .tooManyAttempts:
            startWaiting("Demasiados intentos. Código nuevo")
            return PairingProtocol.errorFor(result)
        case .wrongCode(let left):
            notice = "Código incorrecto (quedan \(left))"
            WKInterfaceDevice.current().play(.failure)
            return PairingProtocol.errorFor(result)
        case .expired, .noActiveCode:
            return PairingProtocol.errorFor(result)
        }
    }
}
