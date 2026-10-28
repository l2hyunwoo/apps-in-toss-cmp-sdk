package io.github.l2hyunwoo.ait

class ReviewApi internal constructor(private val client: AppsInTossClient) {
    /** Asks the user to review the mini app. Requires [Capability.REVIEW]. */
    suspend fun request() {
        client.call("reviewRequest")
    }
}
