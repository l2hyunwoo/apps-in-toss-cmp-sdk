package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ShareApi internal constructor(private val client: AppsInTossClient) {
    /** Shares plain text. Requires [Capability.SHARE]. */
    suspend fun sendMessage(message: String) {
        require(message.isNotBlank()) { "Share text cannot be blank" }
        client.call("share", buildJsonObject { put("message", message) })
    }
}
