package com.rishavmishra010.railcam.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rishavmishra010.railcam.data.datastore.MetadataPreferences
import com.rishavmishra010.railcam.domain.model.Client
import com.rishavmishra010.railcam.domain.model.Direction
import com.rishavmishra010.railcam.domain.model.MetadataValues
import com.rishavmishra010.railcam.domain.model.RailPosition
import com.rishavmishra010.railcam.domain.model.RailSide
import com.rishavmishra010.railcam.domain.model.RailType
import com.rishavmishra010.railcam.domain.model.defaultZoneOrSubClient
import com.rishavmishra010.railcam.domain.model.todaySessionDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = MetadataPreferences(application)

    /**
     * In-memory source of truth for the form. Edits update it synchronously, so text fields
     * never wait on a DataStore disk write/read round-trip per keystroke (which caused typing
     * lag and cursor jumps). Changes are persisted to DataStore in the background, debounced.
     */
    private val _metadata = MutableStateFlow(MetadataValues(sessionDate = todaySessionDate()))
    val metadata: StateFlow<MetadataValues> = _metadata.asStateFlow()

    private var hasLocalEdits = false
    private var pendingWrite: Job? = null

    init {
        viewModelScope.launch {
            val stored = preferences.metadataFlow.first()
            if (!hasLocalEdits) _metadata.value = stored
        }
    }

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

    fun setInstantMode(enabled: Boolean) {
        viewModelScope.launch {
            preferences.updateInstantMode(enabled)
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
        hasLocalEdits = true
        _metadata.update(transform)
        pendingWrite?.cancel()
        pendingWrite = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            persistNow()
        }
    }

    private suspend fun persistNow() {
        // NonCancellable so a write that has started is never torn by a newer keystroke.
        withContext(NonCancellable) {
            preferences.updateMetadata(_metadata.value)
        }
    }

    /**
     * Saves the form to DataStore before the camera opens. The camera reads the session
     * from DataStore (not from memory), so it survives Android killing the app while it is
     * in the background.
     */
    suspend fun prepareCaptureSession(): MetadataValues {
        pendingWrite?.cancel()
        val session = _metadata.value
        persistNow()
        return session
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    override fun onCleared() {
        // Flush any edit still waiting on the debounce.
        if (pendingWrite?.isActive == true) {
            val values = _metadata.value
            kotlinx.coroutines.GlobalScope.launch { preferences.updateMetadata(values) }
        }
        super.onCleared()
    }

    private companion object {
        const val PERSIST_DEBOUNCE_MS = 300L
    }
}
