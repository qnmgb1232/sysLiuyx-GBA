package com.sysliux.gba.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sysliux.gba.data.local.SaveDao
import com.sysliux.gba.data.local.entity.SaveEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaveManagementViewModel @Inject constructor(
    private val saveDao: SaveDao
) : ViewModel() {

    fun getSavesForGame(gamePath: String): Flow<List<SaveEntity>> {
        return saveDao.getSavesForGame(gamePath)
    }

    fun deleteSave(save: SaveEntity) {
        viewModelScope.launch {
            saveDao.deleteSave(save)
        }
    }

    suspend fun createSave(gamePath: String, slot: Int): SaveEntity {
        val save = SaveEntity(
            gamePath = gamePath,
            slot = slot,
            name = "存档 $slot",
            createdAt = System.currentTimeMillis()
        )
        saveDao.insertSave(save)
        return save
    }
}
