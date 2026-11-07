package app.gitstore.tg.data

import app.gitstore.tg.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object Repository {

    suspend fun loadApps(): List<StoreApp> = withContext(Dispatchers.IO) {
        val links = fetch(Config.APPS_URL).lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        coroutineScope {
            links.map { link -> async { runCatching { parse(fetch(link)) }.getOrNull() } }
                .awaitAll()
                .filterNotNull()
        }
    }

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return conn.getInputStream().bufferedReader().use { it.readText() }
    }

    private fun parse(json: String): StoreApp {
        val o = JSONObject(json)
        val category = o.optString("category", "other").lowercase()
        val type = when (o.optString("type").lowercase()) {
            "game", "games" -> AppType.GAME
            "app", "apps" -> AppType.APP
            else -> if (category in setOf("game", "games", "entertainment")) AppType.GAME else AppType.APP
        }
        return StoreApp(
            id = o.getString("id"),
            name = o.getString("name"),
            icon = o.optString("icon"),
            description = o.optString("description"),
            category = category,
            apk = o.getString("apk"),
            version = o.optString("version", ""),
            changelog = o.optString("changelog", ""),
            type = type
        )
    }
}
