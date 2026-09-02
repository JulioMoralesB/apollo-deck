package com.apollox10.apollodeck.core.net

import com.apollox10.apollodeck.core.model.ActionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// Executes a Service action against the backend's dynamic dispatcher route.
// Not a Retrofit call — the HTTP method is chosen per-action in the
// backend's services.yaml (GET/POST/PUT/DELETE/PATCH), which Retrofit's
// method annotations can't express at runtime.
class ActionExecutor(
    private val authenticatedClient: OkHttpClient,
    private val baseUrl: String,
    private val json: Json,
) {
    suspend fun execute(
        endpoint: String,
        method: String,
        bodyJson: String? = null,
    ): ActionResult = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrl().resolve(endpoint)
            ?: error("Invalid action endpoint: $endpoint")

        val requestBody = bodyJson?.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .method(method.uppercase(), requestBody)
            .build()

        authenticatedClient.newCall(request).execute().use { response ->
            val raw = response.body?.string()
            if (raw.isNullOrBlank()) {
                ActionResult(success = response.isSuccessful, statusCode = response.code)
            } else {
                try {
                    json.decodeFromString(ActionResult.serializer(), raw)
                } catch (e: Exception) {
                    ActionResult(success = response.isSuccessful, statusCode = response.code, body = raw)
                }
            }
        }
    }
}
