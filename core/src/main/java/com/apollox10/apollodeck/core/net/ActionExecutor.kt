package com.apollox10.apollodeck.core.net

import com.apollox10.apollodeck.core.model.ActionResult
import com.apollox10.apollodeck.core.model.isScriptEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// Runs an Action — a Home Assistant script, identified by its entity id
// (see isScriptEndpoint). Goes through script.turn_on rather than calling
// the script's own service (POST api/services/script/<name>): that one
// blocks until the script finishes, so a slow script (a light fading in, a
// multi-step Spotify call) would hit the HTTP timeout and read as a
// failure. turn_on returns as soon as it has started, so success here
// means "started".
class ActionExecutor(
    private val authenticatedClient: OkHttpClient,
    private val baseUrl: String,
) {
    suspend fun execute(endpoint: String): ActionResult = withContext(Dispatchers.IO) {
        if (!isScriptEndpoint(endpoint)) {
            return@withContext ActionResult(success = false, message = "Reconfigure this action")
        }

        val body = buildJsonObject { put("entity_id", JsonPrimitive(endpoint)) }
        val request = Request.Builder()
            .url(baseUrl.toHttpUrl().resolve("api/services/script/turn_on")!!)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        authenticatedClient.newCall(request).execute().use { response ->
            ActionResult(success = response.isSuccessful, statusCode = response.code)
        }
    }
}
