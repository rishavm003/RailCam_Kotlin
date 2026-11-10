import Foundation
import UIKit

/// Saved photos and their records.
///
/// JPEGs go to Documents/RailCam, which the Files app shows under
/// "On My iPhone > RailCam" (UIFileSharingEnabled), so filenames are kept when exporting.
/// Records are kept in Application Support/photos.json, newest first.
@MainActor
final class PhotoStore: ObservableObject {
    @Published private(set) var photos: [PhotoRecord] = []

    let photosDirectory: URL
    private let recordsURL: URL
    private let ioQueue = DispatchQueue(label: "railcam.photostore.io", qos: .utility)

    init(fileManager: FileManager = .default) {
        let docs = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        photosDirectory = docs.appendingPathComponent("RailCam", isDirectory: true)
        let support = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        recordsURL = support.appendingPathComponent("photos.json")

        try? fileManager.createDirectory(at: photosDirectory, withIntermediateDirectories: true)
        try? fileManager.createDirectory(at: support, withIntermediateDirectories: true)

        if let data = try? Data(contentsOf: recordsURL),
           let decoded = try? JSONDecoder().decode([PhotoRecord].self, from: data) {
            // Drop records whose file was deleted from the Files app.
            photos = decoded
                .filter { fileManager.fileExists(atPath: photosDirectory.appendingPathComponent($0.filename).path) }
                .sorted { $0.timestamp > $1.timestamp }
        }
    }

    func fileURL(for photo: PhotoRecord) -> URL {
        photosDirectory.appendingPathComponent(photo.filename)
    }

    func photo(withId id: UUID) -> PhotoRecord? {
        photos.first { $0.id == id }
    }

    /// Writes the JPEG off the main thread, then adds the record.
    func save(
        jpeg: Data,
        metadata: MetadataValues,
        isAutoMode: Bool,
        tiltAngle: Double,
        takenAt: Date
    ) async throws -> PhotoRecord {
        let baseName = isAutoMode
            ? FilenameGenerator.generateMobileFilename(takenAt)
            : FilenameGenerator.generate(metadata)
        // Several photos in one session share the same structured name, so a
        // millisecond suffix keeps each file unique (same rule as Android).
        let millis = Int64(takenAt.timeIntervalSince1970 * 1000)
        let filename = String(baseName.dropLast(4)) + "_\(millis).jpg"
        let url = photosDirectory.appendingPathComponent(filename)

        try await runOnIOQueue { try jpeg.write(to: url, options: .atomic) }

        let record = PhotoRecord(
            filename: filename,
            client: metadata.client.rawValue,
            zoneOrSubClient: metadata.zoneOrSubClient,
            testSiteName: metadata.testSiteName,
            stationCode: metadata.stationCode,
            lineDetails: metadata.lineDetails,
            kmMarker: metadata.kmMarker,
            oheNumber: metadata.oheNumber,
            railType: metadata.railType.rawValue,
            railSide: metadata.railType == .TANGENT ? metadata.railSide.rawValue : "",
            direction: metadata.direction.rawValue,
            railPosition: metadata.railType == .CURVE ? metadata.railPosition.rawValue : "",
            sessionDate: metadata.sessionDate,
            tiltAngle: tiltAngle,
            timestamp: takenAt
        )
        photos.insert(record, at: 0)
        persist()
        return record
    }

    func delete(ids: Set<UUID>) {
        let removed = photos.filter { ids.contains($0.id) }
        photos.removeAll { ids.contains($0.id) }
        let urls = removed.map(fileURL(for:))
        ioQueue.async {
            for url in urls { try? FileManager.default.removeItem(at: url) }
        }
        persist()
    }

    private func persist() {
        let snapshot = photos
        let url = recordsURL
        ioQueue.async {
            if let data = try? JSONEncoder().encode(snapshot) {
                try? data.write(to: url, options: .atomic)
            }
        }
    }

    private func runOnIOQueue(_ work: @escaping () throws -> Void) async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            ioQueue.async {
                do {
                    try work()
                    cont.resume()
                } catch {
                    cont.resume(throwing: error)
                }
            }
        }
    }
}
