import XCTest
@testable import RailCam

/// Same cases as the Android FilenameGeneratorTest, so both apps name files identically.
final class FilenameGeneratorTests: XCTestCase {

    func testIndianRailwayFormat() {
        var m = MetadataValues()
        m.client = .INDIAN_RAILWAY
        m.zoneOrSubClient = "NR"
        m.testSiteName = "site alpha"
        m.stationCode = "ndls"
        m.direction = .UP
        m.oheNumber = "OHE-12"
        m.kmMarker = "142"
        m.railType = .TANGENT
        m.railSide = .LEFT
        m.sessionDate = "09-09-2026"
        XCTAssertEqual(FilenameGenerator.generate(m), "SITE_ALPHA_IR_NR_NDLS_UP_OHE_12_142_LEFT_09092026.jpg")
    }

    func testDfccilFormat() {
        var m = MetadataValues()
        m.client = .DFCCIL
        m.zoneOrSubClient = "EDFC"
        m.testSiteName = "site beta"
        m.stationCode = "mgs"
        m.direction = .DN
        m.oheNumber = "OHE-99"
        m.kmMarker = "CUR-05"
        m.railType = .CURVE
        m.railPosition = .HIGH
        m.sessionDate = "09-09-2026"
        XCTAssertEqual(FilenameGenerator.generate(m), "SITE_BETA_DFCCIL_EDFC_MGS_DN_CUR_05_HIGH_RAIL_OHE_99_09092026.jpg")
    }

    func testMetroFormat() {
        var m = MetadataValues()
        m.client = .METRO
        m.zoneOrSubClient = "DMRC"
        m.lineDetails = "Red Line"
        m.testSiteName = "site gamma"
        m.stationCode = "rpk"
        m.direction = .UP
        m.oheNumber = "OHE-01"
        m.kmMarker = "TAN-01"
        m.railType = .TANGENT
        m.railSide = .RIGHT
        m.sessionDate = "09-09-2026"
        XCTAssertEqual(FilenameGenerator.generate(m), "SITE_GAMMA_DMRC_RED_LINE_RPK_UP_TAN_01_RIGHT_OHE_01_09092026.jpg")
    }

    func testEmptyMetadataFallsBackToM1() {
        var m = MetadataValues()
        m.client = .INDIAN_RAILWAY
        m.zoneOrSubClient = ""
        m.sessionDate = ""
        XCTAssertEqual(FilenameGenerator.generate(m), "M_1_IR_M_1_M_1_UP_M_1_M_1_LEFT_M_1.jpg")
    }

    func testMobileFilenameFormat() {
        let name = FilenameGenerator.generateMobileFilename(Date(timeIntervalSince1970: 1_700_000_000))
        XCTAssertTrue(name.hasPrefix("IMG_"))
        XCTAssertTrue(name.hasSuffix(".jpg"))
    }

    func testValidateRejectsBadDate() {
        var m = MetadataValues()
        m.sessionDate = "2026-09-09"
        XCTAssertNotNil(FilenameGenerator.validate(m))
        m.sessionDate = "09-09-2026"
        XCTAssertNil(FilenameGenerator.validate(m))
    }

    func testCropRectIsCentredTwoToOne() {
        let rect = ImageCropper.cropRect(width: 4032, height: 3024)
        XCTAssertEqual(rect.width, 3225)
        XCTAssertEqual(rect.height, 1612)
        XCTAssertEqual(rect.midX, 2016, accuracy: 1)
        XCTAssertEqual(rect.midY, 1512, accuracy: 1)
    }
}
