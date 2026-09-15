package com.rishavmishra010.railcam.util

import com.rishavmishra010.railcam.domain.model.Client
import com.rishavmishra010.railcam.domain.model.Direction
import com.rishavmishra010.railcam.domain.model.MetadataValues
import com.rishavmishra010.railcam.domain.model.RailPosition
import com.rishavmishra010.railcam.domain.model.RailSide
import com.rishavmishra010.railcam.domain.model.RailType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilenameGeneratorTest {

    @Test
    fun generate_irFormat() {
        val metadata = MetadataValues(
            client = Client.INDIAN_RAILWAY,
            zoneOrSubClient = "NR",
            testSiteName = "site alpha",
            stationCode = "ndls",
            direction = Direction.UP,
            oheNumber = "OHE-12",
            kmMarker = "142",
            railType = RailType.TANGENT,
            railSide = RailSide.LEFT,
            sessionDate = "09-09-2026"
        )

        val filename = FilenameGenerator.generate(metadata, 1_700_000_000_000L)

        assertEquals("SITE_ALPHA_IR_NR_NDLS_UP_OHE_12_142_LEFT_09092026.jpg", filename)
    }

    @Test
    fun generate_dfccilFormat() {
        val metadata = MetadataValues(
            client = Client.DFCCIL,
            zoneOrSubClient = "EDFC",
            testSiteName = "site beta",
            stationCode = "mgs",
            direction = Direction.DN,
            oheNumber = "OHE-99",
            kmMarker = "CUR-05",
            railType = RailType.CURVE,
            railPosition = RailPosition.HIGH,
            sessionDate = "09-09-2026"
        )

        val filename = FilenameGenerator.generate(metadata, 1_700_000_000_000L)

        assertEquals("SITE_BETA_DFCCIL_EDFC_MGS_DN_CUR_05_HIGH_RAIL_OHE_99_09092026.jpg", filename)
    }

    @Test
    fun generate_metroFormat() {
        val metadata = MetadataValues(
            client = Client.METRO,
            zoneOrSubClient = "DMRC",
            lineDetails = "Red Line",
            testSiteName = "site gamma",
            stationCode = "rpk",
            direction = Direction.UP,
            oheNumber = "OHE-01",
            kmMarker = "TAN-01",
            railType = RailType.TANGENT,
            railSide = RailSide.RIGHT,
            sessionDate = "09-09-2026"
        )

        val filename = FilenameGenerator.generate(metadata, 1_700_000_000_000L)

        assertEquals("SITE_GAMMA_DMRC_RED_LINE_RPK_UP_TAN_01_RIGHT_OHE_01_09092026.jpg", filename)
    }

    @Test
    fun validate_requiresMetroLineDetails() {
        val invalidMetro = MetadataValues(
            client = Client.METRO,
            zoneOrSubClient = "DMRC",
            lineDetails = "",
            kmMarker = "12",
            oheNumber = "34"
        )
        assertNotNull(FilenameGenerator.validate(invalidMetro))
    }

    @Test
    fun generateMobileFilename_format() {
        val filename = FilenameGenerator.generateMobileFilename(1_700_000_000_000L)
        assertTrue(filename.startsWith("IMG_"))
        assertTrue(filename.endsWith(".jpg"))
    }
}
