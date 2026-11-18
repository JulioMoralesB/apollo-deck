package com.apollox10.apollodeck.core.store

import android.content.Context

// Holds the Home Assistant long-lived access token. It doesn't expire and
// there's no refresh step, which is what lets widgets, tiles and background
// workers keep working indefinitely without an interactive prompt.
class TokenStore(context: Context) {
    private val prefs = encryptedPrefs(context)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .apply {
                // Left behind by the pre-Home Assistant dashboard login
                // (JWT pair + optional Cloudflare Access service token).
                LEGACY_KEYS.forEach { remove(it) }
            }
            .apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN = "ha_long_lived_token"
        val LEGACY_KEYS = listOf(
            "access_token",
            "refresh_token",
            "cf_access_client_id",
            "cf_access_client_secret",
        )
    }
}
