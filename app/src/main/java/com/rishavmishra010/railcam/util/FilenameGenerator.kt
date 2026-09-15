package com.rishavmishra010.railcam.util

import com.rishavmishra010.railcam.domain.model.Client
import com.rishavmishra010.railcam.domain.model.MetadataValues
import com.rishavmishra010.railcam.domain.model.RailType

object FilenameGenerator {

    fun generateBaseFilename(metadata: MetadataValues): String {
        val site = sanitizeToken(metadata.testSiteName)
        val station = sanitizeToken(metadata.stationCode)
        val direction = metadata.direction.name
        val ohe = sanitizeToken(metadata.oheNumber)
        val km = sanitizeToken(metadata.kmMarker) // Curve or Tangent Name
        val railPosOrSide = sanitizeToken(
            if (metadata.railType == RailType.CURVE) {
                metadata.railPosition.displayName
            } else {
                metadata.railSide.name
            }
        )

        val parts = when (metadata.client) {
            Client.INDIAN_RAILWAY -> {
                val clientCode = metadata.client.fileCode
                val zone = sanitizeToken(metadata.zoneOrSubClient)
                listOf(site, clientCode, zone, station, direction, ohe, km, railPosOrSide)
            }
            Client.DFCCIL -> {
                val clientCode = metadata.client.fileCode
                val corridor = sanitizeToken(metadata.zoneOrSubClient)
                listOf(site, clientCode, corridor, station, direction, km, railPosOrSide, ohe)
            }
            Client.METRO -> {
                val metroNetwork = sanitizeToken(metadata.zoneOrSubClient)
                val lineDetails = sanitizeToken(metadata.lineDetails)
                listOf(site, metroNetwork, lineDetails, station, direction, km, railPosOrSide, ohe)
            }
        }

        return parts.filter { it.isNotBlank() }.joinToString("_")
    }

    fun generate(metadata: MetadataValues, timestamp: Long): String {
        val sessionDate = metadata.sessionDate.replace("-", "")
        val baseName = generateBaseFilename(metadata)
        return "${baseName}_${sessionDate}.jpg"
    }

    fun generateMobileFilename(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.US)
        return "IMG_${sdf.format(java.util.Date(timestamp))}.jpg"
    }

    fun validate(metadata: MetadataValues): String? {
        if (metadata.sessionDate.isNotBlank() && !SESSION_DATE_PATTERN.matches(metadata.sessionDate)) {
            return "Session date must be DD-MM-YYYY"
        }
        return null
    }

    private fun sanitizeToken(value: String): String =
        value.trim().uppercase().replace(Regex("[^A-Z0-9]"), "_").trim('_')

    private val SESSION_DATE_PATTERN = Regex("^\\d{2}-\\d{2}-\\d{4}$")
}
