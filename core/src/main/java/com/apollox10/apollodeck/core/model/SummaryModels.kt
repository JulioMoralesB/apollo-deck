package com.apollox10.apollodeck.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

// Free Games Notifier's SummaryResponse: { service, active_promotions[], last_check_at }
@Serializable
data class FreeGamesPromotion(
    val title: String,
    val store: String,
    @SerialName("end_date") val endDate: String? = null,
    val link: String? = null,
)

@Serializable
data class FreeGamesSummary(
    val service: String,
    @SerialName("active_promotions") val activePromotions: List<FreeGamesPromotion> = emptyList(),
)

// CaduTrack's /summary: { expired, expiring_soon, next: { name, expires_at }[] }
// `next` holds every item tied for the most urgent expiration date, not just one.
@Serializable
data class CaduTrackNextItem(
    val name: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class CaduTrackSummary(
    val expired: Int,
    @SerialName("expiring_soon") val expiringSoon: Int,
    val next: List<CaduTrackNextItem> = emptyList(),
)

// Mirrors the web dashboard's SummaryPanel.jsx: each service defines its own
// summary contract (the backend's summary_dispatcher proxies the raw
// upstream JSON through as-is), so this dispatches on response shape rather
// than on service name — an unrecognized shape renders nothing rather than
// crashing, same as the web.
sealed interface ServiceSummary {
    data class FreeGames(val data: FreeGamesSummary) : ServiceSummary
    data class CaduTrack(val data: CaduTrackSummary) : ServiceSummary
    data class Error(val message: String) : ServiceSummary
    data object Unknown : ServiceSummary
}

private val defaultJson = Json { ignoreUnknownKeys = true }

fun parseServiceSummary(raw: String, json: Json = defaultJson): ServiceSummary {
    return try {
        val obj = json.parseToJsonElement(raw).jsonObject
        when {
            "active_promotions" in obj -> ServiceSummary.FreeGames(json.decodeFromString(raw))
            "expired" in obj && "expiring_soon" in obj -> ServiceSummary.CaduTrack(json.decodeFromString(raw))
            else -> ServiceSummary.Unknown
        }
    } catch (e: Exception) {
        ServiceSummary.Error(e.message ?: "Failed to parse summary")
    }
}
