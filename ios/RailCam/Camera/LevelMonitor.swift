import CoreMotion
import Foundation
import UIKit

/// Horizon tilt of the phone held in landscape, from the gravity vector.
/// 0° means the long edge of the phone is level. Smoothed and rate-limited like the Android
/// sensor code, with hysteresis on the "level" state so it does not flicker.
final class LevelMonitor: ObservableObject {
    @Published private(set) var tilt: Double = 0
    @Published private(set) var isLevel = false

    private let motion = CMMotionManager()
    private let queue: OperationQueue = {
        let q = OperationQueue()
        q.name = "railcam.motion"
        q.maxConcurrentOperationCount = 1
        return q
    }()
    private var smoothed: Double?
    private var lastPublished: Double = .nan
    private var levelState = false

    private static let smoothing = 0.25
    private static let levelEnter = 0.8
    private static let levelExit = 1.2

    func start() {
        guard motion.isDeviceMotionAvailable, !motion.isDeviceMotionActive else { return }
        motion.deviceMotionUpdateInterval = 1.0 / 60.0
        motion.startDeviceMotionUpdates(to: queue) { [weak self] data, _ in
            guard let self, let g = data?.gravity else { return }
            self.handle(gravityX: g.x, gravityY: g.y)
        }
    }

    func stop() {
        motion.stopDeviceMotionUpdates()
        smoothed = nil
    }

    private func handle(gravityX: Double, gravityY: Double) {
        // Rotation of the phone about the screen axis; 0 = portrait upright, ±90 = landscape.
        let roll = atan2(gravityX, -gravityY) * 180 / .pi
        let nominal: Double = roll >= 0 ? 90 : -90
        let raw = wrap(roll - nominal)

        let value: Double
        if let previous = smoothed {
            value = wrap(previous + Self.smoothing * wrap(raw - previous))
        } else {
            value = raw
        }
        smoothed = value

        let magnitude = abs(value)
        let nowLevel = levelState ? magnitude <= Self.levelExit : magnitude <= Self.levelEnter
        let becameLevel = nowLevel && !levelState
        levelState = nowLevel

        // Skip sub-visible changes so the UI is not redrawn needlessly.
        let shouldPublish = lastPublished.isNaN || abs(value - lastPublished) >= 0.05
        if shouldPublish { lastPublished = value }
        guard shouldPublish || becameLevel || nowLevel != isLevelSnapshot else { return }
        isLevelSnapshot = nowLevel

        Task { @MainActor in
            if shouldPublish { self.tilt = value }
            if self.isLevel != nowLevel { self.isLevel = nowLevel }
            if becameLevel {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            }
        }
    }

    /// Last level state sent to the main thread (read on the motion queue only).
    private var isLevelSnapshot = false

    private func wrap(_ degrees: Double) -> Double {
        var d = degrees.truncatingRemainder(dividingBy: 360)
        if d > 180 { d -= 360 }
        if d < -180 { d += 360 }
        return d
    }
}
