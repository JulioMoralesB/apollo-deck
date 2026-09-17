package com.apollox10.apollodeck.widget

import com.apollox10.apollodeck.core.model.CaduTrackSummary
import com.apollox10.apollodeck.core.model.ServiceSummary
import com.apollox10.apollodeck.core.model.formatEta

enum class LineEmphasis { Normal, Warning, Danger }

// CaduTrack now splits expired items into their own `expired_products` field
// (see SummaryModels.kt) rather than conflating them into `next`. Both
// summary widgets still only have room for one bucket at a time, so this
// picks whichever is more urgent: expired outranks merely-soon.
private fun caduTrackItemsExpired(data: CaduTrackSummary): Boolean =
    data.expiredProducts.isNotEmpty()

// Both summary widgets show a list of item names (game titles, or CaduTrack's
// soonest-expiring items) rather than per-item metadata — tapping the widget
// opens the full panel for that detail. Same shape-dispatch reasoning as
// SummaryPanel.jsx/SummarySection in DashboardScreen.kt: unrecognized shapes/
// errors still produce something to show rather than a blank widget.
fun rowLabel(summary: ServiceSummary): String = when (summary) {
    is ServiceSummary.FreeGames -> "Free now"
    is ServiceSummary.CaduTrack -> if (caduTrackItemsExpired(summary.data)) "Expired" else "Expiring soon"
    is ServiceSummary.Error -> "Error"
    ServiceSummary.Unknown -> ""
}

// More descriptive label used only where there's a full line to spend on
// it — the combined widget's expanded row format (label on its own line,
// not sharing width with the value like the compact row does). The
// per-service widget keeps the terser rowLabel for its header since the
// service name is already right above it as the widget's own title.
fun expandedRowLabel(summary: ServiceSummary): String = when (summary) {
    is ServiceSummary.FreeGames -> "Free games"
    is ServiceSummary.CaduTrack -> if (caduTrackItemsExpired(summary.data)) "Food expired" else "Food expiring soon"
    is ServiceSummary.Error -> "Error"
    ServiceSummary.Unknown -> ""
}

fun itemNames(summary: ServiceSummary): List<String> = when (summary) {
    is ServiceSummary.FreeGames -> summary.data.activePromotions.map { it.title }
    is ServiceSummary.CaduTrack -> {
        val data = summary.data
        (if (caduTrackItemsExpired(data)) data.expiredProducts else data.next).map { it.name }
    }
    is ServiceSummary.Error -> emptyList()
    ServiceSummary.Unknown -> emptyList()
}

fun emptyPlaceholder(summary: ServiceSummary): String = when (summary) {
    is ServiceSummary.FreeGames -> "No active promotions"
    is ServiceSummary.CaduTrack -> "Nothing tracked"
    is ServiceSummary.Error -> summary.message
    ServiceSummary.Unknown -> ""
}

// A standalone count statement ("5 free games"), for when there's no room
// to name even a single item — "+N more" only makes sense as a continuation
// of names already shown above it; with zero shown, "+N more" claims
// something is already listed when nothing is, which reads as flat-out
// wrong rather than just terse. Always self-descriptive on its own (doesn't
// lean on the header row for context), in case that's ever tight on space too.
fun itemCountPhrase(summary: ServiceSummary, count: Int): String {
    val plural = if (count == 1) "" else "s"
    return when (summary) {
        is ServiceSummary.FreeGames -> "$count free game$plural"
        is ServiceSummary.CaduTrack -> {
            val verb = if (caduTrackItemsExpired(summary.data)) "expired" else "expiring"
            "$count item$plural $verb"
        }
        is ServiceSummary.Error -> summary.message
        ServiceSummary.Unknown -> "$count item$plural"
    }
}

// Danger (red) once the shown items have actually passed their date, not
// just "expiring soon" (amber) — matches the web dashboard's own
// expired/expiring-soon color convention (SummaryPanel.css). Driven by the
// items actually shown (next), not the expiring_soon count, which can — and
// in production does — refer to a different item entirely than whatever's
// currently the most urgent.
fun emphasisFor(summary: ServiceSummary): LineEmphasis = when (summary) {
    is ServiceSummary.CaduTrack -> when {
        caduTrackItemsExpired(summary.data) -> LineEmphasis.Danger
        summary.data.next.isNotEmpty() -> LineEmphasis.Warning
        else -> LineEmphasis.Normal
    }
    is ServiceSummary.Error -> LineEmphasis.Danger
    else -> LineEmphasis.Normal
}

// Joins item names with ", ", truncated so it never cuts a name in half —
// stops at the last name that fully fits within maxChars (reserving room for
// the "+N more" suffix as it goes, since that reservation shrinks as fewer
// items remain) and appends "+N more" for whatever didn't fit. maxChars is
// an estimate from the widget's actual placed size (see
// CombinedSummaryWidgetRemoteViews.estimateCharBudget) — there's no way to
// ask a RemoteViews TextView how much text it actually rendered, so this
// stays approximate by design, deliberately conservative rather than
// risking a mid-word cut.
fun joinTruncated(items: List<String>, maxChars: Int): String {
    if (items.isEmpty()) return ""
    val sb = StringBuilder()
    var shown = 0
    for (i in items.indices) {
        val separator = if (i == 0) "" else ", "
        val remainingAfterThis = items.size - (i + 1)
        val suffixReserve = if (remainingAfterThis > 0) " +$remainingAfterThis more".length else 0
        val candidateLength = sb.length + separator.length + items[i].length + suffixReserve
        if (candidateLength > maxChars) break
        sb.append(separator).append(items[i])
        shown++
    }
    // Not even the first name fits — an extremely narrow placement. A bare
    // character truncation here would silently look like a complete (if
    // odd) word, so mark it explicitly cut off instead.
    if (shown == 0) return items[0].take((maxChars - 1).coerceAtLeast(1)) + "…"
    val remaining = items.size - shown
    return if (remaining > 0) "$sb +$remaining more" else sb.toString()
}
