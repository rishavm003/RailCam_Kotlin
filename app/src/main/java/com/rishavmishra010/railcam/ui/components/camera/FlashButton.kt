package com.rishavmishra010.railcam.ui.components.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rishavmishra010.railcam.R
import com.rishavmishra010.railcam.ui.theme.Amber

@Composable
fun FlashButton(
    isFlashOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "flash_scale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isFlashOn) Amber.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(durationMillis = 200),
        label = "flash_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFlashOn) Amber else Color.White.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 200),
        label = "flash_border"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isFlashOn) Amber else Color.White,
        animationSpec = tween(durationMillis = 200),
        label = "flash_icon_tint"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                id = if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            ),
            contentDescription = if (isFlashOn) "Flash On" else "Flash Off",
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}
