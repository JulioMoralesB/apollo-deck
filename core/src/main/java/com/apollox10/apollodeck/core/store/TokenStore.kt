package com.apollox10.apollodeck.core.store

import android.content.Context

// Holds the access/refresh token pair. The access token is short-lived and
// gets swapped out silently on every 401 (see TokenAuthenticator); the
// refresh token is what lets a widget or tile stay logged in across days
// without ever showing a login prompt, so long as it hasn't expired.
class TokenStore(context: Context) {
    private val prefs = encryptedPrefs(context)

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveTokenPair(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun saveAccessToken(accessToken: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply()
    }

    fun isLoggedIn(): Boolean = getRefreshToken() != null

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
