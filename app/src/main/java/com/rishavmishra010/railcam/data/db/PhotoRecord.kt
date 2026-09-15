package com.rishavmishra010.railcam.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filename: String,
    val uri: String,
    val client: String,
    val zoneOrSubClient: String,
    val testSiteName: String = "",
    val stationCode: String = "",
    val lineDetails: String = "",
    val kmMarker: String,
    val oheNumber: String,
    val railType: String,
    val railSide: String,
    val direction: String,

    val railPosition: String,
    val sessionDate: String,
    val tiltAngle: Float,
    val timestamp: Long
)
