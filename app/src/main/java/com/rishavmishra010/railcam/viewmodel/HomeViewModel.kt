package com.rishavmishra010.railcam.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rishavmishra010.railcam.data.datastore.MetadataPreferences
import com.rishavmishra010.railcam.domain.model.CaptureSessionMetadata
import com.rishavmishra010.railcam.domain.model.Client
import com.rishavmishra010.railcam.domain.model.Direction
import com.rishavmishra010.railcam.domain.model.MetadataValues
import com.rishavmishra010.railcam.domain.model.RailPosition
import com.rishavmishra010.railcam.domain.model.RailSide
import com.rishavmishra010.railcam.domain.model.RailType
import com.rishavmishra010.railcam.domain.model.defaultZoneOrSubClient
import com.rishavmishra010.railcam.domain.model.todaySessionDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = MetadataPreferences(application)
    private var pendingWrite: Job? = null

    val metadata: StateFlow<MetadataValues> = preferences.metadataFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetadataValues(sessionDate = todaySessionDate())
    )

    val themeMode: StateFlow<String> = preferences.themeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "dark"
    )

    val isInstantMode: StateFlow<Boolean> = preferences.instantModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun toggleTheme() {
        viewModelScope.launch {
            val current = themeMode.value
            val next = if (current == "dark") "light" else "dark"
            preferences.updateThemeMode(next)
        }
    }

    fun toggleInstantMode() {
        viewModelScope.launch {
            preferences.updateInstantMode(!isInstantMode.value)
        }
    }

    fun updateClient(client: Client) = updateStored {
        it.copy(client = client, zoneOrSubClient = defaultZoneOrSubClient(client))
    }

    fun updateZoneOrSubClient(value: String) = updateStored { it.copy(zoneOrSubClient = value) }

    fun updateTestSiteName(value: String) = updateStored { it.copy(testSiteName = value) }

    fun updateStationCode(value: String) = updateStored { it.copy(stationCode = value) }

    fun updateLineDetails(value: String) = updateStored { it.copy(lineDetails = value) }

    fun updateKmMarker(kmMarker: String) = updateStored { it.copy(kmMarker = kmMarker) }

    fun updateOheNumber(oheNumber: String) = updateStored { it.copy(oheNumber = oheNumber.uppercase()) }

    fun updateRailType(railType: RailType) = updateStored { it.copy(railType = railType) }

    fun updateRailSide(railSide: RailSide) = updateStored { it.copy(railSide = railSide) }

    fun updateDirection(direction: Direction) = updateStored { it.copy(direction = direction) }


    fun updateRailPosition(railPosition: RailPosition) =
        updateStored { it.copy(railPosition = railPosition) }

    fun updateSessionDate(sessionDate: String) = updateStored { it.copy(sessionDate = sessionDate) }

    private fun updateStored(transform: (MetadataValues) -> MetadataValues) {
        pendingWrite = viewModelScope.launch {
            val latest = preferences.metadataFlow.first()
            preferences.updateMetadata(transform(latest))
        }
    }

    suspend fun prepareCaptureSession(filename: String? = null): MetadataValues {
        pendingWrite?.join()
        val session = preferences.metadataFlow.first()
        CaptureSessionMetadata.values = session
        CaptureSessionMetadata.sessionFilename = filename
        return session
    }
}
