import Foundation
import Combine
import UIKit
import CalmaCore

/// Lado del iPhone: el usuario escribe el código que ve en el reloj y lo confirma.
@MainActor
final class PhonePairingViewModel: ObservableObject {
    @Published private(set) var paired: PairedDevice?
    @Published private(set) var codeInput = ""
    @Published private(set) var isSending = false
    @Published private(set) var message: String?
    @Published private(set) var isError = false

    static let codeLength = 6

    private let store = PairingStore()
    private let connectivity = ConnectivityService.shared

    init() {
        paired = store.load()
        connectivity.onMessage = { [weak self] path, payload, reply in
            self?.handle(path: path, payload: payload)
            reply?(Data())
        }
        connectivity.activate()
    }

    func onCodeChange(_ input: String) {
        codeInput = String(PairingCodeManager.normalize(input).prefix(Self.codeLength))
        message = nil
        isError = false
    }

    func submitCode() {
        let code = codeInput
        guard code.count == Self.codeLength else {
            showError("El código tiene \(Self.codeLength) dígitos")
            return
        }
        guard !isSending else { return }

        isSending = true
        message = "Buscando reloj…"
        isError = false

        Task {
            defer { isSending = false }
            do {
                let request = PairingProtocol.Request(code: code, phoneName: UIDevice.current.name)
                let data = try await connectivity.request(path: PairingProtocol.pathRequest, payload: PairingProtocol.encode(request))
                guard let result = PairingProtocol.decode(PairingProtocol.Result.self, from: data) else {
                    showError(ConnectivityError.badReply.localizedDescription)
                    return
                }
                if result.ok, let token = result.token {
                    let device = PairedDevice(name: result.watchName ?? "Apple Watch", token: token, pairedAtMs: currentTimeMs())
                    store.save(device)
                    paired = device
                    codeInput = ""
                    message = "Reloj vinculado ✅"
                    isError = false
                } else {
                    let extra = result.attemptsLeft.map { " (quedan \($0) intentos)" } ?? ""
                    showError((result.error ?? "No se pudo vincular") + extra)
                }
            } catch {
                showError(error.localizedDescription)
            }
        }
    }

    func unpair() {
        if let watch = store.load() {
            connectivity.send(path: PairingProtocol.pathRevoke, payload: PairingProtocol.encode(PairingProtocol.Revoke(token: watch.token)))
        }
        store.clear()
        paired = nil
        codeInput = ""
        message = "Reloj desvinculado"
        isError = false
    }

    private func handle(path: String, payload: Data) {
        guard path == PairingProtocol.pathRevoke,
              let revoke = PairingProtocol.decode(PairingProtocol.Revoke.self, from: payload),
              let current = store.load(), current.token == revoke.token else { return }
        store.clear()
        paired = nil
        message = "El reloj cerró la sesión"
        isError = true
    }

    private func showError(_ text: String) {
        message = text
        isError = true
    }
}
