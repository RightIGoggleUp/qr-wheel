package com.anotherpatkreation.qrwheel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local on-device list of drawing entries (name + email). No backend needed. */
object LocalStore {
    private const val FILE = "qrwheel"
    private const val KEY = "local_entries"

    fun list(c: Context): MutableList<Entry> {
        val raw = c.getSharedPreferences(FILE, 0).getString(KEY, "[]") ?: "[]"
        val out = mutableListOf<Entry>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Entry(
                    id = o.optLong("id", i.toLong()),
                    name = o.optString("name"),
                    email = o.optString("email")
                ))
            }
        } catch (_: Exception) {}
        return out
    }

    fun add(c: Context, name: String, email: String) {
        val l = list(c)
        l.add(Entry(id = System.currentTimeMillis(), name = name.trim(), email = email.trim()))
        save(c, l)
    }

    fun remove(c: Context, id: Long) {
        save(c, list(c).filterNot { it.id == id }.toMutableList())
    }

    private fun save(c: Context, l: List<Entry>) {
        val arr = JSONArray()
        for (e in l) {
            arr.put(JSONObject().put("id", e.id).put("name", e.name).put("email", e.email))
        }
        c.getSharedPreferences(FILE, 0).edit().putString(KEY, arr.toString()).apply()
    }
}
