package com.rishavmishra010.railcam.ui.components.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishavmishra010.railcam.ui.theme.Amber
import com.rishavmishra010.railcam.ui.theme.SurfaceHigh
import com.rishavmishra010.railcam.ui.theme.TextPrimary

@Composable
fun InstantModeToggle(
    isInstantMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isInstantMode) Amber else SurfaceHigh)
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (isInstantMode) "⚡ Instant: ON" else "📷 Review: ON",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
