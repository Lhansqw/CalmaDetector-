import SwiftUI
import CalmaCore

/// Port de `CalmaDashboardScreen.kt` para iPhone.
struct DashboardView: View {
    @ObservedObject var viewModel: CalmaViewModel
    @ObservedObject var pairing: PhonePairingViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                header
                WatchPairingCard(pairing: pairing)
                StatusPulseCard(level: viewModel.currentAlert, bpmZScore: viewModel.bpmZScore)
                metricsRow
                baselineCard
                scenarioCard
                feedbackCard
                AlertLogCard(alertLog: viewModel.alertLog)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .padding(.bottom, 24)
        }
        .background(Color.calmaBackground.ignoresSafeArea())
        .scrollDismissesKeyboard(.interactively)
    }

    // MARK: - Secciones

    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("CalmaDetector")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(Color.textPrimary)
                Text("Monitoreo Anti-Ansiedad en Tiempo Real")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.textSecondary)
            }
            Spacer()
            Button(viewModel.isRunning ? "⏸ Pausar" : "▶ Reanudar") {
                viewModel.isRunning ? viewModel.pause() : viewModel.start()
            }
            .buttonStyle(.bordered)
            .buttonBorderShape(.capsule)
            .tint(viewModel.isRunning ? .statusCalm : .statusMild)
        }
    }

    private var metricsRow: some View {
        let s = viewModel.latestSample
        return HStack(spacing: 10) {
            MetricCard(title: "Ritmo Cardíaco", value: "\(Int(s.bpm))", unit: "BPM", icon: "❤️",
                       color: s.bpm > 95 ? .statusMild : .statusCalm)
            MetricCard(title: "Variabilidad", value: s.hrvMs.map { "\(Int($0))" } ?? "--", unit: "ms HRV", icon: "🌊",
                       color: s.hrvMs.map { $0 < 30 ? Color.statusMild : .statusCalm } ?? .textSecondary)
            MetricCard(title: "Movimiento", value: String(format: "%.2f", s.motion), unit: s.motion > 1 ? "Activo" : "Quieto",
                       icon: "🏃", color: s.motion > 1 ? .cyanPrimary : .textSecondary)
        }
    }

    private var baselineCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("🎯 Línea Base de Reposo")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.textPrimary)
                Spacer()
                Text(viewModel.baselineReady ? "✅ Lista" : "\(Int(viewModel.baselineProgress * 100))%")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(viewModel.baselineReady ? Color.statusCalm : .statusMild)
            }
            ProgressView(value: viewModel.baselineProgress)
                .tint(viewModel.baselineReady ? .statusCalm : .cyanPrimary)
            if viewModel.baselineReady {
                HStack {
                    Text("BPM Promedio Reposo: \(String(format: "%.1f", viewModel.baselineMeanBpm))")
                    Spacer()
                    Text("HRV Promedio: \(String(format: "%.1f", viewModel.baselineMeanHrv)) ms")
                }
                .font(.system(size: 11))
                .foregroundStyle(Color.textSecondary)
            }
        }
        .padding(16)
        .calmaCard()
    }

    private var scenarioCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("🧪 Simulador de Escenarios")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.textPrimary)
            Text("Prueba cómo reacciona el detector cambiando el estado del usuario:")
                .font(.system(size: 12))
                .foregroundStyle(Color.textSecondary)
                .padding(.bottom, 4)
            ForEach(SimulationScenario.allCases) { scenario in
                let selected = scenario == viewModel.currentScenario
                Button { viewModel.selectScenario(scenario) } label: {
                    Text(scenario.displayName)
                        .font(.system(size: 13, weight: selected ? .bold : .regular))
                        .foregroundStyle(selected ? Color.cyanPrimary : .textPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(selected ? Color.cyanPrimary.opacity(0.25) : .calmaBackground,
                                    in: RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .calmaCard()
    }

    private var feedbackCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("💬 Calibración y Retroalimentación")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.textPrimary)
            Text("Factor de Sensibilidad Actual: \(String(format: "%.2f", viewModel.sensitivity))x")
                .font(.system(size: 12))
                .foregroundStyle(Color.textSecondary)
            HStack(spacing: 8) {
                Button { viewModel.sendFeedback(wasAnxiety: true) } label: {
                    Text("Sí, sentí ansiedad").font(.system(size: 11)).frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(.statusMild)
                Button { viewModel.sendFeedback(wasAnxiety: false) } label: {
                    Text("Fue falsa alarma").font(.system(size: 11)).frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(.textPrimary)
            }
            .padding(.top, 4)
            Button(role: .destructive) { viewModel.resetDetector() } label: {
                Text("Restablecer Estado del Detector").font(.system(size: 12)).frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .tint(.statusImminent)
        }
        .padding(16)
        .calmaCard()
    }
}

// MARK: - Componentes

private struct StatusPulseCard: View {
    let level: AlertLevel
    let bpmZScore: Double
    @State private var pulsing = false

    private var title: String {
        switch level {
        case .none: return "Estado Normal / Calma"
        case .level1Mild: return "Alerta: Estrés Leve"
        case .level2Imminent: return "ALERTA: Estrés / Ansiedad Inminente"
        }
    }

    private var subtitle: String {
        switch level {
        case .none: return "Frecuencia cardíaca y variabilidad estables."
        case .level1Mild: return "Se detectó elevación inusual de BPM en reposo."
        case .level2Imminent: return "⚠️ Elevación crítica de BPM con caída de HRV en reposo. Se recomienda técnica de respiración 4-7-8."
        }
    }

    var body: some View {
        let imminent = level == .level2Imminent
        VStack(spacing: 0) {
            ZStack {
                Circle()
                    .fill(level.color.opacity(0.2))
                    .frame(width: 90, height: 90)
                    .scaleEffect(pulsing ? (imminent ? 1.15 : 1.05) : 0.95)
                Circle()
                    .fill(level.color)
                    .frame(width: 60, height: 60)
                Text(level.icon).font(.system(size: 28))
            }
            .animation(.easeInOut(duration: imminent ? 0.5 : 1.0).repeatForever(autoreverses: true), value: pulsing)
            .padding(.bottom, 16)

            Text(title)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(level.color)
                .multilineTextAlignment(.center)
            Text(subtitle)
                .font(.system(size: 13))
                .foregroundStyle(Color.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.top, 6)
            if level != .none {
                Text("BPM Z-Score: \(String(format: "%.2f", bpmZScore)) σ sobre reposo")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(level.color)
                    .padding(.top, 10)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .calmaCard(border: level.color.opacity(0.5), radius: 16)
        .animation(.easeInOut, value: level)
        .onAppear { pulsing = true }
    }
}

private struct MetricCard: View {
    let title: String
    let value: String
    let unit: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(icon).font(.system(size: 16))
                Spacer()
                Text(unit).font(.system(size: 10)).foregroundStyle(Color.textSecondary)
            }
            .padding(.bottom, 8)
            Text(value)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(color)
            Text(title)
                .font(.system(size: 11))
                .foregroundStyle(Color.textSecondary)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .calmaCard()
    }
}

private struct AlertLogCard: View {
    let alertLog: [AlertEvent]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("📋 Registro de Alertas Disparadas (\(alertLog.count))")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.textPrimary)
            if alertLog.isEmpty {
                Text("No se han disparado alertas en esta sesión.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.textSecondary)
            } else {
                ForEach(alertLog.prefix(5)) { alert in
                    let imminent = alert.level == .level2Imminent
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(imminent ? "Ansiedad inminente" : "Estrés leve")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(imminent ? Color.statusImminent : .statusMild)
                            Text("Escenario: \(alert.scenarioName)")
                                .font(.system(size: 10))
                                .foregroundStyle(Color.textSecondary)
                        }
                        Spacer()
                        Text("\(Int(alert.bpm)) BPM @ \(Self.time(alert.timestampMs))")
                            .font(.system(size: 11))
                            .foregroundStyle(Color.textPrimary)
                    }
                    .padding(8)
                    .background(Color.calmaBackground, in: RoundedRectangle(cornerRadius: 6))
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .calmaCard()
    }

    private static func time(_ ms: Int64) -> String {
        Date(timeIntervalSince1970: Double(ms) / 1000).formatted(date: .omitted, time: .standard)
    }
}

extension View {
    /// Fondo y borde de tarjeta como en la versión Android.
    func calmaCard(border: Color = .calmaBorder, radius: CGFloat = 12) -> some View {
        background(Color.calmaCard, in: RoundedRectangle(cornerRadius: radius))
            .overlay(RoundedRectangle(cornerRadius: radius).stroke(border, lineWidth: 1))
    }
}
