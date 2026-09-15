package com.rishavmishra010.railcam.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object StorageManager {

    private const val TEMP_PHOTO_NAME = "pending_railcam_photo.jpg"
    private val unsafeFilenameChars = Regex("[^a-zA-Z0-9._-]")

    fun saveTempPhoto(context: Context, bitmap: Bitmap): String {
        val tempFile = File(context.cacheDir, TEMP_PHOTO_NAME)

        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        return tempFile.absolutePath
    }

    fun savePermanentPhoto(
        context: Context,
        tempUri: String,
        filename: String,
        deleteTempAfterCopy: Boolean = false
    ): String {
        val safeFilename = sanitizeFilename(filename)
        val tempFile = File(tempUri)
        if (!tempFile.exists()) {
            throw IllegalStateException("Temp photo file not found")
        }

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, safeFilename)
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
                put(MediaStore.Images.Media.DATA, File(picturesDir, safeFilename).absolutePath)
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        try {
            resolver.openOutputStream(imageUri)?.use { outStream ->
                tempFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: throw IllegalStateException("Failed to write photo to MediaStore")

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

        return imageUri.toString()
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
