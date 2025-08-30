// FILE: SettingsScreen.kt

package com.yourcompany.partygameapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack // <<< NEW IMPORT
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource // <<< IMPORT THIS
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.yourcompany.partygameapp.R // <<< IMPORT THIS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit, // <<< ADD THIS PARAMETER
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settingsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { // Back button
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        settings?.let { currentSettings ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.settings_penalties), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                SettingSwitch(
                    title = stringResource(R.string.settings_time_up_penalty),
                    isChecked = currentSettings.timeUpPenalty,
                    onCheckedChange = viewModel::onTimeUpPenaltyChanged
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_give_up_penalty),
                    isChecked = currentSettings.giveUpPenalty,
                    onCheckedChange = viewModel::onGiveUpPenaltyChanged
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text(stringResource(R.string.settings_card_time), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                SettingStepper(
                    title = stringResource(R.string.settings_card_time),
                    value = currentSettings.cardTime,
                    onValueChange = viewModel::onCardTimeChanged,
                    range = 10..120,
                    step = 5,
                    unit = stringResource(R.string.settings_unit_seconds)
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingStepper(
                    title = stringResource(R.string.settings_card_count),
                    value = currentSettings.cardCount,
                    onValueChange = viewModel::onCardCountChanged,
                    range = 5..20,
                    step = 1,
                    unit = ""
                )
            }
        }
    }
}

// ... (SettingSwitch and SettingStepper are correct) ...
@Composable
fun SettingSwitch(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingStepper(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int,
    unit: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange((value - step).coerceIn(range)) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(50.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onValueChange((value + step).coerceIn(range)) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}
