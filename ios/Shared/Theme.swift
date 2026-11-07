import SwiftUI
import CalmaCore

/// Misma paleta que `ui/theme/Color.kt` en Android.
extension Color {
    static let calmaBackground = Color(hex: 0x0F172A)   // Slate 900
    static let calmaCard = Color(hex: 0x1E293B)         // Slate 800
    static let calmaBorder = Color(hex: 0x334155)       // Slate 700

    static let statusCalm = Color(hex: 0x10B981)        // Emerald 500
    static let statusMild = Color(hex: 0xF59E0B)        // Amber 500
    static let statusImminent = Color(hex: 0xEF4444)    // Red 500

    static let cyanPrimary = Color(hex: 0x06B6D4)       // Cyan 500
    static let cyanSecondary = Color(hex: 0x0891B2)     // Cyan 600

    static let textPrimary = Color(hex: 0xF8FAFC)       // Slate 50
    static let textSecondary = Color(hex: 0x94A3B8)     // Slate 400

    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

extension AlertLevel {
    var color: Color {
        switch self {
        case .none: return .statusCalm
        case .level1Mild: return .statusMild
        case .level2Imminent: return .statusImminent
        }
    }

    var icon: String {
        switch self {
        case .none: return "🧘"
        case .level1Mild: return "⚡"
        case .level2Imminent: return "🚨"
        }
    }

    /// Título corto (reloj).
    var shortTitle: String {
        switch self {
        case .none: return "Estado Normal"
        case .level1Mild: return "Estrés Leve"
        case .level2Imminent: return "Ansiedad Inminente"
        }
    }
}
