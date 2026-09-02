package com.apollox10.apollodeck.core.net

import com.apollox10.apollodeck.core.model.AccessTokenResponse
import com.apollox10.apollodeck.core.model.LoginRequest
import com.apollox10.apollodeck.core.model.RefreshRequest
import com.apollox10.apollodeck.core.model.Service
import com.apollox10.apollodeck.core.model.TokenPairResponse
import com.apollox10.apollodeck.core.model.VersionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Fixed-method endpoints only. The action-dispatcher endpoint
// (/services/{slug}/actions/{slug}) has a method that varies per action —
// configured per-service in the backend's services.yaml — which Retrofit
// annotations can't express dynamically. See ActionExecutor for that one.
interface ApolloDeckApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenPairResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AccessTokenResponse

    @GET("version")
    suspend fun getVersion(): VersionResponse

    @GET("services")
    suspend fun getServices(): List<Service>
}
