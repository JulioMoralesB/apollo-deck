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

// Methods OkHttp's Request.Builder.method(String, RequestBody?) refuses to
// pair with a null body — passing null for one of these throws
// IllegalArgumentException("method $method must have a request body.")
// before the request is even sent. Every action call here goes through
// with bodyJson == null (nothing currently populates it — the backend's
// action_dispatcher takes the body to forward upstream from its own
// services.yaml config, not from this request), so any POST/PUT/PATCH
// action with no body configured always hit this. Confirmed on real
// hardware/backend: the web dashboard doesn't have this problem (fetch
// doesn't enforce it), only this OkHttp-based client did.
private val METHODS_REQUIRING_BODY = setOf("POST", "PUT", "PATCH")

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

        val upperMethod = method.uppercase()
        val requestBody = when {
            bodyJson != null -> bodyJson.toRequestBody("application/json".toMediaType())
            upperMethod in METHODS_REQUIRING_BODY -> "".toRequestBody(null)
            else -> null
        }

        val request = Request.Builder()
            .url(url)
            .method(upperMethod, requestBody)
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

    // GET a service's own summary_endpoint and return the raw JSON body —
    // unlike execute(), the response isn't an ActionResult, it's whatever
    // shape that particular service's own summary contract defines (see
    // ServiceSummary/parseServiceSummary), so parsing is left to the caller.
    suspend fun fetchSummary(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl.toHttpUrl().resolve(endpoint)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid summary endpoint: $endpoint"))
            val request = Request.Builder().url(url).get().build()
            authenticatedClient.newCall(request).execute().use { response ->
                val raw = response.body?.string()
                if (response.isSuccessful && !raw.isNullOrBlank()) {
                    Result.success(raw)
                } else {
                    Result.failure(Exception(raw?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
