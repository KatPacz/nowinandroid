// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/ui/pregame/PreGameScreen.kt

package com.yourcompany.partygameapp.ui.pregame

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourcompany.partygameapp.R
import com.yourcompany.partygameapp.domain.model.Deck
import com.yourcompany.partygameapp.domain.model.GameMode
import com.yourcompany.partygameapp.domain.model.Player
import com.yourcompany.partygameapp.domain.model.Team
import com.yourcompany.partygameapp.ui.settings.SettingStepper
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreGameScreen(
    onStartGame: (String, Int, Int, Int, Int, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PreGameViewModel = hiltViewModel()
) {
    val deck by viewModel.selectedDeck.collectAsState()
    val players by viewModel.playerList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { deck?.name?.let { Text(it) } ?: Text(stringResource(R.string.pregame_loading_deck)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (deck == null) {
                CircularProgressIndicator()
            } else {
                DeckInfoCard(deck = deck!!)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.pregame_select_game_mode), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            GameModeDropdown(
                modes = viewModel.availableGameModes,
                selectedMode = viewModel.selectedGameMode,
                onModeSelected = viewModel::onGameModeSelected,
                isExpanded = viewModel.isGameModeDropdownExpanded,
                onDropdownClick = viewModel::onGameModeDropdownClick,
                onDropdownDismiss = viewModel::onGameModeDropdownDismiss
            )

            // --- HOT POTATO SETTINGS (Only visible when HotPotato is selected) ---
            if (viewModel.selectedGameMode == GameMode.HotPotato) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.hot_potato_settings), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SettingStepper(
                    title = stringResource(R.string.hot_potato_time_per_team),
                    value = viewModel.teamTimeSeconds,
                    onValueChange = viewModel::onTeamTimeChange,
                    range = 60..600, // 1-10 minutes
                    step = 30, // 30 seconds increment
                    unit = stringResource(R.string.settings_unit_seconds)
                )
                SettingStepper(
                    title = stringResource(R.string.hot_potato_max_skips_per_team),
                    value = viewModel.maxSkipsPerTeam,
                    onValueChange = viewModel::onMaxSkipsChange,
                    range = 1..5,
                    step = 1,
                    unit = ""
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Display simplified team information
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.hot_potato_teams_info),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    // No longer showing min players warning here.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TeamDisplayCard(team = Team.RED)
                        TeamDisplayCard(team = Team.BLUE)
                    }
                }

            } // End Hot Potato Settings

            // --- PLAYER SELECTION (Only visible for Standard/3Lives modes) ---
            if (viewModel.selectedGameMode != GameMode.HotPotato) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.pregame_pick_or_add_name), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                PlayerDropdown(
                    players = players,
                    selectedPlayer = viewModel.selectedPlayer,
                    onPlayerSelected = viewModel::onPlayerSelected,
                    isExpanded = viewModel.isPlayerDropdownExpanded,
                    onDropdownClick = viewModel::onDropdownClick,
                    onDropdownDismiss = viewModel::onDropdownDismiss
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = viewModel.newPlayerName,
                    onValueChange = viewModel::onNewPlayerNameChange,
                    label = { Text(stringResource(R.string.pregame_add_new_player)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = viewModel::onAddPlayer, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.pregame_add_player_button))
                }
            }
            // --- END PLAYER SELECTION ---


            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    deck?.id?.let { deckId ->
                        val playerIdForGame: Int = if (viewModel.selectedGameMode != GameMode.HotPotato) {
                            viewModel.selectedPlayer?.id ?: 0 // Use actual selected player or 0 if none
                        } else {
                            // For HotPotato, a dummy player ID (0) is sufficient, as actual players are managed internally
                            0
                        }

                        // Pass all current player IDs (or dummy players if none) for HotPotato
                        val playersForHotPotato: List<Player> = if (viewModel.selectedGameMode == GameMode.HotPotato) {
                            if (players.isEmpty()) {
                                // Provide dummy players if no players exist but HotPotato is selected
                                listOf(Player(id = -1, name = "Red Team Player"), Player(id = -2, name = "Blue Team Player"))
                            } else {
                                players
                            }
                        } else {
                            emptyList() // No players needed as argument for other modes
                        }
                        // Serialize player IDs and names as a map for Hot Potato
                        val assignedPlayersJson = com.google.gson.Gson().toJson(playersForHotPotato.map { it.id to it.name }.toMap())

                        onStartGame(
                            viewModel.selectedGameMode.name,
                            deckId,
                            playerIdForGame, // Pass the dummy ID or actual ID
                            viewModel.teamTimeSeconds,
                            viewModel.maxSkipsPerTeam,
                            assignedPlayersJson
                        )
                    }
                },
                // <--- MODIFIED LINE: Removed players.size >= 2 check for Hot Potato -->
                enabled = deck != null && (viewModel.selectedGameMode != GameMode.HotPotato && viewModel.selectedPlayer != null || viewModel.selectedGameMode == GameMode.HotPotato),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.pregame_start_button), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

// DeckInfoCard, PlayerDropdown, GameModeDropdown, TeamDisplayCard remain unchanged from previous commit.
// They are included here for completeness but the content is the same.

@Composable
fun DeckInfoCard(deck: Deck) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(deck.name, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.pregame_category_label, deck.categoryName), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDropdown(
    players: List<Player>,
    selectedPlayer: Player?,
    onPlayerSelected: (Player) -> Unit,
    isExpanded: Boolean,
    onDropdownClick: () -> Unit,
    onDropdownDismiss: () -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { onDropdownClick() }
    ) {
        OutlinedTextField(
            value = selectedPlayer?.name ?: stringResource(R.string.pregame_select_player_placeholder),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.pregame_player_name_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onDropdownDismiss
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { Text(player.name) },
                    onClick = { onPlayerSelected(player) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeDropdown(
    modes: List<GameMode>,
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit,
    isExpanded: Boolean,
    onDropdownClick: () -> Unit,
    onDropdownDismiss: () -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { onDropdownClick() }
    ) {
        OutlinedTextField(
            value = when (selectedMode) {
                GameMode.Standard -> stringResource(R.string.game_mode_standard)
                GameMode.ThreeLives -> stringResource(R.string.game_mode_three_lives)
                GameMode.HotPotato -> stringResource(R.string.game_mode_hot_potato)
            },
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.pregame_game_mode_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onDropdownDismiss
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(when (mode) {
                            GameMode.Standard -> stringResource(R.string.game_mode_standard)
                            GameMode.ThreeLives -> stringResource(R.string.game_mode_three_lives)
                            GameMode.HotPotato -> stringResource(R.string.game_mode_hot_potato)
                        })
                    },
                    onClick = { onModeSelected(mode) }
                )
            }
        }
    }
}

@Composable
fun TeamDisplayCard(team: Team) {
    val backgroundColor = when (team) {
        Team.RED -> Color.Red.copy(alpha = 0.2f)
        Team.BLUE -> Color.Blue.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (team) {
        Team.RED -> Color.Red
        Team.BLUE -> Color.Blue
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (team) {
                    Team.RED -> stringResource(R.string.hot_potato_red_team)
                    Team.BLUE -> stringResource(R.string.hot_potato_blue_team)
                    Team.NONE -> ""
                },
                color = textColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}