package com.apollox10.apollodeck.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
