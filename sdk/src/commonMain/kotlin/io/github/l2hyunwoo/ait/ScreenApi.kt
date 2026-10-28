package io.github.l2hyunwoo.ait

class ScreenApi internal constructor(private val client: AppsInTossClient) {
    /** Closes the mini app screen. Requires [Capability.CLOSE]. */
    suspend fun close() {
        client.call("closeScreen")
    }
}
