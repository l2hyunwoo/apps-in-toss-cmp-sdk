package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class DeviceApi internal constructor(private val client: AppsInTossClient) {
    /** [Capability.HAPTIC]가 필요하다. */
    suspend fun triggerHaptic(type: HapticType) {
        client.call("haptic", buildJsonObject { put("type", type.wire) })
    }

    /** `ko-KR` 같은 사용자 locale이다. [Capability.DEVICE_INFO]가 필요하다. */
    suspend fun locale(): String = value("locale")

    /** [Capability.DEVICE_INFO]가 필요하다. */
    suspend fun os(): DeviceOs {
        val os = value("os")
        return decode("device os") { DeviceOs.entries.first { it.wire == os } }
    }

    /** [url]을 기기의 기본 브라우저나 맞는 앱에서 연다. [Capability.OPEN_URL]가 필요하다. */
    suspend fun openURL(url: String) {
        require(url.isNotBlank()) { "URL cannot be blank" }
        client.call("openURL", buildJsonObject { put("url", url) })
    }

    private suspend fun value(name: String): String {
        val result = client.call("deviceValue", buildJsonObject { put("name", name) })
        return decode("device $name") { Json.parseToJsonElement(result).jsonPrimitive.also { require(it.isString) }.content }
    }
}

enum class DeviceOs(internal val wire: String) { IOS("ios"), ANDROID("android") }

enum class HapticType(internal val wire: String) {
    TICK_WEAK("tickWeak"),
    TAP("tap"),
    TICK_MEDIUM("tickMedium"),
    SOFT_MEDIUM("softMedium"),
    BASIC_WEAK("basicWeak"),
    BASIC_MEDIUM("basicMedium"),
    SUCCESS("success"),
    ERROR("error"),
    WIGGLE("wiggle"),
    CONFETTI("confetti"),
}
