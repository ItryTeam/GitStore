package app.gitstore.tg.ui

import java.util.Locale

fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.getDefault(), "%.1f ГБ", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(Locale.getDefault(), "%.1f МБ", bytes / (1L shl 20).toDouble())
    bytes >= 1024 -> String.format(Locale.getDefault(), "%.0f КБ", bytes / 1024.0)
    else -> "$bytes Б"
}
