package com.apollox10.apollodeck.core.net

import com.apollox10.apollodeck.core.model.HaApiStatus
import com.apollox10.apollodeck.core.model.HaState
import retrofit2.http.GET
import retrofit2.http.Path

// Home Assistant's REST API. Service calls (POST /api/services/...) go
// through ActionExecutor instead, since an Action carries its own
// endpoint/method/body.
interface HomeAssistantApi {

    @GET("api/")
    suspend fun getApiStatus(): HaApiStatus

    @GET("api/states")
    suspend fun getStates(): List<HaState>

    @GET("api/states/{entityId}")
    suspend fun getState(@Path("entityId") entityId: String): HaState
}
