package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ClipboardApi internal constructor(private val client: AppsInTossClient) {
    /** 클립보드 텍스트를 읽는다. 클립보드가 비어 있으면 빈 문자열을 반환한다. [Capability.CLIPBOARD]가 필요하다. */
    suspend fun getText(): PermissionResult<String> {
        val result = client.call("clipboardGetText")
        return decode("clipboard") {
            Json.parseToJsonElement(result).jsonObject.permissionResult { it.jsonPrimitive.also { value -> require(value.isString) }.content }
        }
    }

    /** [Capability.CLIPBOARD]가 필요하다. */
    suspend fun setText(text: String): PermissionResult<Unit> {
        val result = client.call("clipboardSetText", buildJsonObject { put("text", text) })
        return decode("clipboard") {
            Json.parseToJsonElement(result).jsonObject.permissionResult { require(it is JsonNull) }
        }
    }
}
