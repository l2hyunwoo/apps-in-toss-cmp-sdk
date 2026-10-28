package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ClipboardApi internal constructor(private val client: AppsInTossClient) {
    /** Reads clipboard text; an empty clipboard gives an empty string. Requires [Capability.CLIPBOARD]. */
    suspend fun getText(): ClipboardText {
        val result = client.call("clipboardGetText")
        return decode("clipboard") {
            val obj = Json.parseToJsonElement(result).jsonObject
            if (obj.isDenied()) ClipboardText.PermissionDenied else ClipboardText.Text(obj.string("text"))
        }
    }

    /** Requires [Capability.CLIPBOARD]. */
    suspend fun setText(text: String): ClipboardWriteResult {
        val result = client.call("clipboardSetText", buildJsonObject { put("text", text) })
        return decode("clipboard") {
            if (Json.parseToJsonElement(result).jsonObject.isDenied()) ClipboardWriteResult.PERMISSION_DENIED else ClipboardWriteResult.SUCCESS
        }
    }

    private fun JsonObject.isDenied(): Boolean = this["permission"]?.let { require(it.jsonPrimitive.content == "denied") } != null
}

sealed interface ClipboardText {
    data class Text(val text: String) : ClipboardText
    data object PermissionDenied : ClipboardText
}
enum class ClipboardWriteResult { SUCCESS, PERMISSION_DENIED }
