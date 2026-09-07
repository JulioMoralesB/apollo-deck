package com.apollox10.apollodeck.widget

import com.apollox10.apollodeck.core.model.ServiceSummary
import com.apollox10.apollodeck.core.model.formatEta

enum class LineEmphasis { Normal, Warning, Danger }

data class SummaryLine(val label: String, val value: String, val emphasis: LineEmphasis = LineEmphasis.Normal)

// Condenses a full ServiceSummary down to a handful of "label: value" lines
// for a widget — the combined widget shows exactly one line per service (a
// single representative headline), the per-service widget has room for a
// couple more. Same shape-dispatch reasoning as SummaryPanel.jsx/
// SummarySection in DashboardScreen.kt: unrecognized shapes/errors still
// produce something to show rather than a blank row.
fun condensedSummaryLines(summary: ServiceSummary, maxLines: Int): List<SummaryLine> = when (summary) {
    is ServiceSummary.FreeGames -> {
        val promos = summary.data.activePromotions
        if (promos.isEmpty()) {
            listOf(SummaryLine("Free now", "None"))
        } else {
            val headline = promos.first()
            val eta = formatEta(headline.endDate)
            val extra = promos.size - 1
            val headlineValue = headline.title + if (maxLines == 1 && extra > 0) " +$extra" else ""
            buildList {
                add(SummaryLine("Free now", headlineValue))
                if (maxLines > 1) {
                    add(SummaryLine(headline.store, eta?.let { "ends in $it" } ?: "no end date"))
                }
                if (maxLines > 2 && extra > 0) {
                    add(SummaryLine("Also free", "+$extra more"))
                }
            }.take(maxLines)
        }
    }

    is ServiceSummary.CaduTrack -> {
        val data = summary.data
        val soonest = data.next.firstOrNull()
        val headlineValue = when {
            soonest != null -> soonest.name + (formatEta(soonest.expiresAt)?.let { " · $it" } ?: "")
            data.expiringSoon > 0 -> "${data.expiringSoon} items"
            else -> "Nothing tracked"
        }
        val headlineEmphasis = if (data.expiringSoon > 0) LineEmphasis.Warning else LineEmphasis.Normal
        buildList {
            add(SummaryLine("Expiring soon", headlineValue, headlineEmphasis))
            if (maxLines > 1 && data.expired > 0) {
                add(SummaryLine("Expired", "${data.expired}", LineEmphasis.Danger))
            }
            if (maxLines > 2 && data.next.size > 1) {
                add(SummaryLine("Also soon", "+${data.next.size - 1} more"))
            }
        }.take(maxLines)
    }

    is ServiceSummary.Error -> listOf(SummaryLine("Error", summary.message, LineEmphasis.Danger))
    ServiceSummary.Unknown -> emptyList()
}
