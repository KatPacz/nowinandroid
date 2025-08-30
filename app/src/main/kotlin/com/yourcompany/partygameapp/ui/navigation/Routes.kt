// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/ui/navigation/Routes.kt
package com.yourcompany.partygameapp.ui.navigation
import android.net.Uri

object Routes {
    const val ARG_DECK_ID = "deckId"
    const val ARG_PLAYER_ID = "playerId"
    const val ARG_GAME_MODE = "gameMode" // Fixed typo here (removed duplicate 'val')

    const val ARG_HOT_POTATO_TEAM_TIME = "teamTime"
    const val ARG_HOT_POTATO_MAX_SKIPS = "maxSkips"
    const val ARG_HOT_POTATO_PLAYERS_JSON = "playersJson"

    const val HOME = "home"
    const val PRE_GAME = "pre_game/{$ARG_DECK_ID}"
    // Ensure this route matches the number of arguments in the navigation call
    const val GAMEPLAY = "gameplay/{$ARG_DECK_ID}/{$ARG_PLAYER_ID}/{$ARG_GAME_MODE}/{$ARG_HOT_POTATO_TEAM_TIME}/{$ARG_HOT_POTATO_MAX_SKIPS}/{$ARG_HOT_POTATO_PLAYERS_JSON}"
    const val AFTER_GAME = "after_game"
    const val IMPORT = "import"
    const val EXPORT = "export"
    const val LEADERBOARD = "leaderboard"
    const val SETTINGS = "settings"
}