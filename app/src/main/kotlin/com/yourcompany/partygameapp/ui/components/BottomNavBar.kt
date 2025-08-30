// FILE: BottomNavBar.kt

package com.yourcompany.partygameapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource // <<< IMPORT THIS
import com.yourcompany.partygameapp.R // <<< IMPORT THIS
import com.yourcompany.partygameapp.ui.navigation.Routes

sealed class BottomNavItem(val route: String, val icon: ImageVector, val stringResId: Int) { // Use stringResId
    object Home : BottomNavItem(Routes.HOME, Icons.Default.Home, R.string.home_title)
    object Import : BottomNavItem(Routes.IMPORT, Icons.Default.AddCircle, R.string.import_title)
    object Leaderboard : BottomNavItem(Routes.LEADERBOARD, Icons.Default.Leaderboard, R.string.leaderboard_title)
    object Settings : BottomNavItem(Routes.SETTINGS, Icons.Default.Settings, R.string.settings_title)
}

@Composable
fun BottomNavBar(
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Import,
        BottomNavItem.Leaderboard,
        BottomNavItem.Settings
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.stringResId)) },
                label = { Text(stringResource(item.stringResId)) },
                selected = false,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}