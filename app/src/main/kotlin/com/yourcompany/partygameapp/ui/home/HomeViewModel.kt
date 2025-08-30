// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/ui/home/HomeViewModel.kt

package com.yourcompany.partygameapp.ui.home

import android.content.Context
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.partygameapp.data.repository.DeckRepository
import com.yourcompany.partygameapp.domain.model.DeckWithCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import com.yourcompany.partygameapp.data.datastore.LanguageDataStore
import kotlinx.coroutines.launch

sealed interface HomeScreenUiState {
    object Loading : HomeScreenUiState
    data class Success(val decks: List<DeckWithCategory>) : HomeScreenUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val assetManager: AssetManager,
    @ApplicationContext private val context: Context,
    private val languageDataStore: LanguageDataStore
) : ViewModel() {

    val availableLanguages: List<String>

    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    private val _isMenuExpanded = MutableStateFlow(false)
    val isMenuExpanded: StateFlow<Boolean> = _isMenuExpanded

    val uiState: StateFlow<HomeScreenUiState> = _selectedLanguage.flatMapLatest { lang ->
        // <--- MODIFIED LINE: Pass 'lang' to the repository for filtering
        deckRepository.getDecksWithCategoryByLanguage(lang.lowercase(Locale.ROOT))
            .map { decks -> HomeScreenUiState.Success(decks) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeScreenUiState.Loading
    )

    init {
        availableLanguages = getLanguagesFromAssets()
        viewModelScope.launch {
            languageDataStore.selectedLanguageCode.collectLatest { savedLang ->
                val effectiveLanguage = if (availableLanguages.contains(savedLang)) savedLang else "en"
                _selectedLanguage.value = effectiveLanguage
                setAppLocale(effectiveLanguage)
            }
        }
    }

    private fun getLanguagesFromAssets(): List<String> {
        return try {
            val langFolders = assetManager.list("decks") ?: emptyArray()
            langFolders.map { it.lowercase(Locale.ROOT) }.sorted()
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun onLanguageSelected(languageCode: String) {
        viewModelScope.launch {
            languageDataStore.saveSelectedLanguageCode(languageCode)
            _selectedLanguage.value = languageCode
            _isMenuExpanded.value = false
            setAppLocale(languageCode)
        }
    }

    fun onLanguageMenuClick() {
        _isMenuExpanded.value = true
    }

    fun onLanguageMenuDismiss() {
        _isMenuExpanded.value = false
    }

    private fun setAppLocale(languageCode: String) {
        val localeListCompat = LocaleListCompat.create(Locale(languageCode))
        AppCompatDelegate.setApplicationLocales(localeListCompat)
    }
}