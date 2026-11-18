package com.apollox10.apollodeck.core.auth

import android.content.Context
import com.apollox10.apollodeck.core.net.ApiClient
import com.apollox10.apollodeck.core.store.ServerConfigStore
import com.apollox10.apollodeck.core.store.TokenStore

// Connect/disconnect against Home Assistant with a long-lived access token
// (HA → Profile → Security). Widget and tile code should go through this
// rather than touching the stores directly.
object HomeAssistantAuth {

    // Saves the URL + token and checks them against GET /api/. On any
    // failure (bad token, unreachable host) the token is cleared again, so a
    // failed attempt never leaves the app half logged in. Throws whatever
    // Retrofit/OkHttp threw, for the caller to turn into a message.
    suspend fun connect(context: Context, url: String, token: String, debugLogging: Boolean) {
        ServerConfigStore(context).setBaseUrl(url)
        val tokenStore = TokenStore(context)
        tokenStore.saveToken(token)
        try {
            ApiClient.create(context, debugLogging).api.getApiStatus()
        } catch (e: Exception) {
            tokenStore.clear()
            throw e
        }
    }

    fun isConnected(context: Context): Boolean = TokenStore(context).isLoggedIn()

    fun disconnect(context: Context) {
        TokenStore(context).clear()
    }
}
