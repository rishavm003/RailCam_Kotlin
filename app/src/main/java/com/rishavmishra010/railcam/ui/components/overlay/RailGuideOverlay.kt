package com.rishavmishra010.railcam.ui.components.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.rishavmishra010.railcam.ui.theme.Amber
import com.rishavmishra010.railcam.ui.theme.AmberLight

@Composable
fun RailGuideOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Calculate 2:1 aspect ratio dimensions (matching DarkMask geometry)
        val cutoutWidth = canvasWidth * 0.8f
        val cutoutHeight = cutoutWidth / 2f

        val left = (canvasWidth - cutoutWidth) / 2f
        val top = (canvasHeight - cutoutHeight) / 2f
        val right = left + cutoutWidth
        val bottom = top + cutoutHeight
        val centerY = canvasHeight / 2f

        // 1. Draw the Center Alignment Line (Dashed subtle amber line across the center width)
        val dashLength = 10.dp.toPx()
        val spaceLength = 6.dp.toPx()
        var currentX = left + 16.dp.toPx() // Start slightly inside the cutout
        val endX = right - 16.dp.toPx()

        while (currentX < endX) {
            drawLine(
                color = AmberLight.copy(alpha = 0.6f),
                start = Offset(currentX, centerY),
                end = Offset((currentX + dashLength).coerceAtMost(endX), centerY),
                strokeWidth = 1.5.dp.toPx()
            )
            currentX += dashLength + spaceLength
        }

        // 2. Draw tactical Corner Brackets (Thick solid Amber lines)
        val bracketLength = 24.dp.toPx()
        val strokeWidthPx = 3.dp.toPx()

        // Top-Left Corner
        val topLeftPath = Path().apply {
            moveTo(left, top + bracketLength)
            lineTo(left, top)
            lineTo(left + bracketLength, top)
        }
        drawPath(path = topLeftPath, color = Amber, style = Stroke(width = strokeWidthPx))

        // Top-Right Corner
        val topRightPath = Path().apply {
            moveTo(right, top + bracketLength)
            lineTo(right, top)
            lineTo(right - bracketLength, top)
        }
        drawPath(path = topRightPath, color = Amber, style = Stroke(width = strokeWidthPx))

        // Bottom-Left Corner
        val bottomLeftPath = Path().apply {
            moveTo(left, bottom - bracketLength)
            lineTo(left, bottom)
            lineTo(left + bracketLength, bottom)
        }
        drawPath(path = bottomLeftPath, color = Amber, style = Stroke(width = strokeWidthPx))

        // Bottom-Right Corner
        val bottomRightPath = Path().apply {
            moveTo(right, bottom - bracketLength)
            lineTo(right, bottom)
            lineTo(right - bracketLength, bottom)
        }
        drawPath(path = bottomRightPath, color = Amber, style = Stroke(width = strokeWidthPx))
    }
}
