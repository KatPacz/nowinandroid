// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/MainActivity.kt
package com.yourcompany.partygameapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.yourcompany.partygameapp.ui.aftergame.AfterGameScreen
import com.yourcompany.partygameapp.ui.aftergame.AfterGameViewModel
import com.yourcompany.partygameapp.ui.components.BottomNavBar
import com.yourcompany.partygameapp.ui.gameplay.GamePlayScreen
import com.yourcompany.partygameapp.ui.home.HomeScreen
import com.yourcompany.partygameapp.ui.importexport.ExportScreen
import com.yourcompany.partygameapp.ui.importexport.ImportScreen
import com.yourcompany.partygameapp.ui.leaderboard.LeaderboardScreen
import com.yourcompany.partygameapp.ui.navigation.Routes
import com.yourcompany.partygameapp.ui.pregame.PreGameScreen
import com.yourcompany.partygameapp.ui.pregame.PreGameViewModel
import com.yourcompany.partygameapp.ui.settings.SettingsScreen
import com.yourcompany.partygameapp.ui.shared.SharedGameState
import com.yourcompany.partygameapp.ui.theme.PartyGameAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sharedViewModel: SharedGameState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            PartyGameAppTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute in listOf(Routes.HOME, Routes.IMPORT, Routes.LEADERBOARD, Routes.SETTINGS)) {
                            BottomNavBar(onNavigate = { route ->
                                if (route != currentRoute) {
                                    navController.navigate(route)
                                }
                            })
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(route = Routes.HOME) {
                            HomeScreen(
                                onDeckSelected = { deckId -> navController.navigate("pre_game/$deckId") },
                                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                                onNavigateToLeaderboard = { navController.navigate(Routes.LEADERBOARD) }
                            )
                        }
                        composable(
                            route = Routes.PRE_GAME,
                            arguments = listOf(navArgument(Routes.ARG_DECK_ID) { type = NavType.IntType })
                        ) {
                            val preGameViewModel: PreGameViewModel = hiltViewModel()
                            PreGameScreen(
                                viewModel = preGameViewModel,
                                onStartGame = { gameMode, deckId, playerId, teamTime, maxSkips, playersJson ->
                                    sharedViewModel.playerId = playerId
                                    sharedViewModel.playerName = preGameViewModel.playerList.value.find { it.id == playerId }?.name ?: "Player"

                                    // For Hot Potato, we pass the entire list of *all available players*
                                    // The GamePlayViewModel will then randomly assign them to teams.
                                    val encodedPlayersJson = Uri.encode(playersJson) // Encode the JSON string for URI safety

                                    // Always navigate with all 6 arguments to match the GAMEPLAY route pattern
                                    navController.navigate("gameplay/$deckId/$playerId/$gameMode/$teamTime/$maxSkips/$encodedPlayersJson")
                                },
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable(
                            route = Routes.GAMEPLAY,
                            arguments = listOf(
                                navArgument(Routes.ARG_DECK_ID) { type = NavType.IntType },
                                navArgument(Routes.ARG_PLAYER_ID) { type = NavType.IntType },
                                navArgument(Routes.ARG_GAME_MODE) { type = NavType.StringType },
                                navArgument(Routes.ARG_HOT_POTATO_TEAM_TIME) { type = NavType.IntType; defaultValue = 0 },
                                navArgument(Routes.ARG_HOT_POTATO_MAX_SKIPS) { type = NavType.IntType; defaultValue = 0 },
                                navArgument(Routes.ARG_HOT_POTATO_PLAYERS_JSON) { type = NavType.StringType; defaultValue = "[]" }
                            )
                        ) {
                            GamePlayScreen(
                                onGameFinished = { results, redScore, blueScore, winningTeam ->
                                    sharedViewModel.results = results
                                    sharedViewModel.redTeamScore = redScore
                                    sharedViewModel.blueTeamScore = blueScore
                                    sharedViewModel.winningTeam = winningTeam
                                    // Navigate to AfterGame, popping up to PreGame to allow easy replay
                                    navController.navigate(Routes.AFTER_GAME) { popUpTo(Routes.PRE_GAME) { inclusive = true } }
                                }
                            )
                        }
                        composable(route = Routes.AFTER_GAME) {
                            val afterGameViewModel: AfterGameViewModel = hiltViewModel()
                            LaunchedEffect(Unit) {
                                afterGameViewModel.saveGameResults(sharedViewModel.playerId, sharedViewModel.results)
                            }
                            AfterGameScreen(
                                results = sharedViewModel.results,
                                playerName = sharedViewModel.playerName,
                                redTeamScore = sharedViewModel.redTeamScore,
                                blueTeamScore = sharedViewModel.blueTeamScore,
                                winningTeam = sharedViewModel.winningTeam,
                                onPlayAgain = { navController.navigateUp() }, // Go back to PreGame to replay
                                onGoHome = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
                            )
                        }
                        composable(Routes.LEADERBOARD) { LeaderboardScreen(onNavigateBack = { navController.navigateUp() }) }
                        composable(Routes.SETTINGS) { SettingsScreen(onNavigateBack = { navController.navigateUp() }) }
                        composable(Routes.IMPORT) { ImportScreen(onNavigateBack = { navController.navigateUp() }) }
                        composable(Routes.EXPORT) { ExportScreen(onNavigateBack = { navController.navigateUp() }) }
                    }
                }
            }
        }
    }
}