package com.rishavmishra010.railcam.ui.components.camera

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import com.rishavmishra010.railcam.ui.theme.Amber
import com.rishavmishra010.railcam.ui.theme.OverlayDark

/**
 * Full-screen overlay shown after capture so the user can keep or retake the photo.
 *
 * @param photoUri       Temp file path — used as a fallback if [previewBitmap] is null.
 * @param previewBitmap  The already-cropped [Bitmap] held in memory by PendingPhoto.
 *                       When non-null, the image is rendered instantly without any disk
 *                       I/O or JPEG decode (Perf Fix D).
 */
@Composable
fun ReviewOverlay(
    photoUri: String,
    previewBitmap: Bitmap? = null,
    isSaving: Boolean = false,
    onRetake: () -> Unit,
    onKeep: () -> Unit
) {
    val imageBitmap = remember(previewBitmap) { previewBitmap?.asImageBitmap() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Show image: prefer in-memory bitmap (instant, no disk I/O), fall back to URI via Coil
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Captured photo preview",
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
        } else {
            AsyncImage(
                model = photoUri,
                contentDescription = "Captured photo preview",
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
        }

        // Bottom controls bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(vertical = 24.dp, horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onRetake,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E3B))
            ) {
                Text("Retake", color = Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }

            Button(
                onClick = onKeep,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber)
            ) {
                Text("Keep", color = Color.Black, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}
