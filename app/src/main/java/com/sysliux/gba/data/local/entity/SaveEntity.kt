package com.sysliux.gba.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saves",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["path"],
            childColumns = ["gamePath"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gamePath")]
)
data class SaveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gamePath: String,
    val slot: Int, // 1-10
    val name: String,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val playTimeAtSave: Long = 0
)