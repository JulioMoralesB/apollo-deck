package com.apollox10.apollodeck.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

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

// Key ApolloDeckClient.fetchSummary adds next to the service's own fields:
// when Home Assistant last fetched this summary from the service.
const val SUMMARY_FETCHED_AT_KEY = "_fetched_at"

// Parses a summary *as of now*, not as of when it was fetched. Home
// Assistant only polls the apollo services while apollo is up (about an
// hour a day), so a cached summary can be most of a day old — and a
// product "expiring today" yesterday is expired today, a promotion that
// ended this morning is gone. Every consumer (dashboard, widgets, tile)
// parses the cached raw JSON at render time, so doing it here keeps all of
// them honest without each re-deriving it.
fun parseServiceSummary(
    raw: String,
    json: Json = defaultJson,
    today: LocalDate = LocalDate.now(),
    now: Instant = Instant.now(),
): ServiceSummary {
    return try {
        val obj = json.parseToJsonElement(raw).jsonObject
        when {
            "active_promotions" in obj ->
                ServiceSummary.FreeGames(json.decodeFromString<FreeGamesSummary>(raw).withoutEndedPromotions(now))
            "expired" in obj && "expiring_soon" in obj ->
                ServiceSummary.CaduTrack(json.decodeFromString<CaduTrackSummary>(raw).rebucketedFor(today))
            "power_w" in obj -> ServiceSummary.Energy(json.decodeFromString(raw))
            else -> ServiceSummary.Unknown
        }
    } catch (e: Exception) {
        ServiceSummary.Error(e.message ?: "Failed to parse summary")
    }
}

// When Home Assistant last fetched this summary, or null if unknown.
fun summaryFetchedAt(raw: String, json: Json = defaultJson): Instant? = try {
    (json.parseToJsonElement(raw).jsonObject[SUMMARY_FETCHED_AT_KEY] as? JsonPrimitive)?.content
        ?.let { OffsetDateTime.parse(it).toInstant() }
} catch (e: Exception) {
    null
}

// `next` holds only not-yet-expired products sharing the soonest date (see
// CaduTrack's summary contract), so once that date has passed they belong
// in expired_products. Counts follow. A product that was expiring-soon but
// not in `next` can't be moved — the payload never named it — so the
// counts stay approximate until the next fetch.
private fun CaduTrackSummary.rebucketedFor(today: LocalDate): CaduTrackSummary {
    val (nowExpired, stillNext) = next.partition { item ->
        runCatching { LocalDate.parse(item.expiresAt) }.getOrNull()?.isBefore(today) == true
    }
    if (nowExpired.isEmpty()) return this
    return copy(
        expired = expired + nowExpired.size,
        expiredProducts = expiredProducts + nowExpired,
        expiringSoon = (expiringSoon - nowExpired.size).coerceAtLeast(0),
        next = stillNext,
    )
}

// A promotion with no (or an unparseable) end date is kept.
private fun FreeGamesSummary.withoutEndedPromotions(now: Instant): FreeGamesSummary =
    copy(
        activePromotions = activePromotions.filter { promo ->
            val end = promo.endDate?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
            end == null || end.isAfter(now)
        },
    )
