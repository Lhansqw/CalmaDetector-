import Foundation
import Combine
import WatchConnectivity

enum ConnectivityError: LocalizedError {
    case unsupported
    case notActivated
    case notPaired
    case appNotInstalled
    case notReachable
    case badReply

    var errorDescription: String? {
        switch self {
        case .unsupported: return "Este dispositivo no admite Apple Watch."
        case .notActivated: return "La conexión con el reloj aún no está lista. Inténtalo de nuevo."
        case .notPaired: return "No hay ningún Apple Watch emparejado. Empareja el reloj desde la app Watch del iPhone."
        case .appNotInstalled: return "CalmaDetector no está instalada en el reloj. Instálala desde la app Watch del iPhone."
        case .notReachable: return "El reloj no responde. Abre CalmaDetector en el reloj e inténtalo de nuevo."
        case .badReply: return "Respuesta inesperada del reloj."
        }
    }
}

/// Envoltorio de `WCSession` (equivalente a la Wearable Data Layer de Android).
///
/// Cada mensaje es un diccionario `["path": String, "payload": Data]`, con el JSON de `PairingProtocol`.
@MainActor
final class ConnectivityService: NSObject, ObservableObject {
    static let shared = ConnectivityService()

    /// Mensaje recibido. `reply` solo existe si el otro lado espera respuesta; hay que llamarlo siempre.
    var onMessage: ((_ path: String, _ payload: Data, _ reply: ((Data) -> Void)?) -> Void)?

    @Published private(set) var isReachable = false

    private var wcSession: WCSession? { WCSession.isSupported() ? WCSession.default : nil }

    func activate() {
        guard let session = wcSession, session.delegate == nil else { return }
        session.delegate = self
        session.activate()
    }

    /// Envía un mensaje y espera la respuesta del otro dispositivo.
    func request(path: String, payload: Data) async throws -> Data {
        guard let session = wcSession else { throw ConnectivityError.unsupported }
        guard session.activationState == .activated else { throw ConnectivityError.notActivated }
        #if os(iOS)
        guard session.isPaired else { throw ConnectivityError.notPaired }
        guard session.isWatchAppInstalled else { throw ConnectivityError.appNotInstalled }
        #endif
        guard session.isReachable else { throw ConnectivityError.notReachable }

        return try await withCheckedThrowingContinuation { continuation in
            session.sendMessage(
                ["path": path, "payload": payload],
                replyHandler: { reply in
                    if let data = reply["payload"] as? Data {
                        continuation.resume(returning: data)
                    } else {
                        continuation.resume(throwing: ConnectivityError.badReply)
                    }
                },
                errorHandler: { error in
                    continuation.resume(throwing: error)
                }
            )
        }
    }

    /// Envía sin esperar respuesta. Si el otro lado no está accesible, queda en cola y llega después.
    func send(path: String, payload: Data) {
        guard let session = wcSession, session.activationState == .activated else { return }
        let message: [String: Any] = ["path": path, "payload": payload]
        if session.isReachable {
            session.sendMessage(message, replyHandler: nil) { _ in
                session.transferUserInfo(message)
            }
        } else {
            session.transferUserInfo(message)
        }
    }

    private func deliver(_ message: [String: Any], reply: (([String: Any]) -> Void)?) {
        guard let path = message["path"] as? String else {
            reply?([:])
            return
        }
        let payload = message["payload"] as? Data ?? Data()
        let wrappedReply: ((Data) -> Void)? = reply.map { r -> (Data) -> Void in
            return { data in r(["payload": data]) }
        }
        if let onMessage {
            onMessage(path, payload, wrappedReply)
        } else {
            wrappedReply?(Data())
        }
    }
}

extension ConnectivityService: WCSessionDelegate {
    nonisolated func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        let reachable = session.isReachable
        Task { @MainActor in self.isReachable = reachable }
    }

    nonisolated func sessionReachabilityDidChange(_ session: WCSession) {
        let reachable = session.isReachable
        Task { @MainActor in self.isReachable = reachable }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {
        Task { @MainActor in self.deliver(message, reply: replyHandler) }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        Task { @MainActor in self.deliver(message, reply: nil) }
    }

    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        Task { @MainActor in self.deliver(userInfo, reply: nil) }
    }

    #if os(iOS)
    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}

    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        // Cambio de reloj: reactivar para hablar con el nuevo.
        session.activate()
    }
    #endif
}
