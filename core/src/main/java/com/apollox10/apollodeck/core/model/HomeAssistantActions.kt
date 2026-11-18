package com.apollox10.apollodeck.core.model

// An Action's `endpoint` is the Home Assistant script's entity id
// (`script.add_song_to_n`): unique per script, which is what widget/tile
// configs and the watch grid's selection key actions by. ActionExecutor
// turns it into a script.turn_on call. Anything else is a config saved
// against the old apollo-server-dashboard endpoints and must be
// reconfigured.
fun isScriptEndpoint(endpoint: String?): Boolean = endpoint?.startsWith("script.") == true

// HA icons are Material Design Icons names (`mdi:lightbulb-on`). The app's
// icon dictionaries (app/ and wear/ IconMapping.kt) key by their own ids,
// so known equivalents are translated here and anything else passes through
// as the bare MDI name (e.g. "brightness-7"), which those dictionaries also
// recognize where it's worth mapping.
private val MDI_TO_APP_ICON = mapOf(
    "lightbulb-on" to "lightbulb",
    "lightbulb-outline" to "lightbulb",
    "music-note" to "music4",
    "music" to "music4",
    "gamepad-variant" to "gamepad",
    "gamepad-variant-outline" to "gamepad",
    "food-apple" to "hand-platter",
    "food-apple-outline" to "hand-platter",
    "home-assistant" to "home",
    "lightning-bolt" to "zap",
    "flash" to "zap",
)

fun haIconToAppIcon(haIcon: String?): String? {
    val name = haIcon?.removePrefix("mdi:")?.takeIf { it.isNotBlank() } ?: return null
    return MDI_TO_APP_ICON[name] ?: name
}
