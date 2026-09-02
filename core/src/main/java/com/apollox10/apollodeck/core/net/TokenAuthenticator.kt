package com.apollox10.apollodeck.core.net

import com.apollox10.apollodeck.core.model.RefreshRequest
import com.apollox10.apollodeck.core.store.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

// Mirrors the web dashboard's authFetch: on a 401, exchange the refresh
// token for a new access token and retry once — silently, no re-login
// unless the refresh token itself is invalid or expired. This is exactly
// what lets a background widget or Wear tile refresh keep working without
// ever showing an interactive prompt.
//
// `preAuthApi` must be built on a client with no authenticator attached —
// calling the authenticated client here would recurse into this same
// authenticate() on the refresh request itself.
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val preAuthApi: ApolloDeckApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null // already retried once — give up

        val refreshToken = tokenStore.getRefreshToken() ?: return null

        val newAccessToken = runBlocking {
            try {
                preAuthApi.refresh(RefreshRequest(refreshToken)).accessToken
            } catch (e: Exception) {
                null
            }
        } ?: return null

        tokenStore.saveAccessToken(newAccessToken)

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
