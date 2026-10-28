package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ScreenApi internal constructor(private val client: AppsInTossClient) {
    /** Closes the mini app screen. Requires [Capability.CLOSE]. */
    suspend fun close() {
        client.call("closeScreen")
    }

    /** Keeps the screen on app-wide; restore it when leaving. Returns the applied state. Requires [Capability.SCREEN_AWAKE]. */
    suspend fun setAwakeMode(enabled: Boolean): Boolean = enabledResult("awake mode", client.call("screenAwakeMode", flag("enabled", enabled)))

    /** Blocks screen capture when [enabled]. Returns the applied state. Requires [Capability.SECURE_SCREEN]. */
    suspend fun setSecure(enabled: Boolean): Boolean = enabledResult("secure screen", client.call("screenSecure", flag("enabled", enabled)))

    /** Turns the iOS swipe-back gesture on or off. Requires [Capability.IOS_SWIPE_BACK]. */
    suspend fun setIosSwipeBack(isEnabled: Boolean) {
        client.call("screenIosSwipeBack", flag("isEnabled", isEnabled))
    }

    /** Sets the app-wide orientation; restore it when leaving. Requires [Capability.ORIENTATION]. */
    suspend fun setOrientation(type: DeviceOrientation) {
        client.call("screenOrientation", buildJsonObject { put("type", type.name.lowercase()) })
    }

    private fun flag(key: String, value: Boolean) = buildJsonObject { put(key, value) }

    private fun enabledResult(name: String, result: String): Boolean = decode(name) {
        Json.parseToJsonElement(result).jsonObject.getValue("enabled").jsonPrimitive.boolean
    }
}

enum class DeviceOrientation { PORTRAIT, LANDSCAPE }
