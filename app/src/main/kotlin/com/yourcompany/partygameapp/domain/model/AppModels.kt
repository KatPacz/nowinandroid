// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/domain/model/AppModels.kt

package com.yourcompany.partygameapp.domain.model

data class DeckWithCategory(val deckId: Int, val deckName: String, val categoryName: String, val categoryColorHex: String)
data class Deck(val id: Int, val name: String, val categoryName: String)
data class Card(val text: String)
data class Player(val id: Int = 0, val name: String)
// <<< FIX: Add playerId to CardResult >>>
data class CardResult(val cardText: String, val points: Int, val playerId: Int? = null)
data class GameSettings(val timeUpPenalty: Boolean, val giveUpPenalty: Boolean, val cardTime: Int, val cardCount: Int)
enum class GameMode { Standard, ThreeLives, HotPotato }
enum class Team { RED, BLUE, NONE }
data class HotPotatoSettings(val teamTimeSeconds: Int, val maxSkipsPerTeam: Int)