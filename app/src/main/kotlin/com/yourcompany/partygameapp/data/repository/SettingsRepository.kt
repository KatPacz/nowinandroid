// FILE: SettingsRepository.kt

package com.yourcompany.partygameapp.data.repository

import com.yourcompany.partygameapp.data.datastore.SettingsDataStore
import com.yourcompany.partygameapp.domain.model.GameSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    val gameSettingsFlow: Flow<GameSettings> = settingsDataStore.settingsFlow

    suspend fun setTimeUpPenalty(isEnabled: Boolean) {
        settingsDataStore.setTimeUpPenalty(isEnabled)
    }

    suspend fun setGiveUpPenalty(isEnabled: Boolean) {
        settingsDataStore.setGiveUpPenalty(isEnabled)
    }

    suspend fun setCardTime(seconds: Int) {
        settingsDataStore.setCardTime(seconds)
    }

    suspend fun setCardCount(count: Int) {
        settingsDataStore.setCardCount(count)
    }
}