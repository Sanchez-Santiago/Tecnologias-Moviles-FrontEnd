package com.undef.superahorrosanchezpucci.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesDataStore(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val INDIVIDUAL_GROUP_ID = stringPreferencesKey("individual_group_id")
        private val ACTIVE_GROUP_ID = stringPreferencesKey("active_group_id")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val name = prefs[THEME_MODE] ?: return@map ThemeMode.SYSTEM
        try {
            ThemeMode.valueOf(name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val individualGroupIdFlow: Flow<String?> = context.dataStore.data.map { it[INDIVIDUAL_GROUP_ID] }
    val activeGroupIdFlow: Flow<String?> = context.dataStore.data.map { it[ACTIVE_GROUP_ID] }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.name
        }
    }

    suspend fun saveGroupIds(individual: String?, active: String?) {
        context.dataStore.edit { prefs ->
            if (individual != null) prefs[INDIVIDUAL_GROUP_ID] = individual
            if (active != null) prefs[ACTIVE_GROUP_ID] = active
        }
    }
}
