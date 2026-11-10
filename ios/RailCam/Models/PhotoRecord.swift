import Foundation

/// One saved photo. Same fields as the Android Room entity `PhotoRecord`.
/// `filename` is the JPEG's name inside the app's Documents/RailCam folder.
struct PhotoRecord: Codable, Identifiable, Equatable, Hashable {
    var id: UUID = UUID()
    var filename: String
    var client: String
    var zoneOrSubClient: String
    var testSiteName: String
    var stationCode: String
    var lineDetails: String
    var kmMarker: String
    var oheNumber: String
    var railType: String
    var railSide: String
    var direction: String
    var railPosition: String
    var sessionDate: String
    var tiltAngle: Double
    var timestamp: Date
}
