package com.sysliux.gba.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserSettings(
    val volume: Int = 100,
    val showFps: Boolean = false,
    val buttonAlpha: Float = 0.5f,
    val defaultSpeed: Float = 1.0f,
    val lastRomPath: String? = null
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val VOLUME = intPreferencesKey("volume")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val BUTTON_ALPHA = floatPreferencesKey("button_alpha")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val LAST_ROM_PATH = stringPreferencesKey("last_rom_path")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            volume = prefs[Keys.VOLUME] ?: 100,
            showFps = prefs[Keys.SHOW_FPS] ?: false,
            buttonAlpha = prefs[Keys.BUTTON_ALPHA] ?: 0.5f,
            defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            lastRomPath = prefs[Keys.LAST_ROM_PATH]
        )
    }

    suspend fun setVolume(volume: Int) {
        context.dataStore.edit { it[Keys.VOLUME] = volume }
    }

    suspend fun setShowFps(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FPS] = show }
    }

    suspend fun setButtonAlpha(alpha: Float) {
        context.dataStore.edit { it[Keys.BUTTON_ALPHA] = alpha }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_SPEED] = speed }
    }

    suspend fun setLastRomPath(path: String) {
        context.dataStore.edit { it[Keys.LAST_ROM_PATH] = path }
    }
}