package com.rishavmishra010.railcam.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object StorageManager {

    private const val TEMP_PHOTO_NAME = "pending_railcam_photo.jpg"
    private val unsafeFilenameChars = Regex("[^a-zA-Z0-9._-]")

    /** Returns a unique path in the cache dir for a pending photo (nothing is written yet). */
    fun newTempPhotoPath(context: Context): String =
        File(context.cacheDir, "pending_railcam_${System.nanoTime()}.jpg").absolutePath

    /** Encodes [bitmap] as JPEG into [path]. */
    fun writeTempPhoto(path: String, bitmap: Bitmap) {
        FileOutputStream(File(path)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
    }

    fun saveTempPhoto(context: Context, bitmap: Bitmap): String {
        val path = newTempPhotoPath(context)
        writeTempPhoto(path, bitmap)
        return path
    }

    /** Result of [savePermanentPhoto]: the MediaStore URI and the filename actually written. */
    data class SavedPhoto(val uri: String, val displayName: String)

    fun savePermanentPhoto(
        context: Context,
        tempUri: String,
        filename: String,
        deleteTempAfterCopy: Boolean = false
    ): SavedPhoto {
        val safeFilename = sanitizeFilename(filename)
        val tempFile = File(tempUri)
        if (!tempFile.exists()) {
            throw IllegalStateException(
                "Temp photo file not found at '$tempUri'. " +
                "The file may have been deleted due to memory pressure before Keep was tapped."
            )
        }

        val resolver = context.contentResolver
        // Several photos in one session share the same structured name, so a millisecond
        // timestamp is appended to keep each file unique. This exact name is used on disk
        // on every Android version and returned so the database records the same name.
        val uniqueDisplayName = safeFilename.removeSuffix(".jpg") + "_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueDisplayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/RailCam"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "RailCam"
                )
                if (!picturesDir.exists() && !picturesDir.mkdirs()) {
                    throw IllegalStateException("Failed to create photo directory")
                }
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, File(picturesDir, uniqueDisplayName).absolutePath)
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        try {
            // Some OEM MediaStore implementations (Realme/OPPO/OnePlus) return null
            // for openOutputStream on the first attempt. Retry up to 3 times with a
            // short delay before giving up.
            var outStream = resolver.openOutputStream(imageUri)
            if (outStream == null) {
                Log.w("StorageManager", "openOutputStream returned null on first try, retrying...")
                for (attempt in 2..3) {
                    Thread.sleep(100)
                    outStream = resolver.openOutputStream(imageUri)
                    if (outStream != null) {
                        Log.d("StorageManager", "openOutputStream succeeded on attempt $attempt")
                        break
                    }
                    Log.w("StorageManager", "openOutputStream still null on attempt $attempt")
                }
            }
            outStream?.use { stream ->
                tempFile.inputStream().use { inStream ->
                    inStream.copyTo(stream)
                }
            } ?: throw IllegalStateException(
                "Failed to open output stream for MediaStore URI after 3 attempts. " +
                "This is a known OEM MediaStore issue on Realme/OPPO/OnePlus devices."
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(imageUri, null, null)
            throw e
        }

        if (deleteTempAfterCopy) {
            deleteTempPhoto(tempUri)
        }

        return SavedPhoto(imageUri.toString(), uniqueDisplayName)
    }

    fun deleteTempPhoto(tempUri: String?) {
        if (tempUri.isNullOrBlank()) return
        val tempFile = File(tempUri)
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    fun deletePhotoFile(context: Context, uri: String?) {
        if (uri.isNullOrBlank()) return
        if (uri.startsWith("content://")) {
            try {
                context.contentResolver.delete(Uri.parse(uri), null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val file = File(uri)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun sanitizeFilename(filename: String): String {
        val baseName = filename.substringAfterLast('/').substringAfterLast('\\')
        val sanitized = baseName.replace(unsafeFilenameChars, "_")
        require(sanitized.isNotBlank() && sanitized.endsWith(".jpg", ignoreCase = true)) {
            "Invalid photo filename"
        }
        return sanitized
    }
}
