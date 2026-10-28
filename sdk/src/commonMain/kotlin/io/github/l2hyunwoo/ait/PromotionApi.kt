package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class PromotionApi internal constructor(private val client: AppsInTossClient) {
    /** Grants Toss points with a console promotion code. Requires [Capability.PROMOTION_REWARD]. */
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

    /** [errorCode] is a documented promotion code such as `4100` or `4113`, or `UNKNOWN_ERROR`. */
    data class Failed(val errorCode: String) : PromotionRewardResult
}
