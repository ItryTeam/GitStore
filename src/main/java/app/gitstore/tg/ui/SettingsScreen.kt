package app.gitstore.tg.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import app.gitstore.tg.StoreViewModel

private const val SITE = "https://itryteam.github.io/gitstore/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: StoreViewModel,
    onUpdates: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit
) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val user = vm.user
    var cache by remember { mutableLongStateOf(vm.cacheSize()) }
    val updates = vm.updates().size
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Настройки") }) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Аккаунт
            ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    if (user == null) {
                        Text("Аккаунт Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Войдите, чтобы история скачиваний и рекомендации сохранялись между устройствами.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                            Icon(Icons.Rounded.AccountCircle, null)
                            Spacer(Modifier.width(8.dp)); Text("Войти через Google")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (user.photo != null) {
                                AsyncImage(
                                    model = user.photo, contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(CircleShape)
                                )
                            } else {
                                Icon(Icons.Rounded.AccountCircle, null, Modifier.size(56.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    user.email, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = onSync, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Sync, null)
                                Spacer(Modifier.width(6.dp)); Text("Синхронизация")
                            }
                            OutlinedButton(onClick = onSignOut, modifier = Modifier.weight(1f)) { Text("Выйти") }
                        }
                    }
                }
            }

            // Остальные настройки
            ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        modifier = Modifier.clickable(onClick = onUpdates),
                        leadingContent = { Icon(Icons.Rounded.SystemUpdate, null) },
                        headlineContent = { Text("Обновления") },
                        supportingContent = { Text(if (updates > 0) "Доступно: $updates" else "Всё актуально") },
                        trailingContent = { Icon(Icons.Rounded.ChevronRight, null) }
                    )
                    HorizontalDivider()
                    ListItem(
                        modifier = Modifier.clickable {
                            val freed = vm.clearCache()
                            cache = vm.cacheSize()
                            Toast.makeText(ctx, "Освобождено: ${formatSize(freed)}", Toast.LENGTH_SHORT).show()
                        },
                        leadingContent = { Icon(Icons.Rounded.DeleteSweep, null) },
                        headlineContent = { Text("Очистить кеш загрузок") },
                        supportingContent = { Text("Скачанные APK: ${formatSize(cache)}") }
                    )
                    HorizontalDivider()
                    ListItem(
                        modifier = Modifier.clickable { uri.openUri(SITE + "add/") },
                        leadingContent = { Icon(Icons.Rounded.AddCircle, null) },
                        headlineContent = { Text("Добавить своё приложение") },
                        supportingContent = { Text("Отправьте заявку на публикацию в GitStore") },
                        trailingContent = { Icon(Icons.Rounded.OpenInNew, null) }
                    )
                    HorizontalDivider()
                    ListItem(
                        modifier = Modifier.clickable { uri.openUri(SITE) },
                        leadingContent = { Icon(Icons.Rounded.Language, null) },
                        headlineContent = { Text("Сайт GitStore") },
                        supportingContent = { Text("itryteam.github.io/gitstore") },
                        trailingContent = { Icon(Icons.Rounded.OpenInNew, null) }
                    )
                    HorizontalDivider()
                    ListItem(
                        modifier = Modifier.clickable { uri.openUri(SITE + "support/") },
                        leadingContent = { Icon(Icons.Rounded.Help, null) },
                        headlineContent = { Text("Поддержка") },
                        supportingContent = { Text("Вопрос, ошибка, идея или жалоба — без регистрации") },
                        trailingContent = { Icon(Icons.Rounded.OpenInNew, null) }
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Rounded.Palette, null) },
                        headlineContent = { Text("Цвета") },
                        supportingContent = {
                            Text(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                    "Monet: цвета берутся из обоев и тёмной темы системы"
                                else "Динамические цвета Monet доступны на Android 12+"
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        leadingContent = { Icon(Icons.Rounded.Info, null) },
                        headlineContent = { Text("GitStore") },
                        supportingContent = { Text("Версия $version · приложений в каталоге: ${vm.apps.size}") }
                    )
                }
            }
        }
    }
}
