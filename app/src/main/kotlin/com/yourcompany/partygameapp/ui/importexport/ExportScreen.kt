// FILE: ExportScreen.kt

package com.yourcompany.partygameapp.ui.importexport

import androidx.compose.material.icons.filled.Warning
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack // <<< NEW IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.yourcompany.partygameapp.R
import com.yourcompany.partygameapp.domain.model.DeckWithCategory
import com.yourcompany.partygameapp.util.FileHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.lifecycle.HiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportExportViewModel = hiltViewModel()
) {
    val decks by viewModel.allDecksForExport.collectAsState()
    val deckToExport by viewModel.deckToExport.collectAsState()
    val context = LocalContext.current

    val fileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let { safeUri ->
                deckToExport?.let { data ->
                    val hiltEntryPoint = EntryPointAccessors.fromActivity(
                        context as android.app.Activity,
                        FileHelperEntryPoint::class.java
                    )
                    val fileHelper = hiltEntryPoint.getFileHelper()
                    fileHelper.saveContentToUri(safeUri, data.content)
                }
            }
            viewModel.onFileExported()
        }
    )

    LaunchedEffect(deckToExport) {
        deckToExport?.let {
            fileSaverLauncher.launch(it.suggestedName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (decks.isEmpty()) {
                // <<< START OF NEW CODE: Empty State >>>
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.export_no_decks_found),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
                // <<< END OF NEW CODE >>>
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.export_select_decks), style = MaterialTheme.typography.titleMedium)
                    Row {
                        TextButton(onClick = { viewModel.onSelectAll(decks) }) { Text(stringResource(R.string.export_all_button)) }
                        TextButton(onClick = viewModel::onDeselectAll) { Text(stringResource(R.string.export_none_button)) }
                    }
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(decks) { deck ->
                        DeckExportRow(
                            deck = deck,
                            isChecked = viewModel.checkedDeckIds[deck.deckId] ?: false,
                            onCheckedChanged = { isChecked ->
                                viewModel.onDeckCheckedChanged(deck.deckId, isChecked)
                            }
                        )
                    }
                }
            }
            Button(
                onClick = viewModel::onExport,
                enabled = viewModel.checkedDeckIds.any { it.value },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)
            ) {
                Text(stringResource(R.string.export_button), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}


// ... (FileHelperEntryPoint, DeckExportRow are correct) ...
@EntryPoint
@InstallIn(ActivityComponent::class)
interface FileHelperEntryPoint {
    fun getFileHelper(): FileHelper
}

@Composable
fun DeckExportRow(deck: DeckWithCategory, isChecked: Boolean, onCheckedChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChanged(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(deck.deckName, style = MaterialTheme.typography.bodyLarge)
            Text(deck.categoryName, style = MaterialTheme.typography.bodySmall)
        }
        Checkbox(checked = isChecked, onCheckedChange = null)
    }
}
