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

// CaduTrack's /summary: { expired, expired_products[], expiring_soon, next[] }
// `next` and `expired_products` share the same item shape. `next` holds the
// item(s) genuinely expiring soon (not yet expired); `expired_products` holds
// the already-expired ones — split into separate fields, unlike the earlier
// contract where `next` conflated both by always surfacing whichever bucket
// was most urgent.
@Serializable
data class CaduTrackItem(
    val name: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class CaduTrackSummary(
    val expired: Int,
    @SerialName("expired_products") val expiredProducts: List<CaduTrackItem> = emptyList(),
    @SerialName("expiring_soon") val expiringSoon: Int,
    val next: List<CaduTrackItem> = emptyList(),
)

// Home Assistant's own energy totals (sensor.energia_summary — a template
// sensor shaped like the services' summaries so it's discovered the same
// way): live total power, today's kWh (null until the daily meter has a
// value), a rolling 60-day daily series (completed days from
// sensor.energia_historial_diario plus today so far — it starts empty and
// fills up a day at a time), a flat price for an estimated cost, and a
// per-device breakdown.
@Serializable
data class EnergyDay(
    val date: String,
    val kwh: Double,
)

@Serializable
data class EnergyDevice(
    val name: String,
    @SerialName("power_w") val powerW: Double,
)

@Serializable
data class EnergySummary(
    @SerialName("power_w") val powerW: Double,
    @SerialName("today_kwh") val todayKwh: Double? = null,
    @SerialName("last_60_days_kwh") val last60DaysKwh: Double? = null,
    val daily: List<EnergyDay> = emptyList(),
    @SerialName("price_per_kwh") val pricePerKwh: Double? = null,
    val currency: String? = null,
    val devices: List<EnergyDevice> = emptyList(),
) {
    // Null when no price is configured, so callers just skip showing a cost.
    fun costOf(kwh: Double): Double? = pricePerKwh?.takeIf { it > 0 }?.let { kwh * it }
}

// Mirrors the web dashboard's SummaryPanel.jsx: each service defines its own
// summary contract (the backend's summary_dispatcher proxies the raw
// upstream JSON through as-is), so this dispatches on response shape rather
// than on service name — an unrecognized shape renders nothing rather than
// crashing, same as the web.
sealed interface ServiceSummary {
    data class FreeGames(val data: FreeGamesSummary) : ServiceSummary
    data class CaduTrack(val data: CaduTrackSummary) : ServiceSummary
    data class Energy(val data: EnergySummary) : ServiceSummary
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
            "power_w" in obj -> ServiceSummary.Energy(json.decodeFromString(raw))
            else -> ServiceSummary.Unknown
        }
    } catch (e: Exception) {
        ServiceSummary.Error(e.message ?: "Failed to parse summary")
    }
}
