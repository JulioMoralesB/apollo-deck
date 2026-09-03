package com.apollox10.apollodeck.widget

import androidx.compose.ui.graphics.Color
import com.apollox10.apollodeck.R

// A curated palette rather than a free color picker — keeps the configure
// flow to a tap, and every option is already a background/foreground pair
// tuned for contrast, so nothing the user picks can end up illegible.
// drawableRes is the matching res/drawable/widget_bg_<id>.xml, used when
// pushing the widget's real RemoteViews (see WidgetRemoteViews.kt).
data class WidgetAccent(val id: String, val label: String, val background: Color, val text: Color, val drawableRes: Int)

val widgetAccents = listOf(
    WidgetAccent("default", "Default", Color(0xFF1E2124), Color(0xFFEEF0FA), R.drawable.widget_bg_default),
    WidgetAccent("blue", "Blue", Color(0xFF16233A), Color(0xFF5B9BFF), R.drawable.widget_bg_blue),
    WidgetAccent("green", "Green", Color(0xFF17331F), Color(0xFF3DDC84), R.drawable.widget_bg_green),
    WidgetAccent("purple", "Purple", Color(0xFF2A1E3A), Color(0xFFB388FF), R.drawable.widget_bg_purple),
    WidgetAccent("orange", "Orange", Color(0xFF3A2A16), Color(0xFFF5A623), R.drawable.widget_bg_orange),
    WidgetAccent("red", "Red", Color(0xFF3A1616), Color(0xFFFF5C5C), R.drawable.widget_bg_red),
    WidgetAccent("teal", "Teal", Color(0xFF163330), Color(0xFF4DD0C8), R.drawable.widget_bg_teal),
)

fun accentFor(id: String): WidgetAccent = widgetAccents.firstOrNull { it.id == id } ?: widgetAccents.first()
