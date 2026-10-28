package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class NavigationBarApi internal constructor(private val client: AppsInTossClient) {
    /** 넘긴 옵션만 바꾼다. null 인자는 마지막에 적용한 값을 유지한다. [Capability.NAVIGATION_BAR]가 필요하다. */
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
    /** `#RRGGBB` 또는 `#RRGGBBAA`다. 배경이 투명한 동안에는 적용되지 않는다. */
    data class Color(val hex: String) : NavigationBarBackground

    /** 테마의 기본 배경으로 되돌린다(공식 `null`). */
    data object ThemeDefault : NavigationBarBackground
}

enum class NavigationBarTheme { LIGHT, DARK }
