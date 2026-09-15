package com.rishavmishra010.railcam.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.rishavmishra010.railcam.data.db.PhotoRecord
import com.rishavmishra010.railcam.ui.theme.Amber
import com.rishavmishra010.railcam.ui.theme.Background
import com.rishavmishra010.railcam.ui.theme.OverlayDark
import com.rishavmishra010.railcam.viewmodel.PreviewViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    photoId: String,
    navController: NavController,
    viewModel: PreviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val photo by viewModel.photo.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(photoId) {
        photoId.toLongOrNull()?.let { id ->
            viewModel.loadPhoto(id)
        }
    }

    if (photo == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val currentPhoto = photo!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentPhoto.filename, fontSize = 14.sp, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val mediaUri = Uri.parse(currentPhoto.uri)
                        // Retrieve the raw file path from MediaStore and share via FileProvider
                        // to preserve the descriptive filename (e.g. in WhatsApp)
                        val sharedUri = runCatching {
                            val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
                            context.contentResolver.query(mediaUri, projection, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
                                    val path = cursor.getString(columnIndex)
                                    if (!path.isNullOrBlank()) {
                                        val file = java.io.File(path)
                                        if (file.exists()) {
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                file
                                            )
                                        } else null
                                    } else null
                                } else null
                             }
                        }.getOrNull() ?: mediaUri

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/octet-stream" // Share as a raw document/file to preserve filename in WhatsApp
                            putExtra(Intent.EXTRA_STREAM, sharedUri)
                            putExtra(Intent.EXTRA_TITLE, currentPhoto.filename)
                            clipData = android.content.ClipData.newRawUri(null, sharedUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Photo as Document"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OverlayDark,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            AsyncImage(
                model = currentPhoto.uri,
                contentDescription = "Full Screen Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(OverlayDark)
                    .padding(16.dp)
            ) {
                val dateString = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(Date(currentPhoto.timestamp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${currentPhoto.client} • ${currentPhoto.zoneOrSubClient}",
                        color = Amber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(dateString, color = Color.LightGray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val label = if (currentPhoto.railType.equals("TANGENT", ignoreCase = true)) "Tangent" else "Curve"
                    Text(
                        text = "$label: ${currentPhoto.kmMarker} | OHE: ${currentPhoto.oheNumber}",
                        color = Color.White
                    )
                    Text(
                        text = "Tilt: ${String.format(Locale.getDefault(), "%.1f°", currentPhoto.tiltAngle)}",
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildPhotoDetailLine(currentPhoto),
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Photo?") },
            text = { Text("This will permanently remove the photo from your device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deletePhoto { navController.popBackStack() }
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color.DarkGray,
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }
}

private fun buildPhotoDetailLine(photo: PhotoRecord): String {
    val details = mutableListOf<String>()
    if (photo.railSide.isNotBlank()) details.add(photo.railSide)
    if (photo.direction.isNotBlank()) details.add(photo.direction)

    if (photo.railPosition.isNotBlank()) details.add(photo.railPosition)
    return details.joinToString(" | ")
}
