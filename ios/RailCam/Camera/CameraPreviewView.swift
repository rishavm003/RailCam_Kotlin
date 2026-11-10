import AVFoundation
import SwiftUI
import UIKit

/// Full-screen camera preview. Reports taps in both view and camera-device coordinates.
struct CameraPreviewView: UIViewRepresentable {
    let session: AVCaptureSession
    let onTap: (_ viewPoint: CGPoint, _ devicePoint: CGPoint) -> Void

    func makeUIView(context: Context) -> PreviewUIView {
        let view = PreviewUIView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        view.backgroundColor = .black
        let tap = UITapGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.handleTap(_:)))
        view.addGestureRecognizer(tap)
        return view
    }

    func updateUIView(_ uiView: PreviewUIView, context: Context) {
        context.coordinator.onTap = onTap
    }

    func makeCoordinator() -> Coordinator { Coordinator(onTap: onTap) }

    final class Coordinator: NSObject {
        var onTap: (CGPoint, CGPoint) -> Void
        init(onTap: @escaping (CGPoint, CGPoint) -> Void) { self.onTap = onTap }

        @objc func handleTap(_ gesture: UITapGestureRecognizer) {
            guard let view = gesture.view as? PreviewUIView else { return }
            let point = gesture.location(in: view)
            let devicePoint = view.previewLayer.captureDevicePointConverted(fromLayerPoint: point)
            onTap(point, devicePoint)
        }
    }
}

final class PreviewUIView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }

    override func layoutSubviews() {
        super.layoutSubviews()
        // Keep the preview upright when the phone is turned the other way round.
        if let orientation = window?.windowScene?.interfaceOrientation,
           let connection = previewLayer.connection,
           connection.isVideoOrientationSupported {
            connection.videoOrientation = AVCaptureVideoOrientation(orientation)
        }
    }
}
