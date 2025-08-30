// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/ui/aftergame/AfterGameScreen.kt

package com.yourcompany.partygameapp.ui.aftergame

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.EmojiEvents // For winning team medal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourcompany.partygameapp.R
import com.yourcompany.partygameapp.domain.model.CardResult
import com.yourcompany.partygameapp.domain.model.Team

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterGameScreen(
    results: List<CardResult>,
    playerName: String,
    redTeamScore: Int? = null, // Hot Potato specific
    blueTeamScore: Int? = null, // Hot Potato specific
    winningTeam: Team? = null, // Hot Potato specific
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    val totalScore = results.sumOf { it.points }.coerceAtLeast(0)
    val correctAnswers = results.count { it.points > 0 }
    val skippedOrFailed = results.count { it.points <= 0 }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.aftergame_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display winner for Hot Potato, otherwise general congrats
            if (winningTeam != null && winningTeam != Team.NONE) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = stringResource(R.string.aftergame_winning_team_label, winningTeam.name),
                    modifier = Modifier.size(64.dp),
                    tint = if (winningTeam == Team.RED) Color.Red else Color.Blue
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.aftergame_winning_team_label, winningTeam.name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (winningTeam == Team.RED) Color.Red else Color.Blue
                )
            } else {
                Text(stringResource(R.string.aftergame_congrats, playerName), style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(Modifier.height(16.dp))

            // Display scores based on game mode
            if (redTeamScore != null && blueTeamScore != null) {
                // Hot Potato Score Display
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.hot_potato_red_team) + " $redTeamScore", style = MaterialTheme.typography.titleLarge, color = Color.Red, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.hot_potato_blue_team) + " $blueTeamScore", style = MaterialTheme.typography.titleLarge, color = Color.Blue, fontWeight = FontWeight.Bold)
                }
            } else {
                // Standard/3Lives Score Display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.aftergame_correct_label), tint = Color.Green)
                    Text(" $correctAnswers", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(24.dp))
                    Icon(imageVector = Icons.Filled.Dangerous, contentDescription = stringResource(R.string.aftergame_incorrect_label), tint = Color.Red)
                    Text(" $skippedOrFailed", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.aftergame_score) + " $totalScore", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results) { result -> ResultRow(result = result) }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onGoHome, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.aftergame_main_menu_button)) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onPlayAgain, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.aftergame_play_again_button)) }
            }
        }
    }
}

@Composable
fun ResultRow(result: CardResult) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(result.cardText, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = when {
                result.points > 0 -> "+${result.points}"
                else -> "${result.points}"
            },
            color = if (result.points > 0) Color.Green else if (result.points < 0) Color.Red else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}