package com.rishavmishra010.railcam.domain.usecase

import android.content.Context
import com.rishavmishra010.railcam.data.db.PhotoRecord
import com.rishavmishra010.railcam.data.repository.PhotoRepository
import com.rishavmishra010.railcam.domain.model.CaptureSessionMetadata
import com.rishavmishra010.railcam.domain.model.MetadataValues
import com.rishavmishra010.railcam.domain.model.PendingPhoto
import com.rishavmishra010.railcam.domain.model.RailType
import com.rishavmishra010.railcam.util.FilenameGenerator
import com.rishavmishra010.railcam.util.StorageManager

class SavePhotoUseCase(private val repository: PhotoRepository) {

    suspend operator fun invoke(
        context: Context,
        pendingPhoto: PendingPhoto,
        metadata: MetadataValues,
        isInstantMode: Boolean = false
    ) {
        val filename = if (isInstantMode) {
            FilenameGenerator.generateMobileFilename(pendingPhoto.takenAt)
        } else {
            CaptureSessionMetadata.sessionFilename ?: FilenameGenerator.generate(metadata, pendingPhoto.takenAt)
        }
        var permanentUri: String? = null

        try {
            permanentUri = StorageManager.savePermanentPhoto(
                context = context,
                tempUri = pendingPhoto.uri,
                filename = filename,
                deleteTempAfterCopy = false
            )

            val record = PhotoRecord(
                filename = filename,
                uri = permanentUri,
                client = metadata.client.name,
                zoneOrSubClient = metadata.zoneOrSubClient,
                testSiteName = metadata.testSiteName,
                stationCode = metadata.stationCode,
                lineDetails = metadata.lineDetails,
                kmMarker = metadata.kmMarker,
                oheNumber = metadata.oheNumber,
                railType = metadata.railType.name,
                railSide = if (metadata.railType == RailType.TANGENT) metadata.railSide.name else "",
                direction = metadata.direction.name,

                railPosition = if (metadata.railType == RailType.CURVE) metadata.railPosition.name else "",
                sessionDate = metadata.sessionDate,
                tiltAngle = pendingPhoto.tiltAtCapture,
                timestamp = pendingPhoto.takenAt
            )

            repository.insert(record)
            StorageManager.deleteTempPhoto(pendingPhoto.uri)
        } catch (e: Exception) {
            StorageManager.deletePhotoFile(context, permanentUri)
            throw e
        }
    }
}
