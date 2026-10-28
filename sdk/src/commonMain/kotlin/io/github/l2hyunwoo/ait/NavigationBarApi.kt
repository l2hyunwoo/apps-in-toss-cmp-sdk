package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class NavigationBarApi internal constructor(private val client: AppsInTossClient) {
    /** Changes only the given options; a null argument keeps the last applied value. Requires [Capability.NAVIGATION_BAR]. */
    suspend fun setOptions(
        withBackButton: Boolean? = null,
        withHomeButton: Boolean? = null,
        withTitle: Boolean? = null,
        transparentBackground: Boolean? = null,
        backgroundColor: NavigationBarBackground? = null,
        theme: NavigationBarTheme? = null,
    ) {
        if (backgroundColor is NavigationBarBackground.Color) {
            require(COLOR.matches(backgroundColor.hex)) { "backgroundColor must be #RRGGBB or #RRGGBBAA" }
        }
        val payload = buildJsonObject {
            withBackButton?.let { put("withBackButton", it) }
            withHomeButton?.let { put("withHomeButton", it) }
            withTitle?.let { put("withTitle", it) }
            transparentBackground?.let { put("transparentBackground", it) }
            when (backgroundColor) {
                is NavigationBarBackground.Color -> put("backgroundColor", backgroundColor.hex)
                NavigationBarBackground.ThemeDefault -> put("backgroundColor", JsonNull)
                null -> Unit
            }
            theme?.let { put("theme", it.name.lowercase()) }
        }
        client.call("navigationBarOptions", payload)
    }

    private companion object {
        val COLOR = Regex("#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})")
    }
}

sealed interface NavigationBarBackground {
    /** `#RRGGBB` or `#RRGGBBAA`. Not applied while the background is transparent. */
    data class Color(val hex: String) : NavigationBarBackground

    /** Restores the theme's default background (official `null`). */
    data object ThemeDefault : NavigationBarBackground
}

enum class NavigationBarTheme { LIGHT, DARK }
