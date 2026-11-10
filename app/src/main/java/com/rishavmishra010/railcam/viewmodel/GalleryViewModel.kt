package com.rishavmishra010.railcam.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rishavmishra010.railcam.data.db.AppDatabase
import com.rishavmishra010.railcam.data.db.PhotoRecord
import com.rishavmishra010.railcam.data.repository.PhotoRepository
import com.rishavmishra010.railcam.domain.usecase.GetPhotosUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoRepository
    private val getPhotosUseCase: GetPhotosUseCase

    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PhotoRepository(database.photoDao())
        getPhotosUseCase = GetPhotosUseCase(repository)
    }

    val photos: StateFlow<List<PhotoRecord>> = getPhotosUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleSelection(photoId: Long) {
        val current = _selectedPhotoIds.value
        _selectedPhotoIds.value = if (current.contains(photoId)) {
            current - photoId
        } else {
            current + photoId
        }
    }

    fun selectAll() {
        val allIds = photos.value.map { it.id }.toSet()
        _selectedPhotoIds.value = allIds
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    /** Deletes all currently-selected photos from MediaStore and the local DB. */
    fun deleteSelectedPhotos() {
        val resolver = getApplication<Application>().contentResolver
        val selectedIds = _selectedPhotoIds.value
        val selectedPhotos = photos.value.filter { selectedIds.contains(it.id) }
        viewModelScope.launch(Dispatchers.IO) {
            for (photo in selectedPhotos) {
                // Remove from MediaStore (content:// URI)
                runCatching {
                    resolver.delete(Uri.parse(photo.uri), null, null)
                }
                // Remove from Room DB
                repository.delete(photo.id)
            }
        }
        clearSelection()
    }
}
