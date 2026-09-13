package com.nmoreland.cognitivenexus.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000/"

private val Context.backendSettingsDataStore by preferencesDataStore(name = "backend_settings")

class BackendSettingsRepository(private val context: Context) {
    private val backendUrlKey = stringPreferencesKey("backend_url")

    val backendUrl: Flow<String> = context.backendSettingsDataStore.data.map { preferences ->
        preferences[backendUrlKey] ?: DEFAULT_BACKEND_URL
    }

    suspend fun saveBackendUrl(url: String) {
        context.backendSettingsDataStore.edit { preferences ->
            preferences[backendUrlKey] = url.trim().let { if (it.endsWith('/')) it else "$it/" }
        }
    }
}
