package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ShareApi internal constructor(private val client: AppsInTossClient) {
    /** 일반 텍스트를 공유한다. [Capability.SHARE]가 필요하다. */
    suspend fun sendMessage(message: String) {
        require(message.isNotBlank()) { "Share text cannot be blank" }
        client.call("share", buildJsonObject { put("message", message) })
    }

    /**
     * 토스 앱에서 [path](`intoss://` 딥링크)를 여는 링크를 만든다. [Capability.SHARE_LINK]가 필요하다.
     * 이전 버전 토스 앱은 [ogImageUrl]을 무시한다.
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
