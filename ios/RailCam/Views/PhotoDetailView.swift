import SwiftUI

struct PhotoDetailView: View {
    let photoId: UUID
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var photoStore: PhotoStore
    @State private var showDeleteConfirm = false
    @State private var shareItems: ShareItems?

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "dd MMM yyyy, HH:mm"
        return f
    }()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let photo = photoStore.photo(withId: photoId) {
                VStack(spacing: 0) {
                    Spacer()
                    ThumbnailImage(url: photoStore.fileURL(for: photo), maxPixel: 2400)
                        .aspectRatio(2, contentMode: .fit)
                    Spacer()
                    details(photo)
                }
                .toolbar {
                    ToolbarItemGroup(placement: .navigationBarTrailing) {
                        Button {
                            shareItems = ShareItems(urls: [photoStore.fileURL(for: photo)])
                        } label: { Image(systemName: "square.and.arrow.up") }
                            .accessibilityLabel("Share")
                        Button { showDeleteConfirm = true } label: { Image(systemName: "trash") }
                            .tint(Palette.alertRed)
                            .accessibilityLabel("Delete")
                    }
                }
                .alert("Delete Photo?", isPresented: $showDeleteConfirm) {
                    Button("Delete", role: .destructive) {
                        photoStore.delete(ids: [photo.id])
                        dismiss()
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("This will permanently remove the photo from your device. This action cannot be undone.")
                }
            } else {
                Text("Photo not found.").foregroundColor(.white)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.black, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .sheet(item: $shareItems) { items in
            ShareSheet(items: items.urls)
        }
    }

    private func details(_ photo: PhotoRecord) -> some View {
        let railLabel = photo.railType == RailType.TANGENT.rawValue ? "Tangent" : "Curve"
        let clientName = Client(rawValue: photo.client)?.displayName ?? photo.client
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("\(clientName) • \(photo.zoneOrSubClient)")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Palette.amber)
                Spacer()
                Text(Self.dateFormatter.string(from: photo.timestamp))
                    .font(.system(size: 12))
                    .foregroundColor(Color(white: 0.8))
            }
            HStack {
                Text("\(railLabel): \(photo.kmMarker)  •  OHE: \(photo.oheNumber)")
                    .font(.system(size: 13, weight: .medium))
                Spacer()
                Text(String(format: "Tilt: %.1f°", photo.tiltAngle))
                    .font(.system(size: 13, weight: .semibold))
            }
            .foregroundColor(.white)
            Text(detailLine(photo))
                .font(.system(size: 12))
                .foregroundColor(.gray)
            Text(photo.filename)
                .font(.system(size: 11))
                .foregroundColor(.gray)
                .textSelection(.enabled)
        }
        .padding(20)
        .background(Color(white: 0.08))
    }

    private func detailLine(_ p: PhotoRecord) -> String {
        var parts: [String] = []
        if !p.testSiteName.isEmpty { parts.append("Site: \(p.testSiteName)") }
        if !p.stationCode.isEmpty { parts.append("Station: \(p.stationCode)") }
        if !p.lineDetails.isEmpty { parts.append("Line: \(p.lineDetails)") }
        parts.append("Dir: \(p.direction)")
        if !p.railSide.isEmpty { parts.append("Side: \(p.railSide)") }
        if let pos = RailPosition(rawValue: p.railPosition) { parts.append(pos.displayName) }
        if !p.sessionDate.isEmpty { parts.append(p.sessionDate) }
        return parts.joined(separator: "  •  ")
    }
}
