// FILE: app/src/main/kotlin/com/yourcompany/partygameapp/ui/importexport/ImportScreen.kt

package com.yourcompany.partygameapp.ui.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourcompany.partygameapp.R
import com.yourcompany.partygameapp.ui.home.HomeViewModel // Keep this for now, will remove if not strictly needed
import com.yourcompany.partygameapp.util.FileHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale // <--- ADD THIS IMPORT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportExportViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel() // Can remove this if language selection is fully handled by ImportExportViewModel
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> viewModel.onFileSelected(uri) }
    )
    val categories by viewModel.categories.collectAsState()
    // val selectedLanguage by homeViewModel.selectedLanguage.collectAsState() // <--- REMOVE THIS LINE

    val availableImportLanguages by viewModel.availableImportLanguages.collectAsState() // <--- NEW LINE
    val selectedImportLanguage by viewModel.selectedImportLanguage.collectAsState()     // <--- NEW LINE
    val isLanguageDropdownExpanded by viewModel.isLanguageDropdownExpanded.collectAsState() // <--- NEW LINE

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.import_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) { // Back button
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("text/plain", "text/csv")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.import_select_file)) }
            Text(stringResource(R.string.import_file_types), style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = viewModel.newDeckName,
                onValueChange = viewModel::onDeckNameChange,
                label = { Text(viewModel.selectedFileName ?: stringResource(R.string.import_new_deck_name_label)) },
                placeholder = { Text(stringResource(R.string.import_deck_name_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Category Dropdown with Empty State/Warning
            if (categories.isEmpty() && viewModel.selectedCategoryName.isBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.import_no_categories_warning), tint = MaterialTheme.colorScheme.error)
                    Text(
                        text = stringResource(R.string.import_no_categories_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                CategoryDropdown(
                    categories = categories.map { it.name },
                    selectedCategory = viewModel.selectedCategoryName,
                    onCategoryChange = viewModel::onCategoryChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // <--- NEW CODE BLOCK START: Language Dropdown --->
            LanguageDropdown(
                availableLanguages = availableImportLanguages,
                selectedLanguage = selectedImportLanguage,
                onLanguageSelected = viewModel::onLanguageSelectedForImport,
                isExpanded = isLanguageDropdownExpanded,
                onDropdownClick = viewModel::onLanguageDropdownClick,
                onDropdownDismiss = viewModel::onLanguageDropdownDismiss
            )
            // <--- NEW CODE BLOCK END --->

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { viewModel.onSave() }, // <--- CHANGED: No longer passing language from HomeViewModel
                enabled = viewModel.selectedFileUri != null && viewModel.newDeckName.isNotBlank() && viewModel.selectedCategoryName.isNotBlank() && selectedImportLanguage.isNotBlank(), // <--- MODIFIED ENABLED LOGIC
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(stringResource(R.string.import_save_button), style = MaterialTheme.typography.titleLarge) }
        }
    }
}

// ... (CategoryDropdown is correct) ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded }
    ) {
        OutlinedTextField( // Changed to OutlinedTextField for consistency with mockups
            value = selectedCategory,
            onValueChange = onCategoryChange,
            label = { Text(stringResource(R.string.import_category_label)) }, // Use string resource
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        val filteredOptions = categories.filter {
            it.contains(selectedCategory, ignoreCase = true)
        }

        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                filteredOptions.forEach { categoryName ->
                    DropdownMenuItem(
                        text = { Text(categoryName) },
                        onClick = {
                            onCategoryChange(categoryName)
                            isExpanded = false
                        }
                    )
                }
            }
        }
    }
}


// <--- NEW CODE BLOCK START: Language Dropdown Composable --->
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(
    availableLanguages: List<String>,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    isExpanded: Boolean,
    onDropdownClick: () -> Unit,
    onDropdownDismiss: () -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { onDropdownClick() }
    ) {
        OutlinedTextField(
            value = selectedLanguage.uppercase(Locale.ROOT),
            onValueChange = { /* readOnly */ },
            readOnly = true,
            label = { Text(stringResource(R.string.import_deck_language_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onDropdownDismiss
        ) {
            availableLanguages.forEach { langCode ->
                DropdownMenuItem(
                    text = { Text(langCode.uppercase(Locale.ROOT)) },
                    onClick = { onLanguageSelected(langCode) }
                )
            }
        }
    }
}
// <--- NEW CODE BLOCK END --->