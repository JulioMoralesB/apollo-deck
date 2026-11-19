package com.apollox10.apollodeck.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

// Shared by the dashboard screen and the summary widgets so both read a date
// the same way. Compares by local calendar day rather than raw elapsed
// hours — same fix as the web dashboard's formatEta (SummaryPanel.jsx): a
// date one calendar day out should read as "1 day" regardless of the
// viewer's UTC offset. Handles both a bare "YYYY-MM-DD" (CaduTrack's
// expires_at) and a full ISO-8601 instant (Free Games Notifier's end_date).
fun formatEta(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val targetDate = try {
        if (iso.length == 10) {
            LocalDate.parse(iso)
        } else {
            Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    } catch (e: Exception) {
        return null
    }
    val diffDays = ChronoUnit.DAYS.between(LocalDate.now(), targetDate)
    return when {
        diffDays < 0 -> "expired"
        diffDays == 0L -> "today"
        diffDays == 1L -> "1 day"
        else -> "$diffDays days"
    }
}

// Shared by the dashboard panel, the summary widgets and the Wear energy
// tile, so all of them render the same numbers the same way.
fun formatWatts(watts: Double): String = "${watts.roundToInt()} W"

fun formatKwh(kwh: Double): String = String.format(Locale.US, "%.2f kWh", kwh)

// An estimate (flat price, not CFE's tiered tariff), hence the "~". Whole
// units once it's big enough that cents are noise.
fun formatCost(amount: Double): String =
    if (amount >= 100) "~$${amount.roundToInt()}" else String.format(Locale.US, "~$%.2f", amount)

// "Updated 14 h ago" — only once the data is old enough to matter (an
// hour); fresher than that, saying so is just noise.
fun formatDataAge(fetchedAt: Instant?, now: Instant = Instant.now()): String? {
    val minutes = fetchedAt?.let { ChronoUnit.MINUTES.between(it, now) } ?: return null
    return when {
        minutes < 60 -> null
        minutes < 24 * 60 -> "Updated ${minutes / 60} h ago"
        else -> "Updated ${minutes / (24 * 60)} d ago"
    }
}
