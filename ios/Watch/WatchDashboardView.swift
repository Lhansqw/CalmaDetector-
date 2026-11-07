import SwiftUI
import CalmaCore

/// Port de `CalmaWearApp.kt` para Apple Watch.
struct WatchDashboardView: View {
    @ObservedObject var viewModel: CalmaViewModel
    var pairedPhoneName: String?
    var onUnpair: () -> Void = {}

    var body: some View {
        NavigationStack {
            List {
                statusCard
                metricsCard
                baselineCard

                if viewModel.supportsSensors {
                    Section("Fuente de datos") {
                        Picker("Fuente", selection: Binding(get: { viewModel.dataSource }, set: viewModel.selectDataSource)) {
                            ForEach(DataSource.allCases) { Text($0.displayName).tag($0) }
                        }
                        if let message = viewModel.sensorMessage {
                            Text(message)
                                .font(.caption2)
                                .foregroundStyle(Color.statusMild)
                        }
                        if viewModel.dataSource == .sensors && !viewModel.isRunning {
                            Button("Reintentar", action: viewModel.start)
                        }
                    }
                }

                if viewModel.dataSource == .simulator {
                    Section("Simular escenario") {
                        ForEach(SimulationScenario.allCases) { scenario in
                            Button {
                                viewModel.selectScenario(scenario)
                            } label: {
                                Text(scenario.displayName)
                                    .font(.footnote)
                                    .lineLimit(1)
                            }
                            .listRowBackground(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(scenario == viewModel.currentScenario ? Color.cyanPrimary.opacity(0.6) : Color.calmaCard)
                            )
                        }
                    }
                }

                Section("Calibración") {
                    Button("Sí, sentí ansiedad") { viewModel.sendFeedback(wasAnxiety: true) }
                        .listRowBackground(RoundedRectangle(cornerRadius: 10).fill(Color.statusMild.opacity(0.7)))
                    Button("Fue falsa alarma") { viewModel.sendFeedback(wasAnxiety: false) }
                    Button("Restablecer estado", role: .destructive) { viewModel.resetDetector() }
                        .font(.footnote)
                }

                if let pairedPhoneName {
                    Section("Sesión") {
                        Text("📱 Vinculado con \(pairedPhoneName)")
                            .font(.caption2)
                            .foregroundStyle(Color.textPrimary)
                        Button("Desvincular iPhone", role: .destructive, action: onUnpair)
                            .font(.footnote)
                    }
                }
            }
            .navigationTitle("CalmaDetector")
        }
    }

    // MARK: - Tarjetas

    private var statusCard: some View {
        let level = viewModel.currentAlert
        return VStack(spacing: 2) {
            Text(level.icon).font(.system(size: 28))
            Text(level.shortTitle)
                .font(.headline)
                .foregroundStyle(level.color)
                .multilineTextAlignment(.center)
            if level == .level2Imminent {
                Text("🧘 Inicia respiración 4-7-8")
                    .font(.caption2)
                    .foregroundStyle(Color.textPrimary)
                    .padding(.top, 2)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)
        .listRowBackground(RoundedRectangle(cornerRadius: 12).fill(level.color.opacity(0.2)))
        .animation(.easeInOut, value: level)
    }

    private var metricsCard: some View {
        let s = viewModel.latestSample
        let active = s.motion > 1.0
        return HStack {
            metric(value: "\(Int(s.bpm))", label: "BPM")
            Spacer()
            metric(value: s.hrvMs.map { "\(Int($0))" } ?? "--", label: "HRV ms")
            Spacer()
            metric(value: active ? "🏃" : "🧘", label: active ? "Activo" : "Quieto")
        }
        .padding(.horizontal, 4)
    }

    private func metric(value: String, label: String) -> some View {
        VStack(spacing: 0) {
            Text(value)
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(Color.textPrimary)
                .lineLimit(1)
            Text(label)
                .font(.system(size: 10))
                .foregroundStyle(Color.textSecondary)
                .lineLimit(1)
        }
    }

    private var baselineCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("Línea base")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.textSecondary)
                Spacer()
                Text(viewModel.baselineReady ? "\(Int(viewModel.baselineMeanBpm)) BPM" : "\(Int(viewModel.baselineProgress * 100))%")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(viewModel.baselineReady ? Color.statusCalm : .statusMild)
            }
            ProgressView(value: viewModel.baselineProgress)
                .tint(viewModel.baselineReady ? .statusCalm : .cyanPrimary)
        }
    }
}

#Preview {
    WatchDashboardView(viewModel: CalmaViewModel(), pairedPhoneName: "iPhone")
}
