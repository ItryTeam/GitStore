package com.example.appstore

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appstore.auth.DriveSync
import com.example.appstore.auth.GoogleAuth
import com.example.appstore.data.*
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

class StoreViewModel(private val app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    var apps by mutableStateOf<List<StoreApp>>(emptyList()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set
    var user by mutableStateOf(prefs.user()); private set
    var recentIds by mutableStateOf(prefs.recent()); private set
    val downloads = mutableStateMapOf<String, DownloadState>()
    val sizes = mutableStateMapOf<String, Long>()

    var installedVersions by mutableStateOf<Map<String, String>>(emptyMap()); private set

    private var cachedToken: String? = null
    private val installQueue = ArrayDeque<String>()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading = true; error = null
            runCatching { Repository.loadApps() }
                .onSuccess {
                    apps = it
                    downloads.filterValues { s -> s is DownloadState.Ready }.keys.forEach { k -> downloads.remove(k) }
                    refreshInstalled()
                }
                .onFailure { error = it.message ?: "Не удалось загрузить список" }
            loading = false
        }
    }

    fun byId(id: String) = apps.find { it.id == id }

    fun refreshInstalled() {
        installedVersions = apps.mapNotNull { a ->
            InstalledApps.versionOf(app, a.id)?.let { a.id to it }
        }.toMap()
        apps.forEach { a ->
            if (statusOf(a) is AppStatus.Installed && downloads[a.id] is DownloadState.Ready) downloads.remove(a.id)
        }
    }

    fun progressOf(a: StoreApp): Float? = (downloads[a.id] as? DownloadState.Progress)?.value

    fun appsOf(type: AppType): List<StoreApp> = apps.filter { it.type == type }

    fun statusOf(a: StoreApp): AppStatus = InstalledApps.statusOf(a, installedVersions[a.id])

    fun updates(): List<StoreApp> = apps.filter { statusOf(it) is AppStatus.UpdateAvailable }

    fun recommended(type: AppType? = null): List<StoreApp> {
        val recent = recentIds.mapNotNull { byId(it) }
        val cats = recent.map { it.category }.toSet()
        val fresh = apps.filter {
            it.id !in recentIds && it.id !in installedVersions && (type == null || it.type == type)
        }
        val (sameCat, other) = fresh.partition { it.category in cats }
        return (sameCat + other).take(10)
    }


    private suspend fun fetchApk(a: StoreApp): Boolean {
        if (downloads[a.id] is DownloadState.Ready) return true
        downloads[a.id] = DownloadState.Progress(0f)
        return runCatching {
            Installer.download(app, a) { downloads[a.id] = DownloadState.Progress(it) }
        }.fold(
            onSuccess = { file ->
                downloads[a.id] = DownloadState.Ready(file.absolutePath)
                prefs.addRecent(a.id); recentIds = prefs.recent()
                cachedToken?.let { t -> pushHistory(t, interactive = false) }
                true
            },
            onFailure = {
                downloads.remove(a.id)
                Toast.makeText(app, "Ошибка загрузки: ${it.message}", Toast.LENGTH_LONG).show()
                false
            }
        )
    }

    fun download(a: StoreApp) {
        if (downloads[a.id] is DownloadState.Progress) return
        viewModelScope.launch { if (fetchApk(a)) install(a) }
    }

    fun install(a: StoreApp) {
        (downloads[a.id] as? DownloadState.Ready)?.let { Installer.install(app, it.path) }
    }

    fun updateAll() {
        val list = updates().filter { downloads[it.id] !is DownloadState.Progress }
        if (list.isEmpty()) return
        viewModelScope.launch {
            list.forEach { fetchApk(it) }
            installQueue.clear()
            installQueue.addAll(list.filter { downloads[it.id] is DownloadState.Ready }.map { it.id })
            nextInstall()
        }
    }

    fun continueQueue() {
        if (installQueue.isNotEmpty()) nextInstall()
    }

    private fun nextInstall() {
        if (!app.packageManager.canRequestPackageInstalls()) {
            installQueue.clear()
            val any = apps.firstNotNullOfOrNull { downloads[it.id] as? DownloadState.Ready }
            any?.let { Installer.install(app, it.path) }
            return
        }
        while (installQueue.isNotEmpty()) {
            val a = byId(installQueue.removeFirst()) ?: continue
            if (statusOf(a) is AppStatus.UpdateAvailable && downloads[a.id] is DownloadState.Ready) {
                install(a)
                return
            }
        }
    }

    fun open(a: StoreApp) {
        if (!InstalledApps.open(app, a.id)) {
            Toast.makeText(app, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstall(a: StoreApp) = InstalledApps.uninstall(app, a.id)

    fun loadSize(a: StoreApp) {
        if (sizes.containsKey(a.id)) return
        viewModelScope.launch {
            runCatching { Installer.fetchSize(a.apk) }.getOrNull()
                ?.takeIf { it > 0 }?.let { sizes[a.id] = it }
        }
    }


    fun signIn(activity: Activity, launchConsent: (IntentSenderRequest) -> Unit) {
        viewModelScope.launch {
            runCatching { GoogleAuth.signIn(activity) }
                .onSuccess {
                    user = it; prefs.saveUser(it)
                    sync(activity, interactive = true, launchConsent = launchConsent)
                }
                .onFailure {
                    android.util.Log.e("GitStore", "signIn failed", it)
                    val hint = if (it is androidx.credentials.exceptions.GetCredentialCancellationException)
                        "окно закрылось само — вероятно, Google не принял настройки приложения " +
                        "(проверьте package name, SHA-1 и Web client ID)"
                    else it.message
                    Toast.makeText(app, "Вход не удался: $hint", Toast.LENGTH_LONG).show()
                }
        }
    }

    fun signOut(activity: Activity) {
        viewModelScope.launch {
            runCatching { GoogleAuth.signOut(activity) }
            user = null; prefs.saveUser(null); cachedToken = null
        }
    }

    fun sync(activity: Activity, interactive: Boolean, launchConsent: (IntentSenderRequest) -> Unit) {
        if (user == null) return
        viewModelScope.launch {
            runCatching { DriveSync.authorize(activity) }
                .onSuccess { r ->
                    val pi = r.pendingIntent
                    if (r.hasResolution() && pi != null) {
                        if (interactive) launchConsent(IntentSenderRequest.Builder(pi.intentSender).build())
                    } else {
                        r.accessToken?.let { pushHistory(it, interactive) }
                    }
                }
                .onFailure {
                    if (interactive) Toast.makeText(app, "Синхронизация: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    fun onConsentResult(activity: Activity, data: Intent?) {
        runCatching { Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data) }
            .onSuccess { r -> r.accessToken?.let { t -> viewModelScope.launch { pushHistory(t, true) } } }
            .onFailure { Toast.makeText(app, "Доступ к Drive не выдан", Toast.LENGTH_SHORT).show() }
    }

    private suspend fun pushHistory(token: String, interactive: Boolean) {
        cachedToken = token
        runCatching { DriveSync.merge(token, prefs.recent()) }
            .onSuccess {
                prefs.setRecent(it); recentIds = it
                if (interactive) Toast.makeText(app, "История синхронизирована", Toast.LENGTH_SHORT).show()
            }
            .onFailure {
                if (interactive) Toast.makeText(app, "Не удалось синхронизировать: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun apkDir() = java.io.File(app.cacheDir, "apks")

    fun cacheSize(): Long =
        apkDir().walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearCache(): Long {
        val size = cacheSize()
        apkDir().listFiles()?.forEach { it.delete() }
        downloads.filterValues { it is DownloadState.Ready }.keys.forEach { downloads.remove(it) }
        return size
    }
}
