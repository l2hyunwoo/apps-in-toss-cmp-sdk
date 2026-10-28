package io.github.l2hyunwoo.ait

import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.*

class AuthApi internal constructor(private val client: AppsInTossClient) {
    private val loginMutex = Mutex()

    /** Requires [Capability.TOSS_LOGIN]. Fails with [ErrorCode.IN_PROGRESS] while another login runs. */
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

    /** Returns whether the user linked Toss login. Requires [Capability.TOSS_LOGIN_INTEGRATION]. */
    suspend fun isIntegrated(): Boolean {
        val result = client.call("isIntegrated")
        return decode("login integration") { Json.parseToJsonElement(result).jsonPrimitive.boolean }
    }

    /** Signs the transaction [txId] with the Toss certificate. Requires [Capability.TOSS_SIGN]. */
    suspend fun sign(txId: String) {
        require(txId.isNotBlank()) { "txId cannot be blank" }
        client.call("sign", buildJsonObject { put("txId", txId) })
    }
}

data class TossLoginAuthorization(val authorizationCode: String, val referrer: String)
