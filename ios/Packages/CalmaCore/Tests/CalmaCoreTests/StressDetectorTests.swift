import XCTest
@testable import CalmaCore

final class StressDetectorTests: XCTestCase {
    private var rng = SeededGenerator(seed: 7)
    private var t: Int64 = 0

    private func feed(_ d: StressDetector, _ n: Int, bpm: Double, hrv: Double, motion: Double) -> [AlertLevel] {
        (0..<n).map { _ in
            t += 10_000
            return d.onSample(Sample(
                timestampMs: t,
                bpm: bpm + Double.random(in: -2.0..<2.0, using: &rng),
                hrvMs: hrv + Double.random(in: -3.0..<3.0, using: &rng),
                motion: motion
            ))
        }
    }

    private func trainedDetector() -> StressDetector {
        let d = StressDetector(baseline: Baseline(minSamples: 100))
        _ = feed(d, 120, bpm: 65, hrv: 50, motion: 0.05)
        return d
    }

    func testEjercicioNoDisparaAlertas() {
        let d = trainedDetector()
        let alerts = feed(d, 30, bpm: 120, hrv: 20, motion: 3.0)
        XCTAssertTrue(alerts.allSatisfy { $0 == .none })
    }

    func testEstresEnReposoDisparaUnaAlertaYElNivelSeMantiene() {
        let d = trainedDetector()
        let alerts = feed(d, 30, bpm: 100, hrv: 22, motion: 0.05)
        XCTAssertEqual(alerts.filter { $0 == .level2Imminent }.count, 1) // una sola vez por el cooldown
        XCTAssertEqual(d.currentLevel, .level2Imminent)                  // pero el estado sigue activo
    }

    func testLaSensibilidadSeAcumulaYTieneLimites() {
        let d = trainedDetector()
        for _ in 0..<20 { d.onUserFeedback(wasAnxiety: false) }
        XCTAssertEqual(d.currentSensitivity, 1.5, accuracy: 1e-9)
        for _ in 0..<20 { d.onUserFeedback(wasAnxiety: true) }
        XCTAssertEqual(d.currentSensitivity, 0.8, accuracy: 1e-9)
    }

    func testRunningStatsCalculaMediaYDesviacion() {
        let s = RunningStats()
        [2.0, 4, 4, 4, 5, 5, 7, 9].forEach(s.add)
        XCTAssertEqual(s.mean, 5, accuracy: 1e-9)
        XCTAssertEqual(s.std, 2.138089935, accuracy: 1e-6)
    }
}
