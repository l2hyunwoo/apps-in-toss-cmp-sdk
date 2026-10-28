package io.github.l2hyunwoo.ait

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*

/**
 * 인앱 결제다. 모든 지급은 앱 서버가 결정한다. `grant`에서 서버를 호출하고 그 응답을 반환한다.
 * `grant`가 false를 반환하거나 예외를 던지거나 끝나지 않으면(취소 또는 [AppsInTossClient.close]) 호스트는
 * 지급 실패로 기록한다. 이런 주문은 [getPendingOrders]와 [completeProductGrant]로 복구한다.
 */
class IapApi internal constructor(private val client: AppsInTossClient) {
    /** 단건 주문 페이지를 연다. 앱 서버가 주문을 지급하면 [grant]가 true를 반환한다. [Capability.IAP]가 필요하다. */
    suspend fun createOneTimePurchaseOrder(sku: String, grant: suspend (orderId: String) -> Boolean): PurchaseResult {
        require(sku.isNotBlank()) { "SKU cannot be blank" }
        return order("iapOneTimeOrder", buildJsonObject { put("sku", sku) }) { grant(it.string("orderId")) }
    }

    /** 구독 주문 페이지를 연다. [offerId]가 null이면 기본 가격을 쓴다. [Capability.IAP_SUBSCRIPTION]가 필요하다. */
    suspend fun createSubscriptionPurchaseOrder(
        sku: String,
        offerId: String? = null,
        grant: suspend (orderId: String, subscriptionId: String?) -> Boolean,
    ): PurchaseResult {
        require(sku.isNotBlank()) { "SKU cannot be blank" }
        require(offerId == null || offerId.isNotBlank()) { "Offer id cannot be blank" }
        val payload = buildJsonObject {
            put("sku", sku)
            put("offerId", offerId)
        }
        return order("iapSubscriptionOrder", payload) { grant(it.string("orderId"), it.optionalString("subscriptionId")) }
    }

    /** [Capability.IAP]가 필요하다. */
    suspend fun getProductItemList(): List<IapProduct> {
        val result = client.call("iapProducts")
        return decode("product list") {
            Json.parseToJsonElement(result).jsonObject.getValue("products").jsonArray.map { it.jsonObject.toProduct() }
        }
    }

    /** 결제는 됐지만 아직 지급 완료를 보고하지 않은 주문이다. [Capability.IAP]가 필요하다. */
    suspend fun getPendingOrders(): List<PendingOrder> {
        val result = client.call("iapPendingOrders")
        return decode("pending orders") {
            Json.parseToJsonElement(result).jsonObject.getValue("orders").jsonArray.map {
                val obj = it.jsonObject
                PendingOrder(obj.string("orderId"), obj.string("sku"), obj.string("paymentCompletedDate"))
            }
        }
    }

    /** 이전 페이지의 [CompletedOrRefundedOrders.nextKey]를 [key]로 넘긴다. null이면 첫 페이지를 읽는다. [Capability.IAP]가 필요하다. */
    suspend fun getCompletedOrRefundedOrders(key: String? = null): CompletedOrRefundedOrders {
        require(key == null || key.isNotBlank()) { "Page key cannot be blank" }
        val result = client.call("iapCompletedOrders", buildJsonObject { put("key", key) })
        return decode("completed orders") {
            val obj = Json.parseToJsonElement(result).jsonObject
            CompletedOrRefundedOrders(
                hasNext = obj.getValue("hasNext").jsonPrimitive.boolean,
                nextKey = obj.optionalString("nextKey"),
                orders = obj.getValue("orders").jsonArray.map {
                    val order = it.jsonObject
                    CompletedOrRefundedOrder(order.string("orderId"), order.string("sku"), OrderStatus.valueOf(order.string("status")), order.string("date"))
                },
            )
        }
    }

    /** 앱 서버가 [orderId]를 지급했음을 보고하고 호스트의 결과를 반환한다. [Capability.IAP]가 필요하다. */
    suspend fun completeProductGrant(orderId: String): Boolean {
        require(orderId.isNotBlank()) { "Order id cannot be blank" }
        val result = client.call("iapCompleteGrant", buildJsonObject { put("orderId", orderId) })
        return decode("grant completion") { Json.parseToJsonElement(result).jsonPrimitive.boolean }
    }

    /** [Capability.IAP_SUBSCRIPTION]가 필요하다. */
    suspend fun getSubscriptionInfo(orderId: String): SubscriptionInfo {
        require(orderId.isNotBlank()) { "Order id cannot be blank" }
        val result = client.call("iapSubscriptionInfo", buildJsonObject { put("orderId", orderId) })
        return decode("subscription info") {
            val obj = Json.parseToJsonElement(result).jsonObject.getValue("subscription").jsonObject
            SubscriptionInfo(
                catalogId = obj.getValue("catalogId").jsonPrimitive.long,
                status = obj.string("status"),
                expiresAt = obj.optionalString("expiresAt"),
                isAutoRenew = obj.getValue("isAutoRenew").jsonPrimitive.boolean,
                gracePeriodExpiresAt = obj.optionalString("gracePeriodExpiresAt"),
                isAccessible = obj.getValue("isAccessible").jsonPrimitive.boolean,
            )
        }
    }

    private suspend fun order(method: String, payload: JsonObject, grant: suspend (JsonObject) -> Boolean): PurchaseResult {
        val event = client.stream(method, payload) { JsonPrimitive(grant(it)) }.first()
        return decode("purchase order") {
            when (event.string("type")) {
                "success" -> PurchaseResult.Success(event.getValue("data").jsonObject.toOrder())
                "error" -> PurchaseResult.Failure(PurchaseError.valueOf(event.string("code")))
                else -> error("Unknown order event")
            }
        }
    }
}

private fun JsonObject.optionalString(key: String): String? = this[key]?.takeUnless { it is JsonNull }?.let { string(key) }

private fun JsonObject.toOrder() = PurchaseOrder(
    orderId = string("orderId"),
    displayName = string("displayName"),
    displayAmount = string("displayAmount"),
    amount = getValue("amount").jsonPrimitive.double,
    currency = string("currency"),
    fraction = getValue("fraction").jsonPrimitive.int,
    miniAppIconUrl = optionalString("miniAppIconUrl"),
)

private fun JsonObject.toProduct(): IapProduct {
    val type = IapProductType.valueOf(string("type"))
    return IapProduct(
        sku = string("sku"),
        type = type,
        displayName = string("displayName"),
        displayAmount = string("displayAmount"),
        iconUrl = string("iconUrl"),
        description = string("description"),
        renewalCycle = if (type == IapProductType.SUBSCRIPTION) RenewalCycle.valueOf(string("renewalCycle")) else null,
        offers = this["offers"]?.takeUnless { it is JsonNull }?.jsonArray?.map {
            val offer = it.jsonObject
            val offerType = IapOfferType.valueOf(offer.string("type"))
            IapOffer(offerType, offer.string("offerId"), offer.string("period"), if (offerType == IapOfferType.FREE_TRIAL) null else offer.string("displayAmount"))
        }.orEmpty(),
    )
}

sealed interface PurchaseResult {
    data class Success(val order: PurchaseOrder) : PurchaseResult
    data class Failure(val error: PurchaseError) : PurchaseResult
}

/** 문서화된 주문 오류 코드다. [PRODUCT_NOT_GRANTED_BY_PARTNER]는 지급이 false이거나 실패한 뒤에 온다. */
enum class PurchaseError {
    INVALID_PRODUCT_ID,
    PAYMENT_PENDING,
    NETWORK_ERROR,
    INVALID_USER_ENVIRONMENT,
    ITEM_ALREADY_OWNED,
    APP_MARKET_VERIFICATION_FAILED,
    TOSS_SERVER_VERIFICATION_FAILED,
    INTERNAL_ERROR,
    KOREAN_ACCOUNT_ONLY,
    USER_CANCELED,
    PRODUCT_NOT_GRANTED_BY_PARTNER,
}

data class PurchaseOrder(
    val orderId: String,
    val displayName: String,
    val displayAmount: String,
    val amount: Double,
    val currency: String,
    val fraction: Int,
    val miniAppIconUrl: String?,
)

enum class IapProductType { CONSUMABLE, NON_CONSUMABLE, SUBSCRIPTION }
enum class RenewalCycle { WEEKLY, MONTHLY, YEARLY }
enum class IapOfferType { FREE_TRIAL, NEW_SUBSCRIPTION, RETURNING }

/** [renewalCycle]은 [IapProductType.SUBSCRIPTION]에서만 값이 있다. */
data class IapProduct(
    val sku: String,
    val type: IapProductType,
    val displayName: String,
    val displayAmount: String,
    val iconUrl: String,
    val description: String,
    val renewalCycle: RenewalCycle?,
    val offers: List<IapOffer>,
)

/** [IapOfferType.FREE_TRIAL]에서는 [displayAmount]가 null이다. */
data class IapOffer(val type: IapOfferType, val offerId: String, val period: String, val displayAmount: String?)

data class PendingOrder(val orderId: String, val sku: String, val paymentCompletedDate: String)

enum class OrderStatus { COMPLETED, REFUNDED }
data class CompletedOrRefundedOrder(val orderId: String, val sku: String, val status: OrderStatus, val date: String)
data class CompletedOrRefundedOrders(val hasNext: Boolean, val nextKey: String?, val orders: List<CompletedOrRefundedOrder>)

/** 공식 타입이 문서에 없는 값도 허용하므로 [status]는 문자열로 둔다. */
data class SubscriptionInfo(
    val catalogId: Long,
    val status: String,
    val expiresAt: String?,
    val isAutoRenew: Boolean,
    val gracePeriodExpiresAt: String?,
    val isAccessible: Boolean,
)
