package com.rishavmishra010.railcam.domain.model


enum class Client(val displayName: String, val fileCode: String) {
    INDIAN_RAILWAY("IR", "IR"),
    DFCCIL("DFCCIL", "DFCCIL"),
    METRO("Metro", "METRO")
}

enum class DfccilCorridor(val displayName: String) {
    EDFC("EDFC"),
    WDFC("WDFC")
}

enum class MetroNetwork(val displayName: String, val fileCode: String) {
    DMRC("DMRC", "DMRC"),
    BMRC("BMRC", "BMRC"),
    MMRCL("MMRCL", "MMRCL"),
    DHAKA_METRO("Dhaka Metro", "DHAKA")
}

enum class RailType(val displayName: String, val fileCode: String) {
    TANGENT("Tangent Rail", "TAN"),
    CURVE("Curve Rail", "CUR")
}

enum class RailSide { LEFT, RIGHT }

enum class Direction { UP, DN }


enum class RailPosition(val displayName: String, val fileCode: String) {
    HIGH("High Rail", "HIGH"),
    LOW("Low Rail", "LOW")
}

object RailwayZones {
    val all = listOf(
        "NR", "NWR", "NER", "NFR", "ER", "ECR", "ECoR",
        "SER", "SCR", "CR", "WR", "WCR", "NCR", "SECR", "SR", "SWR"
    )
}

data class MetadataValues(
    val client: Client = Client.INDIAN_RAILWAY,
    val zoneOrSubClient: String = RailwayZones.all.first(),
    val testSiteName: String = "",
    val stationCode: String = "",
    val lineDetails: String = "",
    val kmMarker: String = "",
    val oheNumber: String = "",
    val railType: RailType = RailType.TANGENT,
    val railSide: RailSide = RailSide.LEFT,
    val direction: Direction = Direction.UP,
    val railPosition: RailPosition = RailPosition.HIGH,
    val sessionDate: String = todaySessionDate()
)

fun todaySessionDate(): String {
    val date = java.time.LocalDate.now()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
    return date.format(formatter)
}

fun defaultZoneOrSubClient(client: Client): String = when (client) {
    Client.INDIAN_RAILWAY -> RailwayZones.all.first()
    Client.DFCCIL -> DfccilCorridor.EDFC.name
    Client.METRO -> MetroNetwork.DMRC.fileCode
}

fun subClientOptions(client: Client): List<String> = when (client) {
    Client.INDIAN_RAILWAY -> RailwayZones.all
    Client.DFCCIL -> DfccilCorridor.entries.map { it.name }
    Client.METRO -> MetroNetwork.entries.map { it.fileCode }
}
