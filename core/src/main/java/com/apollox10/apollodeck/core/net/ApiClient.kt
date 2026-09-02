package com.apollox10.apollodeck.core.net

import android.content.Context
import com.apollox10.apollodeck.core.store.CloudflareAccessStore
import com.apollox10.apollodeck.core.store.ServerConfigStore
import com.apollox10.apollodeck.core.store.TokenStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// Bundles everything a caller needs to talk to the backend: an
// unauthenticated API for login/refresh, an authenticated API (auto-refreshes
// on 401 via TokenAuthenticator) for everything else, and the action
// executor for the dynamic-method dispatcher route.
class ApolloDeckClient(
    val preAuthApi: ApolloDeckApi,
    val authenticatedApi: ApolloDeckApi,
    val actionExecutor: ActionExecutor,
    val tokenStore: TokenStore,
    val serverConfigStore: ServerConfigStore,
    val cloudflareAccessStore: CloudflareAccessStore,
)

object ApiClient {

    /**
     * @param debugLogging Enable full HTTP request/response logging. Pass
     *   `BuildConfig.DEBUG` from the calling module — never true in a
     *   release build, it logs the bearer token.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun create(context: Context, debugLogging: Boolean = false): ApolloDeckClient {
        val serverConfigStore = ServerConfigStore(context)
        val tokenStore = TokenStore(context)
        val cloudflareAccessStore = CloudflareAccessStore(context)

        val baseUrl = serverConfigStore.getBaseUrl()
            ?: error("Server URL not configured — call ServerConfigStore.setBaseUrl() first")

        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (debugLogging) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        // No auth header, no authenticator — used for login and for the
        // refresh call itself, so refreshing never recurses into retrying
        // its own request.
        val preAuthClient = OkHttpClient.Builder()
            .addInterceptor(CloudflareAccessInterceptor(cloudflareAccessStore))
            .addInterceptor(loggingInterceptor)
            .build()

        val preAuthApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(preAuthClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApolloDeckApi::class.java)

        val authenticatedClient = OkHttpClient.Builder()
            .addInterceptor(CloudflareAccessInterceptor(cloudflareAccessStore))
            .addInterceptor(AuthHeaderInterceptor(tokenStore))
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(tokenStore, preAuthApi))
            .build()

        val authenticatedApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(authenticatedClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApolloDeckApi::class.java)

        val actionExecutor = ActionExecutor(authenticatedClient, baseUrl, json)

        return ApolloDeckClient(
            preAuthApi = preAuthApi,
            authenticatedApi = authenticatedApi,
            actionExecutor = actionExecutor,
            tokenStore = tokenStore,
            serverConfigStore = serverConfigStore,
            cloudflareAccessStore = cloudflareAccessStore,
        )
    }
}
