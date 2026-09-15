package com.rishavmishra010.railcam.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rishavmishra010.railcam.R
import com.rishavmishra010.railcam.navigation.Screen
import com.rishavmishra010.railcam.ui.components.camera.InstantModeToggle
import com.rishavmishra010.railcam.ui.components.forms.MetadataForm
import com.rishavmishra010.railcam.ui.theme.LevelGreen
import com.rishavmishra010.railcam.util.FilenameGenerator
import com.rishavmishra010.railcam.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val metadataState = viewModel.metadata.collectAsState()
    val metadata = metadataState.value
    val themeMode by viewModel.themeMode.collectAsState()
    val isInstantMode by viewModel.isInstantMode.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val validationError = FilenameGenerator.validate(metadata)
    val isValid = validationError == null
    
    val generatedFilename by remember {
        androidx.compose.runtime.derivedStateOf {
            val currentMetadata = metadataState.value
            if (FilenameGenerator.validate(currentMetadata) != null) return@derivedStateOf null
            FilenameGenerator.generate(currentMetadata, System.currentTimeMillis())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_railcam_logo),
                    contentDescription = "RailCam logo",
                    modifier = Modifier.size(52.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "RailCam",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rail grinding field capture",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                Icon(
                    imageVector = if (themeMode == "dark") Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Session details",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                MetadataForm(
                    values = metadata,
                    onClientChange = viewModel::updateClient,
                    onZoneOrSubClientChange = viewModel::updateZoneOrSubClient,
                    onLineDetailsChange = viewModel::updateLineDetails,
                    onTestSiteNameChange = viewModel::updateTestSiteName,
                    onStationCodeChange = viewModel::updateStationCode,
                    onKmMarkerChange = viewModel::updateKmMarker,
                    onOheNumberChange = viewModel::updateOheNumber,
                    onRailTypeChange = viewModel::updateRailType,
                    onRailSideChange = viewModel::updateRailSide,
                    onDirectionChange = viewModel::updateDirection,

                    onRailPositionChange = viewModel::updateRailPosition,
                    onSessionDateChange = viewModel::updateSessionDate
                )
            }
        }

        StatusChip(
            message = validationError ?: "Ready to capture",
            isValid = isValid
        )

        if (generatedFilename != null) {
            Text(
                text = "Filename: $generatedFilename",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { navController.navigate(Screen.Gallery.route) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("Gallery", fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    val finalFilename = generatedFilename ?: FilenameGenerator.generate(metadata, System.currentTimeMillis())
                    coroutineScope.launch {
                        viewModel.prepareCaptureSession()
                        navController.navigate(Screen.Capture.route)
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Start Camera", fontWeight = FontWeight.SemiBold)
            }
        }

        InstantModeToggle(
            isInstantMode = isInstantMode,
            onToggle = { viewModel.toggleInstantMode() },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StatusChip(message: String, isValid: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isValid) Color(0xFF1A2E1A) else Color(0xFF2E1A1A))
            .border(
                1.dp,
                if (isValid) LevelGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (isValid) LevelGreen else MaterialTheme.colorScheme.error,
                    CircleShape
                )
        )
        Text(
            text = message,
            color = if (isValid) LevelGreen else MaterialTheme.colorScheme.error,
            fontSize = 13.sp
        )
    }
}
