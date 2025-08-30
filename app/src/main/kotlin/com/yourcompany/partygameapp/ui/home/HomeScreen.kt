package com.yourcompany.partygameapp.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.yourcompany.partygameapp.R
import com.yourcompany.partygameapp.domain.model.DeckWithCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDeckSelected: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLeaderboard: () -> Unit, // <<< ADD THIS PARAMETER
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    // Leaderboard Button
                    IconButton(onClick = onNavigateToLeaderboard) {
                        Icon(
                            Icons.Filled.Leaderboard,
                            contentDescription = stringResource(R.string.leaderboard_title)
                        )
                    }
                    // Settings Button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                    // Buy a Coffee button
                    IconButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://ko-fi.com/your_profile")
                            )
                            context.startActivity(intent)
                        },
                    ) {
                        Icon(
                            Icons.Default.Coffee,
                            contentDescription = stringResource(R.string.home_buy_coffee_button)
                        )
                    }

                    LanguageDropDownMenu(viewModel = viewModel)
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is HomeScreenUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is HomeScreenUiState.Success -> {
                    if (state.decks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_no_decks),
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        DeckGrid(decks = state.decks, onDeckSelected = onDeckSelected)
                    }
                }
            }
        }
    }
}
@Composable
fun LanguageDropDownMenu(viewModel: HomeViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isMenuExpanded by viewModel.isMenuExpanded.collectAsState()

    Box {
        TextButton(onClick = { viewModel.onLanguageMenuClick() }) {
            Text(
                text = selectedLanguage.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { viewModel.onLanguageMenuDismiss() },
        ) {
            viewModel.availableLanguages.forEach { languageCode ->
                DropdownMenuItem(
                    text = { Text(languageCode.uppercase(Locale.ROOT)) },
                    onClick = { viewModel.onLanguageSelected(languageCode) },
                )
            }
        }
    }
}

@Composable
fun DeckGrid(
    decks: List<DeckWithCategory>,
    onDeckSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(decks) { deck ->
            DeckTile(deck = deck, onClick = { onDeckSelected(deck.deckId) })
        }
    }
}

@Composable
fun DeckTile(deck: DeckWithCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(deck.categoryColorHex.toColorInt())),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = deck.deckName,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
