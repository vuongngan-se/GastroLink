package tech.davidmartinezmuelas.gastrolink.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {

    private val useAiRecommendationsKey = booleanPreferencesKey("use_ai_recommendations")
    private val soloProfileJsonKey = stringPreferencesKey("solo_profile_json")
    private val savedProfilesJsonKey = stringPreferencesKey("saved_profiles_json")

    val useAiRecommendationsFlow: Flow<Boolean> = context.appDataStore.data
        .map { preferences -> preferences[useAiRecommendationsKey] ?: false }

    val soloProfileJsonFlow: Flow<String?> = context.appDataStore.data
        .map { preferences -> preferences[soloProfileJsonKey] }

    val savedProfilesJsonFlow: Flow<String?> = context.appDataStore.data
        .map { preferences -> preferences[savedProfilesJsonKey] }

    suspend fun setUseAiRecommendations(enabled: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[useAiRecommendationsKey] = enabled
        }
    }

    suspend fun saveSoloProfileJson(json: String) {
        context.appDataStore.edit { preferences ->
            preferences[soloProfileJsonKey] = json
        }
    }

    suspend fun saveSavedProfilesJson(json: String) {
        context.appDataStore.edit { preferences ->
            preferences[savedProfilesJsonKey] = json
        }
    }

    suspend fun clearAllPreferences() {
        context.appDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
