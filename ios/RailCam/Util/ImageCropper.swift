import CoreGraphics
import UIKit

/// Crops a captured photo to the 2:1 guide box shown on screen (centred, 80% of the width),
/// the same geometry as the Android `CropHelper`.
enum ImageCropper {
    /// The crop rectangle for an upright image of the given size.
    static func cropRect(width: CGFloat, height: CGFloat) -> CGRect {
        let cropW = (width * 0.8).rounded(.down)
        let cropH = min((cropW / 2).rounded(.down), height)
        return CGRect(
            x: ((width - cropW) / 2).rounded(.down),
            y: ((height - cropH) / 2).rounded(.down),
            width: cropW,
            height: cropH
        )
    }

    /// Returns the cropped, upright image and its JPEG data. Safe to call off the main thread.
    static func crop(jpeg: Data, quality: CGFloat = 0.9) -> (image: UIImage, jpeg: Data)? {
        guard let source = UIImage(data: jpeg) else { return nil }
        // `size` already accounts for the EXIF orientation, so the maths is in upright space.
        let rect = cropRect(width: source.size.width * source.scale, height: source.size.height * source.scale)

        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: rect.size, format: format)
        let cropped = renderer.image { _ in
            source.draw(in: CGRect(
                x: -rect.minX,
                y: -rect.minY,
                width: source.size.width * source.scale,
                height: source.size.height * source.scale
            ))
        }
        guard let data = cropped.jpegData(compressionQuality: quality) else { return nil }
        return (cropped, data)
    }
}
