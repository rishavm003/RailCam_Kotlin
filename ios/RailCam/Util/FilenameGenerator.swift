import Foundation

/// Port of the Android `FilenameGenerator`; both apps must produce identical names.
enum FilenameGenerator {
    static let fallback = "M_1"

    static func generateBaseFilename(_ m: MetadataValues) -> String {
        let site = sanitizeToken(m.testSiteName)
        let station = sanitizeToken(m.stationCode)
        let direction = m.direction.rawValue
        let ohe = sanitizeToken(m.oheNumber)
        let km = sanitizeToken(m.kmMarker) // Curve or Tangent name
        let railPosOrSide = sanitizeToken(
            m.railType == .CURVE ? m.railPosition.displayName : m.railSide.rawValue
        )

        let parts: [String]
        switch m.client {
        case .INDIAN_RAILWAY:
            let zone = sanitizeToken(m.zoneOrSubClient)
            parts = [site, m.client.fileCode, zone, station, direction, ohe, km, railPosOrSide]
        case .DFCCIL:
            let corridor = sanitizeToken(m.zoneOrSubClient)
            parts = [site, m.client.fileCode, corridor, station, direction, km, railPosOrSide, ohe]
        case .METRO:
            let network = sanitizeToken(m.zoneOrSubClient)
            let line = sanitizeToken(m.lineDetails)
            parts = [site, network, line, station, direction, km, railPosOrSide, ohe]
        }
        return parts.joined(separator: "_")
    }

    static func generate(_ m: MetadataValues) -> String {
        let trimmedDate = m.sessionDate.trimmingCharacters(in: .whitespaces)
        let date = trimmedDate.isEmpty ? fallback : m.sessionDate.replacingOccurrences(of: "-", with: "")
        return "\(generateBaseFilename(m))_\(date).jpg"
    }

    private static let mobileFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyyMMdd_HHmmss_SSS"
        return f
    }()

    /// Name used in Auto mode, e.g. IMG_20261110_143015_123.jpg
    static func generateMobileFilename(_ date: Date = Date()) -> String {
        "IMG_\(mobileFormatter.string(from: date)).jpg"
    }

    /// Returns an error message, or nil when the metadata is valid.
    static func validate(_ m: MetadataValues) -> String? {
        let date = m.sessionDate.trimmingCharacters(in: .whitespaces)
        if !date.isEmpty && date.range(of: #"^\d{2}-\d{2}-\d{4}$"#, options: .regularExpression) == nil {
            return "Session date must be DD-MM-YYYY"
        }
        return nil
    }

    /// Uppercases, replaces anything outside A-Z / 0-9 with "_", trims "_" from the ends.
    static func sanitizeToken(_ value: String) -> String {
        let upper = value.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        var out = ""
        for scalar in upper.unicodeScalars {
            let isAZ = scalar.value >= 65 && scalar.value <= 90
            let is09 = scalar.value >= 48 && scalar.value <= 57
            out.unicodeScalars.append((isAZ || is09) ? scalar : "_")
        }
        let trimmed = out.trimmingCharacters(in: CharacterSet(charactersIn: "_"))
        return trimmed.isEmpty ? fallback : trimmed
    }
}
