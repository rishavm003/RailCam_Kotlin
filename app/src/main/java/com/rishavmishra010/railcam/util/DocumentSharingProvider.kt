package com.rishavmishra010.railcam.util

import android.net.Uri
import androidx.core.content.FileProvider

class DocumentSharingProvider : FileProvider() {
    override fun getType(uri: Uri): String? {
        // Return image/jpeg so WhatsApp and target apps treat the file as a .jpg image document
        // rather than falling back to an unformatted .bin stream.
        return "image/jpeg"
    }
}
