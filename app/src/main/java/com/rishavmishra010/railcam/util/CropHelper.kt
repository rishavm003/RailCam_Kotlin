package com.rishavmishra010.railcam.util

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

object CropHelper {

    fun cropTo2x1(imageProxy: ImageProxy): Bitmap {
        val bitmap = imageProxy.toBitmap()

        val width = bitmap.width
        val height = bitmap.height

        // Match the DarkMask cutout (80% of the width, 2:1 ratio)
        val targetWidth = (width * 0.8f).toInt()
        val targetHeight = (targetWidth / 2).toInt()

        val xOffset = (width - targetWidth) / 2
        val yOffset = (height - targetHeight) / 2

        val cropped = Bitmap.createBitmap(bitmap, xOffset, yOffset, targetWidth, targetHeight)
        if (cropped !== bitmap) {
            bitmap.recycle()
        }
        return cropped
    }
}
