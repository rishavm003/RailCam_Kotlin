package com.rishavmishra010.railcam.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rishavmishra010.railcam.data.datastore.MetadataPreferences
import com.rishavmishra010.railcam.data.db.AppDatabase
import com.rishavmishra010.railcam.data.repository.PhotoRepository
import com.rishavmishra010.railcam.domain.model.PendingPhoto
import com.rishavmishra010.railcam.domain.usecase.SavePhotoUseCase
import com.rishavmishra010.railcam.util.CropHelper
import com.rishavmishra010.railcam.util.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class CaptureViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val appContext: Context = application.applicationContext

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Reused across sensor callbacks so the ~50 Hz sensor stream allocates nothing.
    private val rotationMatrix = FloatArray(9)
    private val adjustedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var smoothedTilt = Float.NaN

    private val _tiltAngle = MutableStateFlow(0f)
    val tiltAngle: StateFlow<Float> = _tiltAngle.asStateFlow()

    /** True while the phone is level. Uses hysteresis (enter ≤ 0.8°, leave > 1.2°)
     *  so hovering at the edge does not toggle it on every sensor tick. */
    private val _isAtLevel = MutableStateFlow(false)
    val isAtLevel: StateFlow<Boolean> = _isAtLevel.asStateFlow()

    private val _pendingPhoto = MutableStateFlow<PendingPhoto?>(null)
    val pendingPhoto: StateFlow<PendingPhoto?> = _pendingPhoto.asStateFlow()

    private val _isProcessingCapture = MutableStateFlow(false)
    val isProcessingCapture: StateFlow<Boolean> = _isProcessingCapture.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    /** Background JPEG encode of the pending photo's temp file; Keep waits for it. */
    private var pendingTempWrite: Deferred<Unit>? = null

    /** Heavy image work runs one capture at a time so burst shooting in Instant mode
     *  never holds several full-size bitmaps in memory at once. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val imageDispatcher = Dispatchers.Default.limitedParallelism(1)

    /** Survives viewModelScope cancellation so temp-file cleanup always finishes. */
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        smoothedTilt = Float.NaN
        rotationSensor?.let {
            // SENSOR_DELAY_GAME (~50 Hz) instead of SENSOR_DELAY_UI (~16 Hz) so the
            // level line moves smoothly instead of in visible steps.
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        if (e.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, e.values)
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_Y,
            SensorManager.AXIS_MINUS_X,
            adjustedMatrix
        )
        SensorManager.getOrientation(adjustedMatrix, orientation)
        val raw = Math.toDegrees(orientation[2].toDouble()).toFloat() * -1f

        // Low-pass filter (wrap-aware around ±180°) to remove sensor jitter.
        smoothedTilt = if (smoothedTilt.isNaN()) {
            raw
        } else {
            wrapDegrees(smoothedTilt + TILT_SMOOTHING * wrapDegrees(raw - smoothedTilt))
        }
        val angle = smoothedTilt

        // Skip sub-visible changes so the UI is not invalidated needlessly.
        if (abs(angle - _tiltAngle.value) >= 0.05f) {
            _tiltAngle.value = angle
        }

        val absAngle = abs(angle)
        val atLevel = _isAtLevel.value
        if (!atLevel && absAngle <= LEVEL_ENTER_DEG) {
            _isAtLevel.value = true
        } else if (atLevel && absAngle > LEVEL_EXIT_DEG) {
            _isAtLevel.value = false
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

    /**
     * Normal mode: crop, show the review overlay as soon as a screen-sized preview is ready,
     * and encode the full-size temp JPEG in the background while the user decides.
     */
    fun processPhoto(context: Context, imageProxy: ImageProxy, currentTilt: Float) {
        if (_pendingPhoto.value != null) {
            imageProxy.close()
            _isProcessingCapture.value = false
            return
        }
        val takenAt = System.currentTimeMillis()

        viewModelScope.launch(imageDispatcher) {
            try {
                val frame = try {
                    CropHelper.copyFrame(imageProxy)
                } finally {
                    imageProxy.close()
                }
                val cropped = CropHelper.cropTo2x1(frame)
                val preview = CropHelper.scaledForDisplay(cropped)
                val width = cropped.width
                val height = cropped.height
                val tempPath = StorageManager.newTempPhotoPath(appContext)

                pendingTempWrite = cleanupScope.async {
                    try {
                        StorageManager.writeTempPhoto(tempPath, cropped)
                    } finally {
                        if (cropped !== preview) cropped.recycle()
                    }
                }

                // The preview bitmap is not recycled manually: it may still be drawn during
                // the screen exit animation, and the GC reclaims it safely on Android 8+.
                _pendingPhoto.value = PendingPhoto(
                    uri = tempPath,
                    width = width,
                    height = height,
                    takenAt = takenAt,
                    tiltAtCapture = currentTilt,
                    previewBitmap = preview
                )
            } catch (e: Throwable) {
                Log.e(TAG, "processPhoto failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Failed to process photo", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isProcessingCapture.value = false
            }
        }
    }

    /**
     * Instant mode: the camera is released for the next shot as soon as the JPEG bytes are
     * copied out; cropping and saving continue in the background.
     */
    fun processPhotoInstant(context: Context, imageProxy: ImageProxy, currentTilt: Float) {
        val takenAt = System.currentTimeMillis()
        val frame = try {
            CropHelper.copyFrame(imageProxy)
        } catch (e: Throwable) {
            Log.e(TAG, "copyFrame failed", e)
            null
        } finally {
            imageProxy.close()
            _isProcessingCapture.value = false
        }
        if (frame == null) {
            Toast.makeText(appContext, "Could not save photo", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(imageDispatcher) {
            var tempPath: String? = null
            try {
                val cropped = CropHelper.cropTo2x1(frame)
                val width = cropped.width
                val height = cropped.height
                tempPath = StorageManager.saveTempPhoto(appContext, cropped)
                cropped.recycle()

                val photo = PendingPhoto(
                    uri = tempPath,
                    width = width,
                    height = height,
                    takenAt = takenAt,
                    tiltAtCapture = currentTilt
                )
                withContext(Dispatchers.IO) {
                    savePhotoUseCase(appContext, photo, currentMetadata(), isInstantMode = true)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Photo saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "processPhotoInstant failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Could not save photo", Toast.LENGTH_SHORT).show()
                }
            } finally {
                StorageManager.deleteTempPhoto(tempPath)
            }
        }
    }

    fun clearPendingPhoto() {
        val current = _pendingPhoto.value ?: return
        _pendingPhoto.value = null
        val write = pendingTempWrite
        pendingTempWrite = null
        cleanupScope.launch {
            runCatching { write?.await() }
            StorageManager.deleteTempPhoto(current.uri)
        }
    }

    fun savePhoto(context: Context, onSaved: () -> Unit = {}) {
        val currentPhoto = _pendingPhoto.value ?: return
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                pendingTempWrite?.await()
                savePhotoUseCase(appContext, currentPhoto, currentMetadata(), isInstantMode = isInstantMode.value)

                withContext(Dispatchers.Main) {
                    onSaved()
                    clearPendingPhoto()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "savePhoto failed: ${e.javaClass.simpleName}: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Error saving photo: ${e.message?.take(80)}", Toast.LENGTH_LONG).show()
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Session metadata, read from DataStore once per camera session. Reading from disk
     *  (rather than an in-memory singleton) keeps it correct after process death. */
    private val sessionMetadata by lazy {
        viewModelScope.async(Dispatchers.IO) { metadataPreferences.metadataFlow.first() }
    }

    private suspend fun currentMetadata() = sessionMetadata.await()

    override fun onCleared() {
        stopSensors()
        clearPendingPhoto()
        super.onCleared()
    }

    private companion object {
        const val TAG = "CaptureVM"
        const val TILT_SMOOTHING = 0.25f
        const val LEVEL_ENTER_DEG = 0.8f
        const val LEVEL_EXIT_DEG = 1.2f

        fun wrapDegrees(deg: Float): Float {
            var d = deg % 360f
            if (d > 180f) d -= 360f
            if (d < -180f) d += 360f
            return d
        }
    }
}
