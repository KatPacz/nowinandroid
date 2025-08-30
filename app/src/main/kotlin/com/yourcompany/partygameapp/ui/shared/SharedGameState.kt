// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/ui/shared/SharedGameState.kt

package com.yourcompany.partygameapp.ui.shared

import com.yourcompany.partygameapp.domain.model.CardResult
import com.yourcompany.partygameapp.domain.model.Team
import javax.inject.Inject
import javax.inject.Singleton

// This is now a simple class that holds mutable state, injected as a Singleton.
// It does NOT extend ViewModel.
@Singleton // This annotation tells Hilt to create one instance for the entire app.
class SharedGameState @Inject constructor() {
    var results: List<CardResult> = emptyList()
    var playerId: Int = 0
    var playerName: String = "Player"
    var redTeamScore: Int? = null
    var blueTeamScore: Int? = null
    var winningTeam: Team? = null
}