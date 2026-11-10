import SwiftUI
import UIKit

/// Same palette as the Android `ui/theme/Color.kt`, switching with the light/dark setting.
enum Palette {
    static let amber = Color(hex: 0xF5A623)
    static let amberLight = Color(hex: 0xFFF6E5)
    static let levelGreen = Color(hex: 0x10B981)
    static let levelYellow = Color(hex: 0xF59E0B)
    static let alertRed = Color(hex: 0xEF4444)

    static let background = dynamic(light: 0xF8F9FB, dark: 0x0D0E12)
    static let surface = dynamic(light: 0xFFFFFF, dark: 0x16181F)
    static let surfaceHigh = dynamic(light: 0xF0F2F5, dark: 0x20232D)
    static let textPrimary = dynamic(light: 0x111827, dark: 0xF0F2F5)
    static let textSecondary = dynamic(light: 0x4B5563, dark: 0x9EA3B0)
    static let border = dynamic(light: 0xE5E7EB, dark: 0x282C37)

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

extension UIColor {
    convenience init(hex: UInt32, alpha: CGFloat = 1) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: alpha
        )
    }
}

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(UIColor(hex: hex, alpha: CGFloat(opacity)))
    }
}

/// Colour of the level readout: green near level, yellow when close, red otherwise
/// (upside-down angles count as level, as on Android).
func tiltColor(_ tilt: Double) -> Color {
    let a = abs(tilt)
    if a <= 5 || a >= 175 { return Palette.levelGreen }
    if a <= 15 || a >= 165 { return Palette.levelYellow }
    return Palette.alertRed
}
