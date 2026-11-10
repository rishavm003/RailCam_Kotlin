import SwiftUI

struct HomeView: View {
    @Binding var path: [Route]
    @EnvironmentObject private var settings: SettingsStore
    @State private var showCamera = false

    var body: some View {
        let validationError = FilenameGenerator.validate(settings.metadata)
        let isValid = validationError == nil

        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                header

                // Session form card
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("SESSION CONFIGURATION")
                            .font(.system(size: 11, weight: .bold))
                            .kerning(1)
                            .foregroundColor(Palette.amber)
                        Text("Site & Track Parameters")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Palette.textPrimary)
                    }
                    MetadataFormView()
                }
                .padding(20)
                .background(RoundedRectangle(cornerRadius: 20).fill(Palette.surface))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(Palette.border, lineWidth: 1))

                // Status + filename preview
                VStack(spacing: 8) {
                    StatusChip(message: validationError ?? "System ready • Metadata validated", isValid: isValid)
                    HStack(alignment: .firstTextBaseline) {
                        Text("Target File:")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(Palette.textSecondary)
                        Spacer(minLength: 8)
                        Text(FilenameGenerator.generate(settings.metadata))
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(Palette.amber)
                            .multilineTextAlignment(.trailing)
                            .textSelection(.enabled)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(RoundedRectangle(cornerRadius: 10).fill(Palette.surfaceHigh.opacity(0.5)))
                }

                // Actions
                HStack(spacing: 12) {
                    Button {
                        path.append(.gallery)
                    } label: {
                        Text("Gallery")
                            .font(.system(size: 15, weight: .semibold))
                            .frame(maxWidth: .infinity, minHeight: 54)
                            .foregroundColor(Palette.textPrimary)
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Palette.border, lineWidth: 1))
                    }
                    .buttonStyle(.plain)

                    Button {
                        hideKeyboard()
                        showCamera = true
                    } label: {
                        Text("Start Camera")
                            .font(.system(size: 15, weight: .bold))
                            .frame(maxWidth: .infinity, minHeight: 54)
                            .foregroundColor(isValid ? .black : Palette.textSecondary)
                            .background(
                                RoundedRectangle(cornerRadius: 14)
                                    .fill(isValid ? Palette.amber : Palette.surfaceHigh)
                            )
                    }
                    .buttonStyle(.plain)
                    .disabled(!isValid)
                    .layoutPriority(1)
                }

                SaveModeSelectorView(isAutoMode: $settings.isAutoMode)
            }
            .padding(20)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Palette.background.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .fullScreenCover(isPresented: $showCamera) {
            CaptureView()
        }
    }

    private var header: some View {
        HStack {
            HStack(spacing: 14) {
                Image("Logo")
                    .resizable()
                    .scaledToFill()
                    .frame(width: 48, height: 48)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .accessibilityLabel("RailCam logo")
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text("RailCam")
                            .font(.system(size: 26, weight: .heavy))
                            .kerning(-0.5)
                            .foregroundColor(Palette.textPrimary)
                        Text("PRO")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(Palette.amber)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(RoundedRectangle(cornerRadius: 6).fill(Palette.amber.opacity(0.15)))
                    }
                    Text("Field Inspection & Capture System")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(Palette.textSecondary)
                }
            }
            Spacer()
            Button {
                settings.isDarkTheme.toggle()
            } label: {
                Image(systemName: settings.isDarkTheme ? "sun.max.fill" : "moon.fill")
                    .font(.system(size: 18))
                    .foregroundColor(Palette.amber)
                    .frame(width: 44, height: 44)
                    .background(Circle().fill(Palette.surfaceHigh))
                    .overlay(Circle().stroke(Palette.border, lineWidth: 1))
            }
            .accessibilityLabel("Toggle theme")
        }
    }

    private func hideKeyboard() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

private struct StatusChip: View {
    let message: String
    let isValid: Bool

    var body: some View {
        let color = isValid ? Palette.levelGreen : Palette.alertRed
        HStack(spacing: 12) {
            Circle().fill(color).frame(width: 10, height: 10)
            Text(message)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(color)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 12).fill(color.opacity(0.12)))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(color.opacity(0.5), lineWidth: 1))
    }
}
