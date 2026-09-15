package com.rishavmishra010.railcam.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert
    suspend fun insertPhoto(photo: PhotoRecord): Long

    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoRecord>>

    @Query("SELECT * FROM photos WHERE id = :id")
    fun getPhotoById(id: Long): Flow<PhotoRecord?>

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)
}
