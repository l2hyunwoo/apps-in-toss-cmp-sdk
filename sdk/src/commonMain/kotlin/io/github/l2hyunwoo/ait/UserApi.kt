package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class UserApi internal constructor(private val client: AppsInTossClient) {
    /** 익명 사용자 키 해시를 반환한다. [Capability.USER_ID]가 필요하다. */
    suspend fun getAnonymousKey(): String {
        val result = client.call("anonymousKey")
        return decode("user key") {
            val obj = Json.parseToJsonElement(result).jsonObject
            require(obj.string("type") == "HASH")
            obj.string("hash").also { require(it.isNotBlank()) }
        }
    }

    /** 서버가 익명 키로 교환할 일회용 코드를 발급한다. [Capability.USER_KEY_AUTH_CODE]가 필요하다. */
    suspend fun createAnonymousKeyAuthCode(): AnonymousKeyAuthCode {
        val result = client.call("anonymousKeyAuthCode")
        return decode("anonymous key auth code") {
            val obj = Json.parseToJsonElement(result).jsonObject
            AnonymousKeyAuthCode(obj.string("code").also { require(it.isNotBlank()) }, obj.getValue("expiresIn").jsonPrimitive.int)
        }
    }

    /**
     * [consentedUserDataKey]로 등록한 데이터 묶음을 요청하고, 동의가 필요하면 약관을 보여 준다.
     * [Capability.CONSENTED_DATA]가 필요하다.
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

    /** [ageGates]에 대한 iOS 신고 연령대를 읽는다. [Capability.DECLARED_AGE_RANGE]가 필요하다. */
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

/** [expiresIn]초가 지나기 전에 [code]를 서버로 보낸다. 한 번만 쓸 수 있다. */
data class AnonymousKeyAuthCode(val code: String, val expiresIn: Int)

sealed interface ConsentedDataResult {
    /** 키는 `USER_NAME` 같은 공식 `ConsentedUserDataKey` 이름이다. 값이 없는 항목은 빠진다. */
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
