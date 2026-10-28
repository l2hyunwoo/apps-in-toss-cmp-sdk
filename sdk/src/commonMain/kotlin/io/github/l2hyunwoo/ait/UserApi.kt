package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class UserApi internal constructor(private val client: AppsInTossClient) {
    /** Returns the anonymous user key hash. Requires [Capability.USER_ID]. */
    suspend fun getAnonymousKey(): String {
        val result = client.call("anonymousKey")
        return decode("user key") {
            val obj = Json.parseToJsonElement(result).jsonObject
            require(obj.string("type") == "HASH")
            obj.string("hash").also { require(it.isNotBlank()) }
        }
    }
}
