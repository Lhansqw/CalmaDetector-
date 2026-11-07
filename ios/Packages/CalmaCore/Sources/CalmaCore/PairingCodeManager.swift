import Foundation

/// Milisegundos desde 1970, igual que `System.currentTimeMillis()` en Android.
public func currentTimeMs() -> Int64 {
    Int64(Date().timeIntervalSince1970 * 1000)
}

/// Código de vinculación que muestra el reloj.
public struct ActiveCode: Equatable, Sendable {
    public let code: String
    public let expiresAtMs: Int64

    /// "482913" -> "482 913" para que sea fácil de leer en el reloj.
    public var formatted: String {
        guard code.count == 6 else { return code }
        return "\(code.prefix(3)) \(code.suffix(3))"
    }
}

public enum VerifyResult: Equatable, Sendable {
    case success
    case wrongCode(attemptsLeft: Int)
    case expired
    case tooManyAttempts
    case noActiveCode
}

/// Genera y valida códigos de un solo uso (estilo "device code").
/// El código caduca tras `ttlMs` y se invalida tras `maxAttempts` intentos fallidos.
public final class PairingCodeManager {
    public let ttlMs: Int64
    private let clock: () -> Int64
    private let maxAttempts: Int
    private let digits: Int

    public private(set) var active: ActiveCode?
    private var failedAttempts = 0

    public init(
        clock: @escaping () -> Int64 = currentTimeMs,
        ttlMs: Int64 = 5 * 60_000,
        maxAttempts: Int = 5,
        digits: Int = 6
    ) {
        self.clock = clock
        self.ttlMs = ttlMs
        self.maxAttempts = maxAttempts
        self.digits = digits
    }

    @discardableResult
    public func newCode() -> ActiveCode {
        var bound = 1
        for _ in 0..<digits { bound *= 10 }
        // SystemRandomNumberGenerator es criptográficamente seguro en Apple y Linux.
        var rng = SystemRandomNumberGenerator()
        let value = Int.random(in: 0..<bound, using: &rng)
        let raw = String(value)
        let code = String(repeating: "0", count: max(0, digits - raw.count)) + raw
        failedAttempts = 0
        let c = ActiveCode(code: code, expiresAtMs: clock() + ttlMs)
        active = c
        return c
    }

    public func isExpired() -> Bool {
        guard let a = active else { return true }
        return clock() >= a.expiresAtMs
    }

    public func verify(_ input: String) -> VerifyResult {
        guard let current = active else { return .noActiveCode }
        if clock() >= current.expiresAtMs {
            active = nil
            return .expired
        }
        if Self.constantTimeEquals(Self.normalize(input), current.code) {
            active = nil // un solo uso
            return .success
        }
        failedAttempts += 1
        if failedAttempts >= maxAttempts {
            active = nil
            return .tooManyAttempts
        }
        return .wrongCode(attemptsLeft: maxAttempts - failedAttempts)
    }

    public func clear() {
        active = nil
        failedAttempts = 0
    }

    /// Quita espacios, guiones y cualquier carácter que no sea dígito ASCII.
    public static func normalize(_ input: String) -> String {
        String(input.filter { ("0"..."9").contains($0) })
    }

    /// Comparación en tiempo constante.
    static func constantTimeEquals(_ a: String, _ b: String) -> Bool {
        let x = Array(a.utf8), y = Array(b.utf8)
        guard x.count == y.count else { return false }
        var diff: UInt8 = 0
        for i in 0..<x.count { diff |= x[i] ^ y[i] }
        return diff == 0
    }

    /// Token de sesión aleatorio (32 bytes en base64url, sin relleno).
    public static func newToken() -> String {
        var rng = SystemRandomNumberGenerator()
        let bytes = (0..<32).map { _ in UInt8.random(in: .min ... .max, using: &rng) }
        return Data(bytes).base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
