import Foundation

/// Mensajes que intercambian iPhone y Apple Watch por WatchConnectivity.
///
/// Flujo (igual que en Android):
///  1. El reloj muestra un código de 6 dígitos.
///  2. El usuario lo escribe en el iPhone -> el iPhone envía `pathRequest` con el código.
///  3. El reloj lo valida y responde (ok + token de sesión, o error).
///  4. Cualquiera de los dos puede desvincular enviando `pathRevoke` con el token.
public enum PairingProtocol {
    public static let pathRequest = "/calma/pair/request"
    public static let pathRevoke = "/calma/pair/revoke"

    public struct Request: Codable, Equatable, Sendable {
        public var code: String
        public var phoneName: String
        public init(code: String, phoneName: String) {
            self.code = code
            self.phoneName = phoneName
        }
    }

    public struct Result: Codable, Equatable, Sendable {
        public var ok: Bool
        public var token: String?
        public var watchName: String?
        public var error: String?
        public var attemptsLeft: Int?
        public init(ok: Bool, token: String? = nil, watchName: String? = nil, error: String? = nil, attemptsLeft: Int? = nil) {
            self.ok = ok
            self.token = token
            self.watchName = watchName
            self.error = error
            self.attemptsLeft = attemptsLeft
        }
    }

    /// El token identifica la vinculación: solo se acepta una desvinculación con el token correcto.
    public struct Revoke: Codable, Equatable, Sendable {
        public var token: String
        public init(token: String) { self.token = token }
    }

    public static func encode<T: Encodable>(_ value: T) -> Data {
        (try? JSONEncoder().encode(value)) ?? Data()
    }

    public static func decode<T: Decodable>(_ type: T.Type, from data: Data) -> T? {
        try? JSONDecoder().decode(type, from: data)
    }

    /// Texto de error para mostrar al usuario según el resultado de la verificación.
    public static func errorFor(_ result: VerifyResult) -> Result {
        switch result {
        case .success:
            return Result(ok: true)
        case .wrongCode(let left):
            return Result(ok: false, error: "Código incorrecto", attemptsLeft: left)
        case .expired:
            return Result(ok: false, error: "El código caducó. Usa el nuevo código del reloj")
        case .tooManyAttempts:
            return Result(ok: false, error: "Demasiados intentos. El reloj generó un código nuevo")
        case .noActiveCode:
            return Result(ok: false, error: "El reloj no está esperando una vinculación")
        }
    }
}
