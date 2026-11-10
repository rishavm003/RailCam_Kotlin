package com.rishavmishra010.railcam.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import androidx.camera.core.ImageProxy

/**
 * Crops captured frames to the 2:1 strip shown by the DarkMask cutout
 * (80 % of the upright image width, centred).
 */
object CropHelper {

    /** A captured frame copied out of the camera buffer so the [ImageProxy] can be closed immediately. */
    class CapturedFrame(val jpeg: ByteArray?, val fallback: Bitmap?, val rotationDegrees: Int)

    /**
     * Copies the frame out of [imageProxy]. Cheap (a memcpy of the JPEG bytes), so the
     * caller can close the proxy right away and free the camera for the next shot.
     * The proxy is NOT closed here.
     */
    fun copyFrame(imageProxy: ImageProxy): CapturedFrame {
        val rotation = imageProxy.imageInfo.rotationDegrees
        return if (imageProxy.format == ImageFormat.JPEG) {
            val buffer = imageProxy.planes[0].buffer
            buffer.rewind()
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            CapturedFrame(bytes, null, rotation)
        } else {
            CapturedFrame(null, imageProxy.toBitmap(), rotation)
        }
    }

    /**
     * Produces the upright 2:1 crop. For JPEG frames only the crop region is decoded
     * (BitmapRegionDecoder), which is roughly half the memory and time of decoding the
     * full frame and then cropping it.
     */
    fun cropTo2x1(frame: CapturedFrame): Bitmap {
        val cropped = if (frame.jpeg != null) {
            decodeCropRegion(frame.jpeg, frame.rotationDegrees)
        } else {
            val full = frame.fallback ?: error("Captured frame has no image data")
            val rect = cropRect(full.width, full.height, frame.rotationDegrees)
            val out = Bitmap.createBitmap(full, rect.left, rect.top, rect.width(), rect.height())
            if (out !== full) full.recycle()
            out
        }
        return rotate(cropped, frame.rotationDegrees)
    }

    /**
     * Downscales [source] so its width is at most [maxWidth] — used for the on-screen
     * review image, which never needs the full capture resolution.
     * Returns [source] itself when it is already small enough.
     */
    fun scaledForDisplay(source: Bitmap, maxWidth: Int = 1920): Bitmap {
        if (source.width <= maxWidth) return source
        val height = ((source.height.toLong() * maxWidth) / source.width).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, maxWidth, height, true)
    }

    /** Centred crop rectangle in buffer (unrotated) coordinates. */
    internal fun cropRect(bufferWidth: Int, bufferHeight: Int, rotationDegrees: Int): Rect {
        val swapped = rotationDegrees % 180 != 0
        val uprightWidth = if (swapped) bufferHeight else bufferWidth
        val cropW = (uprightWidth * 0.8f).toInt()
        val cropH = cropW / 2
        val (rw, rh) = if (swapped) cropH to cropW else cropW to cropH
        val left = (bufferWidth - rw) / 2
        val top = (bufferHeight - rh) / 2
        return Rect(left, top, left + rw, top + rh)
    }

    private fun decodeCropRegion(jpeg: ByteArray, rotationDegrees: Int): Bitmap {
        val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(jpeg, 0, jpeg.size)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(jpeg, 0, jpeg.size, false)
        }
        return try {
            val rect = cropRect(decoder.width, decoder.height, rotationDegrees)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            decoder.decodeRegion(rect, options) ?: error("Failed to decode crop region")
        } finally {
            decoder.recycle()
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
