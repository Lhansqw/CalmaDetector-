import Foundation

/// Una lectura combinada de sensores. `hrvMs` puede ser nil si el reloj no la expone.
public struct Sample: Equatable, Codable, Sendable {
    public var timestampMs: Int64
    public var bpm: Double
    /// RMSSD o SDNN en ms (nil si no disponible).
    public var hrvMs: Double?
    /// Desviación estándar de la magnitud del acelerómetro (m/s²).
    public var motion: Double

    public init(timestampMs: Int64, bpm: Double, hrvMs: Double?, motion: Double) {
        self.timestampMs = timestampMs
        self.bpm = bpm
        self.hrvMs = hrvMs
        self.motion = motion
    }
}

public enum AlertLevel: String, Codable, Sendable {
    case none
    case level1Mild
    case level2Imminent
}

/// Estadística incremental (Welford) para la línea base.
public final class RunningStats {
    public private(set) var n: Int64 = 0
    public private(set) var mean: Double = 0
    private var m2: Double = 0

    public var std: Double { n > 1 ? (m2 / Double(n - 1)).squareRoot() : 0 }

    public init() {}

    public func add(_ x: Double) {
        n += 1
        let d = x - mean
        mean += d / Double(n)
        m2 += d * (x - mean)
    }

    public func restore(n: Int64, mean: Double, std: Double) {
        self.n = n
        self.mean = mean
        self.m2 = std * std * Double(max(n - 1, 0))
    }
}

/// Línea base personal: solo aprende de muestras en reposo real (poco movimiento).
/// Se considera "lista" tras `minSamples` muestras.
public final class Baseline {
    public let bpm = RunningStats()
    public let hrv = RunningStats()
    public let minSamples: Int64

    public init(minSamples: Int64 = 2_000) {
        self.minSamples = minSamples
    }

    public var isReady: Bool { bpm.n >= minSamples }

    public func learn(_ s: Sample, restMotionMax: Double) {
        guard s.motion <= restMotionMax else { return }
        bpm.add(s.bpm)
        if let h = s.hrvMs { hrv.add(h) }
    }

    /// z-score del BPM (positivo = por encima del reposo).
    public func bpmZ(_ bpmNow: Double) -> Double {
        let sd = max(bpm.std, 3.0) // piso para evitar divisiones por casi-cero
        return (bpmNow - bpm.mean) / sd
    }

    /// z-score del HRV (negativo = HRV por debajo de lo normal).
    public func hrvZ(_ hrvNow: Double) -> Double? {
        guard hrv.n >= 30 else { return nil }
        let sd = max(hrv.std, 3.0)
        return (hrvNow - hrv.mean) / sd
    }
}

public struct DetectorConfig: Sendable {
    public var windowMs: Int64
    public var minSamplesInWindow: Int
    public var restMotionMax: Double
    public var activeMotion: Double
    public var l1EnterZ: Double, l1ExitZ: Double
    public var l2EnterZ: Double, l2ExitZ: Double
    public var hrvLowZ: Double
    public var sustainedFraction: Double
    public var cooldownL1Ms: Int64
    public var cooldownL2Ms: Int64

    public init(
        windowMs: Int64 = 3 * 60_000,          // ventana de evaluación
        minSamplesInWindow: Int = 12,
        restMotionMax: Double = 0.35,          // "casi inmóvil" (no exactamente 0)
        activeMotion: Double = 1.5,            // movimiento claro => descartar
        l1EnterZ: Double = 1.5, l1ExitZ: Double = 0.8,
        l2EnterZ: Double = 2.5, l2ExitZ: Double = 1.5,
        hrvLowZ: Double = -1.0,                // HRV "baja" para refuerzo
        sustainedFraction: Double = 0.7,       // % de la ventana que debe cumplir
        cooldownL1Ms: Int64 = 20 * 60_000,
        cooldownL2Ms: Int64 = 10 * 60_000
    ) {
        self.windowMs = windowMs
        self.minSamplesInWindow = minSamplesInWindow
        self.restMotionMax = restMotionMax
        self.activeMotion = activeMotion
        self.l1EnterZ = l1EnterZ
        self.l1ExitZ = l1ExitZ
        self.l2EnterZ = l2EnterZ
        self.l2ExitZ = l2ExitZ
        self.hrvLowZ = hrvLowZ
        self.sustainedFraction = sustainedFraction
        self.cooldownL1Ms = cooldownL1Ms
        self.cooldownL2Ms = cooldownL2Ms
    }
}

/// Port directo de `StressDetector.kt`: mismas reglas, umbrales y cooldowns.
public final class StressDetector {
    public let baseline: Baseline
    private let cfg: DetectorConfig
    private var window: [Sample] = []
    private var current: AlertLevel = .none
    private var lastL1Ms: Int64 = Int64.min / 2
    private var lastL2Ms: Int64 = Int64.min / 2

    /// 1.0 = normal; >1 = menos sensible (tras "no fue ansiedad"); <1 = más sensible.
    private var sensitivity = 1.0

    /// Nivel en el que está el usuario ahora (aunque la alerta no se repita por el cooldown).
    public var currentLevel: AlertLevel { current }
    public var currentSensitivity: Double { sensitivity }

    public init(baseline: Baseline, config: DetectorConfig = DetectorConfig()) {
        self.baseline = baseline
        self.cfg = config
    }

    /// Procesa una muestra y devuelve el nivel de alerta que debe dispararse (o `.none`).
    @discardableResult
    public func onSample(_ s: Sample) -> AlertLevel {
        // 1) Aprender línea base solo en reposo y mientras no estemos en alerta
        if current == .none { baseline.learn(s, restMotionMax: cfg.restMotionMax) }

        // 2) Mantener ventana
        window.append(s)
        while let first = window.first, s.timestampMs - first.timestampMs > cfg.windowMs {
            window.removeFirst()
        }
        guard baseline.isReady, window.count >= cfg.minSamplesInWindow else { return .none }

        // 3) Contexto de movimiento: si hay actividad clara en la ventana, se descarta
        let size = Double(window.count)
        let activeShare = Double(window.filter { $0.motion > cfg.activeMotion }.count) / size
        if activeShare > 0.2 {
            current = .none
            return .none
        }
        let restShare = Double(window.filter { $0.motion <= cfg.restMotionMax * 2 }.count) / size
        if restShare < cfg.sustainedFraction { return .none }

        // 4) Evaluar condición sostenida con histéresis
        let l1Enter = cfg.l1EnterZ * sensitivity
        let l2Enter = cfg.l2EnterZ * sensitivity
        let l1Threshold = current != .none ? cfg.l1ExitZ : l1Enter
        let l2Threshold = current == .level2Imminent ? cfg.l2ExitZ : l2Enter

        let l1Frac = fractionWhere { baseline.bpmZ($0.bpm) > l1Threshold }
        let l2Frac = fractionWhere { baseline.bpmZ($0.bpm) > l2Threshold }
        let hrvLowFrac = fractionHrvLow()

        let l2Met = l2Frac >= cfg.sustainedFraction && (hrvLowFrac == nil || hrvLowFrac! >= 0.5)
        let l1Met = l1Frac >= cfg.sustainedFraction && (hrvLowFrac == nil || hrvLowFrac! >= 0.3)

        let target: AlertLevel = l2Met ? .level2Imminent : (l1Met ? .level1Mild : .none)

        // 5) Cooldown: solo disparar si pasó el tiempo; el estado interno sí se actualiza
        let now = s.timestampMs
        current = target
        switch target {
        case .level2Imminent:
            if now - lastL2Ms >= cfg.cooldownL2Ms { lastL2Ms = now; return target }
            return .none
        case .level1Mild:
            if now - lastL1Ms >= cfg.cooldownL1Ms && now - lastL2Ms >= cfg.cooldownL2Ms {
                lastL1Ms = now
                return target
            }
            return .none
        case .none:
            return .none
        }
    }

    /// Feedback del usuario tras una alerta: ajusta la sensibilidad.
    public func onUserFeedback(wasAnxiety: Bool) {
        sensitivity = wasAnxiety ? max(0.8, sensitivity - 0.05) : min(1.5, sensitivity + 0.1)
    }

    /// Reinicia el estado y los cooldowns (la línea base aprendida se conserva).
    public func resetState() {
        current = .none
        window.removeAll()
        lastL1Ms = Int64.min / 2
        lastL2Ms = Int64.min / 2
    }

    private func fractionWhere(_ pred: (Sample) -> Bool) -> Double {
        Double(window.filter(pred).count) / Double(window.count)
    }

    /// Fracción de muestras con HRV baja, o nil si no hay datos de HRV suficientes.
    private func fractionHrvLow() -> Double? {
        let withHrv = window.compactMap { $0.hrvMs }
        guard withHrv.count >= 3 else { return nil }
        let z = withHrv.compactMap { baseline.hrvZ($0) }
        guard !z.isEmpty else { return nil }
        return Double(z.filter { $0 < cfg.hrvLowZ }.count) / Double(z.count)
    }
}
