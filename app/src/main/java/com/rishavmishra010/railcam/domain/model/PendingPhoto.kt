package com.rishavmishra010.railcam.domain.model

data class PendingPhoto(
    val uri: String,
    val width: Int,
    val height: Int,
    val takenAt: Long,
    val tiltAtCapture: Float,
)
