package com.sysliux.gba.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sysliux.gba.data.local.entity.GameEntity
import com.sysliux.gba.data.local.entity.SaveEntity

@Database(
    entities = [GameEntity::class, SaveEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun saveDao(): SaveDao
}