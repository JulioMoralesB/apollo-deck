package com.apollox10.apollodeck.core.auth

import com.apollox10.apollodeck.core.model.LoginRequest
import com.apollox10.apollodeck.core.net.ApolloDeckClient
import com.apollox10.apollodeck.core.store.TokenStore

// High-level login/logout, mirroring the web dashboard's utils/auth.js.
// Widget and tile code should go through this rather than touching
// TokenStore or the pre-auth API directly.
class AuthRepository(private val client: ApolloDeckClient) {

    val tokenStore: TokenStore get() = client.tokenStore

    fun isLoggedIn(): Boolean = client.tokenStore.isLoggedIn()

    suspend fun login(username: String, password: String) {
        val tokens = client.preAuthApi.login(LoginRequest(username, password))
        client.tokenStore.saveTokenPair(tokens.accessToken, tokens.refreshToken)
    }

    fun logout() {
        client.tokenStore.clear()
    }
}
