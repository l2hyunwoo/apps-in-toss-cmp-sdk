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
}

data class TossLoginAuthorization(val authorizationCode: String, val referrer: String)
