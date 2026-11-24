import SwiftUI
import WatchKit
import CalmaCore

@main
struct CalmaWatchApp: App {
    var body: some Scene {
        WindowGroup {
            WatchRootView()
        }
    }
}

/// Igual que `WearActivity.kt`: abre directo el panel.
@MainActor
struct WatchRootView: View {
    @StateObject private var viewModel = CalmaViewModel(sensorProvider: HealthSensorProvider())

    var body: some View {
        WatchDashboardView(viewModel: viewModel)
            .onAppear {
                viewModel.onAlert = { level in
                    WKInterfaceDevice.current().play(level == .level2Imminent ? .notification : .directionUp)
                }
            }
    }
}
