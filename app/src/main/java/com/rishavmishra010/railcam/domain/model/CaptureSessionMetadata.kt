package com.rishavmishra010.railcam.domain.model

/**
 * Metadata frozen when the engineer taps "Start Camera" on the home screen.
 * Used when saving photos so filename/DB always match the configured session.
 */
object CaptureSessionMetadata {
    @Volatile
    var values: MetadataValues = MetadataValues()

    @Volatile
    var sessionFilename: String? = null
}
