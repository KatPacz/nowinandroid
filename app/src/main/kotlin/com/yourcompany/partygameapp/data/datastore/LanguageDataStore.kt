// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/data/datastore/LanguageDataStore.kt

package com.yourcompany.partygameapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Create a DataStore instance for language preferences
private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

@Singleton
class LanguageDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_LANGUAGE_CODE = stringPreferencesKey("selected_language_code")
    }

    // Flow to expose the selected language
    val selectedLanguageCode: Flow<String> = context.languageDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE_CODE] ?: "en" // Default to English
        }

    // Function to save the selected language
    suspend fun saveSelectedLanguageCode(languageCode: String) {
        context.languageDataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE_CODE] = languageCode
        }
    }
}