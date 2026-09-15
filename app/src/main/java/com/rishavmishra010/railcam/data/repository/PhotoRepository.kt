package com.rishavmishra010.railcam.data.repository

import com.rishavmishra010.railcam.data.db.PhotoDao
import com.rishavmishra010.railcam.data.db.PhotoRecord
import kotlinx.coroutines.flow.Flow

class PhotoRepository(private val photoDao: PhotoDao) {
    val allPhotos: Flow<List<PhotoRecord>> = photoDao.getAllPhotos()

    suspend fun insert(photo: PhotoRecord) {
        photoDao.insertPhoto(photo)
    }

    suspend fun delete(id: Long) {
        photoDao.deletePhoto(id)
    }

    fun getPhoto(id: Long): Flow<PhotoRecord?> = photoDao.getPhotoById(id)
}
