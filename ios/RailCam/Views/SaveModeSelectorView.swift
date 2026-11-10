import SwiftUI

/// Manual / Auto choice, matching the Android SaveModeSelector.
struct SaveModeSelectorView: View {
    @Binding var isAutoMode: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("After taking a photo")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Palette.textSecondary)
                .padding(.leading, 4)

            HStack(spacing: 4) {
                option(label: "Manual", icon: "checklist", selected: !isAutoMode) { isAutoMode = false }
                option(label: "Auto", icon: "bolt.fill", selected: isAutoMode) { isAutoMode = true }
            }
            .padding(4)
            .background(RoundedRectangle(cornerRadius: 12).fill(Palette.surfaceHigh))
            .animation(.easeOut(duration: 0.15), value: isAutoMode)

            Text(isAutoMode
                 ? "Every photo is saved automatically as soon as you take it."
                 : "You see each photo first, then choose Keep or Retake.")
                .font(.system(size: 11))
                .foregroundColor(Palette.textSecondary)
                .padding(.horizontal, 4)
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 16).fill(Palette.surface))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Palette.border, lineWidth: 1))
    }

    private func option(label: String, icon: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: icon).font(.system(size: 15))
                Text(label).font(.system(size: 14, weight: selected ? .bold : .medium))
            }
            .frame(maxWidth: .infinity, minHeight: 40)
            .foregroundColor(selected ? .black : Palette.textSecondary)
            .background(RoundedRectangle(cornerRadius: 10).fill(selected ? Palette.amber : Color.clear))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}
