package com.sysliux.gba.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysliux.gba.data.preferences.UserPreferences
import com.sysliux.gba.data.preferences.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val settings: StateFlow<UserSettings> = userPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun setVolume(volume: Int) {
        viewModelScope.launch {
            userPreferences.setVolume(volume)
        }
    }

    fun setShowFps(show: Boolean) {
        viewModelScope.launch {
            userPreferences.setShowFps(show)
        }
    }

    fun setButtonAlpha(alpha: Float) {
        viewModelScope.launch {
            userPreferences.setButtonAlpha(alpha)
        }
    }

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch {
            userPreferences.setDefaultSpeed(speed)
        }
    }
}
