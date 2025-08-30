// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/ui/importexport/ImportExportViewModel.kt

package com.yourcompany.partygameapp.ui.importexport

import com.yourcompany.partygameapp.R
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.partygameapp.data.database.entity.CategoryEntity
import com.yourcompany.partygameapp.data.repository.DeckRepository
import com.yourcompany.partygameapp.domain.model.DeckWithCategory
import com.yourcompany.partygameapp.util.FileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import javax.inject.Inject
import android.content.res.AssetManager
import com.yourcompany.partygameapp.data.datastore.LanguageDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class DeckToExport(val suggestedName: String, val content: List<String>)

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val fileHelper: FileHelper,
    @ApplicationContext private val context: Context,
    private val assetManager: AssetManager,
    private val languageDataStore: LanguageDataStore
) : ViewModel() {

    // --- IMPORT STATE ---
    var selectedFileUri by mutableStateOf<Uri?>(null)
        private set
    var selectedFileName by mutableStateOf<String?>(null)
        private set
    var newDeckName by mutableStateOf("")
        private set
    var selectedCategoryName by mutableStateOf("")
        private set

    private val _availableImportLanguages = MutableStateFlow<List<String>>(emptyList())
    val availableImportLanguages: StateFlow<List<String>> = _availableImportLanguages

    // <--- MOVED: Declare _selectedImportLanguage and selectedImportLanguage BEFORE categories --- >
    private val _selectedImportLanguage = MutableStateFlow("en")
    val selectedImportLanguage: StateFlow<String> = _selectedImportLanguage
    // <--- END MOVED BLOCK --- >

    private val _isLanguageDropdownExpanded = MutableStateFlow(false)
    val isLanguageDropdownExpanded: StateFlow<Boolean> = _isLanguageDropdownExpanded

    // Categories now depend on selectedImportLanguage
    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryEntity>> = selectedImportLanguage.flatMapLatest { langCode ->
        deckRepository.getAllCategories(langCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        viewModelScope.launch {
            _availableImportLanguages.value = getLanguagesFromAssets(assetManager)
            languageDataStore.selectedLanguageCode.firstOrNull()?.let { savedLang ->
                _selectedImportLanguage.value = savedLang
            } ?: run {
                val systemDefault = Locale.getDefault().language.lowercase(Locale.ROOT)
                _selectedImportLanguage.value = if (_availableImportLanguages.value.contains(systemDefault)) systemDefault else _availableImportLanguages.value.firstOrNull() ?: "en"
            }
        }
    }

    fun onFileSelected(uri: Uri?) {
        selectedFileUri = uri
        if (uri != null) {
            val fileName = fileHelper.getFileNameFromUri(uri)
            selectedFileName = fileName
            newDeckName = fileName?.removeSuffix(".txt")?.removeSuffix(".csv") ?: ""
        } else {
            selectedFileName = null
            newDeckName = ""
        }
    }
    fun onDeckNameChange(name: String) { newDeckName = name }
    fun onCategoryChange(name: String) { selectedCategoryName = name }

    fun onLanguageSelectedForImport(languageCode: String) {
        _selectedImportLanguage.value = languageCode
        _isLanguageDropdownExpanded.value = false
    }

    fun onLanguageDropdownClick() {
        _isLanguageDropdownExpanded.value = true
    }

    fun onLanguageDropdownDismiss() {
        _isLanguageDropdownExpanded.value = false
    }

    fun onSave() {
        val uri = selectedFileUri ?: return
        if (newDeckName.isBlank() || selectedCategoryName.isBlank()) {
            Toast.makeText(context, "Deck name and category cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        val importLanguageCode = selectedImportLanguage.value

        viewModelScope.launch {
            try {
                val lines = readFileContent(uri)
                deckRepository.importNewDeck(newDeckName.trim(), selectedCategoryName.trim(), importLanguageCode, lines)
                Toast.makeText(
                    context,
                    context.getString(R.string.import_success_toast, newDeckName),
                    Toast.LENGTH_SHORT
                ).show()
                resetState()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun readFileContent(uri: Uri): List<String> {
        val stringList = mutableListOf<String>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readLines().forEach { stringList.add(it) }
            }
        }
        return stringList
    }

    private fun resetState() {
        selectedFileUri = null
        selectedFileName = null
        newDeckName = ""
        selectedCategoryName = ""
    }


    // --- EXPORT STATE ---
    val allDecksForExport: StateFlow<List<DeckWithCategory>> = deckRepository.getAllDecksForExport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val checkedDeckIds = mutableStateMapOf<Int, Boolean>()

    private val _deckToExport = MutableStateFlow<DeckToExport?>(null)
    val deckToExport: StateFlow<DeckToExport?> = _deckToExport


    // --- EXPORT EVENTS ---
    fun onDeckCheckedChanged(deckId: Int, isChecked: Boolean) { checkedDeckIds[deckId] = isChecked }
    fun onSelectAll(decks: List<DeckWithCategory>) { decks.forEach { checkedDeckIds[it.deckId] = true } }
    fun onDeselectAll() { checkedDeckIds.clear() }

    fun onExport() {
        val selectedIds = checkedDeckIds.filter { it.value }.keys
        if (selectedIds.isEmpty()) {
            Toast.makeText(context, "No decks selected", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val firstDeckId = selectedIds.first()
            val deckInfo = allDecksForExport.value.find { it.deckId == firstDeckId } ?: return@launch

            val cards = deckRepository.getCardsForDeck(firstDeckId)
            val cardLines = cards.map { it.text }

            _deckToExport.value = DeckToExport(
                suggestedName = "${deckInfo.deckName}.txt",
                content = cardLines
            )
        }
    }

    fun onFileExported() {
        _deckToExport.value = null
        checkedDeckIds.clear()
        Toast.makeText(context, "Deck exported successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun getLanguagesFromAssets(assetManager: AssetManager): List<String> {
        return try {
            assetManager.list("decks")?.map { it.lowercase(Locale.ROOT) }?.sorted() ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }
}