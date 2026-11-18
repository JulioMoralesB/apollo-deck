package com.apollox10.apollodeck.core.net

import com.apollox10.apollodeck.core.store.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
