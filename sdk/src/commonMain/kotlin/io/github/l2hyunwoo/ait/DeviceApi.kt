package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class DeviceApi internal constructor(private val client: AppsInTossClient) {
    /** Requires [Capability.HAPTIC]. */
    suspend fun triggerHaptic(type: HapticType) {
        client.call("haptic", buildJsonObject { put("type", type.wire) })
    }

    /** User locale such as `ko-KR`. Requires [Capability.DEVICE_INFO]. */
    suspend fun locale(): String = value("locale")

    /** Requires [Capability.DEVICE_INFO]. */
    suspend fun os(): DeviceOs {
        val os = value("os")
        return decode("device os") { DeviceOs.entries.first { it.wire == os } }
    }

    /** Opens [url] in the device's default browser or a matching app. Requires [Capability.OPEN_URL]. */
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
