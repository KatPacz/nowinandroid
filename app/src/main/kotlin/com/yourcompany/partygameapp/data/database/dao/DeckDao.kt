// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/data/database/dao/DeckDao.kt
package com.yourcompany.partygameapp.data.database.dao

import androidx.room.*
import com.yourcompany.partygameapp.data.database.entity.DeckEntity
import com.yourcompany.partygameapp.data.database.tuple.DeckWithCategoryTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun getDeckCount(): Int

    @Transaction
    @Query("""
        SELECT
            decks.*,
            categories.id AS category_id,
            categories.name AS category_name,
            categories.colorHex AS category_colorHex,
            categories.languageCode AS category_languageCode  /* <--- NEW LINE */
        FROM decks
        INNER JOIN categories ON decks.categoryId = categories.id
        WHERE decks.languageCode = :languageCode
    """)
    fun getDecksWithCategoryByLanguage(languageCode: String): Flow<List<DeckWithCategoryTuple>>

    @Transaction
    @Query("""
        SELECT
            decks.*,
            categories.id AS category_id,
            categories.name AS category_name,
            categories.colorHex AS category_colorHex,
            categories.languageCode AS category_languageCode  /* <--- NEW LINE */
        FROM decks
        INNER JOIN categories ON decks.categoryId = categories.id
        WHERE decks.id = :deckId
    """)
    fun getDeckWithCategoryById(deckId: Int): Flow<DeckWithCategoryTuple?>

    @Transaction
    @Query("""
        SELECT
            decks.*,
            categories.id AS category_id,
            categories.name AS category_name,
            categories.colorHex AS category_colorHex,
            categories.languageCode AS category_languageCode  /* <--- NEW LINE */
        FROM decks
        INNER JOIN categories ON decks.categoryId = categories.id
    """)
    fun getAllDecksWithCategory(): Flow<List<DeckWithCategoryTuple>>
}