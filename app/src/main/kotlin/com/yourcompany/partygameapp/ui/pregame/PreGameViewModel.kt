// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/ui/pregame/PreGameViewModel.kt

package com.yourcompany.partygameapp.ui.pregame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.partygameapp.data.repository.DeckRepository
import com.yourcompany.partygameapp.data.repository.PlayerRepository
import com.yourcompany.partygameapp.domain.model.Deck
import com.yourcompany.partygameapp.domain.model.GameMode
import com.yourcompany.partygameapp.domain.model.Player
import com.yourcompany.partygameapp.domain.model.Team
import com.yourcompany.partygameapp.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreGameViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val playerRepository: PlayerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Int = checkNotNull(savedStateHandle[Routes.ARG_DECK_ID])

    // --- STATE FLOWS (Data from the database) ---
    val selectedDeck: StateFlow<Deck?> = deckRepository.getDeckById(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val playerList: StateFlow<List<Player>> = playerRepository.getAllPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- GENERAL UI STATE ---
    var newPlayerName by mutableStateOf("")
        private set
    var selectedPlayer by mutableStateOf<Player?>(null)
        private set
    var isPlayerDropdownExpanded by mutableStateOf(false)
        private set

    val availableGameModes = GameMode.values().toList()
    var selectedGameMode by mutableStateOf(GameMode.Standard)
        private set
    var isGameModeDropdownExpanded by mutableStateOf(false)
        private set

    // --- HOT POTATO SPECIFIC STATE ---
    var teamTimeSeconds by mutableStateOf(300) // Default 5 minutes (300 seconds)
        private set
    var maxSkipsPerTeam by mutableStateOf(2) // Default 2 skips per team
        private set

    // --- EVENTS (Functions the UI can call) ---
    fun onNewPlayerNameChange(name: String) { newPlayerName = name }
    fun onAddPlayer() {
        if (newPlayerName.isNotBlank()) {
            viewModelScope.launch {
                playerRepository.insertPlayer(newPlayerName.trim())
                newPlayerName = ""
            }
        }
    }
    fun onPlayerSelected(player: Player) {
        selectedPlayer = player
        isPlayerDropdownExpanded = false
    }
    fun onDropdownClick() { isPlayerDropdownExpanded = true }
    fun onDropdownDismiss() { isPlayerDropdownExpanded = false }

    fun onGameModeSelected(mode: GameMode) {
        selectedGameMode = mode
        isGameModeDropdownExpanded = false
    }
    fun onGameModeDropdownClick() { isGameModeDropdownExpanded = true }
    fun onGameModeDropdownDismiss() { isGameModeDropdownExpanded = false }

    // --- HOT POTATO SPECIFIC EVENTS ---
    fun onTeamTimeChange(seconds: Int) {
        teamTimeSeconds = seconds.coerceIn(60, 600) // 1 to 10 minutes
    }
    fun onMaxSkipsChange(skips: Int) {
        maxSkipsPerTeam = skips.coerceIn(1, 5) // 1 to 5 skips
    }
}