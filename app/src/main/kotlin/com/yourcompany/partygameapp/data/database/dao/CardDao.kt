// FILE: CardDao.kt

package com.yourcompany.partygameapp.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourcompany.partygameapp.data.database.entity.CardEntity

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Query("SELECT * FROM cards WHERE deckId = :deckId")
    suspend fun getCardsForDeck(deckId: Int): List<CardEntity>
}