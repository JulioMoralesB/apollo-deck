package com.apollox10.apollodeck.core.net

import android.content.Context
import com.apollox10.apollodeck.core.model.Action
import com.apollox10.apollodeck.core.model.HaState
import com.apollox10.apollodeck.core.model.Service
import com.apollox10.apollodeck.core.model.haIconToAppIcon
import com.apollox10.apollodeck.core.store.ConfirmActionStore
import com.apollox10.apollodeck.core.store.ServerConfigStore
import com.apollox10.apollodeck.core.store.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit

private const val SUMMARY_ATTRIBUTE = "summary"
private const val SCRIPTS_SERVICE_NAME = "Home Assistant"
private val SUMMARY_NAME_SUFFIX = Regex(" summary$", RegexOption.IGNORE_CASE)

// Everything a caller needs to talk to Home Assistant, which is the app's
// only backend: HA runs 24/7 on the Raspberry Pi and polls the apollo
// services' /api/summary endpoints while apollo is up, keeping the last
// good response in a template sensor (Server-Documentation ADR 013).
class ApolloDeckClient(
    val api: HomeAssistantApi,
    val actionExecutor: ActionExecutor,
    val tokenStore: TokenStore,
    val serverConfigStore: ServerConfigStore,
    private val confirmActionStore: ConfirmActionStore,
) {

    // Two kinds of service come out of HA's states:
    // - any sensor carrying a `summary` attribute, so adding one is HA
    //   configuration only, no app change. The name drops the sensor's
    //   " summary" suffix ("CaduTrack summary" -> "CaduTrack"), which is what
    //   widget configs and the summary cache are keyed by;
    // - one "Home Assistant" service whose actions are HA's scripts. Only
    //   scripts, not switches: HA's switch list mixes in config toggles and
    //   switches that cut power to things (e.g. a server's smart plug), so a
    //   switch worth a button gets wrapped in a script on the HA side.
    suspend fun getServices(): List<Service> {
        val states = api.getStates()
        val summaryServices = states
            .filter { it.entityId.startsWith("sensor.") && it.attributes[SUMMARY_ATTRIBUTE] is JsonObject }
            .map { state ->
                Service(
                    name = state.friendlyName().replace(SUMMARY_NAME_SUFFIX, ""),
                    status = if (state.state == "unavailable") "offline" else "online",
                    icon = haIconToAppIcon(state.stringAttribute("icon")),
                    summaryEntityId = state.entityId,
                )
            }
        val confirmRequired = confirmActionStore.getAll()
        val scriptActions = states
            .filter { it.entityId.startsWith("script.") && it.state != "unavailable" }
            .map { state ->
                Action(
                    label = state.friendlyName(),
                    icon = haIconToAppIcon(state.stringAttribute("icon")) ?: "play",
                    endpoint = state.entityId,
                    method = "POST",
                    confirm = state.entityId in confirmRequired,
                )
            }
            .sortedBy { it.label.lowercase() }
        val scriptsService = if (scriptActions.isEmpty()) null else Service(
            name = SCRIPTS_SERVICE_NAME,
            status = "online",
            icon = "home",
            actions = scriptActions,
        )
        return (summaryServices + listOfNotNull(scriptsService)).sortedBy { it.name.lowercase() }
    }

    // The raw /api/summary JSON exactly as the service returned it, so the
    // existing shape-dispatching parser (parseServiceSummary) reads it as-is.
    suspend fun fetchSummary(entityId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val summary = api.getState(entityId).attributes[SUMMARY_ATTRIBUTE]
            if (summary is JsonObject) {
                Result.success(summary.toString())
            } else {
                Result.failure(IllegalStateException("No summary yet for $entityId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

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

        val baseUrl = serverConfigStore.getBaseUrl()
            ?: error("Home Assistant URL not configured — call ServerConfigStore.setBaseUrl() first")

        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (debugLogging) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(tokenStore))
            .addInterceptor(loggingInterceptor)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(HomeAssistantApi::class.java)

        return ApolloDeckClient(
            api = api,
            actionExecutor = ActionExecutor(httpClient, baseUrl),
            tokenStore = tokenStore,
            serverConfigStore = serverConfigStore,
            confirmActionStore = ConfirmActionStore(context),
        )
    }
}

private fun HaState.stringAttribute(key: String): String? = (attributes[key] as? JsonPrimitive)?.content

private fun HaState.friendlyName(): String = stringAttribute("friendly_name") ?: entityId
