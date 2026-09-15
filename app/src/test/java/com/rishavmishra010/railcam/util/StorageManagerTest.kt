package com.rishavmishra010.railcam.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StorageManagerTest {

    @Test
    fun sanitizeFilename_stripsPathSegments() {
        val sanitized = StorageManager.sanitizeFilename("../evil/SITE_UP_LEFT_KM_1.jpg")
        assertEquals("SITE_UP_LEFT_KM_1.jpg", sanitized)
    }

    @Test
    fun sanitizeFilename_rejectsInvalidNames() {
        assertThrows(IllegalArgumentException::class.java) {
            StorageManager.sanitizeFilename("not-a-photo.png")
        }
    }
}
