// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/data/database/tuple/DeckWithCategoryTuple.kt
package com.yourcompany.partygameapp.data.database.tuple

import androidx.room.Embedded
import com.yourcompany.partygameapp.data.database.entity.CategoryEntity
import com.yourcompany.partygameapp.data.database.entity.DeckEntity

data class DeckWithCategoryTuple(
    @Embedded val deck: DeckEntity,
    @Embedded(prefix = "category_") val category: CategoryEntity // <--- ENSURE prefix = "category_" is present
)