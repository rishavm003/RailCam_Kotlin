package com.rishavmishra010.railcam.domain.model

import android.graphics.Bitmap

data class PendingPhoto(
    val uri: String,
    val width: Int,
    val height: Int,
    val takenAt: Long,
    val tiltAtCapture: Float,
    /** The already-cropped Bitmap kept in memory for instant review display.
     *  Avoids re-reading and decoding the file from disk every time the overlay appears.
     *  Will be null if the Bitmap was recycled before the review state was set. */
    val previewBitmap: Bitmap? = null,
)
