package io.github.l2hyunwoo.ait

internal expect fun bridgeHost(): String
internal expect fun bridgeSupports(capability: String): Boolean
internal expect fun bridgeCall(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
internal expect fun bridgeStream(method: String, payload: String, onEvent: (String) -> Unit, failure: (String) -> Unit): () -> Unit

fun createAppsInTossClient(): AppsInTossClient = AppsInTossClient(object : AppsInTossTransport {
    override val host get() = when (bridgeHost()) {
        "toss" -> RuntimeHost.APPS_IN_TOSS
        "sandbox" -> RuntimeHost.SANDBOX
        "mock" -> RuntimeHost.MOCK
        else -> RuntimeHost.BROWSER
    }
    override fun supports(capability: Capability) = bridgeSupports(capability.name)
    override fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit) = bridgeCall(method, payload, success, failure)
    override fun stream(method: String, payload: String, onEvent: (String) -> Unit, failure: (String) -> Unit) = bridgeStream(method, payload, onEvent, failure)
})
