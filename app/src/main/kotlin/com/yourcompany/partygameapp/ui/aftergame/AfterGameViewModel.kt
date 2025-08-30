// FILE: AfterGameViewModel.kt

package com.yourcompany.partygameapp.ui.aftergame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.partygameapp.data.repository.PlayerRepository
import com.yourcompany.partygameapp.domain.model.CardResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AfterGameViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {

    fun saveGameResults(playerId: Int, results: List<CardResult>) {
        if (playerId == 0) return // Don't save stats for guest player

        viewModelScope.launch {
            val score = results.sumOf { it.points }.coerceAtLeast(0)
            val correctGuesses = results.count { it.points > 0 }
            playerRepository.updatePlayerStats(playerId, correctGuesses, score)
        }
    }
}