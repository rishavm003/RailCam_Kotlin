package com.rishavmishra010.railcam.ui.components.forms

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishavmishra010.railcam.domain.model.Client
import com.rishavmishra010.railcam.domain.model.Direction
import com.rishavmishra010.railcam.domain.model.DfccilCorridor
import com.rishavmishra010.railcam.domain.model.MetadataValues
import com.rishavmishra010.railcam.domain.model.MetroNetwork
import com.rishavmishra010.railcam.domain.model.RailPosition
import com.rishavmishra010.railcam.domain.model.RailSide
import com.rishavmishra010.railcam.domain.model.RailType
import com.rishavmishra010.railcam.domain.model.RailwayZones
import androidx.compose.material3.MaterialTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fieldShape = RoundedCornerShape(12.dp)
private val segmentHeight = 48.dp

@Composable
fun MetadataForm(
    values: MetadataValues,
    onClientChange: (Client) -> Unit,
    onZoneOrSubClientChange: (String) -> Unit,
    onLineDetailsChange: (String) -> Unit,
    onTestSiteNameChange: (String) -> Unit,
    onStationCodeChange: (String) -> Unit,
    onKmMarkerChange: (String) -> Unit,
    onOheNumberChange: (String) -> Unit,
    onRailTypeChange: (RailType) -> Unit,
    onRailSideChange: (RailSide) -> Unit,
    onDirectionChange: (Direction) -> Unit,

    onRailPositionChange: (RailPosition) -> Unit,
    onSessionDateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ClientSelection(
            selected = values.client,
            onSelected = onClientChange
        )

        ZoneSubClientSelection(
            client = values.client,
            selected = values.zoneOrSubClient,
            onSelected = onZoneOrSubClientChange
        )

        LineDetailsInput(
            visible = values.client == Client.METRO,
            value = values.lineDetails,
            onValueChange = onLineDetailsChange
        )

        TestSiteNameInput(
            value = values.testSiteName,
            onValueChange = onTestSiteNameChange
        )

        StationCodeInput(
            value = values.stationCode,
            onValueChange = onStationCodeChange
        )

        DirectionSelection(
            selected = values.direction,
            onSelected = onDirectionChange
        )

        LocationInputs(
            oheNumber = values.oheNumber,
            onOheNumberChange = onOheNumberChange
        )

        RailTypeSelection(
            selected = values.railType,
            onSelected = onRailTypeChange
        )

        RailNameInput(
            railType = values.railType,
            value = values.kmMarker,
            onValueChange = onKmMarkerChange
        )

        TangentRailDetails(
            visible = values.railType == RailType.TANGENT,
            railSide = values.railSide,
            onRailSideChange = onRailSideChange
        )

        CurveRailDetails(
            visible = values.railType == RailType.CURVE,
            railPosition = values.railPosition,
            onRailPositionChange = onRailPositionChange
        )

        SessionDateField(
            sessionDate = values.sessionDate,
            onSessionDateChange = onSessionDateChange
        )
    }
}

@Composable
private fun ClientSelection(
    selected: Client,
    onSelected: (Client) -> Unit
) {
    SegmentedControl(
        label = "Client",
        options = Client.entries.map { it.displayName },
        selected = selected.displayName,
        onSelected = { label ->
            Client.entries.firstOrNull { it.displayName == label }?.let(onSelected)
        }
    )
}

@Composable
private fun ZoneSubClientSelection(
    client: Client,
    selected: String,
    onSelected: (String) -> Unit
) {
    AnimatedContent(
        targetState = client,
        transitionSpec = { fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically() },
        label = "zone_subclient"
    ) { currentClient ->
        when (currentClient) {
            Client.INDIAN_RAILWAY -> {
                MetadataDropdown(
                    label = "Select Zone",
                    options = RailwayZones.all,
                    selected = selected,
                    onSelected = onSelected
                )
            }

            Client.DFCCIL -> {
                SegmentedControl(
                    label = "Corridor",
                    options = DfccilCorridor.entries.map { it.displayName },
                    selected = DfccilCorridor.entries.firstOrNull { it.name == selected }?.displayName
                        ?: DfccilCorridor.EDFC.displayName,
                    onSelected = { label ->
                        DfccilCorridor.entries.firstOrNull { it.displayName == label }?.let {
                            onSelected(it.name)
                        }
                    }
                )
            }

            Client.METRO -> {
                MetadataDropdown(
                    label = "Metro Network",
                    options = MetroNetwork.entries.map { it.displayName },
                    selected = MetroNetwork.entries.firstOrNull { it.fileCode == selected }?.displayName
                        ?: MetroNetwork.DMRC.displayName,
                    onSelected = { label ->
                        MetroNetwork.entries.firstOrNull { it.displayName == label }?.let {
                            onSelected(it.fileCode)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TestSiteNameInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    var textInput by remember(value) { mutableStateOf(value) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel("Test Site Name")
        OutlinedTextField(
            value = textInput,
            onValueChange = {
                textInput = it
                onValueChange(it)
            },
            placeholder = { Text("Enter test site name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            shape = fieldShape,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}

@Composable
private fun StationCodeInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    var textInput by remember(value) { mutableStateOf(value) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel("Station Code")
        OutlinedTextField(
            value = textInput,
            onValueChange = {
                textInput = it
                onValueChange(it)
            },
            placeholder = { Text("Enter station code", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            shape = fieldShape,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}

@Composable
private fun LineDetailsInput(
    visible: Boolean,
    value: String,
    onValueChange: (String) -> Unit
) {
    AnimatedVisibilityBlock(visible = visible) {
        var textInput by remember(value) { mutableStateOf(value) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormFieldLabel("Line Details")
            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    onValueChange(it)
                },
                placeholder = { Text("e.g. Red Line or Line 1", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
}

@Composable
private fun LocationInputs(
    oheNumber: String,
    onOheNumberChange: (String) -> Unit
) {
    var oheInput by remember(oheNumber) { mutableStateOf(oheNumber) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FormFieldLabel("OHE Details")
        OutlinedTextField(
            value = oheInput,
            onValueChange = {
                oheInput = it
                onOheNumberChange(it)
            },
            placeholder = { Text("Enter OHE structure/mast number", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            shape = fieldShape,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}

@Composable
private fun RailNameInput(
    railType: RailType,
    value: String,
    onValueChange: (String) -> Unit
) {
    var textInput by remember(value) { mutableStateOf(value) }
    val label = if (railType == RailType.TANGENT) "Tangent Name" else "Curve Name"
    val placeholder = if (railType == RailType.TANGENT) "e.g. TAN-01" else "e.g. CUR-02"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(label)
        OutlinedTextField(
            value = textInput,
            onValueChange = {
                textInput = it
                onValueChange(it)
            },
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            shape = fieldShape,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}

@Composable
private fun RailTypeSelection(
    selected: RailType,
    onSelected: (RailType) -> Unit
) {
    SegmentedControl(
        label = "Rail Type",
        options = RailType.entries.map { it.displayName },
        selected = selected.displayName,
        onSelected = { label ->
            RailType.entries.firstOrNull { it.displayName == label }?.let(onSelected)
        }
    )
}

@Composable
private fun DirectionSelection(
    selected: Direction,
    onSelected: (Direction) -> Unit
) {
    SegmentedControl(
        label = "Direction",
        options = Direction.entries.map { it.name },
        selected = selected.name,
        onSelected = { name -> onSelected(Direction.valueOf(name)) }
    )
}

@Composable
private fun TangentRailDetails(
    visible: Boolean,
    railSide: RailSide,
    onRailSideChange: (RailSide) -> Unit
) {
    AnimatedVisibilityBlock(visible = visible) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SegmentedControl(
                label = "Rail Side",
                options = RailSide.entries.map { it.name },
                selected = railSide.name,
                onSelected = { name -> onRailSideChange(RailSide.valueOf(name)) }
            )
        }
    }
}

@Composable
private fun CurveRailDetails(
    visible: Boolean,
    railPosition: RailPosition,
    onRailPositionChange: (RailPosition) -> Unit
) {
    AnimatedVisibilityBlock(visible = visible) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SegmentedControl(
                label = "Rail Position",
                options = RailPosition.entries.map { it.displayName },
                selected = railPosition.displayName,
                onSelected = { label ->
                    RailPosition.entries.firstOrNull { it.displayName == label }?.let(onRailPositionChange)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDateField(
    sessionDate: String,
    onSessionDateChange: (String) -> Unit
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel("Date")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePickerDialog = true }
        ) {
            OutlinedTextField(
                value = sessionDate,
                onValueChange = onSessionDateChange,
                enabled = false,
                placeholder = { Text("DD-MM-YYYY", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }

    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePickerDialog = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        onSessionDateChange(sdf.format(Date(millis)))
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = if (label == "Select Zone") selected else selected

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(label)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = if (label == "Metro Network") {
                    MetroNetwork.entries.firstOrNull { it.fileCode == selected }?.displayName ?: selected
                } else {
                    displayValue
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = fieldShape,
                colors = fieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(56.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(
                                if (label == "Metro Network") {
                                    MetroNetwork.entries.firstOrNull { it.displayName == option }?.fileCode
                                        ?: option
                                } else {
                                    option
                                }
                            )
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityBlock(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        content()
    }
}

@Composable
private fun FormFieldLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SegmentedControl(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(label)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(segmentHeight)
                .clip(fieldShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, fieldShape)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (option == "IR") FontWeight.Bold else (if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                        fontSize = if (option.length > 8) 12.sp else 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary
)
