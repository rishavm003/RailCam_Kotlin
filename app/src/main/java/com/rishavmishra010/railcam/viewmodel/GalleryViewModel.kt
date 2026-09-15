package com.rishavmishra010.railcam.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rishavmishra010.railcam.data.db.AppDatabase
import com.rishavmishra010.railcam.data.db.PhotoRecord
import com.rishavmishra010.railcam.data.repository.PhotoRepository
import com.rishavmishra010.railcam.domain.usecase.GetPhotosUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoRepository
    private val getPhotosUseCase: GetPhotosUseCase

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
}
