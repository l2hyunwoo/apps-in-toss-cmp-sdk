package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class DeviceApi internal constructor(private val client: AppsInTossClient) {
    /** Requires [Capability.HAPTIC]. */
    suspend fun triggerHaptic(type: HapticType) {
        client.call("haptic", buildJsonObject { put("type", type.wire) })
    }
}

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
