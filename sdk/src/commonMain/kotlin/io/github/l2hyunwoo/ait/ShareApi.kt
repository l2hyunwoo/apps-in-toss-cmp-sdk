package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ShareApi internal constructor(private val client: AppsInTossClient) {
    /** Shares plain text. Requires [Capability.SHARE]. */
    suspend fun sendMessage(message: String) {
        require(message.isNotBlank()) { "Share text cannot be blank" }
        client.call("share", buildJsonObject { put("message", message) })
    }

    /**
     * Creates a link that opens [path] (an `intoss://` deep link) in the Toss app. Requires [Capability.SHARE_LINK].
     * Older Toss apps ignore [ogImageUrl].
     */
    suspend fun createLink(path: String, ogImageUrl: String? = null): String {
        require(path.startsWith("intoss://")) { "path must start with intoss://" }
        require(ogImageUrl == null || ogImageUrl.isNotBlank()) { "ogImageUrl cannot be blank" }
        val payload = buildJsonObject {
            put("path", path)
            ogImageUrl?.let { put("ogImageUrl", it) }
        }
        val result = client.call("shareLink", payload)
        return decode("share link") { Json.parseToJsonElement(result).jsonPrimitive.also { require(it.isString && it.content.isNotBlank()) }.content }
    }
}
