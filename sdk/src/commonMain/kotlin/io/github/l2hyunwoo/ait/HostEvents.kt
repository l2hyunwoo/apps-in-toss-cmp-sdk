package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive

data class SafeAreaInsets(val top: Double, val left: Double, val right: Double, val bottom: Double)
sealed interface HostEvent {
    data object Back : HostEvent
    data object Home : HostEvent
    data class SafeArea(val insets: SafeAreaInsets) : HostEvent
}

internal fun decodeEvent(obj: JsonObject): HostEvent {
    return when (obj.getValue("type").jsonPrimitive.content) {
        "back" -> HostEvent.Back

        "home" -> HostEvent.Home

        "safeArea" -> {
            fun inset(key: String): Double {
                val value = obj.getValue(key).jsonPrimitive
                require(!value.isString)
                return value.double.also { require(it.isFinite() && it >= 0) }
            }
            HostEvent.SafeArea(SafeAreaInsets(inset("top"), inset("left"), inset("right"), inset("bottom")))
        }

        else -> error("Unknown host event")
    }
}
