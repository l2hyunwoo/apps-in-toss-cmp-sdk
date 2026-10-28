package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ScreenApi internal constructor(private val client: AppsInTossClient) {
    /** 미니앱 화면을 닫는다. [Capability.CLOSE]가 필요하다. */
    suspend fun close() {
        client.call("closeScreen")
    }

    /** 앱 전체에서 화면을 켜 둔다. 화면을 떠날 때 되돌린다. 적용된 상태를 반환한다. [Capability.SCREEN_AWAKE]가 필요하다. */
    suspend fun setAwakeMode(enabled: Boolean): Boolean = enabledResult("awake mode", client.call("screenAwakeMode", flag("enabled", enabled)))

    /** [enabled]이면 화면 캡처를 막는다. 적용된 상태를 반환한다. [Capability.SECURE_SCREEN]가 필요하다. */
    suspend fun setSecure(enabled: Boolean): Boolean = enabledResult("secure screen", client.call("screenSecure", flag("enabled", enabled)))

    /** iOS 스와이프 뒤로 가기 제스처를 켜거나 끈다. [Capability.IOS_SWIPE_BACK]가 필요하다. */
    suspend fun setIosSwipeBack(isEnabled: Boolean) {
        client.call("screenIosSwipeBack", flag("isEnabled", isEnabled))
    }

    /** 앱 전체의 화면 방향을 정한다. 화면을 떠날 때 되돌린다. [Capability.ORIENTATION]가 필요하다. */
    suspend fun setOrientation(type: DeviceOrientation) {
        client.call("screenOrientation", buildJsonObject { put("type", type.name.lowercase()) })
    }

    private fun flag(key: String, value: Boolean) = buildJsonObject { put(key, value) }

    private fun enabledResult(name: String, result: String): Boolean = decode(name) {
        Json.parseToJsonElement(result).jsonObject.getValue("enabled").jsonPrimitive.boolean
    }
}

enum class DeviceOrientation { PORTRAIT, LANDSCAPE }
