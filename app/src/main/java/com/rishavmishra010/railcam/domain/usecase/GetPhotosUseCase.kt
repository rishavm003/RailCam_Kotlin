package com.rishavmishra010.railcam.domain.usecase

import com.rishavmishra010.railcam.data.db.PhotoRecord
import com.rishavmishra010.railcam.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow

class GetPhotosUseCase(private val repository: PhotoRepository) {
    operator fun invoke(): Flow<List<PhotoRecord>> {
        return repository.allPhotos
    }
}
