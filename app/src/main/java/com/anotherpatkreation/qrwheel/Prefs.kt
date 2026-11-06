package com.anotherpatkreation.qrwheel

import android.content.Context

object Prefs {
    private const val FILE = "qrwheel"
    private const val KEY_SIGNUP_URL = "signup_url"
    private const val KEY_API_BASE = "api_base"
    private const val KEY_ADMIN_TOKEN = "admin_token"
    const val DEFAULT_SIGNUP_URL = "https://lenovo.highspot.com/items/6ab6621456d0d11fe3c8c144#1"

    fun signupUrl(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_SIGNUP_URL, DEFAULT_SIGNUP_URL) ?: DEFAULT_SIGNUP_URL

    fun apiBase(c: Context): String =
        (c.getSharedPreferences(FILE, 0).getString(KEY_API_BASE, "") ?: "").trim().trimEnd('/')

    fun adminToken(c: Context): String =
        c.getSharedPreferences(FILE, 0).getString(KEY_ADMIN_TOKEN, "") ?: ""

    fun save(c: Context, signupUrl: String, apiBase: String, adminToken: String) {
        c.getSharedPreferences(FILE, 0).edit()
            .putString(KEY_SIGNUP_URL, signupUrl.trim().ifBlank { DEFAULT_SIGNUP_URL })
            .putString(KEY_API_BASE, apiBase.trim())
            .putString(KEY_ADMIN_TOKEN, adminToken.trim())
            .apply()
    }
}
