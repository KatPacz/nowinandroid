// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/data/database/dao/CategoryDao.kt
package com.yourcompany.partygameapp.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourcompany.partygameapp.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    // <--- MODIFIED LINE: Added languageCode to the WHERE clause
    @Query("SELECT * FROM categories WHERE name = :name AND languageCode = :languageCode LIMIT 1")
    suspend fun getCategoryByName(name: String, languageCode: String): CategoryEntity?

    // <--- MODIFIED LINE: Added languageCode to the WHERE clause for filtering
    @Query("SELECT * FROM categories WHERE languageCode = :languageCode ORDER BY name ASC")
    fun getAllCategories(languageCode: String): Flow<List<CategoryEntity>>
}