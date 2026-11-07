package com.example.appstore.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

sealed interface AppStatus {
    data object NotInstalled : AppStatus
    data class Installed(val version: String) : AppStatus
    data class UpdateAvailable(val installedVersion: String) : AppStatus
}

object InstalledApps {

    /** versionName установленного приложения или null, если не установлено */
    fun versionOf(context: Context, packageName: String): String? = try {
        context.packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    fun statusOf(app: StoreApp, installedVersion: String?): AppStatus {
        if (installedVersion == null) return AppStatus.NotInstalled
        return if (isNewer(app.version, installedVersion)) AppStatus.UpdateAvailable(installedVersion)
        else AppStatus.Installed(installedVersion)
    }

    /** true, если remote строго новее local ("1.10.0" > "1.9.2") */
    fun isNewer(remote: String, local: String): Boolean {
        if (remote.isBlank()) return false
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parts(v: String) =
        v.trim().trimStart('v', 'V').split('.', '-', '_').map { it.toIntOrNull() ?: 0 }

    fun open(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    /** Открывает системный диалог удаления */
    fun uninstall(context: Context, packageName: String) {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
