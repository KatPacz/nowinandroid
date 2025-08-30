// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/data/database/entity/CategoryEntity.kt
package com.yourcompany.partygameapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val languageCode: String // <--- NEW LINE: Add languageCode
)