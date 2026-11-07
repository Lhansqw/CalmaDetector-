import Foundation

/// Escenarios del simulador (mismos valores que en Android).
public enum SimulationScenario: String, CaseIterable, Identifiable, Sendable {
    case reposoNormal
    case ejercicio
    case estresAnsiedad
    case recuperacion

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .reposoNormal: return "1. Reposo Normal"
        case .ejercicio: return "2. Ejercicio Físico"
        case .estresAnsiedad: return "3. Estrés en Reposo"
        case .recuperacion: return "4. Recuperación"
        }
    }

    public var targetBpm: Double {
        switch self {
        case .reposoNormal: return 65
        case .ejercicio: return 120
        case .estresAnsiedad: return 102
        case .recuperacion: return 66
        }
    }

    public var targetHrv: Double {
        switch self {
        case .reposoNormal: return 50
        case .ejercicio: return 20
        case .estresAnsiedad: return 22
        case .recuperacion: return 48
        }
    }

    public var motion: Double {
        switch self {
        case .ejercicio: return 3.0
        default: return 0.05
        }
    }

    /// Genera una muestra con ruido alrededor de los valores del escenario.
    public func makeSample<G: RandomNumberGenerator>(at timestampMs: Int64, using rng: inout G) -> Sample {
        Sample(
            timestampMs: timestampMs,
            bpm: targetBpm + Double.random(in: -3.0..<3.0, using: &rng),
            hrvMs: targetHrv + Double.random(in: -4.0..<4.0, using: &rng),
            motion: max(0, motion + Double.random(in: -0.02..<0.05, using: &rng))
        )
    }
}

/// Generador con semilla para que la demo y los tests sean reproducibles.
public struct SeededGenerator: RandomNumberGenerator {
    private var state: UInt64

    public init(seed: UInt64) {
        state = seed == 0 ? 0x9E3779B97F4A7C15 : seed
    }

    /// SplitMix64.
    public mutating func next() -> UInt64 {
        state &+= 0x9E3779B97F4A7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58476D1CE4E5B9
        z = (z ^ (z >> 27)) &* 0x94D049BB133111EB
        return z ^ (z >> 31)
    }
}
