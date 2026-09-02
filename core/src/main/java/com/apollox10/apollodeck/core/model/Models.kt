package com.apollox10.apollodeck.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class AccessTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int,
)

@Serializable
data class TokenPairResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class VersionResponse(
    val version: String,
)

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
)

@Serializable
data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    @SerialName("status_code") val statusCode: Int? = null,
    val body: String? = null,
)
