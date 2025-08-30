// FILE: PlayerDao.kt

package com.yourcompany.partygameapp.data.database.dao

import androidx.room.*
import com.yourcompany.partygameapp.data.database.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Insert
    suspend fun insertPlayer(player: PlayerEntity)

    @Query("SELECT * FROM players WHERE id = :playerId LIMIT 1")
    suspend fun getPlayerById(playerId: Int): PlayerEntity?

    @Query("""
        UPDATE players
        SET
            gamesPlayed = gamesPlayed + 1,
            totalCorrectGuesses = totalCorrectGuesses + :correctGuesses,
            totalPoints = totalPoints + :score
        WHERE id = :playerId
    """)
    suspend fun updatePlayerStats(playerId: Int, correctGuesses: Int, score: Int)
}