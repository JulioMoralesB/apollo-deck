package com.apollox10.apollodeck.core.store

import android.content.Context

// The base URL has no implicit value the way it does for the web dashboard
// (which is always served from the backend's own origin) — a native app has
// to be told where the server lives, once, on first run.
class ServerConfigStore(context: Context) {
    private val prefs = encryptedPrefs(context)

    fun getBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)

    fun setBaseUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_BASE_URL).apply()
    }

    private companion object {
        const val KEY_BASE_URL = "server_base_url"
    }
}
