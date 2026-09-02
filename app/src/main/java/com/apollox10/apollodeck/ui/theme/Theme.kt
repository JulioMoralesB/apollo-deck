package com.apollox10.apollodeck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

// Mirrors the web dashboard's palette (App.css custom properties) so the
// app feels like the same product, not a separate skin.
val DarkGray = Color(0xFF16181A)
val BlueishDarkGray = Color(0xFF1E2124)
val BorderColor = Color(0xFF2A2D30)
val BlueishWhite = Color(0xFFEEF0FA)
val ErrorRed = Color(0xFFFF5C5C)
val OnlineGreen = Color(0xFF3DDC84)

private val ApolloDeckColorScheme = darkColorScheme(
    background = DarkGray,
    surface = BlueishDarkGray,
    onBackground = BlueishWhite,
    onSurface = BlueishWhite,
    primary = BlueishWhite,
    onPrimary = DarkGray,
    error = ErrorRed,
    onError = DarkGray,
    outline = BorderColor,
)

val MonospaceTextStyle = TextStyle(fontFamily = FontFamily.Monospace)

@Composable
fun ApolloDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ApolloDeckColorScheme,
        content = content,
    )
}

// Shared text field styling — used anywhere the app collects text input
// (login, settings) so every form looks like the same product.
@Composable
fun apolloTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
    unfocusedBorderColor = BorderColor,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = MaterialTheme.colorScheme.onBackground,
)
