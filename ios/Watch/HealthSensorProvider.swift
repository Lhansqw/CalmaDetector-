import Foundation
import HealthKit
import CoreMotion
import CalmaCore

/// Lee pulso (HealthKit, sesión de entrenamiento "Mente y cuerpo"), HRV (HealthKit)
/// y movimiento (acelerómetro) y entrega una `Sample` cada 5 segundos.
///
/// - El pulso en directo solo está disponible con una `HKWorkoutSession` activa.
/// - El Apple Watch mide HRV (SDNN) pocas veces al día; solo se usa si es reciente.
///   Si no hay, la muestra lleva `hrvMs = nil` y el detector funciona solo con BPM.
@MainActor
final class HealthSensorProvider: NSObject, LiveSampleProvider {
    enum SensorError: LocalizedError {
        case unavailable
        case denied
        var errorDescription: String? {
            switch self {
            case .unavailable: return "Salud no está disponible en este dispositivo."
            case .denied: return "Permite el acceso a Salud en Ajustes > Salud > Acceso a datos > CalmaDetector."
            }
        }
    }

    private static let emitIntervalNs: UInt64 = 5_000_000_000
    private static let maxHrvAge: TimeInterval = 15 * 60

    private let healthStore = HKHealthStore()
    private let motionManager = CMMotionManager()
    private let heartRateType = HKQuantityType(.heartRate)
    private let hrvType = HKQuantityType(.heartRateVariabilitySDNN)

    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?
    private var hrvQuery: HKAnchoredObjectQuery?
    private var emitTask: Task<Void, Never>?

    private var latestBpm: Double?
    private var latestHrv: (value: Double, date: Date)?
    private var magnitudes: [Double] = []

    func start(onSample: @escaping @MainActor (Sample) -> Void) async throws {
        guard HKHealthStore.isHealthDataAvailable() else { throw SensorError.unavailable }
        stop()

        let toShare: Set<HKSampleType> = [HKObjectType.workoutType()]
        let toRead: Set<HKObjectType> = [heartRateType, hrvType]
        try await healthStore.requestAuthorization(toShare: toShare, read: toRead)
        // Sin permiso de entrenamiento no hay pulso en directo. (iOS no revela si se negó la lectura.)
        if healthStore.authorizationStatus(for: HKObjectType.workoutType()) == .sharingDenied {
            throw SensorError.denied
        }

        try startWorkout()
        startHrvQuery()
        startMotion()

        emitTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: Self.emitIntervalNs)
                guard !Task.isCancelled, let self else { return }
                if let sample = self.makeSample() { onSample(sample) }
            }
        }
    }

    func stop() {
        emitTask?.cancel()
        emitTask = nil
        motionManager.stopAccelerometerUpdates()
        if let hrvQuery { healthStore.stop(hrvQuery) }
        hrvQuery = nil

        if let session, let builder {
            session.end()
            // No guardamos un entrenamiento en Salud cada vez que se usa la app.
            builder.endCollection(withEnd: Date()) { _, _ in
                builder.discardWorkout()
            }
        }
        session = nil
        builder = nil
        latestBpm = nil
        magnitudes.removeAll()
    }

    // MARK: - Muestra

    private func makeSample() -> Sample? {
        guard let bpm = latestBpm else { return nil }
        let hrv = latestHrv.flatMap { Date().timeIntervalSince($0.date) <= Self.maxHrvAge ? $0.value : nil }
        let motion = Self.standardDeviation(magnitudes)
        magnitudes.removeAll(keepingCapacity: true)
        return Sample(timestampMs: currentTimeMs(), bpm: bpm, hrvMs: hrv, motion: motion)
    }

    static func standardDeviation(_ values: [Double]) -> Double {
        guard values.count > 1 else { return 0 }
        let mean = values.reduce(0, +) / Double(values.count)
        let variance = values.reduce(0) { $0 + ($1 - mean) * ($1 - mean) } / Double(values.count - 1)
        return variance.squareRoot()
    }

    // MARK: - Pulso (sesión de entrenamiento)

    private func startWorkout() throws {
        let config = HKWorkoutConfiguration()
        config.activityType = .mindAndBody
        config.locationType = .indoor

        let session = try HKWorkoutSession(healthStore: healthStore, configuration: config)
        let builder = session.associatedWorkoutBuilder()
        builder.dataSource = HKLiveWorkoutDataSource(healthStore: healthStore, workoutConfiguration: config)
        builder.delegate = self
        self.session = session
        self.builder = builder

        let start = Date()
        session.startActivity(with: start)
        builder.beginCollection(withStart: start) { _, _ in }
    }

    // MARK: - HRV

    private func startHrvQuery() {
        let since = Date().addingTimeInterval(-Self.maxHrvAge)
        let predicate = HKQuery.predicateForSamples(withStart: since, end: nil, options: .strictStartDate)
        let handler: (HKAnchoredObjectQuery, [HKSample]?, [HKDeletedObject]?, HKQueryAnchor?, Error?) -> Void = { [weak self] _, samples, _, _, _ in
            guard let latest = (samples as? [HKQuantitySample])?.max(by: { $0.endDate < $1.endDate }) else { return }
            let value = latest.quantity.doubleValue(for: .secondUnit(with: .milli))
            let date = latest.endDate
            Task { @MainActor in
                guard let self else { return }
                if self.latestHrv == nil || date > self.latestHrv!.date {
                    self.latestHrv = (value, date)
                }
            }
        }
        let query = HKAnchoredObjectQuery(type: hrvType, predicate: predicate, anchor: nil, limit: HKObjectQueryNoLimit, resultsHandler: handler)
        query.updateHandler = handler
        healthStore.execute(query)
        hrvQuery = query
    }

    // MARK: - Movimiento

    private func startMotion() {
        guard motionManager.isAccelerometerAvailable else { return }
        motionManager.accelerometerUpdateInterval = 1.0 / 25.0
        motionManager.startAccelerometerUpdates(to: .main) { [weak self] data, _ in
            guard let a = data?.acceleration else { return }
            // El acelerómetro da "g"; el detector espera m/s² (igual que Android).
            let magnitude = (a.x * a.x + a.y * a.y + a.z * a.z).squareRoot() * 9.80665
            MainActor.assumeIsolated {
                self?.magnitudes.append(magnitude)
            }
        }
    }
}

extension HealthSensorProvider: HKLiveWorkoutBuilderDelegate {
    nonisolated func workoutBuilder(_ workoutBuilder: HKLiveWorkoutBuilder, didCollectDataOf collectedTypes: Set<HKSampleType>) {
        let type = HKQuantityType(.heartRate)
        guard collectedTypes.contains(type),
              let quantity = workoutBuilder.statistics(for: type)?.mostRecentQuantity() else { return }
        let bpm = quantity.doubleValue(for: HKUnit.count().unitDivided(by: .minute()))
        Task { @MainActor in self.latestBpm = bpm }
    }

    nonisolated func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) {}
}
