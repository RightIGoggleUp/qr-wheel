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
                    email = o.optString("email"),
                    won = o.optBoolean("won", false),
                    opt1 = o.optString("opt1"),
                    opt2 = o.optString("opt2"),
                    opt3 = o.optString("opt3")
                ))
            }
        } catch (_: Exception) {}
        return out
    }

    fun add(c: Context, name: String, email: String, opts: List<String> = listOf("", "", "")) {
        val l = list(c)
        l.add(Entry(
            id = System.currentTimeMillis(), name = name.trim(), email = email.trim(),
            opt1 = opts.getOrElse(0) { "" }, opt2 = opts.getOrElse(1) { "" }, opt3 = opts.getOrElse(2) { "" }
        ))
        save(c, l)
    }

    fun remove(c: Context, id: Long) {
        save(c, list(c).filterNot { it.id == id }.toMutableList())
    }

    fun markWon(c: Context, id: Long) {
        save(c, list(c).map { if (it.id == id) it.copy(won = true) else it }.toMutableList())
    }

    fun clearAll(c: Context) {
        save(c, mutableListOf())
    }

    fun resetWon(c: Context) {
        save(c, list(c).map { it.copy(won = false) }.toMutableList())
    }

    private fun save(c: Context, l: List<Entry>) {
        val arr = JSONArray()
        for (e in l) {
            arr.put(JSONObject().put("id", e.id).put("name", e.name).put("email", e.email).put("won", e.won).put("opt1", e.opt1).put("opt2", e.opt2).put("opt3", e.opt3))
        }
        c.getSharedPreferences(FILE, 0).edit().putString(KEY, arr.toString()).apply()
    }
}
