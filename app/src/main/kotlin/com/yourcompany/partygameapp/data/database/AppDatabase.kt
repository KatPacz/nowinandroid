// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/data/database/AppDatabase.kt
package com.yourcompany.partygameapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yourcompany.partygameapp.data.database.dao.*
import com.yourcompany.partygameapp.data.database.entity.*

@Database(
    entities = [
        CategoryEntity::class,
        DeckEntity::class,
        CardEntity::class,
        PlayerEntity::class
    ],
    version = 3, // <--- MODIFIED LINE: Increment database version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun deckDao(): DeckDao
    abstract fun playerDao(): PlayerDao
}