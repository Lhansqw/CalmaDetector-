import SwiftUI

@main
struct CalmaPhoneApp: App {
    @StateObject private var viewModel = CalmaViewModel()
    @StateObject private var pairing = PhonePairingViewModel()

    var body: some Scene {
        WindowGroup {
            DashboardView(viewModel: viewModel, pairing: pairing)
                .preferredColorScheme(.dark)
        }
    }
}
