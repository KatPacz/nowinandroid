// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/data/repository/DeckRepository.kt
package com.yourcompany.partygameapp.data.repository

import com.yourcompany.partygameapp.data.database.dao.CardDao
import com.yourcompany.partygameapp.data.database.dao.CategoryDao
import com.yourcompany.partygameapp.data.database.dao.DeckDao
import com.yourcompany.partygameapp.data.database.entity.CardEntity
import com.yourcompany.partygameapp.data.database.entity.CategoryEntity
import com.yourcompany.partygameapp.data.database.entity.DeckEntity
import com.yourcompany.partygameapp.domain.model.Card
import com.yourcompany.partygameapp.domain.model.Deck
import com.yourcompany.partygameapp.domain.model.DeckWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeckRepository @Inject constructor(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val categoryDao: CategoryDao
) {
    // <--- MODIFIED LINE: Pass languageCode to DAO
    fun getDecksWithCategoryByLanguage(languageCode: String): Flow<List<DeckWithCategory>> =
        deckDao.getDecksWithCategoryByLanguage(languageCode).map { list ->
            list.map { tuple ->
                DeckWithCategory(tuple.deck.id, tuple.deck.name, tuple.category.name, tuple.category.colorHex)
            }
        }

    fun getDeckById(deckId: Int): Flow<Deck?> =
        deckDao.getDeckWithCategoryById(deckId).map { tuple ->
            tuple?.let { Deck(it.deck.id, it.deck.name, it.category.name) }
        }

    suspend fun getCardsForDeck(deckId: Int): List<Card> =
        cardDao.getCardsForDeck(deckId).map { Card(it.text) }

    // <--- MODIFIED LINE: Pass languageCode to DAO
    fun getAllCategories(languageCode: String): Flow<List<CategoryEntity>> = categoryDao.getAllCategories(languageCode)

    // <--- MODIFIED FUNCTION: Added languageCode parameter
    private suspend fun getOrCreateCategory(name: String, languageCode: String): Int {
        // <--- MODIFIED LINE: Pass languageCode to getCategoryByName
        val existingCategory = categoryDao.getCategoryByName(name, languageCode)
        if (existingCategory != null) {
            return existingCategory.id
        } else {
            val colorPalette = listOf("#F4A261", "#6B8E85", "#4A5859", "#2A2D43", "#E9C46A", "#F4A261", "#E76F51")
            val newCategory = CategoryEntity(name = name, colorHex = colorPalette.random(), languageCode = languageCode) // <--- ADDED languageCode
            return categoryDao.insertCategory(newCategory).toInt()
        }
    }

    // <--- MODIFIED FUNCTION: importNewDeck now passes languageCode to getOrCreateCategory
    suspend fun importNewDeck(deckName: String, categoryName: String, languageCode: String, cardLines: List<String>) {
        val categoryId = getOrCreateCategory(categoryName, languageCode) // <--- MODIFIED LINE
        val newDeck = DeckEntity(
            name = deckName,
            description = "Imported deck",
            categoryId = categoryId,
            languageCode = languageCode.lowercase()
        )
        val newDeckId = deckDao.insertDeck(newDeck).toInt()
        cardLines.forEach { line ->
            if (line.isNotBlank()) {
                val card = CardEntity(text = line, deckId = newDeckId)
                cardDao.insertCard(card)
            }
        }
    }

    fun getAllDecksForExport(): Flow<List<DeckWithCategory>> {
        return deckDao.getAllDecksWithCategory().map { list ->
            list.map { tuple ->
                DeckWithCategory(tuple.deck.id, tuple.deck.name, tuple.category.name, tuple.category.colorHex)
            }
        }
    }
}