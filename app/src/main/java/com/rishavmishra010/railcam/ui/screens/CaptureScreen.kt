package com.rishavmishra010.railcam.ui.screens

import android.Manifest
import android.content.Context
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
import com.google.accompanist.permissions.rememberPermissionState
import com.rishavmishra010.railcam.ui.components.camera.FlashButton
import com.rishavmishra010.railcam.ui.components.camera.ReviewOverlay
import com.rishavmishra010.railcam.ui.components.camera.ShutterButton
import com.rishavmishra010.railcam.ui.components.overlay.DarkMask
import com.rishavmishra010.railcam.ui.components.overlay.LevelIndicator
import com.rishavmishra010.railcam.ui.components.overlay.RailGuideOverlay
import com.rishavmishra010.railcam.ui.theme.Amber
import com.rishavmishra010.railcam.viewmodel.CaptureViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
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
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val tiltAngle by viewModel.tiltAngle.collectAsState()
    val pendingPhoto by viewModel.pendingPhoto.collectAsState()
    val isProcessingCapture by viewModel.isProcessingCapture.collectAsState()
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
        // ResolutionSelector: no fixed aspect ratio restriction so the sensor's maximum
        // native output size is selected, then we crop in software to 2:1.
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            .build()

        ImageCapture.Builder()
            // MAXIMIZE_QUALITY: sensor captures at full resolution and applies
            // post-processing for maximum sharpness (equivalent to qualityPrioritization:'quality')
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setJpegQuality(98)
            .build()
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    var focusOffset by remember { mutableStateOf<Offset?>(null) }
    var showFocusRing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.startSensors()
        onDispose { viewModel.stopSensors() }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (!cameraPermissionState.status.isGranted) return@LaunchedEffect

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

    LaunchedEffect(focusOffset) {
        if (focusOffset != null) {
            showFocusRing = true
            delay(1000)
            showFocusRing = false
        }
    }

    LaunchedEffect(camera, isFlashOn) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    if (cameraPermissionState.status.isGranted) {
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
                                camera?.let { cam ->
                                    val factory = previewView.meteringPointFactory
                                    val point = factory.createPoint(offset.x, offset.y)
                                    val action = FocusMeteringAction.Builder(
                                        point,
                                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                    ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                    cam.cameraControl.startFocusAndMetering(action)
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

            if (showFocusRing && focusOffset != null) {
                val animScale by animateFloatAsState(
                    targetValue = if (showFocusRing) 1.2f else 1f,
                    animationSpec = tween(200),
                    label = "ring_scale"
                )
                val animAlpha by animateFloatAsState(
                    targetValue = if (showFocusRing) 1f else 0f,
                    animationSpec = tween(200),
                    label = "ring_alpha"
                )

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

            DarkMask()
            RailGuideOverlay()
            LevelIndicator(tiltAngle = tiltAngle)

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

            pendingPhoto?.let { photo ->
                ReviewOverlay(
                    photoUri = photo.uri,
                    onRetake = { viewModel.clearPendingPhoto() },
                    onKeep = {
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
