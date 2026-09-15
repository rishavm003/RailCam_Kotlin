package com.rishavmishra010.railcam.ui.components.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rishavmishra010.railcam.ui.theme.Amber
import com.rishavmishra010.railcam.ui.theme.OverlayDark

@Composable
fun ReviewOverlay(
    photoUri: String,
    onRetake: () -> Unit,
    onKeep: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayDark)
    ) {
        // Display the cropped 2:1 image in the exact center
        AsyncImage(
            model = photoUri,
            contentDescription = "Review Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.8f) // Matches the 80% width from the DarkMask cutout
                .aspectRatio(2f) // Forces the 2:1 ratio container
                .align(Alignment.Center)
        )

        // Action Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth(0.6f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onRetake,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Retake", color = Color.White, fontSize = 16.sp)
            }

            Button(
                onClick = onKeep,
                colors = ButtonDefaults.buttonColors(containerColor = Amber)
            ) {
                Text("Keep", color = Color.Black, fontSize = 16.sp)
            }
        }
    }
}
