// FILE: PlayerRepository.kt

package com.yourcompany.partygameapp.data.repository

import com.yourcompany.partygameapp.data.database.dao.PlayerDao
import com.yourcompany.partygameapp.data.database.entity.PlayerEntity
import com.yourcompany.partygameapp.domain.model.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDao
) {
    fun getAllPlayers(): Flow<List<Player>> = playerDao.getAllPlayers().map { entities ->
        entities.map { Player(id = it.id, name = it.name) }
    }

    suspend fun insertPlayer(name: String) {
        playerDao.insertPlayer(PlayerEntity(name = name))
    }

    suspend fun getPlayerById(id: Int): PlayerEntity? {
        return playerDao.getPlayerById(id)
    }

    suspend fun updatePlayerStats(playerId: Int, correctGuesses: Int, score: Int) {
        playerDao.updatePlayerStats(playerId, correctGuesses, score)
    }

    fun getPlayersForLeaderboard(): Flow<List<PlayerEntity>> {
        return playerDao.getAllPlayers()
    }
}