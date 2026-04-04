package com.sysliux.gba.core

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysliux.gba.data.local.GameDao
import com.sysliux.gba.data.local.entity.GameEntity
import com.sysliux.gba.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class EmulatorState(
    val isLoaded: Boolean = false,
    val isRunning: Boolean = false,
    val currentSpeed: Float = 1.0f,
    val fps: Int = 0,
    val currentGame: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class GbaCoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val gbaCore = GbaCore()

    private val _state = MutableStateFlow(EmulatorState())
    val state: StateFlow<EmulatorState> = _state.asStateFlow()

    private var renderJob: Job? = null
    private val frameMutex = Mutex()
    private var lastFrameTime = 0L
    private var frameCounter = 0
    private var fpsUpdateTime = 0L

    // Target FPS: 60
    private val framePeriodNanos = 1_000_000_000L / 60

    init {
        System.loadLibrary("gba") // 加载 native library
    }

    fun loadGame(romPath: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val loaded = gbaCore.loadRom(romPath)
                    if (loaded) {
                        // 更新数据库
                        val game = GameEntity(
                            path = romPath,
                            name = File(romPath).nameWithoutExtension
                        )
                        gameDao.insertGame(game)
                        userPreferences.setLastRomPath(romPath)

                        _state.value = _state.value.copy(
                            isLoaded = true,
                            currentGame = romPath,
                            errorMessage = null
                        )
                    } else {
                        _state.value = _state.value.copy(
                            errorMessage = "Failed to load ROM: $romPath"
                        )
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(
                        errorMessage = "Error loading ROM: ${e.message}"
                    )
                }
            }
        }
    }

    fun start() {
        if (!_state.value.isLoaded) {
            _state.value = _state.value.copy(
                errorMessage = "No ROM loaded"
            )
            return
        }

        // Cancel any existing render job
        renderJob?.cancel()

        gbaCore.start()
        _state.value = _state.value.copy(isRunning = true, errorMessage = null)
        startRenderLoop()
    }

    fun pause() {
        renderJob?.cancel()
        renderJob = null
        gbaCore.pause()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun reset() {
        pause()
        gbaCore.reset()
    }

    fun setSpeed(speed: Float) {
        // Clamp speed to valid range
        val clampedSpeed = speed.coerceIn(0.1f, 10.0f)
        gbaCore.setSpeed(clampedSpeed)
        _state.value = _state.value.copy(currentSpeed = clampedSpeed)
    }

    fun setVolume(volume: Int) {
        val clampedVolume = volume.coerceIn(0, 100)
        gbaCore.setVolume(clampedVolume)
        viewModelScope.launch {
            userPreferences.setVolume(clampedVolume)
        }
    }

    fun keyDown(button: Int) {
        gbaCore.keyDown(button)
    }

    fun keyUp(button: Int) {
        gbaCore.keyUp(button)
    }

    fun saveState(slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = gbaCore.saveState(slot)
            if (!success) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to save state to slot $slot"
                )
            }
        }
    }

    fun loadState(slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = gbaCore.loadState(slot)
            if (!success) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to load state from slot $slot"
                )
            }
        }
    }

    suspend fun getFrameBuffer(): ByteArray {
        return frameMutex.withLock {
            gbaCore.getFrameBuffer()
        }
    }

    fun getAudioSamples(): ShortArray {
        return gbaCore.getAudioSamples()
    }

    fun unload() {
        pause()
        gbaCore.unloadRom()
        _state.value = EmulatorState()
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun startRenderLoop() {
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            lastFrameTime = System.nanoTime()
            fpsUpdateTime = System.currentTimeMillis()
            frameCounter = 0

            while (_state.value.isRunning) {
                val currentTime = System.nanoTime()
                val elapsed = currentTime - lastFrameTime

                if (elapsed >= framePeriodNanos) {
                    // Step the emulator
                    gbaCore.stepFrame()
                    lastFrameTime = currentTime

                    // FPS counter
                    frameCounter++
                    val now = System.currentTimeMillis()
                    if (now - fpsUpdateTime >= 1000) {
                        _state.value = _state.value.copy(fps = frameCounter)
                        frameCounter = 0
                        fpsUpdateTime = now
                    }
                } else {
                    // Yield to avoid busy-waiting
                    delay(1)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        unload()
    }
}
