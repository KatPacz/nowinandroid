// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/di/AppModule.kt

package com.yourcompany.partygameapp.di

import android.content.Context
import android.content.res.AssetManager
import androidx.room.Room
import com.yourcompany.partygameapp.data.database.AppDatabase
import com.yourcompany.partygameapp.data.database.DatabaseCallback
import com.yourcompany.partygameapp.data.database.dao.*
import com.yourcompany.partygameapp.data.datastore.LanguageDataStore // <--- NEW IMPORT
import com.yourcompany.partygameapp.data.datastore.SettingsDataStore
import com.yourcompany.partygameapp.data.repository.DeckRepository
import com.yourcompany.partygameapp.data.repository.PlayerRepository
import com.yourcompany.partygameapp.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>,
        deckDaoProvider: Provider<DeckDao>,
        cardDaoProvider: Provider<CardDao>
    ): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "party_game_app_db")
            .addCallback(DatabaseCallback(context, categoryDaoProvider, deckDaoProvider, cardDaoProvider))
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAssetManager(@ApplicationContext context: Context): AssetManager = context.assets

    // --- DAO Providers ---
    @Provides @Singleton fun provideDeckDao(db: AppDatabase): DeckDao = db.deckDao()
    @Provides @Singleton fun providePlayerDao(db: AppDatabase): PlayerDao = db.playerDao()
    @Provides @Singleton fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides @Singleton fun provideCardDao(db: AppDatabase): CardDao = db.cardDao()

    // --- DataStore Providers ---
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides // <--- NEW CODE BLOCK START: Provide LanguageDataStore
    @Singleton
    fun provideLanguageDataStore(@ApplicationContext context: Context): LanguageDataStore {
        return LanguageDataStore(context)
    }
    // <--- NEW CODE BLOCK END

    // --- Repository Providers ---
    @Provides @Singleton
    fun provideDeckRepository(deckDao: DeckDao, cardDao: CardDao, categoryDao: CategoryDao): DeckRepository =
        DeckRepository(deckDao, cardDao, categoryDao)

    @Provides @Singleton
    fun providePlayerRepository(playerDao: PlayerDao): PlayerRepository = PlayerRepository(playerDao)

    @Provides @Singleton
    fun provideSettingsRepository(settingsDataStore: SettingsDataStore): SettingsRepository =
        SettingsRepository(settingsDataStore)
}