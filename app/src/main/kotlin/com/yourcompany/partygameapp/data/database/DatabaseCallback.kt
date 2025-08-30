// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/data/database/DatabaseCallback.kt
package com.yourcompany.partygameapp.data.database

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yourcompany.partygameapp.data.database.dao.CardDao
import com.yourcompany.partygameapp.data.database.dao.CategoryDao
import com.yourcompany.partygameapp.data.database.dao.DeckDao
import com.yourcompany.partygameapp.data.database.entity.CardEntity
import com.yourcompany.partygameapp.data.database.entity.CategoryEntity
import com.yourcompany.partygameapp.data.database.entity.DeckEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import javax.inject.Provider

class DatabaseCallback(
    private val context: Context,
    private val categoryDaoProvider: Provider<CategoryDao>,
    private val deckDaoProvider: Provider<DeckDao>,
    private val cardDaoProvider: Provider<CardDao>
) : RoomDatabase.Callback() {

    private val TAG = "DATABASE_DEBUG"

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        Log.d(TAG, "----------------------------------------------------")
        Log.d(TAG, "DATABASE OPENED. Checking if population is needed.")
        Log.d(TAG, "----------------------------------------------------")
        CoroutineScope(Dispatchers.IO).launch {
            checkAndPopulate()
        }
    }

    private suspend fun checkAndPopulate() {
        try {
            // Check if any categories exist for any language, or if no decks exist at all.
            // A more robust check might involve counting total categories/decks or using a preference flag.
            // For simplicity, let's assume if 'en' categories are missing, we need to populate.
            val enCategoryCount = categoryDaoProvider.get().getAllCategories("en").first().size
            if (enCategoryCount == 0) { // If main language categories are empty, assume fresh install
                Log.d(TAG, "DATABASE IS EMPTY (or 'en' categories missing). Starting population process...")
                populateFromAssets()
            } else {
                Log.d(TAG, "Database already has data. Skipping population.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR in checkAndPopulate", e)
        }
    }

    private suspend fun populateFromAssets() {
        Log.d(TAG, "-> Starting populateFromAssets().")
        val deckDao = deckDaoProvider.get()
        val categoryDao = categoryDaoProvider.get()
        val cardDao = cardDaoProvider.get()
        val assetManager = context.assets

        val colorPalette = listOf("#F4A261", "#6B8E85", "#4A5859", "#2A2D43", "#E9C46A", "#F4A261", "#E76F51")
        var colorIndex = 0

        try {
            val langFolders = assetManager.list("decks")
            if (langFolders.isNullOrEmpty()) {
                Log.e(TAG, "CRITICAL ERROR: 'decks' folder in assets is empty or does not exist.")
                return
            }
            Log.d(TAG, "Found language folders: ${langFolders.joinToString()}")

            for (langFolder in langFolders) {
                val deckFiles = assetManager.list("decks/$langFolder")
                if (deckFiles.isNullOrEmpty()) {
                    Log.w(TAG, "Warning: Language folder 'decks/$langFolder' is empty.")
                    continue
                }
                Log.d(TAG, "In '$langFolder', found deck files: ${deckFiles.joinToString()}")

                for (fileName in deckFiles) {
                    if (!fileName.endsWith(".txt")) continue
                    val categoryName = fileName.removeSuffix(".txt").replaceFirstChar { it.titlecase(Locale.ROOT) }
                    val color = colorPalette[colorIndex % colorPalette.size]
                    colorIndex++

                    val languageCode = langFolder.lowercase(Locale.ROOT) // Use the folder name as language code
                    Log.d(TAG, "Processing: Lang='$languageCode', Category='$categoryName'")

                    // <--- MODIFIED LINE: Pass languageCode to getCategoryByName and insertCategory --->
                    var existingCategory = categoryDao.getCategoryByName(categoryName, languageCode)
                    val categoryId = if (existingCategory != null) {
                        existingCategory.id
                    } else {
                        categoryDao.insertCategory(CategoryEntity(name = categoryName, colorHex = color, languageCode = languageCode)).toInt()
                    }
                    Log.d(TAG, "  - Processed category '$categoryName' with ID: $categoryId for lang '$languageCode'")


                    val deckId = deckDao.insertDeck(DeckEntity(
                        name = categoryName,
                        description = "A collection of $categoryName",
                        categoryId = categoryId, // categoryId is already an Int
                        languageCode = languageCode // <--- MODIFIED LINE: Pass languageCode to DeckEntity
                    ))
                    Log.d(TAG, "  - Inserted deck '$categoryName' with ID: $deckId for lang '$languageCode'")

                    val path = "decks/$langFolder/$fileName"
                    assetManager.open(path).bufferedReader().useLines { lines ->
                        lines.filter { it.isNotBlank() }.forEach { line ->
                            cardDao.insertCard(CardEntity(text = line, deckId = deckId.toInt()))
                        }
                    }
                    Log.d(TAG, "  - Successfully inserted cards from $path")
                }
            }
            Log.d(TAG, "-> DATABASE POPULATION COMPLETE.")
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL UNEXPECTED ERROR during population", e)
        }
    }
}