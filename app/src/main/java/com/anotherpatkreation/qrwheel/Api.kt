package com.anotherpatkreation.qrwheel

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object Api {
    private const val TIMEOUT = 12000

    data class NotifyResult(val ok: Boolean, val emailed: Boolean, val reason: String)

    fun fetchEntries(base: String, token: String): List<Entry> {
        val conn = URL("$base/api/entries").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode != 200) throw RuntimeException("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader().readText()
            val arr = JSONArray(body)
            val out = ArrayList<Entry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Entry(
                        id = o.optLong("id"),
                        name = o.optString("name"),
                        email = o.optString("email"),
                        sells = o.optString("sells")
                    )
                )
            }
            return out
        } finally {
            conn.disconnect()
        }
    }

    fun notifyWinner(base: String, token: String, e: Entry): NotifyResult {
        val conn = URL("$base/api/notify-winner").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject()
                .put("entry_id", e.id)
                .put("name", e.name)
                .put("email", e.email)
                .toString()
            OutputStreamWriter(conn.outputStream).use { it.write(payload) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText() ?: "{}"
            return try {
                val o = JSONObject(body)
                NotifyResult(o.optBoolean("ok"), o.optBoolean("emailed"), o.optString("reason"))
            } catch (ex: Exception) {
                NotifyResult(false, false, "http_$code")
            }
        } finally {
            conn.disconnect()
        }
    }
}
