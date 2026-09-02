package com.apollox10.apollodeck.core.store

import android.content.Context

data class CloudflareAccessCredentials(
    val clientId: String,
    val clientSecret: String,
)

// Optional. Only needed when the backend sits behind Cloudflare Access with
// a Service Token policy (the non-interactive counterpart to the browser
// login flow Access normally does) — see the auth writeup in
// apollo-server-dashboard for why this is the recommended layer for a
// background client instead of a hand-rolled login.
class CloudflareAccessStore(context: Context) {
    private val prefs = encryptedPrefs(context)

    fun getCredentials(): CloudflareAccessCredentials? {
        val clientId = prefs.getString(KEY_CLIENT_ID, null) ?: return null
        val clientSecret = prefs.getString(KEY_CLIENT_SECRET, null) ?: return null
        return CloudflareAccessCredentials(clientId, clientSecret)
    }

    fun setCredentials(credentials: CloudflareAccessCredentials) {
        prefs.edit()
            .putString(KEY_CLIENT_ID, credentials.clientId)
            .putString(KEY_CLIENT_SECRET, credentials.clientSecret)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_CLIENT_ID)
            .remove(KEY_CLIENT_SECRET)
            .apply()
    }

    private companion object {
        const val KEY_CLIENT_ID = "cf_access_client_id"
        const val KEY_CLIENT_SECRET = "cf_access_client_secret"
    }
}
