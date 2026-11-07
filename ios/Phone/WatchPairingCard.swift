import SwiftUI
import CalmaCore

/// Tarjeta del iPhone para confirmar el código que muestra el reloj.
struct WatchPairingCard: View {
    @ObservedObject var pairing: PhonePairingViewModel
    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("⌚ Reloj vinculado")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color.textPrimary)

            if let paired = pairing.paired {
                Text("\(paired.name) · desde \(Self.dateText(paired.pairedAtMs))")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.statusCalm)
                Button(role: .destructive, action: pairing.unpair) {
                    Text("Desvincular reloj")
                        .font(.system(size: 12))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(.statusImminent)
            } else {
                Text("Abre CalmaDetector en el Apple Watch y escribe aquí el código de 6 dígitos que aparece.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.textSecondary)

                HStack(spacing: 8) {
                    TextField("000000", text: Binding(get: { pairing.codeInput }, set: pairing.onCodeChange))
                        .keyboardType(.numberPad)
                        .textContentType(.oneTimeCode)
                        .font(.system(size: 20, design: .monospaced))
                        .kerning(4)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(Color.textPrimary)
                        .padding(.vertical, 10)
                        .background(Color.calmaBackground, in: RoundedRectangle(cornerRadius: 8))
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(focused ? Color.cyanPrimary : Color.calmaBorder, lineWidth: 1)
                        )
                        .focused($focused)
                        .disabled(pairing.isSending)

                    Button {
                        focused = false
                        pairing.submitCode()
                    } label: {
                        if pairing.isSending {
                            ProgressView().tint(Color.calmaBackground)
                        } else {
                            Text("Vincular")
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.cyanPrimary)
                    .foregroundStyle(Color.calmaBackground)
                    .disabled(pairing.isSending || pairing.codeInput.count != PhonePairingViewModel.codeLength)
                }
            }

            if let message = pairing.message {
                Text(message)
                    .font(.system(size: 12))
                    .foregroundStyle(pairing.isError ? Color.statusImminent : Color.statusCalm)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .calmaCard(border: pairing.paired != nil ? Color.statusCalm.opacity(0.5) : .calmaBorder)
    }

    private static func dateText(_ ms: Int64) -> String {
        Date(timeIntervalSince1970: Double(ms) / 1000).formatted(date: .abbreviated, time: .omitted)
    }
}
