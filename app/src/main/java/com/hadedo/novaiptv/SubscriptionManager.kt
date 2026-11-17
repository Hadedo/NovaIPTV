package com.devkan34.vip.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class SubType { XTREAM, M3U, STALKER }

data class Subscription(
val id: String = UUID.randomUUID().toString(),
val name: String,
val type: SubType,
val url: String,
val username: String = "",
val password: String = "",
val mac: String = ""
)

class SubscriptionManager(context: Context) {
private val prefs = context.getSharedPreferences("devkan_subs", Context.MODE_PRIVATE)

fun getAll(): MutableList<Subscription> {
val raw = prefs.getString("list", "[]") ?: "[]"
val arr = JSONArray(raw)
val list = mutableListOf<Subscription>()
for (i in 0 until arr.length()) {
val o = arr.getJSONObject(i)
list.add(
Subscription(
id = o.getString("id"),
name = o.getString("name"),
type = SubType.valueOf(o.getString("type")),
url = o.getString("url"),
username = o.optString("username"),
password = o.optString("password"),
mac = o.optString("mac")
)
)
}
return list
}

fun saveOrUpdate(sub: Subscription) {
val list = getAll()
val idx = list.indexOfFirst { it.id == sub.id }
if (idx != -1) list[idx] = sub else list.add(sub)
saveList(list)
if (getActiveId() == null) setActive(sub.id)
}

fun delete(id: String) {
val list = getAll().filterNot { it.id == id }
saveList(list)
if (getActiveId() == id) setActive(list.firstOrNull()?.id ?: "")
}

fun setActive(id: String) {
prefs.edit().putString("active_id", id).apply()
}

fun getActiveId(): String? = prefs.getString("active_id", null)?.takeIf { it.isNotEmpty() }

fun getActive(): Subscription? = getAll().find { it.id == getActiveId() } ?: getAll().firstOrNull()

private fun saveList(list: List<Subscription>) {
val arr = JSONArray()
list.forEach { s ->
arr.put(JSONObject().apply {
put("id", s.id)
put("name", s.name)
put("type", s.type.name)
put("url", s.url)
put("username", s.username)
put("password", s.password)
put("mac", s.mac)
})
}
prefs.edit().putString("list", arr.toString()).apply()
}

companion object {
// الملء التلقائي يعمل حصراً للإكستريم فقط
fun parseAutoFill(type: SubType, rawInput: String): Triple<String, String, String>? {
if (type != SubType.XTREAM) return null
return try {
val uri = Uri.parse(rawInput.trim())
val user = uri.getQueryParameter("username") ?: return null
val pass = uri.getQueryParameter("password") ?: return null
val host = "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}"
Triple(host, user, pass)
} catch (_: Exception) {
null
}
}
}
}
