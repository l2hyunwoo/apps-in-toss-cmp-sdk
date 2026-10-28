package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ClipboardApi internal constructor(private val client: AppsInTossClient) {
    /** Reads clipboard text; an empty clipboard gives an empty string. Requires [Capability.CLIPBOARD]. */
    suspend fun getText(): PermissionResult<String> {
        val result = client.call("clipboardGetText")
        return decode("clipboard") {
            Json.parseToJsonElement(result).jsonObject.permissionResult { it.jsonPrimitive.also { value -> require(value.isString) }.content }
        }
    }

    /** Requires [Capability.CLIPBOARD]. */
    suspend fun setText(text: String): PermissionResult<Unit> {
        val result = client.call("clipboardSetText", buildJsonObject { put("text", text) })
        return decode("clipboard") {
            Json.parseToJsonElement(result).jsonObject.permissionResult { require(it is JsonNull) }
        }
    }
}
