package io.github.l2hyunwoo.ait

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*

public class NavigationBarApi internal constructor(private val client: AppsInTossClient) {
    /** 넘긴 옵션만 바꾼다. null 인자는 마지막에 적용한 값을 유지한다. [Capability.NAVIGATION_BAR]가 필요하다. */
    public suspend fun setOptions(
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

    /**
     * 내비게이션 바 오른쪽에 액세서리 버튼을 추가한다. 버튼은 한 번에 하나만 표시되며, 다시 부르면 기존 버튼을 바꾼다.
     * [accessibilityLabel]은 화면에 보이지 않고 스크린리더가 읽는 동작 설명이다(예: "찜하기").
     * [iconName]은 모노톤 아이콘 이름이다(예: "icon-heart-mono").
     * 클릭은 [accessoryButtonClicks]로 받는다. [Capability.NAVIGATION_BAR_ACCESSORY_BUTTON]이 필요하다.
     */
    public suspend fun addAccessoryButton(accessibilityLabel: String, iconName: String) {
        require(accessibilityLabel.isNotBlank()) { "accessibilityLabel must not be blank" }
        require(iconName.isNotBlank()) { "iconName must not be blank" }
        val payload = buildJsonObject {
            put("accessibilityLabel", accessibilityLabel)
            putJsonObject("icon") { put("name", iconName) }
        }
        client.call("navigationBarAccessoryAdd", payload)
    }

    /** [addAccessoryButton]으로 추가한 버튼을 제거한다. [Capability.NAVIGATION_BAR_ACCESSORY_BUTTON]이 필요하다. */
    public suspend fun removeAccessoryButton() {
        client.call("navigationBarAccessoryRemove")
    }

    /**
     * 액세서리 버튼을 누를 때마다 값을 하나 내보낸다. 수집하는 동안만 구독하고, 수집이 끝나면 구독을 해제한다.
     * 클릭을 놓치지 않도록 구독을 먼저 시작한 뒤 [addAccessoryButton]을 부른다.
     * 구독 오류는 [AppsInTossException]으로 끝난다. [Capability.NAVIGATION_BAR_ACCESSORY_BUTTON]이 필요하다.
     */
    public fun accessoryButtonClicks(): Flow<Unit> = client.stream("navigationBarAccessoryClicks").map { }

    private companion object {
        val COLOR = Regex("#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})")
    }
}

public sealed interface NavigationBarBackground {
    /** `#RRGGBB` 또는 `#RRGGBBAA`다. 배경이 투명한 동안에는 적용되지 않는다. */
    public data class Color(val hex: String) : NavigationBarBackground

    /** 테마의 기본 배경으로 되돌린다(공식 `null`). */
    public data object ThemeDefault : NavigationBarBackground
}

public enum class NavigationBarTheme { LIGHT, DARK }
