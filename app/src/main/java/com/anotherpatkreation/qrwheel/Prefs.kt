package com.anotherpatkreation.qrwheel

import android.content.Context

object Prefs {
    private const val FILE = "qrwheel"
    private const val KEY_SIGNUP_URL = "signup_url"
    private const val KEY_ORGANIZER_EMAIL = "organizer_email"
    private const val KEY_PARTICIPANT_SUBJECT = "participant_subject"
    private const val KEY_PARTICIPANT_HTML = "participant_html"
    private const val KEY_WIN_SUBJECT = "winner_subject"
    private const val KEY_WIN_BODY = "winner_body"
    const val DEFAULT_PARTICIPANT_SUBJECT = "Thank you for joining us at FloorDay"
    const val DEFAULT_PARTICIPANT_HTML = """<h2>FloorDay Test</h2>
<p>Thank you for joining us today!</p>
<p><b>This sentence should be bold.</b></p>
<p><i>This sentence should be italic.</i></p>
<p>Visit <a href="https://www.lenovo.com/us">Lenovo.com</a></p>
<p><strong>Patrick Kea</strong><br>
NA ISG Portfolio Enablement MGR<br>
Lenovo</p>"""
    const val DEFAULT_WIN_SUBJECT = ""
    const val DEFAULT_WIN_BODY = "SSG Floor Day November 11th, 2026\n\n\uD83D\uDE4F ----- Life gives you lemons. It\u2019s up to you how sweet your lemonade is. -----\uD83D\uDE4F\n\nPatrick Kea\nNA ISG Portfolio Enablement MGR\n8001 Development Dr.,\nMorrisville, NC 27560\nLenovo\n+1 919 508 5001\npkea@lenovo.com\nLenovo.com/us"
    const val DEFAULT_SIGNUP_URL = "https://lenovo.highspot.com/items/6ab6621456d0d11fe3c8c144#1"

    fun signupUrl(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_SIGNUP_URL, DEFAULT_SIGNUP_URL) ?: DEFAULT_SIGNUP_URL

    fun winnerSubject(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_WIN_SUBJECT, DEFAULT_WIN_SUBJECT) ?: DEFAULT_WIN_SUBJECT

    fun winnerBody(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_WIN_BODY, DEFAULT_WIN_BODY) ?: DEFAULT_WIN_BODY


    private const val KEY_QR_TITLE = "qr_title"
    private const val KEY_QR_BG = "qr_bg_path"
    private const val KEY_QR_LINK = "qr_link_"
    private const val KEY_QR_LABEL = "qr_label_"
    const val DEFAULT_QR_TITLE = "FloorDay"

    fun qrTitle(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_QR_TITLE, DEFAULT_QR_TITLE) ?: DEFAULT_QR_TITLE

    fun qrBgPath(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_QR_BG, "") ?: ""

    fun qrSlotLink(c: Context, i: Int): String {
        val v = c.getSharedPreferences(FILE, 0).getString(KEY_QR_LINK + i, "") ?: ""
        return if (i == 0 && v.isBlank()) signupUrl(c) else v
    }

    fun qrSlotLabel(c: Context, i: Int): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_QR_LABEL + i, "") ?: ""

    fun saveQrSlot(c: Context, i: Int, link: String, label: String) {
        val e = c.getSharedPreferences(FILE, 0).edit()
        if (i == 0) e.putString(KEY_SIGNUP_URL, link.trim()) else e.putString(KEY_QR_LINK + i, link.trim())
        e.putString(KEY_QR_LABEL + i, label.trim()).apply()
    }

    fun saveQrTitle(c: Context, title: String) {
        c.getSharedPreferences(FILE, 0).edit()
            .putString(KEY_QR_TITLE, title.trim().ifBlank { DEFAULT_QR_TITLE }).apply()
    }

    fun setQrBgPath(c: Context, path: String) {
        c.getSharedPreferences(FILE, 0).edit().putString(KEY_QR_BG, path).apply()
    }

    fun saveSignupUrl(c: Context, signupUrl: String) {
        c.getSharedPreferences(FILE, 0).edit()
            .putString(KEY_SIGNUP_URL, signupUrl.trim().ifBlank { DEFAULT_SIGNUP_URL })
            .apply()
    }

    fun organizerEmail(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_ORGANIZER_EMAIL, "") ?: ""

    fun participantSubject(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_PARTICIPANT_SUBJECT, DEFAULT_PARTICIPANT_SUBJECT) ?: DEFAULT_PARTICIPANT_SUBJECT

    fun participantHtml(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_PARTICIPANT_HTML, DEFAULT_PARTICIPANT_HTML) ?: DEFAULT_PARTICIPANT_HTML

    fun saveLocalEmailSettings(c: Context, signupUrl: String, organizer: String, subject: String, html: String) {
        c.getSharedPreferences(FILE, 0).edit()
            .putString(KEY_SIGNUP_URL, signupUrl.trim().ifBlank { DEFAULT_SIGNUP_URL })
            .putString(KEY_ORGANIZER_EMAIL, organizer.trim())
            .putString(KEY_PARTICIPANT_SUBJECT, subject.trim().ifBlank { DEFAULT_PARTICIPANT_SUBJECT })
            .putString(KEY_PARTICIPANT_HTML, html.ifBlank { DEFAULT_PARTICIPANT_HTML })
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

    fun saveLastScan(c: Context, text: String) {
        c.getSharedPreferences(FILE, 0).edit().putString("last_scan_text", text).apply()
    }

    fun lastScan(c: Context): String = c.getSharedPreferences(FILE, 0).getString("last_scan_text", "") ?: ""

    fun saveWinnerTemplate(c: Context, subject: String, body: String) {
        c.getSharedPreferences(FILE, 0).edit()
            .putString(KEY_WIN_SUBJECT, subject)
            .putString(KEY_WIN_BODY, body.ifBlank { DEFAULT_WIN_BODY })
            .apply()
    }
}
