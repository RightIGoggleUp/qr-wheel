package com.anotherpatkreation.qrwheel

import android.content.Context

object Prefs {
    private const val FILE = "qrwheel"
    private const val KEY_SIGNUP_URL = "signup_url"
    private const val KEY_API_BASE = "api_base"
    private const val KEY_ADMIN_TOKEN = "admin_token"
    private const val KEY_WIN_SUBJECT = "winner_subject"
    private const val KEY_WIN_BODY = "winner_body"
    const val DEFAULT_WIN_SUBJECT = ""
    const val DEFAULT_WIN_BODY = "SSG Floor Day November 11th, 2026\n\n\uD83D\uDE4F ----- Life gives you lemons. It\u2019s up to you how sweet your lemonade is. -----\uD83D\uDE4F\n\nPatrick Kea\nNA ISG Portfolio Enablement MGR\n8001 Development Dr.,\nMorrisville, NC 27560\nLenovo\n+1 919 508 5001\npkea@lenovo.com\nLenovo.com/us"
    const val DEFAULT_SIGNUP_URL = "https://lenovo.highspot.com/items/6ab6621456d0d11fe3c8c144#1"

    fun signupUrl(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_SIGNUP_URL, DEFAULT_SIGNUP_URL) ?: DEFAULT_SIGNUP_URL

    fun apiBase(c: Context): String =
        (c.getSharedPreferences(FILE, 0).getString(KEY_API_BASE, "") ?: "").trim().trimEnd('/')

    fun adminToken(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_ADMIN_TOKEN, "") ?: ""

    fun winnerSubject(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_WIN_SUBJECT, DEFAULT_WIN_SUBJECT) ?: DEFAULT_WIN_SUBJECT

    fun winnerBody(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_WIN_BODY, DEFAULT_WIN_BODY) ?: DEFAULT_WIN_BODY

    fun save(c: Context, signupUrl: String, apiBase: String, adminToken: String) {
        c.getSharedPreferences(FILE, 0).edit()
            .putString(KEY_SIGNUP_URL, signupUrl.trim().ifBlank { DEFAULT_SIGNUP_URL })
            .putString(KEY_API_BASE, apiBase.trim())
            .putString(KEY_ADMIN_TOKEN, adminToken.trim())
            .apply()
    }

    data class OptionRow(val on: Boolean, val count: Int, val labels: List<String>)

    fun optionRows(c: Context): List<OptionRow> {
        val p = c.getSharedPreferences(FILE, 0)
        val defaults = listOf(
            OptionRow(true, 2, listOf("IDG", "ISG", "", "")),
            OptionRow(false, 4, listOf("", "", "", "")),
            OptionRow(false, 4, listOf("", "", "", "")),
            OptionRow(false, 4, listOf("", "", "", ""))
        )
        return (1..4).map { r ->
            val d = defaults[r - 1]
            OptionRow(
                on = p.getBoolean("opt_row_" + r + "_on", d.on),
                count = p.getInt("opt_row_" + r + "_count", d.count).coerceIn(1, 4),
                labels = (1..4).map { p.getString("opt_row_" + r + "_label_" + it, d.labels[it - 1]) ?: d.labels[it - 1] }
            )
        }
    }

    fun saveOptionRows(c: Context, rows: List<OptionRow>) {
        val e = c.getSharedPreferences(FILE, 0).edit()
        rows.forEachIndexed { i, row ->
            val r = i + 1
            e.putBoolean("opt_row_" + r + "_on", row.on)
            e.putInt("opt_row_" + r + "_count", row.count.coerceIn(1, 4))
            row.labels.forEachIndexed { j, label -> e.putString("opt_row_" + r + "_label_" + (j + 1), label.trim()) }
        }
        e.apply()
    }

    fun saveWinnerTemplate(c: Context, subject: String, body: String) {
        c.getSharedPreferences(FILE, 0).edit()
            .putString(KEY_WIN_SUBJECT, subject)
            .putString(KEY_WIN_BODY, body.ifBlank { DEFAULT_WIN_BODY })
            .apply()
    }
}
