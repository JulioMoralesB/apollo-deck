package com.apollox10.apollodeck.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Action(
    val label: String,
    val icon: String,
    val href: String? = null,
    val endpoint: String? = null,
    val method: String? = null,
    val confirm: Boolean = false,
    @SerialName("show_response") val showResponse: Boolean = false,
)

@Serializable
data class Service(
    val name: String,
    val status: String,
    val icon: String? = null,
    val url: String? = null,
    val actions: List<Action>? = null,
    // Home Assistant entity that holds this service's last /api/summary
    // response (see ApolloDeckClient.getServices), not a URL.
    val summaryEntityId: String? = null,
)

@Serializable
data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    @SerialName("status_code") val statusCode: Int? = null,
    val body: String? = null,
)

// GET /api/ — Home Assistant answers {"message": "API running."} to any
// valid token, so it doubles as the login check.
@Serializable
data class HaApiStatus(
    val message: String,
)

@Serializable
data class HaState(
    @SerialName("entity_id") val entityId: String,
    val state: String,
    val attributes: JsonObject = JsonObject(emptyMap()),
)
