package io.github.l2hyunwoo.ait

interface AppsInTossTransport {
    val host: RuntimeHost
    fun supports(capability: Capability): Boolean = host != RuntimeHost.BROWSER
    fun anonymousKey(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun login(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun share(text: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun closeScreen(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun subscribe(listener: (String) -> Unit, failure: (String) -> Unit): () -> Unit

    /** Generic path for bridge methods added after the original transport contract. */
    fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit {
        failure("""{"code":"UNSUPPORTED"}""")
        return {}
    }
}
