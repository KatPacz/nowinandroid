// FILE: LeaderboardViewModel.kt

package com.yourcompany.partygameapp.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.partygameapp.data.database.entity.PlayerEntity
import com.yourcompany.partygameapp.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    playerRepository: PlayerRepository
) : ViewModel() {
    val players: StateFlow<List<PlayerEntity>> = playerRepository.getPlayersForLeaderboard()
        .map { list -> list.sortedByDescending { it.totalPoints } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}