import AVFoundation
import SwiftUI
import UIKit

/// A photo waiting for Keep / Retake in Manual mode.
private struct PendingPhoto: Identifiable {
    let id = UUID()
    let image: UIImage
    let jpeg: Data
    let tilt: Double
    let takenAt: Date
}

struct CaptureView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var settings: SettingsStore
    @EnvironmentObject private var photoStore: PhotoStore

    @StateObject private var camera = CameraController()
    // Held in @State (not @StateObject) so 60 Hz tilt updates redraw only the level views.
    @State private var level = LevelMonitor()

    @State private var pending: PendingPhoto?
    @State private var isCapturing = false
    @State private var isSaving = false
    @State private var toast: String?
    @State private var toastTask: Task<Void, Never>?
    @State private var processing: Task<Void, Never>?

    @State private var focusPoint: CGPoint?
    @State private var focusTap = 0
    @State private var sliderVisible = false
    @State private var sliderInteraction = 0

    var body: some View {
        GeometryReader { geo in
            ZStack {
                Color.black.ignoresSafeArea()

                switch camera.permission {
                case .denied:
                    permissionMessage
                default:
                    cameraLayer(size: geo.size)
                }
            }
        }
        .ignoresSafeArea()
        .statusBarHidden(true)
        .onAppear {
            OrientationLock.set(.landscape)
            UIApplication.shared.isIdleTimerDisabled = true
            camera.requestAccessAndStart()
            level.start()
        }
        .onDisappear {
            camera.stop()
            level.stop()
            UIApplication.shared.isIdleTimerDisabled = false
            OrientationLock.set(.portrait)
        }
    }

    // MARK: Camera layer

    @ViewBuilder
    private func cameraLayer(size: CGSize) -> some View {
        ZStack {
            CameraPreviewView(session: camera.session) { viewPoint, devicePoint in
                guard pending == nil else { return }
                camera.focus(at: devicePoint)
                focusPoint = viewPoint
                focusTap += 1
                sliderVisible = true
                sliderInteraction += 1
            }
            .ignoresSafeArea()

            GuideOverlay()
                .allowsHitTesting(false)

            LevelIndicatorView(level: level)
                .allowsHitTesting(false)

            if let point = focusPoint {
                FocusRing(trigger: focusTap)
                    .position(point)
                    .allowsHitTesting(false)
            }

            if sliderVisible, let point = focusPoint, pending == nil {
                ExposureSliderView(
                    value: camera.exposureBias,
                    range: camera.exposureRange,
                    onChange: { camera.setExposureBias($0) },
                    onInteraction: { sliderInteraction += 1 }
                )
                .position(
                    x: min(point.x + 48, size.width - 30),
                    y: min(max(point.y - 60, 100), size.height - 100)
                )
            }

            if pending == nil {
                controls
            }

            if let photo = pending {
                ReviewOverlayView(
                    image: photo.image,
                    isSaving: isSaving,
                    onRetake: { pending = nil },
                    onKeep: { keep(photo) }
                )
                .transition(.opacity)
            }

            if let message = camera.errorMessage {
                Text(message)
                    .foregroundColor(.white)
                    .padding()
                    .background(RoundedRectangle(cornerRadius: 12).fill(Color.black.opacity(0.7)))
            }

            if let toast {
                VStack {
                    Spacer()
                    Text(toast)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(Color.black.opacity(0.75)))
                        .padding(.bottom, 24)
                }
                .allowsHitTesting(false)
                .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.18), value: pending?.id)
        .animation(.easeInOut(duration: 0.18), value: toast)
        .task(id: sliderInteraction) {
            // Hide the exposure slider 3 s after the last tap or drag.
            guard sliderVisible else { return }
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            if !Task.isCancelled { sliderVisible = false }
        }
    }

    private var controls: some View {
        HStack {
            VStack {
                CircleButton(systemName: camera.isFlashOn ? "bolt.fill" : "bolt.slash.fill",
                             tint: camera.isFlashOn ? Palette.amber : .white,
                             label: camera.isFlashOn ? "Flash on" : "Flash off") {
                    camera.toggleFlash()
                }
                Spacer()
                CircleButton(systemName: "xmark", tint: .white, label: "Close camera") {
                    dismiss()
                }
            }
            Spacer()
            ShutterButton(isBusy: isCapturing) { capture() }
                .disabled(isCapturing)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 20)
    }

    private var permissionMessage: some View {
        VStack(spacing: 16) {
            Text("Camera permission is required.")
                .foregroundColor(.white)
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .foregroundColor(Palette.amber)
            Button("Close") { dismiss() }
                .foregroundColor(.white)
        }
    }

    // MARK: Actions

    private func currentVideoOrientation() -> AVCaptureVideoOrientation {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        return AVCaptureVideoOrientation(scene?.interfaceOrientation ?? .landscapeRight)
    }

    private func capture() {
        guard !isCapturing else { return }
        isCapturing = true
        let tilt = level.tilt
        let takenAt = Date()
        let orientation = currentVideoOrientation()
        let autoMode = settings.isAutoMode
        let metadata = settings.metadata

        Task {
            let data: Data
            do {
                data = try await camera.capturePhoto(orientation: orientation)
            } catch {
                isCapturing = false
                showToast(error.localizedDescription)
                return
            }

            if autoMode {
                // Free the shutter immediately; crop + save one photo at a time in the background.
                isCapturing = false
                let previous = processing
                processing = Task {
                    await previous?.value
                    guard let result = await cropped(data) else {
                        showToast("Could not save photo")
                        return
                    }
                    do {
                        _ = try await photoStore.save(jpeg: result.jpeg, metadata: metadata, isAutoMode: true,
                                                      tiltAngle: tilt, takenAt: takenAt)
                        showToast("Photo saved")
                    } catch {
                        showToast("Could not save photo")
                    }
                }
            } else {
                let result = await cropped(data)
                isCapturing = false
                guard let result else {
                    showToast("Failed to process photo")
                    return
                }
                pending = PendingPhoto(image: result.image, jpeg: result.jpeg, tilt: tilt, takenAt: takenAt)
            }
        }
    }

    private func cropped(_ data: Data) async -> (image: UIImage, jpeg: Data)? {
        await Task.detached(priority: .userInitiated) {
            ImageCropper.crop(jpeg: data)
        }.value
    }

    private func keep(_ photo: PendingPhoto) {
        guard !isSaving else { return }
        isSaving = true
        let metadata = settings.metadata
        Task {
            do {
                _ = try await photoStore.save(jpeg: photo.jpeg, metadata: metadata, isAutoMode: false,
                                              tiltAngle: photo.tilt, takenAt: photo.takenAt)
                pending = nil
            } catch {
                showToast("Error saving photo: \(error.localizedDescription)")
            }
            isSaving = false
        }
    }

    private func showToast(_ message: String) {
        toastTask?.cancel()
        toast = message
        toastTask = Task {
            try? await Task.sleep(nanoseconds: 1_800_000_000)
            if !Task.isCancelled { toast = nil }
        }
    }
}

// MARK: - Overlay pieces

/// Darkened screen with the 2:1 cutout, corner brackets and dashed centre line.
private struct GuideOverlay: View {
    var body: some View {
        Canvas { context, size in
            let cutW = size.width * 0.8
            let cutH = cutW / 2
            let rect = CGRect(x: (size.width - cutW) / 2, y: (size.height - cutH) / 2, width: cutW, height: cutH)

            var mask = Path(CGRect(origin: .zero, size: size))
            mask.addRect(rect)
            context.fill(mask, with: .color(Color.black.opacity(0.85)), style: FillStyle(eoFill: true))

            // Dashed centre line
            var line = Path()
            line.move(to: CGPoint(x: rect.minX + 16, y: rect.midY))
            line.addLine(to: CGPoint(x: rect.maxX - 16, y: rect.midY))
            context.stroke(line, with: .color(Palette.amberLight.opacity(0.6)),
                           style: StrokeStyle(lineWidth: 1.5, dash: [10, 6]))

            // Corner brackets
            let b: CGFloat = 24
            var brackets = Path()
            brackets.move(to: CGPoint(x: rect.minX, y: rect.minY + b))
            brackets.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
            brackets.addLine(to: CGPoint(x: rect.minX + b, y: rect.minY))
            brackets.move(to: CGPoint(x: rect.maxX - b, y: rect.minY))
            brackets.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
            brackets.addLine(to: CGPoint(x: rect.maxX, y: rect.minY + b))
            brackets.move(to: CGPoint(x: rect.minX, y: rect.maxY - b))
            brackets.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
            brackets.addLine(to: CGPoint(x: rect.minX + b, y: rect.maxY))
            brackets.move(to: CGPoint(x: rect.maxX - b, y: rect.maxY))
            brackets.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
            brackets.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY - b))
            context.stroke(brackets, with: .color(Palette.amber), lineWidth: 3)
        }
        .ignoresSafeArea()
    }
}

/// Tilt readout and a line that stays parallel to the real horizon.
private struct LevelIndicatorView: View {
    @ObservedObject var level: LevelMonitor

    var body: some View {
        let color = tiltColor(level.tilt)
        ZStack {
            VStack {
                Text(String(format: "%.1f°", locale: Locale(identifier: "en_US_POSIX"), level.tilt))
                    .font(.system(size: 18, weight: .bold).monospacedDigit())
                    .foregroundColor(color)
                    .padding(.top, 16)
                Spacer()
            }
            Capsule()
                .fill(color)
                .frame(width: 160, height: 3)
                .rotationEffect(.degrees(level.tilt))
        }
    }
}

private struct FocusRing: View {
    let trigger: Int
    @State private var scale: CGFloat = 1.3
    @State private var opacity: Double = 0

    var body: some View {
        Circle()
            .stroke(Palette.amber, lineWidth: 2)
            .frame(width: 70, height: 70)
            .scaleEffect(scale)
            .opacity(opacity)
            .task(id: trigger) {
                scale = 1.3
                opacity = 1
                withAnimation(.easeOut(duration: 0.25)) { scale = 1 }
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard !Task.isCancelled else { return }
                withAnimation(.easeIn(duration: 0.3)) { opacity = 0 }
            }
    }
}

/// Vertical exposure slider shown next to the focus point.
private struct ExposureSliderView: View {
    let value: Float
    let range: ClosedRange<Float>
    let onChange: (Float) -> Void
    let onInteraction: () -> Void

    @State private var dragStart: Float?
    private let trackHeight: CGFloat = 160

    var body: some View {
        let span = max(range.upperBound - range.lowerBound, 0.001)
        let fraction = CGFloat((value - range.lowerBound) / span)
        ZStack {
            Capsule()
                .fill(Color.white.opacity(0.4))
                .frame(width: 2, height: trackHeight)
            Image(systemName: "sun.max.fill")
                .font(.system(size: 20))
                .foregroundColor(Palette.amber)
                .offset(y: trackHeight / 2 - fraction * trackHeight)
        }
        .frame(width: 44, height: trackHeight + 24)
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { drag in
                    let start = dragStart ?? value
                    if dragStart == nil { dragStart = start }
                    let delta = Float(-drag.translation.height / trackHeight) * span
                    onChange(start + delta)
                    onInteraction()
                }
                .onEnded { _ in
                    dragStart = nil
                    onInteraction()
                }
        )
        .accessibilityElement()
        .accessibilityLabel("Exposure")
        .accessibilityValue(String(format: "%.1f EV", value))
        .accessibilityAdjustableAction { direction in
            onChange(value + (direction == .increment ? 0.3 : -0.3))
            onInteraction()
        }
    }
}

private struct CircleButton: View {
    let systemName: String
    let tint: Color
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(tint)
                .frame(width: 48, height: 48)
                .background(Circle().fill(Color.black.opacity(0.5)))
        }
        .accessibilityLabel(label)
    }
}

private struct ShutterButton: View {
    let isBusy: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ZStack {
                Circle().stroke(Color.white, lineWidth: 4).frame(width: 76, height: 76)
                Circle().fill(isBusy ? Palette.amber.opacity(0.5) : Palette.amber).frame(width: 62, height: 62)
            }
        }
        .accessibilityLabel("Take photo")
    }
}

private struct ReviewOverlayView: View {
    let image: UIImage
    let isSaving: Bool
    let onRetake: () -> Void
    let onKeep: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.92).ignoresSafeArea()
            VStack(spacing: 20) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal, 40)
                HStack(spacing: 16) {
                    Button(action: onRetake) {
                        Text("Retake")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 150, height: 48)
                            .background(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.6), lineWidth: 1))
                    }
                    Button(action: onKeep) {
                        Group {
                            if isSaving {
                                ProgressView().tint(.black)
                            } else {
                                Text("Keep").font(.system(size: 16, weight: .bold))
                            }
                        }
                        .foregroundColor(.black)
                        .frame(width: 150, height: 48)
                        .background(RoundedRectangle(cornerRadius: 14).fill(Palette.amber))
                    }
                }
                .disabled(isSaving)
            }
            .padding(.vertical, 20)
        }
    }
}
