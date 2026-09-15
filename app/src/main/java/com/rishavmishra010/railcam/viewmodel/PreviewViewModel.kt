package com.rishavmishra010.railcam.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rishavmishra010.railcam.data.db.AppDatabase
import com.rishavmishra010.railcam.data.db.PhotoRecord
import com.rishavmishra010.railcam.data.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhotoRepository
    private var loadJob: Job? = null

    private val _photo = MutableStateFlow<PhotoRecord?>(null)
    val photo: StateFlow<PhotoRecord?> = _photo.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PhotoRepository(database.photoDao())
    }

    fun loadPhoto(id: Long) {
        loadJob?.cancel()
        _photo.value = null
        loadJob = viewModelScope.launch {
            repository.getPhoto(id).collect { record ->
                _photo.value = record
            }
        }
    }

    fun deletePhoto(onDeleted: () -> Unit) {
        val currentPhoto = _photo.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(currentPhoto.uri)
                getApplication<Application>().contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            repository.delete(currentPhoto.id)

            withContext(Dispatchers.Main) {
                onDeleted()
            }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        super.onCleared()
    }
}
