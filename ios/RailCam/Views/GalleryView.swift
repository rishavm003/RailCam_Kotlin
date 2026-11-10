import ImageIO
import SwiftUI
import UIKit

struct GalleryView: View {
    @Binding var path: [Route]
    @EnvironmentObject private var photoStore: PhotoStore

    @State private var selected: Set<UUID> = []
    @State private var showDeleteConfirm = false
    @State private var shareItems: ShareItems?

    private var isSelecting: Bool { !selected.isEmpty }
    private let columns = [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)]

    var body: some View {
        Group {
            if photoStore.photos.isEmpty {
                Text("No photos taken yet.")
                    .foregroundColor(Palette.textSecondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 8) {
                        ForEach(photoStore.photos) { photo in
                            PhotoThumbView(
                                url: photoStore.fileURL(for: photo),
                                photo: photo,
                                isSelected: selected.contains(photo.id),
                                isSelecting: isSelecting
                            )
                            .onTapGesture {
                                if isSelecting {
                                    toggle(photo.id)
                                } else {
                                    path.append(.preview(photo.id))
                                }
                            }
                            .onLongPressGesture { toggle(photo.id) }
                        }
                    }
                    .padding(16)
                }
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle(isSelecting ? "\(selected.count) Selected" : "Gallery (\(photoStore.photos.count) Photos)")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(isSelecting)
        .toolbar {
            if isSelecting {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button { selected.removeAll() } label: { Image(systemName: "xmark") }
                        .accessibilityLabel("Cancel selection")
                }
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button(selected.count == photoStore.photos.count ? "Deselect All" : "Select All") {
                        if selected.count == photoStore.photos.count {
                            selected.removeAll()
                        } else {
                            selected = Set(photoStore.photos.map(\.id))
                        }
                    }
                    Button { showDeleteConfirm = true } label: { Image(systemName: "trash") }
                        .tint(Palette.alertRed)
                        .accessibilityLabel("Delete")
                    Button {
                        let urls = photoStore.photos.filter { selected.contains($0.id) }.map(photoStore.fileURL(for:))
                        shareItems = ShareItems(urls: urls)
                    } label: { Image(systemName: "square.and.arrow.up") }
                        .accessibilityLabel("Share")
                }
            }
        }
        .alert(
            selected.count == 1 ? "Delete 1 photo?" : "Delete \(selected.count) photos?",
            isPresented: $showDeleteConfirm
        ) {
            Button("Delete", role: .destructive) {
                photoStore.delete(ids: selected)
                selected.removeAll()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("The selected photos will be permanently removed from your device. This cannot be undone.")
        }
        .sheet(item: $shareItems) { items in
            ShareSheet(items: items.urls)
        }
    }

    private func toggle(_ id: UUID) {
        if selected.contains(id) { selected.remove(id) } else { selected.insert(id) }
    }
}

struct ShareItems: Identifiable {
    let id = UUID()
    let urls: [URL]
}

/// Shares the JPEG files themselves, so the receiving app keeps the filename.
struct ShareSheet: UIViewControllerRepresentable {
    let items: [URL]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

private struct PhotoThumbView: View {
    let url: URL
    let photo: PhotoRecord
    let isSelected: Bool
    let isSelecting: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ZStack(alignment: .topTrailing) {
                ThumbnailImage(url: url)
                    .aspectRatio(2, contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(isSelected ? Palette.amber : Color.clear, lineWidth: 3)
                    )
                if isSelecting {
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 20))
                        .foregroundColor(isSelected ? Palette.amber : .white)
                        .padding(6)
                }
            }
            Text(photo.filename)
                .font(.system(size: 10))
                .foregroundColor(Palette.textSecondary)
                .lineLimit(1)
                .truncationMode(.middle)
        }
        .contentShape(Rectangle())
    }
}

/// Downsampled thumbnail loaded off the main thread and cached in memory.
struct ThumbnailImage: View {
    let url: URL
    var maxPixel: CGFloat = 600
    @State private var image: UIImage?

    var body: some View {
        ZStack {
            Palette.surfaceHigh
            if let image {
                Image(uiImage: image).resizable().scaledToFill()
            }
        }
        .task(id: url) {
            image = await ThumbnailCache.shared.thumbnail(for: url, maxPixel: maxPixel)
        }
    }
}

final class ThumbnailCache {
    static let shared = ThumbnailCache()
    private let cache = NSCache<NSString, UIImage>()

    func thumbnail(for url: URL, maxPixel: CGFloat) async -> UIImage? {
        let key = "\(url.path)#\(Int(maxPixel))" as NSString
        if let cached = cache.object(forKey: key) { return cached }
        let image = await Task.detached(priority: .userInitiated) { () -> UIImage? in
            let options: [CFString: Any] = [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: maxPixel
            ]
            guard let source = CGImageSourceCreateWithURL(url as CFURL, nil),
                  let cg = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else { return nil }
            return UIImage(cgImage: cg)
        }.value
        if let image { cache.setObject(image, forKey: key) }
        return image
    }
}
