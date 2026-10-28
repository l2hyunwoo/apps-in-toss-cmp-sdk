package io.github.l2hyunwoo.ait

/**
 * Host bridge contract. Payloads and results are JSON strings; each function returns a cleanup that
 * stops delivery and must be safe to call more than once.
 */
interface AppsInTossTransport {
    val host: RuntimeHost
    fun supports(capability: Capability): Boolean = host != RuntimeHost.BROWSER

    /** Runs a one-shot request. Exactly one of [success] or [failure] is called. */
    fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit

    /**
     * Opens an event stream. Events that carry a `replyId` are host questions; answer them with
     * `call("reply", {"replyId": ..., "value": ...})`.
     */
    fun stream(method: String, payload: String, onEvent: (String) -> Unit, failure: (String) -> Unit): () -> Unit
}
