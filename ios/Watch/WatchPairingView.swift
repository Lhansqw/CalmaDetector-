import SwiftUI

/// Pantalla de inicio de sesión del reloj: muestra el código para confirmar desde el iPhone.
struct WatchPairingView: View {
    let code: String
    let secondsLeft: Int
    let totalSeconds: Int
    let notice: String?
    let onNewCode: () -> Void

    private var progress: Double {
        totalSeconds > 0 ? Double(secondsLeft) / Double(totalSeconds) : 0
    }

    private var timeLabel: String {
        String(format: "%d:%02d", secondsLeft / 60, secondsLeft % 60)
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                Text("⌚ Vincular reloj")
                    .font(.footnote)
                    .foregroundStyle(Color.textSecondary)

                Text(code)
                    .font(.system(size: 30, weight: .bold, design: .monospaced))
                    .foregroundStyle(Color.cyanPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                    .accessibilityLabel("Código de vinculación \(code.filter { $0 != " " }.map(String.init).joined(separator: " "))")

                Text("Escríbelo en CalmaDetector de tu iPhone")
                    .font(.caption2)
                    .foregroundStyle(Color.textPrimary)
                    .multilineTextAlignment(.center)

                VStack(spacing: 4) {
                    ProgressView(value: progress)
                        .tint(secondsLeft <= 30 ? .statusMild : .cyanPrimary)
                        .padding(.horizontal, 16)
                    Text("Caduca en \(timeLabel)")
                        .font(.system(size: 11))
                        .foregroundStyle(Color.textSecondary)
                }

                if let notice {
                    Text(notice)
                        .font(.caption2)
                        .foregroundStyle(Color.statusMild)
                        .multilineTextAlignment(.center)
                }

                Button("Nuevo código", action: onNewCode)
                    .controlSize(.small)
                    .padding(.top, 4)
            }
            .frame(maxWidth: .infinity)
        }
        .background(Color.calmaBackground.ignoresSafeArea())
    }
}

#Preview {
    WatchPairingView(code: "482 913", secondsLeft: 272, totalSeconds: 300, notice: "Código incorrecto (quedan 4)", onNewCode: {})
}
