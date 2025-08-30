// FILE: SettingsViewModel.kt

package com.yourcompany.partygameapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.partygameapp.data.repository.SettingsRepository
import com.yourcompany.partygameapp.domain.model.GameSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settingsState: StateFlow<GameSettings?> = settingsRepository.gameSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun onTimeUpPenaltyChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTimeUpPenalty(isEnabled)
        }
    }

    fun onGiveUpPenaltyChanged(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGiveUpPenalty(isEnabled)
        }
    }

    fun onCardTimeChanged(seconds: Int) {
        viewModelScope.launch {
            val clampedValue = seconds.coerceIn(10, 120)
            settingsRepository.setCardTime(clampedValue)
        }
    }

    fun onCardCountChanged(count: Int) {
        viewModelScope.launch {
            val clampedValue = count.coerceIn(5, 20)
            settingsRepository.setCardCount(clampedValue)
        }
    }
}