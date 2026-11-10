import SwiftUI

/// Session form; same fields, order and conditional sections as the Android MetadataForm.
struct MetadataFormView: View {
    @EnvironmentObject private var settings: SettingsStore
    @State private var showDatePicker = false
    @State private var pickedDate = Date()

    var body: some View {
        let m = settings.metadata

        VStack(alignment: .leading, spacing: 20) {
            SegmentedPicker(
                label: "Client",
                options: Client.allCases,
                selected: m.client,
                title: { $0.displayName },
                onSelect: { settings.setClient($0) }
            )

            DropdownPicker(
                label: zoneLabel(m.client),
                options: subClientOptions(m.client),
                selected: m.zoneOrSubClient,
                title: { subClientLabel(m.client, code: $0) },
                onSelect: { settings.metadata.zoneOrSubClient = $0 }
            )

            if m.client == .METRO {
                LabeledTextField(
                    label: "Line Details",
                    placeholder: "e.g. Red Line or Line 1",
                    text: $settings.metadata.lineDetails,
                    capitalization: .words
                )
                .transition(.opacity)
            }

            LabeledTextField(
                label: "Test Site Name",
                placeholder: "Enter test site name",
                text: $settings.metadata.testSiteName,
                capitalization: .words
            )

            LabeledTextField(
                label: "Station Code",
                placeholder: "Enter station code",
                text: $settings.metadata.stationCode
            )

            SegmentedPicker(
                label: "Direction",
                options: Direction.allCases,
                selected: m.direction,
                title: { $0.rawValue },
                onSelect: { settings.metadata.direction = $0 }
            )

            LabeledTextField(
                label: "OHE Details",
                placeholder: "Enter OHE structure/mast number",
                text: $settings.metadata.oheNumber
            )

            SegmentedPicker(
                label: "Rail Type",
                options: RailType.allCases,
                selected: m.railType,
                title: { $0.displayName },
                onSelect: { settings.metadata.railType = $0 }
            )

            LabeledTextField(
                label: m.railType == .TANGENT ? "Tangent Name" : "Curve Name",
                placeholder: m.railType == .TANGENT ? "e.g. TAN-01" : "e.g. CUR-02",
                text: $settings.metadata.kmMarker
            )

            if m.railType == .TANGENT {
                SegmentedPicker(
                    label: "Rail Side",
                    options: RailSide.allCases,
                    selected: m.railSide,
                    title: { $0.rawValue },
                    onSelect: { settings.metadata.railSide = $0 }
                )
                .transition(.opacity)
            } else {
                SegmentedPicker(
                    label: "Rail Position",
                    options: RailPosition.allCases,
                    selected: m.railPosition,
                    title: { $0.displayName },
                    onSelect: { settings.metadata.railPosition = $0 }
                )
                .transition(.opacity)
            }

            VStack(alignment: .leading, spacing: 8) {
                FieldLabel("Date")
                HStack(spacing: 8) {
                    TextField("DD-MM-YYYY", text: $settings.metadata.sessionDate)
                        .keyboardType(.numbersAndPunctuation)
                        .autocorrectionDisabled()
                        .foregroundColor(Palette.textPrimary)
                    Button {
                        pickedDate = sessionDateFormatter.date(from: m.sessionDate) ?? Date()
                        showDatePicker = true
                    } label: {
                        Image(systemName: "calendar")
                            .font(.system(size: 18))
                            .foregroundColor(Palette.amber)
                    }
                    .accessibilityLabel("Pick date")
                }
                .padding(.horizontal, 14)
                .frame(height: 48)
                .background(RoundedRectangle(cornerRadius: 12).fill(Palette.surfaceHigh))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Palette.border, lineWidth: 1))
            }
        }
        .animation(.easeInOut(duration: 0.2), value: m.client)
        .animation(.easeInOut(duration: 0.2), value: m.railType)
        .sheet(isPresented: $showDatePicker) {
            NavigationStack {
                DatePicker("Session date", selection: $pickedDate, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .padding()
                    .navigationTitle("Session date")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showDatePicker = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("OK") {
                                settings.metadata.sessionDate = sessionDateFormatter.string(from: pickedDate)
                                showDatePicker = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
        }
    }

    private func zoneLabel(_ client: Client) -> String {
        switch client {
        case .INDIAN_RAILWAY: return "Select Zone"
        case .DFCCIL: return "Corridor"
        case .METRO: return "Metro Network"
        }
    }
}
