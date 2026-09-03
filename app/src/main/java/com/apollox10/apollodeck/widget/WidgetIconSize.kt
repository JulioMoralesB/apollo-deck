package com.apollox10.apollodeck.widget

// A fixed set rather than a free slider — a widget cell is only ~40-70dp on
// most launchers, so anything outside this range either disappears or blows
// out the layout. Shared by the style picker, its preview, and the real
// widget render (see WidgetRemoteViews.kt) so all three stay in lockstep.
enum class WidgetIconSize(val dp: Int, val label: String) {
    Small(28, "Small"),
    Medium(40, "Medium"),
    Large(52, "Large"),
    ;

    companion object {
        fun fromDp(dp: Int): WidgetIconSize = entries.minByOrNull { kotlin.math.abs(it.dp - dp) } ?: Medium
    }
}

// With no label, the icon isn't sharing the widget with a line of text
// underneath — sized up further to use that freed space rather than
// leaving it empty.
fun WidgetIconSize.effectiveDp(showLabel: Boolean): Int = if (showLabel) dp else (dp * 1.4f).toInt()
