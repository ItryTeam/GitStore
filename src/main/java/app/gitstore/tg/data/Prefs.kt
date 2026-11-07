package app.gitstore.tg.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("store", Context.MODE_PRIVATE)

    fun recent(): List<String> =
        sp.getString("recent", "")!!.split("\n").filter { it.isNotBlank() }

    fun setRecent(list: List<String>) {
        sp.edit().putString("recent", list.take(20).joinToString("\n")).apply()
    }

    fun addRecent(id: String) {
        setRecent(listOf(id) + recent().filter { it != id })
    }

    fun user(): UserInfo? {
        val name = sp.getString("u_name", null) ?: return null
        return UserInfo(name, sp.getString("u_email", "") ?: "", sp.getString("u_photo", null))
    }

    fun saveUser(u: UserInfo?) {
        sp.edit().apply {
            if (u == null) {
                remove("u_name"); remove("u_email"); remove("u_photo")
            } else {
                putString("u_name", u.name); putString("u_email", u.email); putString("u_photo", u.photo)
            }
        }.apply()
    }
}
