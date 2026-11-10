import AVFoundation
import UIKit

enum CameraError: LocalizedError {
    case noCamera
    case captureFailed(String)

    var errorDescription: String? {
        switch self {
        case .noCamera: return "No back camera is available on this device."
        case .captureFailed(let reason): return "Capture failed: \(reason)"
        }
    }
}

/// Owns the AVCaptureSession. All session work runs on `sessionQueue`;
/// published values are only changed on the main thread.
final class CameraController: NSObject, ObservableObject {
    enum Permission { case unknown, granted, denied }

    @Published private(set) var permission: Permission = .unknown
    @Published private(set) var isFlashOn = false
    @Published private(set) var errorMessage: String?
    /// Exposure bias range the device supports, clamped to ±2 EV like the Android slider.
    @Published private(set) var exposureRange: ClosedRange<Float> = -2...2
    @Published private(set) var exposureBias: Float = 0

    let session = AVCaptureSession()
    private let photoOutput = AVCapturePhotoOutput()
    private let sessionQueue = DispatchQueue(label: "railcam.camera.session")
    private var device: AVCaptureDevice?
    private var isConfigured = false
    private var inFlight: [Int64: PhotoCaptureDelegate] = [:]

    // MARK: Lifecycle

    func requestAccessAndStart() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            setPermission(.granted)
            start()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                self?.setPermission(granted ? .granted : .denied)
                if granted { self?.start() }
            }
        default:
            setPermission(.denied)
        }
    }

    private func start() {
        sessionQueue.async { [weak self] in
            guard let self else { return }
            if !self.isConfigured {
                do {
                    try self.configure()
                    self.isConfigured = true
                } catch {
                    DispatchQueue.main.async { self.errorMessage = error.localizedDescription }
                    return
                }
            }
            if !self.session.isRunning { self.session.startRunning() }
        }
    }

    func stop() {
        sessionQueue.async { [weak self] in
            guard let self, self.session.isRunning else { return }
            self.session.stopRunning()
        }
    }

    private func configure() throws {
        session.beginConfiguration()
        defer { session.commitConfiguration() }
        session.sessionPreset = .photo

        guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
            throw CameraError.noCamera
        }
        let input = try AVCaptureDeviceInput(device: camera)
        guard session.canAddInput(input), session.canAddOutput(photoOutput) else {
            throw CameraError.captureFailed("Camera could not be configured.")
        }
        session.addInput(input)
        session.addOutput(photoOutput)
        // Favour shutter speed: field capture needs fast, repeatable shots.
        photoOutput.maxPhotoQualityPrioritization = .balanced
        device = camera

        let low = max(camera.minExposureTargetBias, -2)
        let high = min(camera.maxExposureTargetBias, 2)
        DispatchQueue.main.async {
            self.exposureRange = low...high
        }

        try camera.lockForConfiguration()
        if camera.isFocusModeSupported(.continuousAutoFocus) { camera.focusMode = .continuousAutoFocus }
        if camera.isExposureModeSupported(.continuousAutoExposure) { camera.exposureMode = .continuousAutoExposure }
        camera.unlockForConfiguration()
    }

    // MARK: Controls

    func toggleFlash() {
        isFlashOn.toggle()
    }

    /// Focus and expose at a point in device coordinates (0...1, from the preview layer).
    func focus(at point: CGPoint) {
        sessionQueue.async { [weak self] in
            guard let device = self?.device, (try? device.lockForConfiguration()) != nil else { return }
            if device.isFocusPointOfInterestSupported, device.isFocusModeSupported(.autoFocus) {
                device.focusPointOfInterest = point
                device.focusMode = .autoFocus
            }
            if device.isExposurePointOfInterestSupported, device.isExposureModeSupported(.autoExpose) {
                device.exposurePointOfInterest = point
                device.exposureMode = .autoExpose
            }
            device.isSubjectAreaChangeMonitoringEnabled = true
            device.unlockForConfiguration()
        }
    }

    func setExposureBias(_ value: Float) {
        let clamped = min(max(value, exposureRange.lowerBound), exposureRange.upperBound)
        exposureBias = clamped
        sessionQueue.async { [weak self] in
            guard let device = self?.device, (try? device.lockForConfiguration()) != nil else { return }
            device.setExposureTargetBias(clamped, completionHandler: nil)
            device.unlockForConfiguration()
        }
    }

    // MARK: Capture

    /// Takes a photo oriented for the given interface orientation and returns its JPEG data.
    func capturePhoto(orientation: AVCaptureVideoOrientation) async throws -> Data {
        let flashOn = isFlashOn
        return try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Data, Error>) in
            sessionQueue.async { [weak self] in
                guard let self, self.session.isRunning else {
                    cont.resume(throwing: CameraError.captureFailed("Camera is not running."))
                    return
                }
                if let connection = self.photoOutput.connection(with: .video),
                   connection.isVideoOrientationSupported {
                    connection.videoOrientation = orientation
                }
                let settings = AVCapturePhotoSettings(format: [AVVideoCodecKey: AVVideoCodecType.jpeg])
                settings.photoQualityPrioritization = .speed
                if flashOn, self.photoOutput.supportedFlashModes.contains(.on) {
                    settings.flashMode = .on
                }
                let id = settings.uniqueID
                let delegate = PhotoCaptureDelegate { [weak self] result in
                    self?.sessionQueue.async { self?.inFlight[id] = nil }
                    cont.resume(with: result)
                }
                self.inFlight[id] = delegate
                self.photoOutput.capturePhoto(with: settings, delegate: delegate)
            }
        }
    }

    private func setPermission(_ value: Permission) {
        DispatchQueue.main.async { self.permission = value }
    }
}

private final class PhotoCaptureDelegate: NSObject, AVCapturePhotoCaptureDelegate {
    private let completion: (Result<Data, Error>) -> Void

    init(completion: @escaping (Result<Data, Error>) -> Void) {
        self.completion = completion
    }

    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        if let error {
            completion(.failure(error))
        } else if let data = photo.fileDataRepresentation() {
            completion(.success(data))
        } else {
            completion(.failure(CameraError.captureFailed("No image data.")))
        }
    }
}

extension AVCaptureVideoOrientation {
    init(_ interface: UIInterfaceOrientation) {
        switch interface {
        case .landscapeLeft: self = .landscapeLeft
        case .landscapeRight: self = .landscapeRight
        case .portraitUpsideDown: self = .portraitUpsideDown
        default: self = .portrait
        }
    }
}
