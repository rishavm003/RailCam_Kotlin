import Foundation

// Mirrors app/src/main/java/.../domain/model/MetadataValues.kt so both apps produce the same
// filenames and records. Raw values match the Kotlin enum names (used for storage).

enum Client: String, CaseIterable, Codable, Identifiable {
    case INDIAN_RAILWAY, DFCCIL, METRO
    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .INDIAN_RAILWAY: return "IR"
        case .DFCCIL: return "DFCCIL"
        case .METRO: return "Metro"
        }
    }

    var fileCode: String {
        switch self {
        case .INDIAN_RAILWAY: return "IR"
        case .DFCCIL: return "DFCCIL"
        case .METRO: return "METRO"
        }
    }
}

enum DfccilCorridor: String, CaseIterable {
    case EDFC, WDFC
}

enum MetroNetwork: String, CaseIterable {
    case DMRC, BMRC, MMRCL, DHAKA_METRO

    var displayName: String {
        switch self {
        case .DHAKA_METRO: return "Dhaka Metro"
        default: return rawValue
        }
    }

    var fileCode: String {
        switch self {
        case .DHAKA_METRO: return "DHAKA"
        default: return rawValue
        }
    }
}

enum RailType: String, CaseIterable, Codable, Identifiable {
    case TANGENT, CURVE
    var id: String { rawValue }

    var displayName: String { self == .TANGENT ? "Tangent Rail" : "Curve Rail" }
    var fileCode: String { self == .TANGENT ? "TAN" : "CUR" }
}

enum RailSide: String, CaseIterable, Codable, Identifiable {
    case LEFT, RIGHT
    var id: String { rawValue }
}

enum Direction: String, CaseIterable, Codable, Identifiable {
    case UP, DN
    var id: String { rawValue }
}

enum RailPosition: String, CaseIterable, Codable, Identifiable {
    case HIGH, LOW
    var id: String { rawValue }

    var displayName: String { self == .HIGH ? "High Rail" : "Low Rail" }
    var fileCode: String { rawValue }
}

enum RailwayZones {
    static let all = [
        "NR", "NWR", "NER", "NFR", "ER", "ECR", "ECoR",
        "SER", "SCR", "CR", "WR", "WCR", "NCR", "SECR", "SR", "SWR"
    ]
}

struct MetadataValues: Codable, Equatable {
    var client: Client = .INDIAN_RAILWAY
    var zoneOrSubClient: String = RailwayZones.all[0]
    var testSiteName: String = ""
    var stationCode: String = ""
    var lineDetails: String = ""
    var kmMarker: String = ""
    var oheNumber: String = ""
    var railType: RailType = .TANGENT
    var railSide: RailSide = .LEFT
    var direction: Direction = .UP
    var railPosition: RailPosition = .HIGH
    var sessionDate: String = todaySessionDate()
}

/// Session dates use the same "dd-MM-yyyy" format as the Android app.
let sessionDateFormatter: DateFormatter = {
    let f = DateFormatter()
    f.locale = Locale(identifier: "en_US_POSIX")
    f.dateFormat = "dd-MM-yyyy"
    return f
}()

func todaySessionDate() -> String {
    sessionDateFormatter.string(from: Date())
}

func defaultZoneOrSubClient(_ client: Client) -> String {
    switch client {
    case .INDIAN_RAILWAY: return RailwayZones.all[0]
    case .DFCCIL: return DfccilCorridor.EDFC.rawValue
    case .METRO: return MetroNetwork.DMRC.fileCode
    }
}

func subClientOptions(_ client: Client) -> [String] {
    switch client {
    case .INDIAN_RAILWAY: return RailwayZones.all
    case .DFCCIL: return DfccilCorridor.allCases.map(\.rawValue)
    case .METRO: return MetroNetwork.allCases.map(\.fileCode)
    }
}

/// Label shown in the picker for a sub-client code (e.g. "DHAKA" -> "Dhaka Metro").
func subClientLabel(_ client: Client, code: String) -> String {
    if client == .METRO, let net = MetroNetwork.allCases.first(where: { $0.fileCode == code }) {
        return net.displayName
    }
    return code
}
