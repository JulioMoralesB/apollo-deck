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
// before the request is even sent, so a body-less POST/PUT/PATCH action
// gets an empty body instead. Confirmed on real hardware: fetch() in the
// web dashboard doesn't enforce this, only this OkHttp-based client did.
private val METHODS_REQUIRING_BODY = setOf("POST", "PUT", "PATCH")

// Executes an Action: an arbitrary method + endpoint (+ optional JSON body)
// relative to the Home Assistant base URL, e.g. POST
// api/services/script/turn_on. Not a Retrofit call, since the method and
// path come from the Action at runtime.
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
}
