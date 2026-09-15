package com.rishavmishra010.railcam.ui.components.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun DarkMask() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Fill entire screen with semi-transparent black
        drawRect(color = Color(0xD9000000))

        // Calculate 2:1 aspect ratio dimensions (Width is 2x Height)
        // Taking 80% of the screen width for the cutout width
        val cutoutWidth = canvasWidth * 0.8f
        val cutoutHeight = cutoutWidth / 2f

        val left = (canvasWidth - cutoutWidth) / 2f
        val top = (canvasHeight - cutoutHeight) / 2f

        // 'Punch a hole' in the dark mask using BlendMode.Clear
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(cutoutWidth, cutoutHeight),
            blendMode = BlendMode.Clear
        )
    }
}
