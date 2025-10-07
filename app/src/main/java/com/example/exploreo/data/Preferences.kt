package com.example.exploreo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "exploreo_settings")

object PrefKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val LANGUAGE = stringPreferencesKey("language")
    val MAP_TYPE = stringPreferencesKey("map_type") // Normal, Satellite, Terrain
    val TRAFFIC = booleanPreferencesKey("traffic_enabled")
    val IMAGE_QUALITY = stringPreferencesKey("image_quality") // High, Standard
}

class PreferencesRepository(private val context: Context) {
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.DARK_MODE] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[PrefKeys.LANGUAGE] ?: "English" }
    val mapType: Flow<String> = context.dataStore.data.map { it[PrefKeys.MAP_TYPE] ?: "Normal" }
    val trafficEnabled: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.TRAFFIC] ?: true }
    val imageQuality: Flow<String> = context.dataStore.data.map { it[PrefKeys.IMAGE_QUALITY] ?: "High" }
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.DARK_MODE] = enabled
        }
    }
    suspend fun setLanguage(value: String) {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.LANGUAGE] = value
        }
    }
    suspend fun setMapType(value: String) {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.MAP_TYPE] = value
        }
    }
    suspend fun setTrafficEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.TRAFFIC] = enabled
        }
    }
    suspend fun setImageQuality(value: String) {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.IMAGE_QUALITY] = value
        }
    }
}


