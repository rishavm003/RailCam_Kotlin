package com.rishavmishra010.railcam.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rishavmishra010.railcam.domain.model.Client
import com.rishavmishra010.railcam.domain.model.Direction
import com.rishavmishra010.railcam.domain.model.MetadataValues
import com.rishavmishra010.railcam.domain.model.RailPosition
import com.rishavmishra010.railcam.domain.model.RailSide
import com.rishavmishra010.railcam.domain.model.RailType
import com.rishavmishra010.railcam.domain.model.defaultZoneOrSubClient
import com.rishavmishra010.railcam.domain.model.todaySessionDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "metadata_prefs")

class MetadataPreferences(context: Context) {
    private val dataStore = context.dataStore

    private object Keys {
        val CLIENT = stringPreferencesKey("client")
        val ZONE_OR_SUB_CLIENT = stringPreferencesKey("zone_or_sub_client")
        val TEST_SITE_NAME = stringPreferencesKey("test_site_name")
        val STATION_CODE = stringPreferencesKey("station_code")
        val LINE_DETAILS = stringPreferencesKey("line_details")
        val KM_MARKER = stringPreferencesKey("km_marker")
        val OHE_NUMBER = stringPreferencesKey("ohe_number")
        val RAIL_TYPE = stringPreferencesKey("rail_type")
        val RAIL_SIDE = stringPreferencesKey("rail_side")
        val DIRECTION = stringPreferencesKey("direction")
        val RAIL_POSITION = stringPreferencesKey("rail_position")
        val SESSION_DATE = stringPreferencesKey("session_date")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_INSTANT_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("is_instant_mode")
    }

    val themeFlow: Flow<String> = dataStore.data
        .catch {
            emit(emptyPreferences())
        }
        .map { prefs ->
            prefs[Keys.THEME_MODE] ?: "dark"
        }
        .distinctUntilChanged()

    val instantModeFlow: Flow<Boolean> = dataStore.data
        .catch {
            emit(emptyPreferences())
        }
        .map { prefs ->
            prefs[Keys.IS_INSTANT_MODE] ?: false
        }
        .distinctUntilChanged()

    val metadataFlow: Flow<MetadataValues> = dataStore.data
        .catch {
            emit(emptyPreferences())
        }
        .map { prefs ->
            val client = parseClient(prefs[Keys.CLIENT])
            MetadataValues(
                client = client,
                zoneOrSubClient = prefs[Keys.ZONE_OR_SUB_CLIENT] ?: defaultZoneOrSubClient(client),
                testSiteName = prefs[Keys.TEST_SITE_NAME] ?: "",
                stationCode = prefs[Keys.STATION_CODE] ?: "",
                lineDetails = prefs[Keys.LINE_DETAILS] ?: "",
                kmMarker = prefs[Keys.KM_MARKER] ?: "",
                oheNumber = prefs[Keys.OHE_NUMBER] ?: "",
                railType = parseRailType(prefs[Keys.RAIL_TYPE]),
                railSide = parseRailSide(prefs[Keys.RAIL_SIDE]),
                direction = parseDirection(prefs[Keys.DIRECTION]),
                railPosition = parseRailPosition(prefs[Keys.RAIL_POSITION]),
                sessionDate = prefs[Keys.SESSION_DATE]?.takeIf { it.isNotBlank() } ?: todaySessionDate()
            )
        }
        .distinctUntilChanged()

    suspend fun updateMetadata(values: MetadataValues) {
        dataStore.edit { prefs ->
            prefs[Keys.CLIENT] = values.client.name
            prefs[Keys.ZONE_OR_SUB_CLIENT] = values.zoneOrSubClient
            prefs[Keys.TEST_SITE_NAME] = values.testSiteName
            prefs[Keys.STATION_CODE] = values.stationCode
            prefs[Keys.LINE_DETAILS] = values.lineDetails
            prefs[Keys.KM_MARKER] = values.kmMarker
            prefs[Keys.OHE_NUMBER] = values.oheNumber
            prefs[Keys.RAIL_TYPE] = values.railType.name
            prefs[Keys.RAIL_SIDE] = values.railSide.name
            prefs[Keys.DIRECTION] = values.direction.name
            prefs[Keys.RAIL_POSITION] = values.railPosition.name
            prefs[Keys.SESSION_DATE] = values.sessionDate
        }
    }

    suspend fun updateThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun updateInstantMode(isInstant: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_INSTANT_MODE] = isInstant
        }
    }

    private fun parseClient(raw: String?): Client =
        runCatching { Client.valueOf(raw ?: Client.INDIAN_RAILWAY.name) }
            .getOrDefault(Client.INDIAN_RAILWAY)

    private fun parseRailType(raw: String?): RailType =
        runCatching { RailType.valueOf(raw ?: RailType.TANGENT.name) }
            .getOrDefault(RailType.TANGENT)

    private fun parseRailSide(raw: String?): RailSide =
        runCatching { RailSide.valueOf(raw ?: RailSide.LEFT.name) }
            .getOrDefault(RailSide.LEFT)

    private fun parseDirection(raw: String?): Direction =
        runCatching { Direction.valueOf(raw ?: Direction.UP.name) }
            .getOrDefault(Direction.UP)


    private fun parseRailPosition(raw: String?): RailPosition =
        runCatching { RailPosition.valueOf(raw ?: RailPosition.HIGH.name) }
            .getOrDefault(RailPosition.HIGH)
}
