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

    /** Issues a one-time code that your server exchanges for the anonymous key. Requires [Capability.USER_KEY_AUTH_CODE]. */
    suspend fun createAnonymousKeyAuthCode(): AnonymousKeyAuthCode {
        val result = client.call("anonymousKeyAuthCode")
        return decode("anonymous key auth code") {
            val obj = Json.parseToJsonElement(result).jsonObject
            AnonymousKeyAuthCode(obj.string("code").also { require(it.isNotBlank()) }, obj.getValue("expiresIn").jsonPrimitive.int)
        }
    }

    /**
     * Requests the data bundle registered as [consentedUserDataKey] and shows the terms when consent is needed.
     * Requires [Capability.CONSENTED_DATA].
     */
    suspend fun getConsentedData(consentedUserDataKey: String, shouldRequestAgreementWhenUserDeclined: Boolean? = null): ConsentedDataResult {
        require(consentedUserDataKey.isNotBlank()) { "consentedUserDataKey cannot be blank" }
        val payload = buildJsonObject {
            put("consentedUserDataKey", consentedUserDataKey)
            shouldRequestAgreementWhenUserDeclined?.let { put("shouldRequestAgreementWhenUserDeclined", it) }
        }
        val result = client.call("consentedData", payload)
        return decode("consented data") {
            val obj = Json.parseToJsonElement(result).jsonObject
            val data = obj["data"]
            if (data != null) {
                ConsentedDataResult.Consented(data.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.also { require(it.isString) }.content })
            } else {
                ConsentedDataResult.Failed(ConsentedDataError.valueOf(obj.string("errorCode")))
            }
        }
    }

    /** Reads the iOS declared age range for [ageGates]. Requires [Capability.DECLARED_AGE_RANGE]. */
    suspend fun getDeclaredAgeRange(ageGates: List<Int>): DeclaredAgeRange {
        val result = client.call("declaredAgeRange", buildJsonObject { putJsonArray("ageGates") { ageGates.forEach { add(it) } } })
        return decode("declared age range") {
            val obj = Json.parseToJsonElement(result).jsonObject
            DeclaredAgeRange(
                status = DeclaredAgeRangeStatus.valueOf(obj.string("status")),
                lowerBound = obj.optional("lowerBound")?.int,
                upperBound = obj.optional("upperBound")?.int,
                ageRangeDeclaration = obj.optional("ageRangeDeclaration")?.also { require(it.isString) }?.content,
                activeParentalControls = obj.getValue("activeParentalControls").jsonArray.map { it.jsonPrimitive.also { p -> require(p.isString) }.content },
                isEligibleForAgeFeatures = obj.optional("isEligibleForAgeFeatures")?.boolean,
                errorCode = obj.optional("errorCode")?.also { require(it.isString) }?.content,
            )
        }
    }

    private fun JsonObject.optional(key: String): JsonPrimitive? = get(key)?.takeUnless { it is JsonNull }?.jsonPrimitive
}

/** Send [code] to your server before [expiresIn] seconds pass; it is single-use. */
data class AnonymousKeyAuthCode(val code: String, val expiresIn: Int)

sealed interface ConsentedDataResult {
    /** Keys are the official `ConsentedUserDataKey` names such as `USER_NAME`; items without a value are absent. */
    data class Consented(val data: Map<String, String>) : ConsentedDataResult
    data class Failed(val code: ConsentedDataError) : ConsentedDataResult
}

enum class ConsentedDataError {
    USER_DECLINED,
    UNAVAILABLE,
    TERMS_NOT_SET,
    INVALID_REQUEST,
    CANCELED,
    CONSENTED_USER_DATA_AGREEMENT_FAILED,
    CONSENTED_USER_DATA_INVALID_DATA,
}

data class DeclaredAgeRange(
    val status: DeclaredAgeRangeStatus,
    val lowerBound: Int?,
    val upperBound: Int?,
    val ageRangeDeclaration: String?,
    val activeParentalControls: List<String>,
    val isEligibleForAgeFeatures: Boolean?,
    val errorCode: String?,
)

enum class DeclaredAgeRangeStatus { SHARING, DECLINED_SHARING, UNSUPPORTED_OS, NOT_AVAILABLE, INVALID_REQUEST, FAILED }
