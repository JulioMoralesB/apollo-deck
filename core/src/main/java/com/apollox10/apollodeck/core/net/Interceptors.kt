package com.apollox10.apollodeck.core.net

import com.apollox10.apollodeck.core.store.CloudflareAccessStore
import com.apollox10.apollodeck.core.store.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = tokenStore.getAccessToken()
        val request = if (accessToken != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

class CloudflareAccessInterceptor(private val store: CloudflareAccessStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = store.getCredentials()
        val request = if (credentials != null) {
            chain.request().newBuilder()
                .header("CF-Access-Client-Id", credentials.clientId)
                .header("CF-Access-Client-Secret", credentials.clientSecret)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
