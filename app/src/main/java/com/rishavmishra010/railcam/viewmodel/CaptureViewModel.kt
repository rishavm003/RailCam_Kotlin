package com.rishavmishra010.railcam.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rishavmishra010.railcam.data.datastore.MetadataPreferences
import com.rishavmishra010.railcam.data.db.AppDatabase
import com.rishavmishra010.railcam.data.repository.PhotoRepository
import com.rishavmishra010.railcam.domain.model.CaptureSessionMetadata
import com.rishavmishra010.railcam.domain.model.PendingPhoto
import com.rishavmishra010.railcam.domain.usecase.SavePhotoUseCase
import com.rishavmishra010.railcam.util.CropHelper
import com.rishavmishra010.railcam.util.FilenameGenerator
import com.rishavmishra010.railcam.util.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CaptureViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _tiltAngle = MutableStateFlow(0f)
    val tiltAngle: StateFlow<Float> = _tiltAngle.asStateFlow()

    private val _pendingPhoto = MutableStateFlow<PendingPhoto?>(null)
    val pendingPhoto: StateFlow<PendingPhoto?> = _pendingPhoto.asStateFlow()

    private val _isProcessingCapture = MutableStateFlow(false)
    val isProcessingCapture: StateFlow<Boolean> = _isProcessingCapture.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    private val repository: PhotoRepository
    private val savePhotoUseCase: SavePhotoUseCase
    private val metadataPreferences: MetadataPreferences

    val isInstantMode: StateFlow<Boolean>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PhotoRepository(database.photoDao())
        savePhotoUseCase = SavePhotoUseCase(repository)
        metadataPreferences = MetadataPreferences(application)
        
        isInstantMode = metadataPreferences.instantModeFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    }

    fun startSensors() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)

                val adjustedMatrix = FloatArray(9)
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X,
                    adjustedMatrix
                )

                val orientation = FloatArray(3)
                SensorManager.getOrientation(adjustedMatrix, orientation)

                val angle = Math.toDegrees(orientation[2].toDouble()).toFloat() * -1f
                _tiltAngle.value = angle
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun beginCapture(): Boolean {
        if (_isProcessingCapture.value || _pendingPhoto.value != null) return false
        _isProcessingCapture.value = true
        return true
    }

    fun onCaptureFailed() {
        _isProcessingCapture.value = false
    }

    fun processPhoto(context: Context, imageProxy: ImageProxy, currentTilt: Float) {
        if (_pendingPhoto.value != null) {
            imageProxy.close()
            _isProcessingCapture.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val croppedBitmap = CropHelper.cropTo2x1(imageProxy)
                val width = croppedBitmap.width
                val height = croppedBitmap.height
                val uri = StorageManager.saveTempPhoto(context, croppedBitmap)
                croppedBitmap.recycle()

                _pendingPhoto.value = PendingPhoto(
                    uri = uri,
                    width = width,
                    height = height,
                    takenAt = System.currentTimeMillis(),
                    tiltAtCapture = currentTilt
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Photo Cropped & Cached!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to process photo", Toast.LENGTH_SHORT).show()
                }
            } finally {
                imageProxy.close()
                _isProcessingCapture.value = false
            }
        }
    }

    fun processPhotoInstant(context: Context, imageProxy: ImageProxy, currentTilt: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val croppedBitmap = CropHelper.cropTo2x1(imageProxy)
                val width = croppedBitmap.width
                val height = croppedBitmap.height
                val uri = StorageManager.saveTempPhoto(context, croppedBitmap)
                croppedBitmap.recycle()

                val photo = PendingPhoto(
                    uri = uri,
                    width = width,
                    height = height,
                    takenAt = System.currentTimeMillis(),
                    tiltAtCapture = currentTilt
                )

                val currentMetadata = CaptureSessionMetadata.values.takeIf {
                    FilenameGenerator.validate(it) == null
                } ?: metadataPreferences.metadataFlow.first()

                savePhotoUseCase(context, photo, currentMetadata, isInstantMode = true)
                StorageManager.deleteTempPhoto(uri)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved instantly!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to capture instantly", Toast.LENGTH_SHORT).show()
                }
            } finally {
                imageProxy.close()
                _isProcessingCapture.value = false
            }
        }
    }

    fun clearPendingPhoto() {
        val current = _pendingPhoto.value
        StorageManager.deleteTempPhoto(current?.uri)
        _pendingPhoto.value = null
    }

    fun savePhoto(context: Context, onSaved: () -> Unit = {}) {
        val currentPhoto = _pendingPhoto.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentMetadata = CaptureSessionMetadata.values.takeIf {
                    FilenameGenerator.validate(it) == null
                } ?: metadataPreferences.metadataFlow.first()

                savePhotoUseCase(context, currentPhoto, currentMetadata, isInstantMode = isInstantMode.value)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved to Database!", Toast.LENGTH_SHORT).show()
                    clearPendingPhoto()
                    onSaved()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving photo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCleared() {
        stopSensors()
        clearPendingPhoto()
        super.onCleared()
    }
}
