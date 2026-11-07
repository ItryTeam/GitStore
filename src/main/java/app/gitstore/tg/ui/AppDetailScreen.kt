package app.gitstore.tg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.gitstore.tg.StoreViewModel
import app.gitstore.tg.data.AppStatus
import app.gitstore.tg.data.AppType
import app.gitstore.tg.data.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(vm: StoreViewModel, id: String, onBack: () -> Unit) {
    val app = vm.byId(id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Назад") }
                }
            )
        }
    ) { pad ->
        if (app == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Приложение не найдено")
            }
            return@Scaffold
        }

        LaunchedEffect(app.id) { vm.loadSize(app) }

        val status = vm.statusOf(app)
        val dl = vm.downloads[app.id]
        val size = vm.sizes[app.id]
        val btn = Modifier.fillMaxWidth().height(52.dp)

        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DownloadingIcon(app.icon, 128.dp, (dl as? DownloadState.Progress)?.value)
            Spacer(Modifier.height(16.dp))
            Text(
                app.name, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )

            val sub = when (status) {
                is AppStatus.Installed -> "Установлено · версия ${status.version}"
                is AppStatus.UpdateAvailable ->
                    "Установлена ${status.installedVersion} → доступна ${app.version}"
                AppStatus.NotInstalled ->
                    if (app.version.isNotBlank()) "Версия ${app.version}" else ""
            }
            if (sub.isNotBlank()) {
                Text(
                    sub, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (app.type == AppType.GAME) "Игра" else "Приложение") },
                    leadingIcon = {
                        Icon(
                            if (app.type == AppType.GAME) Icons.Rounded.SportsEsports else Icons.Rounded.Apps,
                            null, Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
                AssistChip(onClick = {}, label = { Text(categoryTitle(app.category)) })
            }
            Spacer(Modifier.height(20.dp))
            
            when {
                dl is DownloadState.Progress -> {
                    Button(onClick = {}, enabled = false, modifier = btn) {
                        Text("Скачивание… ${(dl.value * 100).toInt()}%")
                    }
                    Spacer(Modifier.height(8.dp))
                    if (dl.value <= 0f) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { dl.value }, modifier = Modifier.fillMaxWidth())
                }

                status is AppStatus.Installed -> {
                    Button(onClick = { vm.open(app) }, modifier = btn) {
                        Icon(Icons.Rounded.OpenInNew, null)
                        Spacer(Modifier.width(8.dp)); Text("Открыть")
                    }
                }
                
                dl is DownloadState.Ready -> {
                    FilledTonalButton(onClick = { vm.install(app) }, modifier = btn) {
                        Icon(Icons.Rounded.InstallMobile, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (status is AppStatus.UpdateAvailable) "Установить обновление" else "Установить")
                    }
                }

                status is AppStatus.UpdateAvailable -> {
                    Button(onClick = { vm.download(app) }, modifier = btn) {
                        Icon(Icons.Rounded.SystemUpdate, null)
                        Spacer(Modifier.width(8.dp)); Text("Обновить")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { vm.open(app) }, modifier = btn) { Text("Открыть") }
                }

                else -> Button(onClick = { vm.download(app) }, modifier = btn) {
                    Icon(Icons.Rounded.Download, null)
                    Spacer(Modifier.width(8.dp)); Text("Скачать")
                }
            }

            if (size != null && status !is AppStatus.Installed && dl !is DownloadState.Progress) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Размер загрузки: ${formatSize(size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            if (status !is AppStatus.NotInstalled) {
                TextButton(
                    onClick = { vm.uninstall(app) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.Delete, null)
                    Spacer(Modifier.width(8.dp)); Text("Удалить")
                }
            }

            if (app.changelog.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            if (app.version.isNotBlank()) "Что нового в ${app.version}" else "Что нового",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(app.changelog, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Описание", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(app.description.ifBlank { "Описание отсутствует" }, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
