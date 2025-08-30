// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/ui/gameplay/GamePlayViewModel.kt

package com.yourcompany.partygameapp.ui.gameplay

import android.content.res.Configuration
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourcompany.partygameapp.data.repository.DeckRepository
import com.yourcompany.partygameapp.data.repository.PlayerRepository
import com.yourcompany.partygameapp.data.repository.SettingsRepository
import com.yourcompany.partygameapp.domain.model.Card
import com.yourcompany.partygameapp.domain.model.CardResult
import com.yourcompany.partygameapp.domain.model.GameMode
import com.yourcompany.partygameapp.domain.model.GameSettings
import com.yourcompany.partygameapp.domain.model.HotPotatoSettings
import com.yourcompany.partygameapp.domain.model.Player
import com.yourcompany.partygameapp.domain.model.Team
import com.yourcompany.partygameapp.ui.navigation.Routes
import com.yourcompany.partygameapp.ui.shared.SharedGameState
import com.yourcompany.partygameapp.util.GameGesture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay as coroutineDelay

enum class Answer { CORRECT, SKIP, TIME_UP }

sealed interface GamePlayUiState {
    object Loading : GamePlayUiState
    object Ready : GamePlayUiState
    data class Playing(
        val currentCard: Card,
        val timeLeftMillis: Long,
        val totalTimeMillis: Long,
        val livesRemaining: Int? = null,
        val currentTeam: Team? = null,
        val redTeamTimeLeft: Long? = null,
        val blueTeamTimeLeft: Long? = null,
        val redTeamSkipsLeft: Int? = null,
        val blueTeamSkipsLeft: Int? = null,
        val currentRoundPlayer: Player? = null
    ) : GamePlayUiState
    data class PassingPhone(
        val currentCard: Card,
        val nextTeam: Team,
        val nextPlayerName: String
    ) : GamePlayUiState
    data class Finished(
        val results: List<CardResult>,
        val redTeamScore: Int? = null,
        val blueTeamScore: Int? = null,
        val winningTeam: Team? = null
    ) : GamePlayUiState
}

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val settingsRepository: SettingsRepository,
    private val playerRepository: PlayerRepository,
    private val sharedGameState: SharedGameState,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = "GAMEPLAY_VM_DEBUG"

    // --- Navigation Arguments ---
    private val deckId: Int = checkNotNull(savedStateHandle[Routes.ARG_DECK_ID])
    val gameMode: GameMode = GameMode.valueOf(checkNotNull(savedStateHandle[Routes.ARG_GAME_MODE]))
    private val playerIdArg: Int = checkNotNull(savedStateHandle[Routes.ARG_PLAYER_ID])

    // Hot Potato Specific Arguments
    private val hotPotatoTeamTimeArg: Int = savedStateHandle.get<Int>(Routes.ARG_HOT_POTATO_TEAM_TIME) ?: 300
    private val hotPotatoMaxSkipsArg: Int = savedStateHandle.get<Int>(Routes.ARG_HOT_POTATO_MAX_SKIPS) ?: 2
    private val hotPotatoPlayersJsonArg: String = savedStateHandle.get<String>(Routes.ARG_HOT_POTATO_PLAYERS_JSON) ?: "[]"


    // --- General Game State ---
    private var cardTimeMillis: Long = 20000L
    private lateinit var gameSettings: GameSettings
    private var allCards: List<Card> = emptyList()
    private var currentCardIndex: Int = 0
    private var timer: CountDownTimer? = null
    private val results = mutableListOf<CardResult>()

    // --- "3 Lives" Mode Specific State ---
    private var livesRemaining: Int = 3

    // --- "Hot Potato" Mode Specific State ---
    private var hotPotatoSettings: HotPotatoSettings? = null
    private var redTeamTimeLeftMillis: Long = 0
    private var blueTeamTimeLeftMillis: Long = 0
    private var redTeamSkipsLeft: Int = 0
    private var blueTeamSkipsLeft: Int = 0
    private var currentHotPotatoTeam: Team = Team.RED // This will be randomized later
    private var allGamePlayers: List<Player> = emptyList() // All players for Hot Potato
    private var redTeamPlayers: List<Player> = emptyList()
    private var blueTeamPlayers: List<Player> = emptyList()
    private var currentPlayerTurnIndex: Int = 0 // Index within the current team's player list
    private var hotPotatoTeamTimer: CountDownTimer? = null


    private val _uiState = MutableStateFlow<GamePlayUiState>(GamePlayUiState.Loading)
    val uiState: StateFlow<GamePlayUiState> = _uiState

    init {
        prepareGame()
    }

    private fun prepareGame() {
        viewModelScope.launch {
            gameSettings = settingsRepository.gameSettingsFlow.first()
            cardTimeMillis = gameSettings.cardTime * 1000L
            val allAvailableCards = deckRepository.getCardsForDeck(deckId).shuffled()

            allCards = if (gameMode == GameMode.Standard) {
                allAvailableCards.take(gameSettings.cardCount)
            } else {
                allAvailableCards
            }

            if (gameMode == GameMode.HotPotato) {
                hotPotatoSettings = HotPotatoSettings(hotPotatoTeamTimeArg, hotPotatoMaxSkipsArg)
                redTeamTimeLeftMillis = hotPotatoTeamTimeArg * 1000L
                blueTeamTimeLeftMillis = hotPotatoTeamTimeArg * 1000L
                redTeamSkipsLeft = hotPotatoMaxSkipsArg
                blueTeamSkipsLeft = hotPotatoMaxSkipsArg

                val type = object : TypeToken<Map<Int, String>>() {}.type
                val playerNamesById: Map<Int, String> = Gson().fromJson(hotPotatoPlayersJsonArg, type) ?: emptyMap()
                val actualPlayers = playerNamesById.map { (id, name) -> Player(id, name) }

                // <--- MODIFIED: Handle case of no actual players for Hot Potato --->
                if (actualPlayers.isEmpty()) {
                    Log.d(TAG, "No actual players provided for HotPotato. Using dummy players.")
                    allGamePlayers = listOf(Player(id = -1, name = "Red Team Player"), Player(id = -2, name = "Blue Team Player"))
                } else {
                    allGamePlayers = actualPlayers
                }

                // Randomly assign players to teams from allGamePlayers
                val shuffledPlayers = allGamePlayers.shuffled(Random(System.nanoTime()))
                redTeamPlayers = shuffledPlayers.filterIndexed { index, _ -> index % 2 == 0 }
                blueTeamPlayers = shuffledPlayers.filterIndexed { index, _ -> index % 2 != 0 }

                // If one team ends up empty, move a player from the other team or create a dummy
                if (redTeamPlayers.isEmpty() && blueTeamPlayers.isNotEmpty()) {
                    redTeamPlayers = listOf(blueTeamPlayers.removeAt(0)) // Move one from blue to red
                } else if (blueTeamPlayers.isEmpty() && redTeamPlayers.isNotEmpty()) {
                    blueTeamPlayers = listOf(redTeamPlayers.removeAt(0)) // Move one from red to blue
                }
                else if (redTeamPlayers.isEmpty() && blueTeamPlayers.isEmpty()) {
                    // This case implies allGamePlayers was initially empty. We already handled this
                    // by creating dummy players, so this block should ideally not be reached if previous logic works.
                    redTeamPlayers = listOf(Player(id = -1, name = "Red Team Player"))
                    blueTeamPlayers = listOf(Player(id = -2, name = "Blue Team Player"))
                }


                // Randomly choose starting team
                currentHotPotatoTeam = if (Random.nextBoolean()) Team.RED else Team.BLUE
                Log.d(TAG, "HotPotato: Red Team: ${redTeamPlayers.map { it.name }}, Blue Team: ${blueTeamPlayers.map { it.name }}. Starting Team: $currentHotPotatoTeam")

            } else {
                // For Standard/3Lives, set the single player for results
                allGamePlayers = playerRepository.getAllPlayers().first().filter { it.id == playerIdArg }
            }

            _uiState.value = if (allCards.isNotEmpty()) GamePlayUiState.Ready else GamePlayUiState.Finished(emptyList())
        }
    }

    fun onDeviceRotatedToLandscape() {
        if (_uiState.value is GamePlayUiState.Ready) {
            currentCardIndex = 0
            if (gameMode == GameMode.HotPotato) {
                startHotPotatoRound()
            } else {
                showNextCard()
            }
        }
    }

    // --- Hot Potato Round Management ---
    private fun startHotPotatoRound() {
        Log.d(TAG, "startHotPotatoRound() called. Current card index: $currentCardIndex / ${allCards.size}")
        if (checkHotPotatoGameOver()) {
            Log.d(TAG, "HotPotato GameOver detected in startHotPotatoRound.")
            endGame()
            return
        }

        viewModelScope.launch {
            val currentPlayer = getNextHotPotatoPlayerForTurn()
            if (currentPlayer == null) {
                Log.e(TAG, "No player found for current team. Ending game.")
                endGame()
                return@launch
            }

            if (currentCardIndex >= allCards.size) {
                Log.d(TAG, "All cards played. Ending game.")
                endGame()
                return@launch
            }

            val nextCard = allCards[currentCardIndex]
            _uiState.value = GamePlayUiState.Playing(
                currentCard = nextCard,
                timeLeftMillis = cardTimeMillis,
                totalTimeMillis = cardTimeMillis,
                currentTeam = currentHotPotatoTeam,
                redTeamTimeLeft = redTeamTimeLeftMillis,
                blueTeamTimeLeft = blueTeamTimeLeftMillis,
                redTeamSkipsLeft = redTeamSkipsLeft,
                blueTeamSkipsLeft = blueTeamSkipsLeft,
                currentRoundPlayer = currentPlayer
            )
            startCardTimer()
            startHotPotatoTeamTimer()
        }
    }

    private suspend fun getNextHotPotatoPlayerForTurn(): Player? = withContext(Dispatchers.IO) {
        val teamPlayers = if (currentHotPotatoTeam == Team.RED) redTeamPlayers else blueTeamPlayers
        if (teamPlayers.isEmpty()) {
            Log.e(TAG, "Empty team players list for Hot Potato. currentTeam: $currentHotPotatoTeam")
            return@withContext null
        }

        val playerForTurn = teamPlayers[currentPlayerTurnIndex % teamPlayers.size]
        Log.d(TAG, "Current team: $currentHotPotatoTeam, Player for turn: ${playerForTurn.name}. Current index: $currentPlayerTurnIndex, Team size: ${teamPlayers.size}")
        return@withContext playerForTurn
    }

    private fun startHotPotatoTeamTimer() {
        Log.d(TAG, "startHotPotatoTeamTimer() called for ${currentHotPotatoTeam}. Time Left: ${redTeamTimeLeftMillis / 1000}s / ${blueTeamTimeLeftMillis / 1000}s")
        hotPotatoTeamTimer?.cancel()
        val totalTimeForTeam = if (currentHotPotatoTeam == Team.RED) redTeamTimeLeftMillis else blueTeamTimeLeftMillis

        hotPotatoTeamTimer = object : CountDownTimer(totalTimeForTeam, 50) {
            override fun onTick(millisUntilFinished: Long) {
                if (currentHotPotatoTeam == Team.RED) {
                    redTeamTimeLeftMillis = millisUntilFinished
                } else {
                    blueTeamTimeLeftMillis = millisUntilFinished
                }
                val currentState = _uiState.value
                if (currentState is GamePlayUiState.Playing) {
                    _uiState.value = currentState.copy(
                        redTeamTimeLeft = redTeamTimeLeftMillis,
                        blueTeamTimeLeft = blueTeamTimeLeftMillis
                    )
                }
            }
            override fun onFinish() {
                Log.d(TAG, "${currentHotPotatoTeam} team time is up!")
                if (currentHotPotatoTeam == Team.RED) {
                    redTeamTimeLeftMillis = 0
                } else {
                    blueTeamTimeLeftMillis = 0
                }
                endGame()
            }
        }.start()
    }

    private fun pauseHotPotatoTeamTimer() { hotPotatoTeamTimer?.cancel() }

    private suspend fun switchPlayerAndTeamForHotPotato() {
        Log.d(TAG, "Switching player/team. Current player index: $currentPlayerTurnIndex")
        currentPlayerTurnIndex++

        val currentTeamPlayers = if (currentHotPotatoTeam == Team.RED) redTeamPlayers else blueTeamPlayers
        if (currentTeamPlayers.isEmpty()) {
            Log.e(TAG, "Cannot switch player/team: Current team players list is empty for $currentHotPotatoTeam team. Ending game.")
            endGame()
            return
        }

        // Check if all players in the current team have had their turn, then switch teams
        if (currentPlayerTurnIndex >= currentTeamPlayers.size) {
            currentPlayerTurnIndex = 0 // Reset index for the next team
            currentHotPotatoTeam = if (currentHotPotatoTeam == Team.RED) Team.BLUE else Team.RED
            Log.d(TAG, "Switched team to: $currentHotPotatoTeam")
            // After switching team, update the state of players based on new current team
            val newCurrentPlayer = getNextHotPotatoPlayerForTurn()
            if (newCurrentPlayer != null && _uiState.value is GamePlayUiState.Playing) {
                _uiState.value = (_uiState.value as GamePlayUiState.Playing).copy(currentRoundPlayer = newCurrentPlayer)
            }
        }
    }

    private fun checkHotPotatoGameOver(): Boolean {
        Log.d(TAG, "Checking HotPotato Game Over. Red Time: ${redTeamTimeLeftMillis / 1000}s, Blue Time: ${blueTeamTimeLeftMillis / 1000}s, Red Skips: $redTeamSkipsLeft, Blue Skips: $blueTeamSkipsLeft")

        // Game ends if one team runs out of time or skips
        val redTeamLostByTime = redTeamTimeLeftMillis <= 0
        val blueTeamLostByTime = blueTeamTimeLeftMillis <= 0
        val redTeamLostBySkips = hotPotatoSettings != null && redTeamSkipsLeft <= 0
        val blueTeamLostBySkips = hotPotatoSettings != null && blueTeamSkipsLeft <= 0

        val redTeamScore = results.count { it.points > 0 && redTeamPlayers.any { p -> p.id == it.playerId } }
        val blueTeamScore = results.count { it.points > 0 && blueTeamPlayers.any { p -> p.id == it.playerId } }

        if (redTeamLostByTime || redTeamLostBySkips) {
            _uiState.value = GamePlayUiState.Finished(results, redTeamScore, blueTeamScore, winningTeam = Team.BLUE)
            Log.d(TAG, "Red team lost. Blue wins!")
            return true
        }
        if (blueTeamLostByTime || blueTeamLostBySkips) {
            _uiState.value = GamePlayUiState.Finished(results, redTeamScore, blueTeamScore, winningTeam = Team.RED)
            Log.d(TAG, "Blue team lost. Red wins!")
            return true
        }
        // If both teams somehow run out at the same time, it's a draw
        if ((redTeamLostByTime && blueTeamLostByTime) || (redTeamLostBySkips && blueTeamLostBySkips)) {
            _uiState.value = GamePlayUiState.Finished(results, redTeamScore, blueTeamScore, winningTeam = Team.NONE)
            Log.d(TAG, "HotPotato game ended in a draw!")
            return true
        }

        return false
    }


    fun processAnswer(answer: Answer) {
        if (_uiState.value !is GamePlayUiState.Playing) return
        val currentCard = (_uiState.value as GamePlayUiState.Playing).currentCard
        val currentRoundPlayer = (_uiState.value as GamePlayUiState.Playing).currentRoundPlayer
        val currentRoundPlayerId = currentRoundPlayer?.id ?: 0

        if (gameMode == GameMode.HotPotato) {
            pauseHotPotatoTeamTimer()
            timer?.cancel()
            viewModelScope.launch {
                when (answer) {
                    Answer.CORRECT -> {
                        results.add(CardResult(currentCard.text, 1, playerId = currentRoundPlayerId))
                        val nextPlayerName = getNextHotPotatoPlayerNameForPass() // Get next player's name
                        _uiState.value = GamePlayUiState.PassingPhone(
                            currentCard,
                            // The team in PassingPhone should be the team that *just scored* (current team),
                            // as the next turn will start with the next person from that team.
                            nextTeam = currentHotPotatoTeam,
                            nextPlayerName = nextPlayerName
                        )
                        coroutineDelay(2000) // Hold for 2 seconds to allow passing
                        switchPlayerAndTeamForHotPotato() // Switch player/team logic now
                        currentCardIndex++
                        startHotPotatoRound()
                    }
                    Answer.SKIP -> {
                        if (currentHotPotatoTeam == Team.RED) redTeamSkipsLeft-- else blueTeamSkipsLeft--
                        results.add(CardResult(currentCard.text, 0, playerId = currentRoundPlayerId)) // Skip gives 0 points
                        if (checkHotPotatoGameOver()) { endGame(); return@launch } // Check game over BEFORE next turn
                        currentCardIndex++
                        val nextPlayerName = getNextHotPotatoPlayerNameForPass()
                        _uiState.value = GamePlayUiState.PassingPhone(
                            currentCard,
                            nextTeam = currentHotPotatoTeam,
                            nextPlayerName = nextPlayerName
                        )
                        coroutineDelay(2000)
                        switchPlayerAndTeamForHotPotato()
                        startHotPotatoRound()
                    }
                    Answer.TIME_UP -> {
                        // This should ideally trigger from the timer itself for Hot Potato
                        if (currentHotPotatoTeam == Team.RED) redTeamSkipsLeft-- else blueTeamSkipsLeft-- // Time up is like a skip penalty
                        results.add(CardResult(currentCard.text, -1, playerId = currentRoundPlayerId))
                        if (checkHotPotatoGameOver()) { endGame(); return@launch } // Check game over BEFORE next turn
                        currentCardIndex++
                        val nextPlayerName = getNextHotPotatoPlayerNameForPass()
                        _uiState.value = GamePlayUiState.PassingPhone(
                            currentCard,
                            nextTeam = currentHotPotatoTeam,
                            nextPlayerName = nextPlayerName
                        )
                        coroutineDelay(2000)
                        switchPlayerAndTeamForHotPotato()
                        startHotPotatoRound()
                    }
                }
            }
            return
        }

        // --- Standard / 3 Lives Mode Logic (Existing) ---
        val points = when (answer) {
            Answer.CORRECT -> 1
            Answer.SKIP -> {
                if (gameMode == GameMode.ThreeLives) livesRemaining--
                if (gameSettings.giveUpPenalty) -1 else 0
            }
            Answer.TIME_UP -> {
                if (gameMode == GameMode.ThreeLives) livesRemaining--
                if (gameSettings.timeUpPenalty) -1 else 0
            }
        }
        results.add(CardResult(cardText = currentCard.text, points = points, playerId = playerIdArg))
        if (gameMode == GameMode.ThreeLives && livesRemaining <= 0) {
            endGame()
        } else {
            currentCardIndex++
            showNextCard()
        }
    }

    fun processGesture(gesture: GameGesture) {
        if (_uiState.value !is GamePlayUiState.Playing) return
        if (gameMode == GameMode.HotPotato) {
            return // Gestures are handled by buttons in Hot Potato mode for simplicity
        }

        // Standard/3Lives Mode Gesture Logic
        when (gesture) {
            GameGesture.TILT_FRONT -> processAnswer(Answer.CORRECT)
            GameGesture.TILT_BACK -> {
                processAnswer(Answer.SKIP) // Assuming tilt back is "give up"
            }
            GameGesture.SHAKE -> { /* Implement later if needed */ }
        }
    }

    private fun showNextCard() {
        if (currentCardIndex >= allCards.size) {
            endGame()
            return
        }
        val nextCard = allCards[currentCardIndex]
        _uiState.value = GamePlayUiState.Playing(
            currentCard = nextCard,
            timeLeftMillis = cardTimeMillis,
            totalTimeMillis = cardTimeMillis,
            livesRemaining = if (gameMode == GameMode.ThreeLives) livesRemaining else null
        )
        startCardTimer()
    }

    private fun startCardTimer() {
        Log.d(TAG, "startCardTimer() called for Standard/3Lives.")
        timer?.cancel()
        timer = object : CountDownTimer(cardTimeMillis, 50) {
            override fun onTick(millisUntilFinished: Long) {
                (_uiState.value as? GamePlayUiState.Playing)?.let {
                    _uiState.value = it.copy(timeLeftMillis = millisUntilFinished)
                }
            }
            override fun onFinish() { processAnswer(Answer.TIME_UP) }
        }.start()
    }

    private fun endGame() {
        Log.d(TAG, "endGame() called. Game mode: $gameMode")
        timer?.cancel()
        hotPotatoTeamTimer?.cancel()

        if (gameMode == GameMode.HotPotato) {
            // Recalculate scores and winning team at the very end
            val redTeamPoints = results.count { it.points > 0 && redTeamPlayers.any { p -> p.id == it.playerId } }
            val blueTeamPoints = results.count { it.points > 0 && blueTeamPlayers.any { p -> p.id == it.playerId } }

            val winningTeam = when {
                (redTeamTimeLeftMillis <= 0 || redTeamSkipsLeft <= 0) && !(blueTeamTimeLeftMillis <= 0 || blueTeamSkipsLeft <= 0) -> Team.BLUE // Red team lost, Blue wins
                (blueTeamTimeLeftMillis <= 0 || blueTeamSkipsLeft <= 0) && !(redTeamTimeLeftMillis <= 0 || redTeamSkipsLeft <= 0) -> Team.RED // Blue team lost, Red wins
                // Both teams lost at the same time or a tie by score
                else -> {
                    if (redTeamPoints > blueTeamPoints) Team.RED
                    else if (blueTeamPoints > redTeamPoints) Team.BLUE
                    else Team.NONE // A perfect draw or no winner if conditions not met
                }
            }
            _uiState.value = GamePlayUiState.Finished(results, redTeamPoints, blueTeamPoints, winningTeam)
        } else {
            _uiState.value = GamePlayUiState.Finished(results)
        }
    }

    private suspend fun getNextHotPotatoPlayerNameForPass(): String = withContext(Dispatchers.IO) {
        val teamPlayers = if (currentHotPotatoTeam == Team.RED) redTeamPlayers else blueTeamPlayers
        if (teamPlayers.isEmpty()) return@withContext "Next Player" // Fallback if a team somehow has no players

        val nextPlayerInLineIndex = (currentPlayerTurnIndex + 1) % teamPlayers.size
        // If it's the last player of the current team's turn, the next person is the first player of the other team
        if (nextPlayerInLineIndex == 0) {
            val nextTeam = if (currentHotPotatoTeam == Team.RED) Team.BLUE else Team.RED
            val nextTeamPlayers = if (nextTeam == Team.RED) redTeamPlayers else blueTeamPlayers
            return@withContext nextTeamPlayers.firstOrNull()?.name ?: "Next Player"
        } else {
            return@withContext teamPlayers[nextPlayerInLineIndex].name
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
        hotPotatoTeamTimer?.cancel()
    }
}