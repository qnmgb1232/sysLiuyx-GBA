package com.sysliux.gba.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val path: String,
    val name: String,
    val lastPlayedAt: Long? = null,
    val totalPlayTime: Long = 0,
    val coverPath: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)