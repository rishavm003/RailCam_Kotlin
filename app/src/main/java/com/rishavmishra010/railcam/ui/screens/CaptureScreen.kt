package com.rishavmishra010.railcam.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.widget.Toast
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.rishavmishra010.railcam.ui.components.camera.ExposureSlider
import com.rishavmishra010.railcam.ui.components.camera.FlashButton
import com.rishavmishra010.railcam.ui.components.camera.ReviewOverlay
import com.rishavmishra010.railcam.ui.components.camera.ShutterButton
import com.rishavmishra010.railcam.ui.components.overlay.DarkMask
import com.rishavmishra010.railcam.ui.components.overlay.LevelIndicator
import com.rishavmishra010.railcam.ui.components.overlay.RailGuideOverlay
import com.rishavmishra010.railcam.ui.theme.Amber
import com.rishavmishra010.railcam.util.HapticManager
import com.rishavmishra010.railcam.viewmodel.CaptureViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    navController: NavController,
    viewModel: CaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Android 8–9 have no scoped storage: writing into Pictures/RailCam needs this permission.
    val needsStoragePermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
    val permissionsState = rememberMultiplePermissionsState(
        buildList {
            add(Manifest.permission.CAMERA)
            if (needsStoragePermission) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    )
    val cameraGranted = permissionsState.permissions.any {
        it.permission == Manifest.permission.CAMERA && it.status.isGranted
    }
    val storagePermissionGranted = !needsStoragePermission ||
        permissionsState.permissions.any {
            it.permission == Manifest.permission.WRITE_EXTERNAL_STORAGE && it.status.isGranted
        }

    val tiltAngle by viewModel.tiltAngle.collectAsState()
    val pendingPhoto by viewModel.pendingPhoto.collectAsState()
    val isProcessingCapture by viewModel.isProcessingCapture.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isFlashOn by viewModel.isFlashOn.collectAsState()
    val isInstantMode by viewModel.isInstantMode.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showFlash by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (showFlash) 1f else 0f,
        animationSpec = tween(durationMillis = if (showFlash) 50 else 300),
        label = "flash_alpha"
    )

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        // 4:3 capped at ~12.5 MP (4096×3072). Larger frames add seconds of decode/encode
        // time per shot without visible benefit for the 2:1 crop we keep.
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(4096, 3072),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            )
            .build()

        ImageCapture.Builder()
            // Fix 1: MINIMIZE_LATENCY reduces shutter-to-review delay by skipping
            // heavy sensor post-processing. Quality is still high at 95% JPEG.
            // For field use (review mode) speed of feedback matters more than the
            // marginal quality gain of MAXIMIZE_QUALITY.
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolutionSelector)
            .setJpegQuality(95)
            .build()
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    var focusOffset by remember { mutableStateOf<Offset?>(null) }
    var showFocusRing by remember { mutableStateOf(false) }
    // Incremented on every tap so tapping the same spot twice still restarts the timers.
    var tapCount by remember { mutableIntStateOf(0) }

    // Fix 5: EV (Exposure Value) compensation state
    var evIndex by remember { mutableIntStateOf(0) }
    var showEvSlider by remember { mutableStateOf(false) }
    // Incremented while the user drags the EV slider so it does not hide mid-drag.
    var evInteraction by remember { mutableIntStateOf(0) }


    DisposableEffect(Unit) {
        viewModel.startSensors()
        onDispose { viewModel.stopSensors() }
    }

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Keep the capture rotation in sync with the display. The activity is locked to
    // sensor-landscape, so flipping the phone 180° changes the display rotation without
    // a configuration change; without this, photos taken that way come out upside down.
    DisposableEffect(Unit) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        fun syncRotation() {
            previewView.display?.let { imageCapture.targetRotation = it.rotation }
        }
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == previewView.display?.displayId) syncRotation()
            }
        }
        displayManager.registerDisplayListener(listener, null)
        syncRotation()
        onDispose { displayManager.unregisterDisplayListener(listener) }
    }

    LaunchedEffect(cameraGranted) {
        if (!cameraGranted) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: Exception) {
                e.printStackTrace()
                cameraError = "Camera failed to start"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(cameraProvider) {
        val provider = cameraProvider ?: return@LaunchedEffect
        try {
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            exc.printStackTrace()
            cameraError = "Camera failed to start"
            camera = null
        }
    }

    // As soon as the camera is bound, cancel any locked focus so the sensor
    // enters its native Continuous Auto-Focus (CAAF) mode automatically.
    // This prevents blurry shots caused by the camera staying focused at
    // the wrong distance if the user never taps the viewfinder.
    LaunchedEffect(camera) {
        camera?.cameraControl?.cancelFocusAndMetering()
    }

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            showFocusRing = true
            delay(1000)
            showFocusRing = false
        }
    }

    // EV slider stays up for 3 s after the last tap or drag on it.
    LaunchedEffect(tapCount, evInteraction) {
        if (tapCount > 0) {
            showEvSlider = true
            delay(3000)
            showEvSlider = false
        }
    }

    LaunchedEffect(camera, isFlashOn) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    // Fix 6 — Level-snap haptic: fire once on the false→true transition
    // Collected directly (not via collectAsState) so level changes never recompose this screen.
    LaunchedEffect(Unit) {
        var wasAtLevel = false
        viewModel.isAtLevel.collect { atLevel ->
            if (atLevel && !wasAtLevel) HapticManager.levelSnap(context)
            wasAtLevel = atLevel
        }
    }

    // Fix 6 — Focus-lock haptic: fire once when the focus ring first appears
    LaunchedEffect(showFocusRing) {
        if (showFocusRing) {
            HapticManager.focusLock(context)
        }
    }

    if (cameraGranted) {
        if (cameraError != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text(text = cameraError!!, color = Color.White)
            }
            return
        }

        if (cameraProvider == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Amber)
            }
            return
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camera, pendingPhoto) {
                        if (pendingPhoto != null) return@pointerInput
                        detectTapGestures(
                            onTap = { offset ->
                                focusOffset = offset
                                tapCount++
                                camera?.let { cam ->
                                    val factory = previewView.meteringPointFactory
                                    val point = factory.createPoint(offset.x, offset.y)
                                    // Fix 4: add FLAG_AWB so focus, exposure AND white balance
                                    // all lock together on the tapped rail/surface point.
                                    // Fix 5: FLAG_AE acts as spot metering — automatically dims
                                    // exposure when the user taps a bright/shiny surface.
                                    val action = FocusMeteringAction.Builder(
                                        point,
                                        FocusMeteringAction.FLAG_AF or
                                            FocusMeteringAction.FLAG_AE or
                                            FocusMeteringAction.FLAG_AWB
                                    ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                    cam.cameraControl.startFocusAndMetering(action)

                                    // Fix 5: apply current EV index on every new tap
                                    cam.cameraControl.setExposureCompensationIndex(evIndex)
                                }
                            }
                        )
                    }
                    .pointerInput(camera, pendingPhoto) {
                        if (pendingPhoto != null) return@pointerInput
                        detectTransformGestures { _, _, zoomMultiplier, _ ->
                            camera?.let { cam ->
                                val zoomState = cam.cameraInfo.zoomState.value
                                val currentZoom = zoomState?.zoomRatio ?: 1f
                                val minZoom = zoomState?.minZoomRatio ?: 1f
                                val maxZoom = zoomState?.maxZoomRatio ?: currentZoom
                                val newZoom = (currentZoom * zoomMultiplier).coerceIn(minZoom, maxZoom)
                                cam.cameraControl.setZoomRatio(newZoom)
                            }
                        }
                    }
            )

            // Focus ring: animations live outside the `if` so they actually run
            // (shrinks 1.3→1 and fades in on tap, fades out when hidden).
            val animScale by animateFloatAsState(
                targetValue = if (showFocusRing) 1f else 1.3f,
                animationSpec = tween(200),
                label = "ring_scale"
            )
            val animAlpha by animateFloatAsState(
                targetValue = if (showFocusRing) 1f else 0f,
                animationSpec = tween(200),
                label = "ring_alpha"
            )
            if (animAlpha > 0f && focusOffset != null) {

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                focusOffset!!.x.toInt() - 32.dp.roundToPx(),
                                focusOffset!!.y.toInt() - 32.dp.roundToPx()
                            )
                        }
                        .size(64.dp)
                        .scale(animScale)
                        .alpha(animAlpha)
                        .border(2.dp, Amber, CircleShape)
                )
            }

            // Fix 5: Transient EV slider — appears next to the focus ring after a tap
            focusOffset?.let { tap ->
                val evRange = camera?.cameraInfo?.exposureState?.exposureCompensationRange
                    ?.let { it.lower..it.upper } ?: (-4..4)

                ExposureSlider(
                    visible = showEvSlider,
                    evIndex = evIndex,
                    evRange = evRange,
                    anchorX = tap.x,
                    anchorY = tap.y,
                    onEvChanged = { newIndex ->
                        evIndex = newIndex
                        camera?.cameraControl?.setExposureCompensationIndex(newIndex)
                    },
                    onInteraction = { evInteraction++ }
                )
            }

            DarkMask()
            RailGuideOverlay()

            // Perf Fix A: Pass getTilt as a lambda so only the LevelIndicator Canvas
            // recomposes on each sensor tick (~30 Hz), not the entire CaptureScreen tree.
            LevelIndicator(getTilt = { tiltAngle })

            // Flash toggle button — top-left corner of the camera view
            if (pendingPhoto == null) {
                FlashButton(
                    isFlashOn = isFlashOn,
                    onToggle = { viewModel.toggleFlash() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 20.dp)
                )
            }

            if (pendingPhoto == null) {
                ShutterButton(
                    onClick = {
                        if (isInstantMode && !storagePermissionGranted) {
                            Toast.makeText(context, "Storage permission is needed to save photos", Toast.LENGTH_SHORT).show()
                            permissionsState.launchMultiplePermissionRequest()
                            return@ShutterButton
                        }
                        if (!viewModel.beginCapture()) return@ShutterButton

                        val capturedTilt = tiltAngle
                        coroutineScope.launch {
                            showFlash = true
                            delay(50)
                            showFlash = false
                        }

                        val executor = ContextCompat.getMainExecutor(context)
                        imageCapture.takePicture(
                            executor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    if (isInstantMode) {
                                        viewModel.processPhotoInstant(context, imageProxy, capturedTilt)
                                    } else {
                                        viewModel.processPhoto(context, imageProxy, capturedTilt)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    exception.printStackTrace()
                                    viewModel.onCaptureFailed()
                                }
                            }
                        )
                    },
                    enabled = !isProcessingCapture,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 32.dp)
                )
            }

            if (flashAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashAlpha))
                )
            }

            // Fix 3 (already correct — camera never unbound during review):
            // ReviewOverlay is a Compose overlay on top of the live PreviewView.
            // Perf Fix D: pass previewBitmap so image appears from memory instantly.
            pendingPhoto?.let { photo ->
                ReviewOverlay(
                    photoUri = photo.uri,
                    previewBitmap = photo.previewBitmap,
                    isSaving = isSaving,
                    onRetake = { viewModel.clearPendingPhoto() },
                    onKeep = {
                        if (!storagePermissionGranted) {
                            Toast.makeText(context, "Storage permission is needed to save photos", Toast.LENGTH_SHORT).show()
                            permissionsState.launchMultiplePermissionRequest()
                            return@ReviewOverlay
                        }
                        viewModel.savePhoto(context) {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Camera permission is required.", color = Color.White)
        }
    }
}
