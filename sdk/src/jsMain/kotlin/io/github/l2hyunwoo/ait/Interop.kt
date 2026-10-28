package io.github.l2hyunwoo.ait

import kotlin.js.JsName

@JsName("aitCmpBridge")
private external object Bridge {
    fun host(): String
    fun supports(capability: String): Boolean
    fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun stream(method: String, payload: String, onEvent: (String) -> Unit, failure: (String) -> Unit): () -> Unit
}
internal actual fun bridgeHost(): String = try {
    Bridge.host()
} catch (_: Throwable) {
    "browser"
}

internal actual fun bridgeSupports(capability: String): Boolean = try {
    Bridge.supports(capability)
} catch (_: Throwable) {
    false
}

internal actual fun bridgeCall(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit) = Bridge.call(method, payload, success, failure)
internal actual fun bridgeStream(method: String, payload: String, onEvent: (String) -> Unit, failure: (String) -> Unit) = Bridge.stream(method, payload, onEvent, failure)
