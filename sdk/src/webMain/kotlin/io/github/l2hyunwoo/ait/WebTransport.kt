package io.github.l2hyunwoo.ait

internal expect fun bridgeHost(): String
internal expect fun bridgeSupports(capability: String): Boolean
internal expect fun bridgeAnonymousKey(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
internal expect fun bridgeLogin(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
internal expect fun bridgeShare(text: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
internal expect fun bridgeClose(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
internal expect fun bridgeSubscribe(listener: (String) -> Unit, failure: (String) -> Unit): () -> Unit
internal expect fun bridgeCall(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit

fun createAppsInTossClient(): AppsInTossClient = AppsInTossClient(object : AppsInTossTransport {
    override val host get() = when (bridgeHost()) {
        "toss" -> RuntimeHost.APPS_IN_TOSS
        "sandbox" -> RuntimeHost.SANDBOX
        "mock" -> RuntimeHost.MOCK
        else -> RuntimeHost.BROWSER
    }
    override fun supports(capability: Capability) = bridgeSupports(capability.name)
    override fun anonymousKey(success: (String) -> Unit, failure: (String) -> Unit) = bridgeAnonymousKey(success, failure)
    override fun login(success: (String) -> Unit, failure: (String) -> Unit) = bridgeLogin(success, failure)
    override fun share(text: String, success: (String) -> Unit, failure: (String) -> Unit) = bridgeShare(text, success, failure)
    override fun closeScreen(success: (String) -> Unit, failure: (String) -> Unit) = bridgeClose(success, failure)
    override fun subscribe(listener: (String) -> Unit, failure: (String) -> Unit) = bridgeSubscribe(listener, failure)
    override fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit) = bridgeCall(method, payload, success, failure)
})
