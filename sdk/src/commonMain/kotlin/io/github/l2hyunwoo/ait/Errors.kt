package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

public enum class ErrorCode { UNSUPPORTED, PERMISSION_DENIED, USER_CANCELLED, TIMEOUT, HOST_UNAVAILABLE, INVALID_RESPONSE, SDK_FAILURE, IN_PROGRESS }
public class AppsInTossException(public val code: ErrorCode, message: String) : Exception(message)

internal fun decodeError(raw: String): AppsInTossException {
    val code = runCatching { Json.parseToJsonElement(raw).jsonObject["code"]?.jsonPrimitive?.content }.getOrNull()
    val known = ErrorCode.entries.firstOrNull { it.name == code } ?: ErrorCode.SDK_FAILURE
    return AppsInTossException(known, "Apps in Toss request failed ($known)")
}
