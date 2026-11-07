import Foundation
import Combine
import CalmaCore

struct AlertEvent: Identifiable {
    let id = UUID()
    let timestampMs: Int64
    let level: AlertLevel
    let bpm: Double
    let scenarioName: String
}

/// De dónde salen las muestras: simulador de escenarios o sensores reales (HealthKit en el reloj).
enum DataSource: String, CaseIterable, Identifiable {
    case simulator
    case sensors

    var id: String { rawValue }
    var displayName: String { self == .simulator ? "Simulador" : "Sensores" }
}

/// Fuente de muestras reales. En el Apple Watch la implementa `HealthSensorProvider`.
@MainActor
protocol LiveSampleProvider: AnyObject {
    func start(onSample: @escaping @MainActor (Sample) -> Void) async throws
    func stop()
}

/// Port de `CalmaViewModel.kt`: mismo detector, mismo simulador y mismos estados.
@MainActor
final class CalmaViewModel: ObservableObject {
    @Published private(set) var latestSample = Sample(timestampMs: currentTimeMs(), bpm: 65, hrvMs: 50, motion: 0.05)
    @Published private(set) var currentAlert: AlertLevel = .none
    @Published private(set) var baselineReady = false
    @Published private(set) var baselineProgress: Double = 0
    @Published private(set) var baselineMeanBpm: Double = 0
    @Published private(set) var baselineMeanHrv: Double = 0
    @Published private(set) var bpmZScore: Double = 0
    @Published private(set) var sensitivity: Double = 1
    @Published var currentScenario: SimulationScenario = .reposoNormal
    @Published private(set) var sampleHistory: [Sample] = []
    @Published private(set) var alertLog: [AlertEvent] = []
    @Published private(set) var isRunning = false
    @Published private(set) var dataSource: DataSource = .simulator
    @Published private(set) var sensorMessage: String?

    /// Se llama cuando se dispara una alerta nueva (para vibrar o notificar).
    var onAlert: ((AlertLevel) -> Void)?

    var supportsSensors: Bool { sensorProvider != nil }

    private static let baselineTarget: Int64 = 60 // 60 muestras para demo rápida
    private static let maxAlertLog = 50

    private var baseline: Baseline
    private var detector: StressDetector
    private var simulationTask: Task<Void, Never>?
    private var simulatedTimeMs = currentTimeMs()
    private var rng = SeededGenerator(seed: 42)
    private let sensorProvider: LiveSampleProvider?

    init(sensorProvider: LiveSampleProvider? = nil) {
        self.sensorProvider = sensorProvider
        let made = Self.makeDetector()
        baseline = made.0
        detector = made.1
        pretrainBaseline()
        start()
    }

    private static func makeDetector() -> (Baseline, StressDetector) {
        let b = Baseline(minSamples: baselineTarget)
        return (b, StressDetector(baseline: b, config: DetectorConfig(minSamplesInWindow: 5)))
    }

    /// Pre-entrena parcialmente la línea base para que la demo sea fluida.
    private func pretrainBaseline() {
        for _ in 0..<40 {
            simulatedTimeMs += 10_000
            detector.onSample(Sample(
                timestampMs: simulatedTimeMs,
                bpm: 65 + Double.random(in: -2.0..<2.0, using: &rng),
                hrvMs: 50 + Double.random(in: -3.0..<3.0, using: &rng),
                motion: 0.05 + Double.random(in: 0.0..<0.05, using: &rng)
            ))
        }
        updateBaselineState()
    }

    // MARK: - Control

    func start() {
        guard !isRunning else { return }
        isRunning = true
        switch dataSource {
        case .simulator: startSimulation()
        case .sensors: startSensors()
        }
    }

    func pause() {
        simulationTask?.cancel()
        simulationTask = nil
        sensorProvider?.stop()
        isRunning = false
    }

    func selectScenario(_ scenario: SimulationScenario) {
        currentScenario = scenario
    }

    func selectDataSource(_ source: DataSource) {
        guard source != dataSource else { return }
        if source == .sensors && sensorProvider == nil { return }
        pause()
        dataSource = source
        sensorMessage = nil
        // Línea base nueva: no mezclar datos simulados con datos reales.
        (baseline, detector) = Self.makeDetector()
        if source == .simulator { pretrainBaseline() }
        currentAlert = .none
        sampleHistory = []
        alertLog = []
        bpmZScore = 0
        sensitivity = detector.currentSensitivity
        updateBaselineState()
        start()
    }

    func sendFeedback(wasAnxiety: Bool) {
        detector.onUserFeedback(wasAnxiety: wasAnxiety)
        sensitivity = detector.currentSensitivity
    }

    func resetDetector() {
        detector.resetState()
        currentAlert = .none
        alertLog = []
        sampleHistory = []
    }

    // MARK: - Fuentes de datos

    private func startSimulation() {
        simulationTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_200_000_000) // nueva muestra cada 1,2 s
                guard !Task.isCancelled, let self else { return }
                self.simulatedTimeMs += 10_000
                let sample = self.currentScenario.makeSample(at: self.simulatedTimeMs, using: &self.rng)
                self.process(sample, sourceName: self.currentScenario.displayName)
            }
        }
    }

    private func startSensors() {
        guard let provider = sensorProvider else { return }
        sensorMessage = "Esperando pulso…"
        Task { [weak self] in
            do {
                try await provider.start { sample in
                    guard let self, self.dataSource == .sensors, self.isRunning else { return }
                    self.sensorMessage = nil
                    self.process(sample, sourceName: "Sensores")
                }
            } catch {
                guard let self else { return }
                self.isRunning = false
                self.sensorMessage = "No se pudo leer el pulso: \(error.localizedDescription)"
            }
        }
    }

    private func process(_ sample: Sample, sourceName: String) {
        let newAlert = detector.onSample(sample)

        if newAlert != .none {
            alertLog.insert(AlertEvent(timestampMs: sample.timestampMs, level: newAlert, bpm: sample.bpm, scenarioName: sourceName), at: 0)
            if alertLog.count > Self.maxAlertLog { alertLog.removeLast() }
            onAlert?(newAlert)
        }

        latestSample = sample
        // El estado visible es el nivel actual del detector; newAlert solo indica
        // cuándo se dispara una alerta nueva (devuelve .none durante el cooldown).
        currentAlert = detector.currentLevel
        bpmZScore = baseline.bpmZ(sample.bpm)
        sampleHistory = Array((sampleHistory + [sample]).suffix(20))
        updateBaselineState()
    }

    private func updateBaselineState() {
        baselineReady = baseline.isReady
        baselineProgress = min(1, Double(baseline.bpm.n) / Double(Self.baselineTarget))
        baselineMeanBpm = baseline.bpm.mean
        baselineMeanHrv = baseline.hrv.mean
    }
}
