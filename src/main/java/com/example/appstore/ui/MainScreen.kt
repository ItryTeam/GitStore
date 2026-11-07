package com.example.appstore.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.appstore.StoreViewModel
import com.example.appstore.data.AppType

private enum class Tab(val label: String, val icon: ImageVector) {
    Games("Игры", Icons.Rounded.SportsEsports),
    Apps("Приложения", Icons.Rounded.Apps),
    Settings("Настройки", Icons.Rounded.Settings)
}

@Composable
fun MainScreen(
    vm: StoreViewModel,
    onOpen: (String) -> Unit,
    onUpdates: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit
) {
    // по умолчанию открыта средняя вкладка — "Приложения"
    var selected by rememberSaveable { mutableIntStateOf(Tab.Apps.ordinal) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab.ordinal,
                        onClick = { selected = tab.ordinal },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { pad ->
        // сверху отступ берут на себя сами экраны (statusBarsPadding / TopAppBar)
        Box(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
            when (Tab.entries[selected]) {
                Tab.Games -> StoreTab(vm, AppType.GAME, onOpen, onUpdates, onSignIn, onSignOut, onSync)
                Tab.Apps -> StoreTab(vm, AppType.APP, onOpen, onUpdates, onSignIn, onSignOut, onSync)
                Tab.Settings -> SettingsScreen(vm, onUpdates, onSignIn, onSignOut, onSync)
            }
        }
    }
}
