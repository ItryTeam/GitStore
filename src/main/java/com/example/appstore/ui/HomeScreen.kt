package com.example.appstore.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.appstore.StoreViewModel
import com.example.appstore.data.AppStatus
import com.example.appstore.data.AppType
import com.example.appstore.data.StoreApp
import com.example.appstore.data.UserInfo
import kotlin.random.Random

private val categoryTitles = mapOf(
    "entertainment" to "Развлечения",
    "utilities" to "Утилиты",
    "games" to "Игры",
    "game" to "Игры",
    "social" to "Общение",
    "tools" to "Инструменты",
    "music" to "Музыка",
    "other" to "Другое"
)

fun categoryTitle(c: String) =
    categoryTitles[c] ?: c.replaceFirstChar { it.uppercase() }

/** Одна вкладка магазина: "Игры" или "Приложения" — шапка, рекомендации и карточки */
@Composable
fun StoreTab(
    vm: StoreViewModel,
    type: AppType,
    onOpen: (String) -> Unit,
    onUpdates: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    BackHandler(searching) { searching = false; query = "" }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopBar(
                user = vm.user, searching = searching, query = query,
                updatesCount = vm.updates().size,
                onQuery = { query = it },
                onSearchToggle = { searching = !searching; if (!searching) query = "" },
                onUpdates = onUpdates,
                onSignIn = onSignIn, onSignOut = onSignOut, onSync = onSync
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                vm.loading -> FeedSkeleton()
                vm.error != null -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(vm.error ?: "", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = { vm.refresh() }) { Text("Повторить") }
                }
                searching && query.isNotBlank() -> {
                    // поиск идёт по всему каталогу, а не только по вкладке
                    val found = vm.apps.filter {
                        it.name.contains(query, true) || it.description.contains(query, true)
                    }
                    if (found.isEmpty()) {
                        Text("Ничего не найдено", Modifier.align(Alignment.Center))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(132.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(found, key = { it.id }) {
                                SquareCard(it, vm.statusOf(it), vm.progressOf(it)) { onOpen(it.id) }
                            }
                        }
                    }
                }
                vm.appsOf(type).isEmpty() -> Text(
                    if (type == AppType.GAME) "Игр пока нет" else "Приложений пока нет",
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Feed(vm, type, onOpen)
            }
        }
    }
}

@Composable
private fun Feed(vm: StoreViewModel, type: AppType, onOpen: (String) -> Unit) {
    val list = vm.appsOf(type)
    val recommended = vm.recommended(type)
    val hasRecent = vm.recentIds.isNotEmpty()
    val byCategory = list.groupBy { it.category }
    // "На любой вкус" — случайная подборка, стабильная в течение дня
    val anyTaste = remember(list) {
        list.shuffled(Random(System.currentTimeMillis() / 86_400_000L)).take(12)
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (recommended.isNotEmpty()) item {
            Section(if (hasRecent) "Рекомендуем · по вашим загрузкам" else "Рекомендуем") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) { items(recommended, key = { it.id }) { FeaturedCard(it) { onOpen(it.id) } } }
            }
        }
        byCategory.forEach { (cat, apps) ->
            item(key = "cat_$cat") { AppRow(categoryTitle(cat), apps, vm::statusOf, vm::progressOf, onOpen) }
        }
        if (anyTaste.isNotEmpty()) item { AppRow("На любой вкус", anyTaste, vm::statusOf, vm::progressOf, onOpen) }
    }
}

@Composable
private fun AppRow(
    title: String, list: List<StoreApp>,
    statusOf: (StoreApp) -> AppStatus, progressOf: (StoreApp) -> Float?, onOpen: (String) -> Unit
) {
    Section(title) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) { items(list, key = { it.id }) { SquareCard(it, statusOf(it), progressOf(it)) { onOpen(it.id) } } }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        content()
    }
}

@Composable
private fun TopBar(
    user: UserInfo?, searching: Boolean, query: String, updatesCount: Int,
    onQuery: (String) -> Unit, onSearchToggle: () -> Unit, onUpdates: () -> Unit,
    onSignIn: () -> Unit, onSignOut: () -> Unit, onSync: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (searching) {
                IconButton(onClick = onSearchToggle) { Icon(Icons.Rounded.ArrowBack, "Назад") }
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                TextField(
                    value = query, onValueChange = onQuery,
                    placeholder = { Text("Поиск приложений") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).focusRequester(focus)
                )
            } else {
                Icon(
                    Icons.Rounded.Storefront, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp).size(30.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("GitStore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (updatesCount > 0) {
                    IconButton(onClick = onUpdates) {
                        BadgedBox(badge = { Badge { Text("$updatesCount") } }) {
                            Icon(Icons.Rounded.SystemUpdate, "Обновления")
                        }
                    }
                }
                IconButton(onClick = onSearchToggle) { Icon(Icons.Rounded.Search, "Поиск") }
                AccountButton(user, onSignIn, onSignOut, onSync)
            }
        }
    }
}

@Composable
private fun AccountButton(user: UserInfo?, onSignIn: () -> Unit, onSignOut: () -> Unit, onSync: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { if (user == null) onSignIn() else menu = true }) {
            if (user?.photo != null) {
                AsyncImage(
                    model = user.photo, contentDescription = "Аккаунт",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                )
            } else {
                Icon(Icons.Rounded.AccountCircle, "Войти через Google", Modifier.size(32.dp))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (user != null) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(user.name, style = MaterialTheme.typography.titleSmall)
                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Синхронизировать историю") },
                    leadingIcon = { Icon(Icons.Rounded.Sync, null) },
                    onClick = { menu = false; onSync() }
                )
                DropdownMenuItem(
                    text = { Text("Выйти") },
                    leadingIcon = { Icon(Icons.Rounded.Logout, null) },
                    onClick = { menu = false; onSignOut() }
                )
            }
        }
    }
}
