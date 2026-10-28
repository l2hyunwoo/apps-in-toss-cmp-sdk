package io.github.l2hyunwoo.ait

import kotlin.js.JsName

@JsName("aitCmpBridge")
private external object Bridge {
    fun host(): String
    fun supports(capability: String): Boolean
    fun anonymousKey(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun login(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun share(text: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun closeScreen(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun subscribe(listener: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
}
internal actual fun bridgeHost(): String = try {
    Bridge.host()
} catch (_: Throwable) {
    "browser"
}
internal actual fun bridgeLogin(success: (String) -> Unit, failure: (String) -> Unit) = Bridge.login(success, failure)
internal actual fun bridgeShare(text: String, success: (String) -> Unit, failure: (String) -> Unit) = Bridge.share(text, success, failure)
internal actual fun bridgeClose(success: (String) -> Unit, failure: (String) -> Unit) = Bridge.closeScreen(success, failure)
internal actual fun bridgeSubscribe(listener: (String) -> Unit, failure: (String) -> Unit) = Bridge.subscribe(listener, failure)
internal actual fun bridgeCall(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit) = Bridge.call(method, payload, success, failure)

internal actual fun bridgeAnonymousKey(success: (String) -> Unit, failure: (String) -> Unit) = Bridge.anonymousKey(success, failure)

internal actual fun bridgeSupports(capability: String): Boolean = try {
    Bridge.supports(capability)
} catch (_: Throwable) {
    false
}
