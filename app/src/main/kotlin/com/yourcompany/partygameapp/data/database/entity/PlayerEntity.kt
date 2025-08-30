// FILE: PlayerEntity.kt

package com.yourcompany.partygameapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gamesPlayed: Int = 0,
    val totalCorrectGuesses: Int = 0,
    val totalPoints: Int = 0
)