package com.nmoreland.cognitivenexus.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nmoreland.cognitivenexus.network.BackendUrlNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsDataSource {
    val backendUrl: Flow<String>
    suspend fun saveBackendUrl(url: String)
}


private val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(private val context: Context) : SettingsDataSource {
    private val backendUrlKey = stringPreferencesKey("backend_url")

    override val backendUrl: Flow<String> = context.dataStore.data.map { preferences ->
        normalizeBackendUrl(preferences[backendUrlKey] ?: DEFAULT_BACKEND_URL)
    }

    override suspend fun saveBackendUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[backendUrlKey] = normalizeBackendUrl(url)
        }
    }

    companion object {
        const val DEFAULT_BACKEND_URL = BackendUrlNormalizer.DEFAULT_DEV_BACKEND_URL

        internal fun normalizeBackendUrl(url: String): String {
            return BackendUrlNormalizer.normalize(url, DEFAULT_BACKEND_URL)
        }
    }
}
