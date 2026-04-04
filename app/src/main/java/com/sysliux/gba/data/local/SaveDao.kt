package com.sysliux.gba.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sysliux.gba.data.local.entity.SaveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveDao {
    @Query("SELECT * FROM saves WHERE gamePath = :gamePath ORDER BY slot")
    fun getSavesForGame(gamePath: String): Flow<List<SaveEntity>>

    @Query("SELECT * FROM saves WHERE gamePath = :gamePath AND slot = :slot")
    suspend fun getSave(gamePath: String, slot: Int): SaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSave(save: SaveEntity): Long

    @Delete
    suspend fun deleteSave(save: SaveEntity)

    @Query("DELETE FROM saves WHERE gamePath = :gamePath AND slot = :slot")
    suspend fun deleteSaveSlot(gamePath: String, slot: Int)
}