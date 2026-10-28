package io.github.l2hyunwoo.ait

import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*

class AuthApi internal constructor(private val client: AppsInTossClient) {
    private val loginMutex = Mutex()

    /** [Capability.TOSS_LOGIN]가 필요하다. 다른 로그인이 진행 중이면 [ErrorCode.IN_PROGRESS]로 실패한다. */
    suspend fun login(): TossLoginAuthorization {
        client.checkAvailable()
        if (!loginMutex.tryLock()) throw AppsInTossException(ErrorCode.IN_PROGRESS, "A login is already in progress")
        try {
            val result = client.call("login")
            return decode("login") {
                val obj = Json.parseToJsonElement(result).jsonObject
                val code = obj.string("authorizationCode")
                val referrer = obj.string("referrer")
                require(code.isNotBlank() && referrer in setOf("DEFAULT", "SANDBOX"))
                TossLoginAuthorization(code, referrer)
            }
        } finally {
            loginMutex.unlock()
        }
    }

    /** 사용자가 토스 로그인을 연동했는지 반환한다. [Capability.TOSS_LOGIN_INTEGRATION]가 필요하다. */
    suspend fun isIntegrated(): Boolean {
        val result = client.call("isIntegrated")
        return decode("login integration") { Json.parseToJsonElement(result).jsonPrimitive.boolean }
    }

    /** 토스 인증서로 거래 [txId]에 서명한다. [Capability.TOSS_SIGN]가 필요하다. */
    suspend fun sign(txId: String) {
        require(txId.isNotBlank()) { "txId cannot be blank" }
        client.call("sign", buildJsonObject { put("txId", txId) })
    }
}

data class TossLoginAuthorization(val authorizationCode: String, val referrer: String)
