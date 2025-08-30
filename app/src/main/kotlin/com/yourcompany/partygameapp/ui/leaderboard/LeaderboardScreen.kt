// FILE: .../ui/leaderboard/LeaderboardScreen.kt

package com.yourcompany.partygameapp.ui.leaderboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.yourcompany.partygameapp.R
import com.yourcompany.partygameapp.data.database.entity.PlayerEntity
import dagger.hilt.android.lifecycle.HiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val players by viewModel.players.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.leaderboard_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (players.isEmpty()) {
                // <<< START OF NEW CODE: Empty State >>>
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.leaderboard_no_players),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
                // <<< END OF NEW CODE >>>
            } else {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.leaderboard_rank), modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.leaderboard_player), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.SportsEsports, contentDescription = stringResource(R.string.leaderboard_games_played), modifier = Modifier.width(50.dp))
                    Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.leaderboard_correct_guesses), modifier = Modifier.width(50.dp))
                    Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.leaderboard_total_points), modifier = Modifier.width(50.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                // Player List
                LazyColumn {
                    itemsIndexed(players) { index, player ->
                        PlayerStatRow(rank = index + 1, player = player)
                    }
                }
            }
        }
    }
}

// ... (PlayerStatRow is correct) ...
@Composable
fun PlayerStatRow(rank: Int, player: PlayerEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$rank", modifier = Modifier.width(50.dp))
        Text(text = player.name, modifier = Modifier.weight(1f))
        Text(
            text = "${player.gamesPlayed}",
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "${player.totalCorrectGuesses}",
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "${player.totalPoints}",
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}
