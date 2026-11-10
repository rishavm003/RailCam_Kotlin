import SwiftUI

struct FieldLabel: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.system(size: 13, weight: .semibold))
            .foregroundColor(Palette.textSecondary)
    }
}

/// Row of equal-width options with the selected one filled in amber.
struct SegmentedPicker<Option: Hashable>: View {
    let label: String
    let options: [Option]
    let selected: Option
    let title: (Option) -> String
    let onSelect: (Option) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            FieldLabel(label)
            HStack(spacing: 4) {
                ForEach(options, id: \.self) { option in
                    let isSelected = option == selected
                    Button {
                        onSelect(option)
                    } label: {
                        Text(title(option))
                            .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                            .frame(maxWidth: .infinity, minHeight: 40)
                            .foregroundColor(isSelected ? .black : Palette.textSecondary)
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(isSelected ? Palette.amber : Color.clear)
                            )
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
            }
            .padding(4)
            .background(RoundedRectangle(cornerRadius: 12).fill(Palette.surfaceHigh))
            .animation(.easeOut(duration: 0.15), value: selected)
        }
    }
}

/// Labelled drop-down built on Menu.
struct DropdownPicker: View {
    let label: String
    let options: [String]
    let selected: String
    let title: (String) -> String
    let onSelect: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            FieldLabel(label)
            Menu {
                ForEach(options, id: \.self) { option in
                    Button {
                        onSelect(option)
                    } label: {
                        if option == selected {
                            Label(title(option), systemImage: "checkmark")
                        } else {
                            Text(title(option))
                        }
                    }
                }
            } label: {
                HStack {
                    Text(title(selected))
                        .foregroundColor(Palette.textPrimary)
                    Spacer()
                    Image(systemName: "chevron.down")
                        .foregroundColor(Palette.textSecondary)
                }
                .padding(.horizontal, 14)
                .frame(height: 48)
                .background(RoundedRectangle(cornerRadius: 12).fill(Palette.surfaceHigh))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Palette.border, lineWidth: 1))
            }
        }
    }
}

struct LabeledTextField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var capitalization: TextInputAutocapitalization = .characters

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            FieldLabel(label)
            TextField(placeholder, text: $text)
                .textInputAutocapitalization(capitalization)
                .autocorrectionDisabled()
                .foregroundColor(Palette.textPrimary)
                .padding(.horizontal, 14)
                .frame(height: 48)
                .background(RoundedRectangle(cornerRadius: 12).fill(Palette.surfaceHigh))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Palette.border, lineWidth: 1))
        }
    }
}
