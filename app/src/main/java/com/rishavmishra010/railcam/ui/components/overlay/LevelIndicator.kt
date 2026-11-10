package com.rishavmishra010.railcam.ui.components.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishavmishra010.railcam.ui.theme.AlertRed
import com.rishavmishra010.railcam.ui.theme.LevelGreen
import com.rishavmishra010.railcam.ui.theme.LevelYellow
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private fun tiltIndicatorColor(tiltAngle: Float): Color {
    val absTilt = abs(tiltAngle)
    return when {
        // Upright (0° to ±5°) & Upside Down (±175° to ±180°) -> Green
        absTilt <= 5f || absTilt >= 175f -> LevelGreen
        // Upright warning (±5° to ±15°) & Upside Down warning (±165° to ±175°) -> Yellow
        absTilt <= 15f || absTilt >= 165f -> LevelYellow
        // All other angles (±15° to ±165°) -> Red
        else -> AlertRed
    }
}

/**
 * Live tilt angle as a rotating horizon line + numeric readout.
 *
 * The tilt is read through [getTilt] in places that keep sensor updates cheap:
 *  - the line is rotated inside the Canvas draw block, so each sensor tick only
 *    redraws (no recomposition or layout);
 *  - the text and colour go through derivedStateOf, so they recompose only when the
 *    displayed value (0.1° steps) or the colour band actually changes.
 */
@Composable
fun LevelIndicator(getTilt: () -> Float) {
    val tenths by remember { derivedStateOf { (getTilt() * 10f).roundToInt() } }
    val indicatorColor by remember { derivedStateOf { tiltIndicatorColor(getTilt()) } }
    val label = remember(tenths) { String.format(Locale.US, "%.1f°", tenths / 10f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = label,
            color = indicatorColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val tilt = getTilt()
            val lineLength = 160.dp.toPx()
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            rotate(degrees = tilt) {
                drawLine(
                    color = tiltIndicatorColor(tilt),
                    start = Offset(centerX - lineLength / 2f, centerY),
                    end = Offset(centerX + lineLength / 2f, centerY),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
    }
}
