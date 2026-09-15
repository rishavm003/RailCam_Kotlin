package com.rishavmishra010.railcam.util

import android.net.Uri
import androidx.core.content.FileProvider

class DocumentSharingProvider : FileProvider() {
    override fun getType(uri: Uri): String? {
        // Return a generic stream MIME type to force WhatsApp and other apps
        // to share the image as an uncompressed document/file instead of a photo.
        return "application/octet-stream"
    }
}
