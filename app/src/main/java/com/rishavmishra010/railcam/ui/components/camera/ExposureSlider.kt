package com.rishavmishra010.railcam.ui.components.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishavmishra010.railcam.ui.theme.Amber
import kotlin.math.roundToInt

/**
 * Transient EV (Exposure Value) compensation slider — Fix 5: Anti-Glare Control.
 *
 * Appears at [anchorX] / [anchorY] (the tap-to-focus point) for 3 seconds after
 * any tap, then fades out automatically. The user drags up/down to adjust brightness.
 *
 * @param visible       Whether the slider should be shown (driven by focus ring state).
 * @param evIndex       Current EV compensation index (0 = no adjustment).
 * @param evRange       Valid EV index range from [androidx.camera.core.ExposureState].
 * @param anchorX       X pixel offset of the tap point (positions slider next to ring).
 * @param anchorY       Y pixel offset of the tap point.
 * @param onEvChanged   Called with the new EV index as the user drags.
 */
@Composable
fun ExposureSlider(
    visible: Boolean,
    evIndex: Int,
    evRange: IntRange,
    anchorX: Float,
    anchorY: Float,
    onEvChanged: (Int) -> Unit,
    onInteraction: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val sliderHeightDp = 120
        val sliderWidthDp = 36

        // Position the slider to the right of the focus ring (offset by 48 dp), vertically
        // centred on the tap. anchorX/anchorY are pixels, so the dp values are converted.
        val density = LocalDensity.current
        val xOffsetPx = with(density) { (anchorX + 48.dp.toPx()).roundToInt() }
        val yOffsetPx = with(density) { (anchorY - (sliderHeightDp / 2).dp.toPx()).roundToInt() }

        // The drag handler below lives as long as evRange is unchanged, so read the latest
        // values through rememberUpdatedState (otherwise a second drag restarts from the
        // EV value the slider had when it first appeared).
        val currentEvIndex by rememberUpdatedState(evIndex)
        val currentOnEvChanged by rememberUpdatedState(onEvChanged)
        val currentOnInteraction by rememberUpdatedState(onInteraction)

        var accumulatedDragY by remember { mutableStateOf(0f) }
        var initialEvIndex by remember { mutableIntStateOf(evIndex) }

        Box(
            modifier = Modifier
                .offset { IntOffset(xOffsetPx, yOffsetPx) }
                .width(sliderWidthDp.dp)
                .height(sliderHeightDp.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .pointerInput(evRange) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            accumulatedDragY = 0f
                            initialEvIndex = currentEvIndex
                            currentOnInteraction()
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            currentOnInteraction()
                            accumulatedDragY += dragAmount
                            // Dragging up (negative dragAmount) increases EV (brighter);
                            // dragging down decreases EV (darker). Scale: 1 step per ~15 px.
                            val delta = -(accumulatedDragY / 15f).roundToInt()
                            val newIndex = (initialEvIndex + delta).coerceIn(evRange.first, evRange.last)
                            if (newIndex != currentEvIndex) {
                                currentOnEvChanged(newIndex)
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Sun icon substitute — compact EV label
                Text(
                    text = "☀",
                    color = Amber,
                    fontSize = 14.sp
                )

                // EV value indicator dot — vertical position shows current setting
                val trackRange = evRange.last - evRange.first
                val normalised = if (trackRange > 0) {
                    (evIndex - evRange.first).toFloat() / trackRange
                } else 0.5f

                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .offset(x = (-4).dp, y = ((1f - normalised) * 50).dp)
                            .clip(CircleShape)
                            .background(Amber)
                    )
                }

                // Numeric EV readout
                val sign = if (evIndex > 0) "+" else ""
                Text(
                    text = "$sign$evIndex",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
