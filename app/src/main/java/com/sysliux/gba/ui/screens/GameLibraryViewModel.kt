package com.sysliux.gba.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysliux.gba.data.local.GameDao
import com.sysliux.gba.data.local.entity.GameEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class GameLibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao
) : ViewModel() {

    val games: StateFlow<List<GameEntity>> = gameDao.getAllGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGame(uri: Uri) {
        viewModelScope.launch {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = uri.lastPathSegment ?: "game_${System.currentTimeMillis()}.gba"
            val destFile = File(context.filesDir, "roms/$fileName")

            destFile.parentFile?.mkdirs()
            inputStream?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val game = GameEntity(
                path = destFile.absolutePath,
                name = destFile.nameWithoutExtension
            )
            gameDao.insertGame(game)
        }
    }
}
