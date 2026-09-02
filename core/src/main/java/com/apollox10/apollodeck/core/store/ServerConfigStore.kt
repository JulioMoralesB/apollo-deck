package com.apollox10.apollodeck.core.store

import android.content.Context

// The base URL has no implicit value the way it does for the web dashboard
// (which is always served from the backend's own origin) — a native app has
// to be told where the server lives, once, on first run.
class ServerConfigStore(context: Context) {
    private val prefs = encryptedPrefs(context)

    fun getBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)

    fun setBaseUrl(url: String) {
        // Retrofit's baseUrl() rejects anything without a scheme — rather
        // than surface that as a confusing error, assume https (this app
        // always talks to a self-hosted server over a tunnel, never plain
        // http) unless the user typed an explicit scheme themselves.
        val withScheme = if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(url)) {
            url
        } else {
            "https://$url"
        }
        val normalized = if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_BASE_URL).apply()
    }

    private companion object {
        const val KEY_BASE_URL = "server_base_url"
    }
}
