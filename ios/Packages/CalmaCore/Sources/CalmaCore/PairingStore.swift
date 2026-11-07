import Foundation

/// Dispositivo vinculado guardado en este equipo (en el reloj: el iPhone; en el iPhone: el reloj).
public struct PairedDevice: Codable, Equatable, Sendable {
    public var name: String
    public var token: String
    public var pairedAtMs: Int64

    public init(name: String, token: String, pairedAtMs: Int64) {
        self.name = name
        self.token = token
        self.pairedAtMs = pairedAtMs
    }
}

/// Persistencia mínima de la vinculación en UserDefaults.
public final class PairingStore {
    private let defaults: UserDefaults
    private let key = "calma_pairing"

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    public func load() -> PairedDevice? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(PairedDevice.self, from: data)
    }

    public func save(_ device: PairedDevice) {
        if let data = try? JSONEncoder().encode(device) {
            defaults.set(data, forKey: key)
        }
    }

    public func clear() {
        defaults.removeObject(forKey: key)
    }
}
