package app.gitstore.tg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.gitstore.tg.StoreViewModel
import app.gitstore.tg.data.AppStatus
import app.gitstore.tg.data.DownloadState
import app.gitstore.tg.data.StoreApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(vm: StoreViewModel, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val list = vm.updates()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обновления") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Назад") }
                }
            )
        }
    ) { pad ->
        if (list.isEmpty()) {
            Column(
                Modifier.padding(pad).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Всё актуально", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                Modifier.padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Button(
                        onClick = { vm.updateAll() },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Обновить всё (${list.size})")
                    }
                }
                items(list, key = { it.id }) { UpdateRow(vm, it, onOpen) }
            }
        }
    }
}

@Composable
private fun UpdateRow(vm: StoreViewModel, app: StoreApp, onOpen: (String) -> Unit) {
    val dl = vm.downloads[app.id]
    val installed = (vm.statusOf(app) as? AppStatus.UpdateAvailable)?.installedVersion.orEmpty()

    ElevatedCard(
        onClick = { onOpen(app.id) },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.icon, 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "$installed → ${app.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dl is DownloadState.Progress) {
                    Spacer(Modifier.height(6.dp))
                    if (dl.value <= 0f) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { dl.value }, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.width(8.dp))
            when (dl) {
                is DownloadState.Progress -> Text("${(dl.value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                is DownloadState.Ready -> FilledTonalButton(onClick = { vm.install(app) }) { Text("Установить") }
                null -> FilledTonalButton(onClick = { vm.download(app) }) { Text("Обновить") }
            }
        }
    }
}
