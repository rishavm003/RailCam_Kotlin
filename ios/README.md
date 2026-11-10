# RailCam for iOS

A native SwiftUI version of RailCam with the same features as the Android app:

- Session form (IR / DFCCIL / Metro, zone or corridor, site, station, direction, OHE, tangent or curve rail, date), saved between launches.
- The same file-naming rules as Android (`FilenameGenerator.swift` mirrors the Kotlin one; unit tests check identical output).
- Landscape camera with the 2:1 guide box, dashed centre line, live level readout and haptic tick when level, tap to focus, exposure slider and flash.
- **Manual** mode (Keep / Retake after each shot) and **Auto** mode (every shot saved at once).
- Gallery with multi-select, delete confirmation and sharing; photo detail screen with all metadata.

Requires iOS 16 or later, iPhone only.

## Where photos go

Photos are saved as JPEG files in the app's Documents folder. In the **Files** app they appear under
**On My iPhone › RailCam › RailCam**, with their full RailCam filenames. Sharing from the gallery sends the
files themselves, so the filename is kept (e.g. when sharing to WhatsApp as a document or saving to Files).

## Building

The Xcode project is generated from `project.yml` with [XcodeGen](https://github.com/yonaskolb/XcodeGen),
so `RailCam.xcodeproj` is not committed.

On a Mac with Xcode 16 or later:

```bash
brew install xcodegen
cd ios
xcodegen generate
open RailCam.xcodeproj
```

Pick your Apple ID team under *Signing & Capabilities* to run on a real iPhone (the camera does not work in the simulator).

## Continuous build

`.github/workflows/ios.yml` runs on a GitHub macOS runner whenever anything in `ios/` changes. It generates the project, runs the
unit tests on an iPhone simulator, and builds an unsigned Release build for real devices. No Mac is needed to see whether it compiles.

## Layout

| Folder | Contents |
| --- | --- |
| `RailCam/App` | App entry, orientation lock, navigation |
| `RailCam/Models` | Metadata enums/values and the photo record |
| `RailCam/Data` | Settings (UserDefaults) and photo storage (files + JSON index) |
| `RailCam/Camera` | AVFoundation camera, preview view, CoreMotion level |
| `RailCam/Util` | Filename generator and 2:1 crop |
| `RailCam/Views` | SwiftUI screens and controls |
| `RailCamTests` | Unit tests |

## Installing on an iPhone without a Mac

Every run of the `iOS build` workflow uploads an unsigned **RailCam-unsigned-ipa** file (kept for 30 days):

1. Open the repository's **Actions** tab, choose the latest successful *iOS build* run, and download **RailCam-unsigned-ipa** under *Artifacts* (you must be signed in to GitHub). Unzip it to get `RailCam.ipa`.
2. Install [Sideloadly](https://sideloadly.io) (or AltStore) on your PC, plug in the iPhone, drag `RailCam.ipa` in, and sign in with your Apple ID.
3. On the iPhone, open *Settings › General › VPN & Device Management*, trust your Apple ID, and turn on *Developer Mode* if asked.

With a free Apple ID the app must be reinstalled every 7 days; a paid Apple Developer account removes that limit.
