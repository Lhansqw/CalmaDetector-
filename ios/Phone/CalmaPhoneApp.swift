import SwiftUI

@main
struct CalmaPhoneApp: App {
    @StateObject private var viewModel = CalmaViewModel()

    var body: some Scene {
        WindowGroup {
            DashboardView(viewModel: viewModel)
                .preferredColorScheme(.dark)
        }
    }
}
