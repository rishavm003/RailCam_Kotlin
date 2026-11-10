import Foundation
import SwiftUI

/// Form values, theme and capture mode, persisted in UserDefaults.
/// Equivalent of the Android `MetadataPreferences` DataStore.
@MainActor
final class SettingsStore: ObservableObject {
    private enum Keys {
        static let metadata = "metadata_v1"
        static let darkTheme = "dark_theme"
        static let autoMode = "is_instant_mode"
    }

    private let defaults: UserDefaults

    @Published var metadata: MetadataValues {
        didSet { saveMetadata() }
    }

    @Published var isDarkTheme: Bool {
        didSet { defaults.set(isDarkTheme, forKey: Keys.darkTheme) }
    }

    /// Auto mode saves each photo straight away; Manual mode shows Keep / Retake first.
    @Published var isAutoMode: Bool {
        didSet { defaults.set(isAutoMode, forKey: Keys.autoMode) }
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        var loaded = MetadataValues()
        if let data = defaults.data(forKey: Keys.metadata),
           let decoded = try? JSONDecoder().decode(MetadataValues.self, from: data) {
            loaded = decoded
        }
        if loaded.sessionDate.trimmingCharacters(in: .whitespaces).isEmpty {
            loaded.sessionDate = todaySessionDate()
        }
        metadata = loaded
        isDarkTheme = defaults.object(forKey: Keys.darkTheme) as? Bool ?? true
        isAutoMode = defaults.bool(forKey: Keys.autoMode)
    }

    func setClient(_ client: Client) {
        guard client != metadata.client else { return }
        metadata.client = client
        metadata.zoneOrSubClient = defaultZoneOrSubClient(client)
    }

    private func saveMetadata() {
        if let data = try? JSONEncoder().encode(metadata) {
            defaults.set(data, forKey: Keys.metadata)
        }
    }
}
