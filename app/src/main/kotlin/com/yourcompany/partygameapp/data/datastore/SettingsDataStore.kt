// FILE: SettingsDataStore.kt

package com.yourcompany.partygameapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.yourcompany.partygameapp.domain.model.GameSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val TIME_UP_PENALTY = booleanPreferencesKey("time_up_penalty")
        val GIVE_UP_PENALTY = booleanPreferencesKey("give_up_penalty")
        val CARD_TIME_SECONDS = intPreferencesKey("card_time_seconds")
        val CARD_COUNT = intPreferencesKey("card_count")
    }

    val settingsFlow: Flow<GameSettings> = context.dataStore.data
        .map { preferences ->
            GameSettings(
                timeUpPenalty = preferences[Keys.TIME_UP_PENALTY] ?: true,
                giveUpPenalty = preferences[Keys.GIVE_UP_PENALTY] ?: false,
                cardTime = preferences[Keys.CARD_TIME_SECONDS] ?: 20,
                cardCount = preferences[Keys.CARD_COUNT] ?: 10
            )
        }

    suspend fun setTimeUpPenalty(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TIME_UP_PENALTY] = isEnabled
        }
    }

    suspend fun setGiveUpPenalty(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.GIVE_UP_PENALTY] = isEnabled
        }
    }

    suspend fun setCardTime(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CARD_TIME_SECONDS] = seconds
        }
    }

    suspend fun setCardCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CARD_COUNT] = count
        }
    }
}