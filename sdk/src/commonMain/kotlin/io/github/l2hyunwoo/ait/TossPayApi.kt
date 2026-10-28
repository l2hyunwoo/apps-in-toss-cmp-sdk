package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

/** 토스페이 사용자 인증만 맡는다. 인증에 성공하면 앱 서버가 결제를 승인해야 한다. */
class TossPayApi internal constructor(private val client: AppsInTossClient) {
    /** [payToken]의 결제 시트를 연다. [Capability.TOSS_PAY]가 필요하다. */
    suspend fun authorize(payToken: String): TossPayAuthorization = request("tossPayAuthorize", "payToken", payToken)

    /** [wrappedToken]의 정기 결제 시트를 연다. [Capability.TOSS_PAY_SUBSCRIPTION]가 필요하다. */
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

/** [reason]은 인증 실패 이유를 설명한다. */
data class TossPayAuthorization(val success: Boolean, val reason: String?)
