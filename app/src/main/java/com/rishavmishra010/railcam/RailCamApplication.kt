package com.rishavmishra010.railcam

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.lifecycle.ProcessCameraProvider

class RailCamApplication : Application(), CameraXConfig.Provider {

    override fun onCreate() {
        super.onCreate()
        // Start CameraX initialisation in the background at app launch, so it is
        // already done by the time the user taps "Start Camera".
        ProcessCameraProvider.getInstance(this)
    }

    /**
     * Only the back camera is ever used, so CameraX skips querying the characteristics
     * of every other lens on the device — a noticeable saving on multi-camera phones.
     */
    override fun getCameraXConfig(): CameraXConfig =
        CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
            .setMinimumLoggingLevel(Log.ERROR)
            .build()
}
