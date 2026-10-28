package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

/** Toss Pay user authorization only. The app's server must approve the payment after a successful authorization. */
class TossPayApi internal constructor(private val client: AppsInTossClient) {
    /** Opens the payment sheet for [payToken]. Requires [Capability.TOSS_PAY]. */
    suspend fun authorize(payToken: String): TossPayAuthorization = request("tossPayAuthorize", "payToken", payToken)

    /** Opens the recurring payment sheet for [wrappedToken]. Requires [Capability.TOSS_PAY_SUBSCRIPTION]. */
    suspend fun authorizeSubscription(wrappedToken: String): TossPayAuthorization = request("tossPayAuthorizeSubscription", "wrappedToken", wrappedToken)

    private suspend fun request(method: String, key: String, token: String): TossPayAuthorization {
        require(token.isNotBlank()) { "Token cannot be blank" }
        val result = client.call(method, buildJsonObject { put(key, token) })
        return decode("Toss Pay") {
            val obj = Json.parseToJsonElement(result).jsonObject
            TossPayAuthorization(obj.getValue("success").jsonPrimitive.boolean, obj["reason"]?.takeUnless { it is JsonNull }?.let { obj.string("reason") })
        }
    }
}

/** [reason] explains a failed authorization. */
data class TossPayAuthorization(val success: Boolean, val reason: String?)
