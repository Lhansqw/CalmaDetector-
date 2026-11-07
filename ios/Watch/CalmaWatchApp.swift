import SwiftUI
import WatchKit
import CalmaCore

@main
struct CalmaWatchApp: App {
    @StateObject private var pairing = WatchPairingViewModel()

    var body: some Scene {
        WindowGroup {
            WatchRootView(pairing: pairing)
        }
    }
}

/// Igual que `WearActivity.kt`: código de vinculación hasta iniciar sesión, luego el panel.
@MainActor
struct WatchRootView: View {
    @ObservedObject var pairing: WatchPairingViewModel

    var body: some View {
        switch pairing.state {
        case .loading:
            ProgressView()
        case let .waiting(code, secondsLeft, totalSeconds, notice):
            WatchPairingView(
                code: code,
                secondsLeft: secondsLeft,
                totalSeconds: totalSeconds,
                notice: notice,
                onNewCode: { pairing.startWaiting(nil) }
            )
        case let .paired(phone):
            // El panel solo se crea tras iniciar sesión.
            WatchPairedContainer(phoneName: phone.name, onUnpair: pairing.unpair)
        }
    }
}

@MainActor
private struct WatchPairedContainer: View {
    let phoneName: String
    let onUnpair: () -> Void
    @StateObject private var viewModel = CalmaViewModel(sensorProvider: HealthSensorProvider())

    var body: some View {
        WatchDashboardView(viewModel: viewModel, pairedPhoneName: phoneName, onUnpair: {
            viewModel.pause()
            onUnpair()
        })
        .onAppear {
            viewModel.onAlert = { level in
                WKInterfaceDevice.current().play(level == .level2Imminent ? .notification : .directionUp)
            }
        }
    }
}
