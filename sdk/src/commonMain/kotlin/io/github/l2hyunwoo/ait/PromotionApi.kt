package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class PromotionApi internal constructor(private val client: AppsInTossClient) {
    /** 콘솔 프로모션 코드로 토스 포인트를 지급한다. [Capability.PROMOTION_REWARD]가 필요하다. */
    suspend fun grantReward(promotionCode: String, amount: Long): PromotionRewardResult {
        require(promotionCode.isNotBlank()) { "Promotion code cannot be blank" }
        val result = client.call(
            "promotionGrantReward",
            buildJsonObject {
                put("promotionCode", promotionCode)
                put("amount", amount)
            },
        )
        return decode("promotion reward") {
            val obj = Json.parseToJsonElement(result).jsonObject
            if ("key" in obj) {
                PromotionRewardResult.Granted(obj.string("key").also { require(it.isNotBlank()) })
            } else {
                PromotionRewardResult.Failed(obj.string("errorCode"))
            }
        }
    }
}

sealed interface PromotionRewardResult {
    data class Granted(val key: String) : PromotionRewardResult

    /** [errorCode]는 `4100`, `4113` 같은 문서화된 프로모션 코드이거나 `UNKNOWN_ERROR`다. */
    data class Failed(val errorCode: String) : PromotionRewardResult
}
