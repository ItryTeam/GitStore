package com.example.appstore.data

enum class AppType { GAME, APP }

data class StoreApp(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val category: String,   // любая строка: utilities, social, action, ...
    val apk: String,
    val version: String,
    val changelog: String = "",
    val type: AppType = AppType.APP
)

data class UserInfo(val name: String, val email: String, val photo: String?)

sealed interface DownloadState {
    data class Progress(val value: Float) : DownloadState
    data class Ready(val path: String) : DownloadState
}
