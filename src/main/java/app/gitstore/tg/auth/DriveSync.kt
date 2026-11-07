package app.gitstore.tg.auth

import android.app.Activity
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DriveSync {
    private const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val FILE_NAME = "history.json"
    private const val API = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"

    suspend fun authorize(activity: Activity): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE)))
            .build()
        return Identity.getAuthorizationClient(activity).authorize(request).await()
    }

    suspend fun merge(token: String, local: List<String>): List<String> = withContext(Dispatchers.IO) {
        val fileId = findFile(token)
        val remote = if (fileId != null) parse(call("GET", "$API/$fileId?alt=media", token)) else emptyList()
        val merged = (local + remote).distinct().take(20)

        if (merged != remote) {
            val json = JSONObject().put("recent", JSONArray(merged)).toString()
            if (fileId == null) create(token, json) else update(token, fileId, json)
        }
        merged
    }

    private fun findFile(token: String): String? {
        val q = URLEncoder.encode("name='$FILE_NAME'", "UTF-8")
        val res = call("GET", "$API?spaces=appDataFolder&q=$q&fields=files(id)", token)
        return JSONObject(res).getJSONArray("files").optJSONObject(0)?.getString("id")
    }

    private fun parse(text: String): List<String> {
        val arr = JSONObject(text).optJSONArray("recent") ?: return emptyList()
        return List(arr.length()) { arr.getString(it) }
    }

    private fun create(token: String, json: String) {
        val b = "gitstore_boundary"
        val meta = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
        val body = "--$b\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n" +
                "--$b\r\nContent-Type: application/json\r\n\r\n$json\r\n--$b--"
        call("POST", "$UPLOAD?uploadType=multipart", token, body.toByteArray(), "multipart/related; boundary=$b")
    }

    private fun update(token: String, id: String, json: String) {
        call("PATCH", "$UPLOAD/$id?uploadType=media", token, json.toByteArray(), "application/json")
    }

    private fun call(
        method: String, url: String, token: String,
        body: ByteArray? = null, contentType: String? = null
    ): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Authorization", "Bearer $token")
        if (method == "PATCH") { 
            conn.requestMethod = "POST"
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        } else {
            conn.requestMethod = method
        }
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", contentType)
            conn.outputStream.use { it.write(body) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("Drive $code: ${text.take(200)}")
        return text
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
        addOnSuccessListener { c.resume(it) }
        addOnFailureListener { c.resumeWithException(it) }
    }
}
